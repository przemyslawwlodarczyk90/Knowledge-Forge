package com.example.knowledgeforge.backup;

/**
 * Host/port/nazwa bazy wyciągnięte z db.url (jdbc:postgresql://host:port/database[?params]) —
 * pg_dump/pg_restore nie rozumieją URL-i JDBC, tylko dyskretne flagi -h/-p/-d (+ -U z osobna
 * z db.username, hasło z osobna przez PGPASSWORD — nigdy jako argument procesu).
 */
public record PgConnectionInfo(String host, int port, String database) {

    private static final String PREFIX = "jdbc:postgresql://";
    private static final int DEFAULT_PORT = 5432;

    public static PgConnectionInfo parse(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith(PREFIX)) {
            throw new IllegalStateException(
                    "Unsupported db.url format for backup/restore (expected jdbc:postgresql://host:port/db): " + jdbcUrl);
        }
        String rest = jdbcUrl.substring(PREFIX.length());
        int slash = rest.indexOf('/');
        if (slash < 0) {
            throw new IllegalStateException("db.url is missing a database name: " + jdbcUrl);
        }
        String hostPort = rest.substring(0, slash);
        String dbAndParams = rest.substring(slash + 1);
        int qm = dbAndParams.indexOf('?');
        String database = qm >= 0 ? dbAndParams.substring(0, qm) : dbAndParams;

        int colon = hostPort.indexOf(':');
        String host = colon >= 0 ? hostPort.substring(0, colon) : hostPort;
        int port = DEFAULT_PORT;
        if (colon >= 0) {
            try {
                port = Integer.parseInt(hostPort.substring(colon + 1));
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Invalid port in db.url: " + jdbcUrl, e);
            }
        }
        if (host.isBlank() || database.isBlank()) {
            throw new IllegalStateException("db.url is missing host or database name: " + jdbcUrl);
        }
        return new PgConnectionInfo(host, port, database);
    }
}
