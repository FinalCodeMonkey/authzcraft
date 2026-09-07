package com.fcm.authzcraft.pdp.infrastructure.support;

import org.springframework.stereotype.Component;

import com.fcm.authzcraft.pdp.domain.service.DecisionIdGenerator;

@Component
public class SnowflakeDecisionIdGenerator implements DecisionIdGenerator {
    private static final long EPOCH = 1704067200000L;
    private static final long WORKER_ID = 23L;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    @Override
    public synchronized Long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            timestamp = lastTimestamp;
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 4095L;
            if (sequence == 0L) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << 22) | (WORKER_ID << 12) | sequence;
    }

    private long waitNextMillis(long currentTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= currentTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}