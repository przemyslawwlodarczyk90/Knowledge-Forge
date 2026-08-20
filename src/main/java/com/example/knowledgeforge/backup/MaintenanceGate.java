package com.example.knowledgeforge.backup;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Współdzielony koordynator między DatabaseBackupService, DatabaseRestoreService i
 * MaintenanceModeFilter:
 *  - {@code operationLock} — wzajemne wykluczenie backupu i restore (nigdy jednocześnie),
 *  - {@code maintenanceMode} — gdy true, MaintenanceModeFilter odrzuca nowe żądania
 *    modyfikujące dane (POST/PUT/PATCH/DELETE spoza /api/admin/*) odpowiedzią 503,
 *  - {@code activeWrites} — licznik żądań modyfikujących w locie; restore czeka (z timeoutem),
 *    aż spadnie do zera, zanim ruszy pg_restore,
 *  - {@code awaitingRestart} — trwały stan "restore się udał, ale pula połączeń nie została
 *    bezpiecznie odzyskana" — od tego momentu WSZYSTKIE żądania (także GET) dostają 503,
 *    aż proces zostanie ręcznie zrestartowany (zob. DatabaseRestoreService, BACKUP_AND_RESTORE.txt).
 */
public class MaintenanceGate {

    // MULTI-THREADING:
    // operationLock (ReentrantLock) — wzajemne wykluczenie DatabaseBackupService i
    // DatabaseRestoreService, żeby backup i restore nigdy nie ruszyły na tej samej bazie
    // jednocześnie (pg_dump czytający w trakcie gdy pg_restore nadpisuje dane byłby co
    // najmniej niespójny, a prawdopodobnie zakończyłby się błędem). tryLock() jest
    // NIEBLOKUJĄCE z rozmysłem — druga jednoczesna próba (np. dwa równoległe żądania restore)
    // ma dostać natychmiastową, jawną odmowę (409 z warstwy servletu), a nie czekać w kolejce
    // na operację, która i tak może trwać długo (pg_dump/pg_restore całej bazy). Zakres blokady
    // to WYŁĄCZNIE "czy backup/restore już trwa" — nie chroni activeWrites/maintenanceMode/
    // awaitingRestart (te są już same w sobie thread-safe, przez Atomic*), więc nigdy nie
    // zdobywamy dwóch locków naraz. Zwalniana jawnie przez releaseOperation() w finally
    // wywołującego (DatabaseBackupService/DatabaseRestoreService), zaraz po zakończeniu
    // pg_dump/pg_restore (sukces lub błąd — zawsze).
    private final ReentrantLock operationLock = new ReentrantLock();
    private final AtomicBoolean maintenanceMode = new AtomicBoolean(false);
    private final AtomicInteger activeWrites = new AtomicInteger(0);
    private final AtomicBoolean awaitingRestart = new AtomicBoolean(false);

    /** Próbuje wejść w tryb backupu/restore — nieblokujące; false = druga operacja już trwa. */
    public boolean tryAcquireOperation() {
        return operationLock.tryLock();
    }

    public void releaseOperation() {
        operationLock.unlock();
    }

    public void enterMaintenance() {
        maintenanceMode.set(true);
    }

    public void exitMaintenance() {
        if (!awaitingRestart.get()) {
            maintenanceMode.set(false);
        }
    }

    public boolean isMaintenance() {
        return maintenanceMode.get() || awaitingRestart.get();
    }

    public void markAwaitingRestart() {
        awaitingRestart.set(true);
        maintenanceMode.set(true);
    }

    public boolean isAwaitingRestart() {
        return awaitingRestart.get();
    }

    /** Wołane przez MaintenanceModeFilter dla żądań modyfikujących dane. false = odrzuć (503). */
    public boolean beginWrite() {
        if (isMaintenance()) return false;
        activeWrites.incrementAndGet();
        return true;
    }

    public void endWrite() {
        activeWrites.decrementAndGet();
    }

    /** Czeka aż wszystkie trwające żądania modyfikujące się zakończą, z timeoutem. */
    public boolean awaitDrain(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (activeWrites.get() > 0) {
            if (System.nanoTime() > deadline) return false;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }
}
