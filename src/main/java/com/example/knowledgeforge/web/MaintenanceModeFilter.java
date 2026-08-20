package com.example.knowledgeforge.web;

import com.example.knowledgeforge.backup.MaintenanceGate;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/**
 * Blokuje żądania modyfikujące dane (POST/PUT/PATCH/DELETE) na czas trwania pg_restore
 * (zob. DatabaseRestoreService) — bez tego restore mógłby działać na bazie aktywnie
 * modyfikowanej przez równoległe żądania z HikariCP. Trasy /api/admin/* są zawsze
 * przepuszczane — to właśnie POST /api/admin/backups/restore ustawia ten stan, więc nie
 * może sam siebie zablokować.
 *
 * Gdy aplikacja trwale przeszła w stan "po restore wymagany restart" (MaintenanceGate#isAwaitingRestart),
 * WSZYSTKIE żądania (także GET) dostają 503 — nie udajemy, że aplikacja normalnie działa na
 * niepewnym stanie połączeń.
 */
public class MaintenanceModeFilter implements Filter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final MaintenanceGate gate;

    public MaintenanceModeFilter(MaintenanceGate gate) {
        this.gate = gate;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (gate.isAwaitingRestart()) {
            respondUnavailable(resp, "Application requires a restart after a database restore. "
                    + "Please restart the process before retrying.");
            return;
        }

        String path = req.getRequestURI();
        boolean isAdminRoute = path != null && path.startsWith(req.getContextPath() + "/api/admin/");
        boolean isSafeMethod = SAFE_METHODS.contains(req.getMethod().toUpperCase(java.util.Locale.ROOT));

        if (isSafeMethod || isAdminRoute) {
            chain.doFilter(request, response);
            return;
        }

        if (!gate.beginWrite()) {
            respondUnavailable(resp, "Application is temporarily in maintenance mode "
                    + "(database restore in progress). Please retry shortly.");
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            gate.endWrite();
        }
    }

    private void respondUnavailable(HttpServletResponse resp, String message) throws IOException {
        resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        resp.setHeader("Retry-After", "30");
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"message\":\"" + message.replace("\"", "'") + "\"}");
    }
}
