package org.corfudb.infrastructure.management;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Sets;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

import lombok.NonNull;
import org.corfudb.protocols.wireprotocol.ClusterState;
import org.corfudb.runtime.view.Layout;

/**
 * Poll Report generated by the detectors that poll to detect failed or healed nodes.
 * This is consumed and analyzed by the Management Server to detect changes in the cluster and
 * take appropriate action.
 * Created by zlokhandwala on 3/21/17.
 */
@Data
@Builder
public class PollReport {

    @Default
    private long pollEpoch = Layout.INVALID_EPOCH;
    /**
     * Contains all successful connections. It doesn't contain nodes answered with WrongEpochException
     */
    @Default
    private final ImmutableSet<String> connectedNodes = ImmutableSet.of();
    @Default
    private final ImmutableSet<String> failedNodes = ImmutableSet.of();
    @Default
    private final ImmutableMap<String, Long> wrongEpochs = ImmutableMap.of();
    @Default
    private final boolean currentLayoutSlotUnFilled = false;
    @NonNull
    private final ClusterState clusterState;
}
