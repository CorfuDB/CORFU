package org.corfudb.runtime.view;

import org.corfudb.runtime.view.Layout.LayoutSegment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class LayoutUtil {
    private final UUID clusterId = UUID.randomUUID();

    public Layout getLayout(List<String> servers) {
        long epoch = 0;

        LayoutSegment segment = new LayoutSegment(
                Layout.ReplicationMode.CHAIN_REPLICATION,
                0L,
                -1L,
                Arrays.asList(new Layout.LayoutStripe(new ArrayList<>(servers)))
        );
        return new Layout(
                new ArrayList<>(servers),
                new ArrayList<>(servers),
                Arrays.asList(segment),
                epoch,
                clusterId
        );
    }
}
