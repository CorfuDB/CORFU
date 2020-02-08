package org.corfudb.logreplication.fsm;

import org.corfudb.logreplication.transmitter.DataMessage;
import org.corfudb.logreplication.transmitter.SnapshotReadMessage;
import org.corfudb.logreplication.transmitter.SnapshotReader;
import org.corfudb.runtime.CorfuRuntime;

import java.util.ArrayList;
import java.util.List;

/**
 * Dummy implementation of snapshot reader for testing purposes.
 *
 * This reader attempts to access n entries in the log in a continuous address space and
 * wraps the payload in the DataMessage.
 */
public class TestSnapshotReader implements SnapshotReader {

    private TestTransmitterConfig config;

    private int globalIndex = 0;

    public TestSnapshotReader(TestTransmitterConfig config) {
        this.config = config;
    }

    @Override
    public SnapshotReadMessage read() {
        // Connect to endpoint
        CorfuRuntime runtime = new CorfuRuntime(config.getEndpoint()).connect();
        List<DataMessage> messages = new ArrayList<>();

        int index = globalIndex;

        // Read numEntries in consecutive address space and add to messages to return
        for (int i=index; (i<index+config.getBatchSize() && index<config.getNumEntries()) ; i++) {
            Object data = runtime.getAddressSpaceView().read((long)i).getPayload(runtime);
            messages.add(new DataMessage((byte[])data));
            globalIndex++;
        }

        return new SnapshotReadMessage(messages, globalIndex == config.getNumEntries());
    }

    @Override
    public void reset(long snapshotTimestamp) {
        globalIndex = 0;
    }

    public void setBatchSize(int batchSize) {
        config.setBatchSize(batchSize);
    }
}