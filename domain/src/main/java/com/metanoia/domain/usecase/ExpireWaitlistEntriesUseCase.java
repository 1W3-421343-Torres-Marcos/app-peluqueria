package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.repository.WaitlistRepository;

import java.util.Objects;

/**
 * Marca automáticamente como {@code EXPIRED} todas las entradas abiertas cuya vigencia venció.
 */
public final class ExpireWaitlistEntriesUseCase {

    private final WaitlistRepository waitlistRepository;
    private final Clock clock;

    public ExpireWaitlistEntriesUseCase(WaitlistRepository waitlistRepository, Clock clock) {
        this.waitlistRepository = Objects.requireNonNull(
                waitlistRepository, "waitlistRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Result<Integer> execute() {
        int expired = waitlistRepository.expireBefore(clock.todayEpochDay());
        return Result.success(expired);
    }
}
