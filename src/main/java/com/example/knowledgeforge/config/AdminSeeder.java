package com.example.knowledgeforge.config;

import com.example.knowledgeforge.domain.user.Role;
import com.example.knowledgeforge.domain.user.User;
import com.example.knowledgeforge.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Przy starcie:
 *  1. zakłada konto administratora (app.admin.*), jeśli jeszcze nie istnieje — jak AdminSeeder z mastera,
 *  2. OPCJONALNIE (legacy.default-user.transfer-to) jednorazowo przenosi dane dawnego wspólnego konta
 *     "default" (wersja aplikacji bez logowania) na wskazane konto. Idempotentne: gdy "default" nie
 *     ma już żadnych danych, nic się nie dzieje. Wirtualny korzeń drzewa kategorii (root=true) jest
 *     scalany — każdy użytkownik ma dokładnie jeden.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = Logger.getLogger(AdminSeeder.class.getName());

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppConfig config;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, AppConfig config,
                       JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.config = config;
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin();
        transferLegacyDefaultUserData();
    }

    private void seedAdmin() {
        String username = config.adminUsername();
        if (userRepository.findByUsername(username).isPresent()) {
            log.info("Admin user '" + username + "' already exists — skipping seed");
            return;
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(config.adminEmail());
        admin.setPassword(passwordEncoder.encode(config.adminPassword()));
        admin.setActive(true);
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Admin user '" + username + "' created successfully");
    }

    private void transferLegacyDefaultUserData() {
        String targetName = config.legacyDefaultUserTransferTo();
        if (targetName == null || targetName.isBlank()) return;

        String legacyName = config.legacyDefaultUsername();
        Long legacyId = userRepository.findByUsername(legacyName).map(User::getId).orElse(null);
        Long targetId = userRepository.findByUsername(targetName).map(User::getId).orElse(null);
        if (legacyId == null) return;
        if (targetId == null) {
            log.warning("legacy.default-user.transfer-to='" + targetName + "' — no such user, nothing transferred");
            return;
        }
        if (legacyId.equals(targetId)) return;

        int[] moved = tx.execute(status -> {
            List<UUID> legacyRoots = jdbc.queryForList(
                    "SELECT id FROM category_node WHERE user_id = ? AND root = TRUE", UUID.class, legacyId);
            List<UUID> targetRoots = jdbc.queryForList(
                    "SELECT id FROM category_node WHERE user_id = ? AND root = TRUE", UUID.class, targetId);
            if (!targetRoots.isEmpty()) {
                UUID targetRoot = targetRoots.get(0);
                for (UUID legacyRoot : legacyRoots) {
                    jdbc.update("UPDATE category_node SET parent_id = ? WHERE parent_id = ?", targetRoot, legacyRoot);
                    jdbc.update("UPDATE topic SET category_id = ? WHERE category_id = ?", targetRoot, legacyRoot);
                    jdbc.update("DELETE FROM category_node WHERE id = ?", legacyRoot);
                }
            }
            int categories = jdbc.update("UPDATE category_node SET user_id = ? WHERE user_id = ?", targetId, legacyId);
            int topics = jdbc.update("UPDATE topic SET user_id = ? WHERE user_id = ?", targetId, legacyId);
            int notes = jdbc.update("UPDATE note SET user_id = ? WHERE user_id = ?", targetId, legacyId);
            return new int[]{categories, topics, notes};
        });
        if (moved != null && (moved[0] + moved[1] + moved[2]) > 0) {
            log.warning("Transferred legacy data of '" + legacyName + "' to '" + targetName + "': "
                    + moved[0] + " categories, " + moved[1] + " topics, " + moved[2] + " notes");
        }
    }
}
