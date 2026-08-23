package com.example.knowledgeforge.service;

import com.example.knowledgeforge.dao.AttachmentDao;
import com.example.knowledgeforge.storage.AttachmentStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Diagnostyka spójności baza <-> dysk dla załączników. WYŁĄCZNIE raportuje — nigdy nie
 * usuwa rekordów ani plików. Uruchamiana raz przy starcie (Main.java, tylko log) i dostępna
 * do ręcznego wywołania (np. z testu albo narzędzia administracyjnego).
 *
 * Po przywróceniu starszego backupu bazy mogą się pojawić pliki na dysku, których rekordów
 * już nie ma w przywróconej bazie (backup NIE obejmuje attachments.storage.path) — to
 * oczekiwane i opisane w dokumentacja/BACKUP_AND_RESTORE.txt, nie błąd tej diagnostyki.
 */
public class AttachmentDiagnosticsService {

    private static final Logger log = Logger.getLogger(AttachmentDiagnosticsService.class.getName());

    private final AttachmentDao attachmentDao;
    private final AttachmentStorage storage;

    public AttachmentDiagnosticsService(AttachmentDao attachmentDao, AttachmentStorage storage) {
        this.attachmentDao = attachmentDao;
        this.storage = storage;
    }

    public record Report(List<String> recordsWithMissingFile, List<String> filesWithoutRecord) {
        public boolean isClean() {
            return recordsWithMissingFile.isEmpty() && filesWithoutRecord.isEmpty();
        }
    }

    public Report run() {
        List<String> dbPaths = attachmentDao.findAllRelativePaths();

        List<String> missing = new ArrayList<>();
        for (String relativePath : dbPaths) {
            if (!storage.exists(relativePath)) {
                missing.add(relativePath);
            }
        }

        Set<String> dbPathSet = new HashSet<>(dbPaths);
        List<String> orphans = new ArrayList<>();
        Path base = storage.baseDir();
        try (Stream<Path> walk = Files.walk(base)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().endsWith(".part"))
                    .forEach(p -> {
                        String rel = base.relativize(p).toString().replace('\\', '/');
                        if (!dbPathSet.contains(rel)) orphans.add(rel);
                    });
        } catch (IOException e) {
            log.warning(() -> "Attachment diagnostics: failed to walk storage directory: " + e.getMessage());
        }

        Report report = new Report(missing, orphans);
        if (report.isClean()) {
            log.info("Attachment diagnostics: OK — no missing files, no orphan files");
        } else {
            log.warning(() -> "Attachment diagnostics: " + missing.size() + " DB record(s) point to a missing file, "
                    + orphans.size() + " file(s) on disk have no DB record. This report is informational only — "
                    + "nothing was deleted. Can be expected after restoring an older database backup "
                    + "(backups never include attachments.storage.path) — see dokumentacja/BACKUP_AND_RESTORE.txt.");
        }
        return report;
    }
}
