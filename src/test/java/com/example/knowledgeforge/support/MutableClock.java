package com.example.knowledgeforge.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Clock, którego "teraz" testy mogą dowolnie przestawiać PO tym, jak został już wstrzyknięty do
 * serwisu (Clock samo w sobie jest niemutowalne — bez tej warstwy pośredniej trzeba by tworzyć
 * nowy serwis przy każdej zmianie czasu). Domyślnie realny zegar systemowy; testy czasu
 * (ActualityVerificationService, ActualityVerificationScheduler) podmieniają go na
 * Clock.fixed(...) przez {@link #set}, żeby sprawdzać reguły "dokładnie na granicy okresu" bez
 * prawdziwego oczekiwania.
 */
public final class MutableClock extends Clock {

    private volatile Clock delegate;

    public MutableClock(Clock initial) {
        this.delegate = initial;
    }

    public static MutableClock systemUTC() {
        return new MutableClock(Clock.systemUTC());
    }

    public void set(Instant instant, ZoneId zone) {
        this.delegate = Clock.fixed(instant, zone);
    }

    public void set(Instant instant) {
        set(instant, delegate.getZone());
    }

    @Override
    public ZoneId getZone() {
        return delegate.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return delegate.withZone(zone);
    }

    @Override
    public Instant instant() {
        return delegate.instant();
    }
}
