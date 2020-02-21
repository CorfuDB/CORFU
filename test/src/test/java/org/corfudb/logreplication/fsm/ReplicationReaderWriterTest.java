package org.corfudb.logreplication.fsm;

import org.corfudb.logreplication.message.LogReplicationEntry;
import org.corfudb.logreplication.receive.LogEntryWriter;
import org.corfudb.logreplication.receive.StreamsSnapshotWriter;
import org.corfudb.logreplication.send.LogEntryReader;
import org.corfudb.logreplication.send.SnapshotReadMessage;
import org.corfudb.logreplication.send.StreamsLogEntryReader;
import org.corfudb.logreplication.send.StreamsSnapshotReader;
import org.corfudb.runtime.CorfuRuntime;
import org.corfudb.runtime.collections.CorfuTable;
import org.corfudb.runtime.view.AbstractViewTest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.corfudb.integration.ReplicationReaderWriterIT.generateTransactions;
import static org.corfudb.integration.ReplicationReaderWriterIT.openStreams;
import static org.corfudb.integration.ReplicationReaderWriterIT.printTails;
import static org.corfudb.integration.ReplicationReaderWriterIT.readLogEntryMsgs;
import static org.corfudb.integration.ReplicationReaderWriterIT.verifyData;
import static org.corfudb.integration.ReplicationReaderWriterIT.verifyNoData;
import static org.corfudb.integration.ReplicationReaderWriterIT.writeLogEntryMsgs;

public class ReplicationReaderWriterTest extends AbstractViewTest {
    static private final int START_VAL = 1;
    static private final int NUM_TRANS = 2;

    CorfuRuntime srcDataRuntime = null;
    CorfuRuntime dstDataRuntime = null;
    CorfuRuntime readerRuntime = null;
    CorfuRuntime writerRuntime = null;

    Random random = new Random();
    HashMap<String, CorfuTable<Long, Long>> srcTables = new HashMap<>();
    HashMap<String, CorfuTable<Long, Long>> dstTables = new HashMap<>();
    LogEntryReader logEntryReader;
    LogEntryWriter logEntryWriter;

    /*
     * the in-memory data for corfutables for verification.
     */
    HashMap<String, HashMap<Long, Long>> hashMap = new HashMap<String, HashMap<Long, Long>>();

    /*
     * store message generated by streamsnapshot reader and will play it at the writer side.
     */
    List<LogReplicationEntry> msgQ = new ArrayList<LogReplicationEntry>();

    public void setup() {
        srcDataRuntime = getDefaultRuntime().connect();
        srcDataRuntime = getNewRuntime(getDefaultNode()).setTransactionLogging(true).connect();
        dstDataRuntime = getNewRuntime(getDefaultNode()).setTransactionLogging(true).connect();
        readerRuntime = getNewRuntime(getDefaultNode()).setTransactionLogging(true).connect();
        writerRuntime = getNewRuntime(getDefaultNode()).setTransactionLogging(true).connect();

        LogReplicationConfig config = new LogReplicationConfig(hashMap.keySet(),UUID.randomUUID());
        logEntryReader = new StreamsLogEntryReader(readerRuntime, config);
        logEntryWriter = new LogEntryWriter(writerRuntime, config);
    }

    @Test
    public void testLogEntryReplication() {
        setup();

        openStreams(srcTables, srcDataRuntime);
        generateTransactions(srcTables, hashMap, NUM_TRANS, srcDataRuntime, START_VAL);
        printTails("after writing data to src tables", srcDataRuntime, dstDataRuntime);

        readLogEntryMsgs(msgQ, srcTables.keySet(), readerRuntime);

        writeLogEntryMsgs(msgQ, srcTables.keySet(), writerRuntime);
        printTails("after playing message at dst", srcDataRuntime, dstDataRuntime);
        openStreams(dstTables, dstDataRuntime);

        verifyData("after writing log entry at dst", dstTables, hashMap);
    }

    void readMsgs(List<LogReplicationEntry> msgQ, Set<String> streams, CorfuRuntime rt) {
        LogReplicationConfig config = new LogReplicationConfig(streams, UUID.randomUUID());
        StreamsSnapshotReader reader = new StreamsSnapshotReader(rt, config);

        reader.reset(rt.getAddressSpaceView().getLogTail());
        while (true) {
            SnapshotReadMessage snapshotReadMessage = reader.read(UUID.randomUUID());
            msgQ.addAll(snapshotReadMessage.getMessages());
            if (snapshotReadMessage.isEndRead()) {
                break;
            }
        }
    }

    void writeMsgs(List<LogReplicationEntry> msgQ, Set<String> streams, CorfuRuntime rt) {
        LogReplicationConfig config = new LogReplicationConfig(streams, UUID.randomUUID());
        StreamsSnapshotWriter writer = new StreamsSnapshotWriter(rt, config);

        writer.reset(msgQ.get(0).metadata.getSnapshotTimestamp());

        for (LogReplicationEntry msg : msgQ) {
            writer.apply(msg);
        }
    }

    @Test
    public void testSnapshotReplication() {
        setup();
        openStreams(srcTables, srcDataRuntime);

        generateTransactions(srcTables, hashMap, NUM_TRANS, srcDataRuntime, START_VAL);
        printTails("after writing data to src tables", srcDataRuntime, dstDataRuntime);

        readMsgs(msgQ, hashMap.keySet(), readerRuntime);

        //call clear table
        for (String name : srcTables.keySet()) {
            CorfuTable<Long, Long> table = srcTables.get(name);
            table.clear();
        }

        verifyNoData(srcTables);

        //clear all tables, play messages
        writeMsgs(msgQ, hashMap.keySet(), writerRuntime);

        openStreams(dstTables, dstDataRuntime);
        //verify data with hashtable
        verifyData("after writing log entry at dst", dstTables, hashMap);
    }
}