package com.fcm.authzcraft.pip.infrastructure.support;

import org.springframework.stereotype.Component;

import com.fcm.authzcraft.pip.domain.service.PrincipalIdGenerator;

@Component
public class SnowflakePrincipalIdGenerator implements PrincipalIdGenerator {

    private static final long EPOCH = 1704067200000L;
    private static final long WORKER_ID = 1L;
    private static final long WORKER_ID_SHIFT = 12L;
    private static final long TIMESTAMP_SHIFT = 22L;
    private static final long SEQUENCE_MASK = 4095L;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    @Override
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("System clock moved backwards");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0L) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT) | (WORKER_ID << WORKER_ID_SHIFT) | sequence;
    }

    private long waitNextMillis(long currentTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= currentTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
