package com.fcm.authzcraft.pap.infrastructure.support;

import org.springframework.stereotype.Component;

import com.fcm.authzcraft.pap.domain.service.CatalogIdGenerator;

@Component
public class SnowflakeCatalogIdGenerator implements CatalogIdGenerator {

    private static final long EPOCH = 1704067200000L;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    @Override
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 4095L;
            if (sequence == 0L) {
                while (timestamp <= lastTimestamp) {
                    timestamp = System.currentTimeMillis();
                }
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << 22) | sequence;
    }
}