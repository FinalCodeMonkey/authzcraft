package com.fcm.authzcraft.api.service;

import com.fcm.authzcraft.api.attribute.AttributeSnapshot;
import com.fcm.authzcraft.api.attribute.AttributeSnapshotRequest;

public interface AttributeSnapshotProvider {

    AttributeSnapshot resolve(AttributeSnapshotRequest request);
}