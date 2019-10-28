package org.corfudb.performancetest;

import lombok.extern.slf4j.Slf4j;
import org.corfudb.protocols.wireprotocol.Token;
import org.corfudb.protocols.wireprotocol.TxResolutionInfo;
import org.corfudb.runtime.CorfuRuntime;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Lin Dong on 10/18/19.
 */

@Slf4j
public class SequencerPerformanceTest extends PerformanceTest{
    private long seed;
    private int randomBoundary;
    private long time;
    private int milliToSecond;
    private CorfuRuntime runtime;
    private Random random;

    public SequencerPerformanceTest() {
        loadProperties();
        random = new Random(seed);
    }

    private void loadProperties() {
        seed = Long.parseLong(PROPERTIES.getProperty("sequencerSeed", "1024"));
        randomBoundary = Integer.parseInt(PROPERTIES.getProperty("sequencerRandomBoundary", "100"));
        time = Long.parseLong(PROPERTIES.getProperty("sequencerTime", "100"));
        milliToSecond = Integer.parseInt(PROPERTIES.getProperty("milliToSecond", "1000"));
    }

    private void tokenQuery(CorfuRuntime corfuRuntime, int numRequest) {
        for (int i = 0; i < numRequest; i++) {
            corfuRuntime.getSequencerView().query();
        }
    }

    private void tokenTx(CorfuRuntime corfuRuntime, int numRequest) {
        for (int i = 0; i < numRequest; i++) {
            UUID transactionID = UUID.nameUUIDFromBytes("transaction".getBytes());
            UUID stream = UUID.randomUUID();
            Map<UUID, Set<byte[]>> conflictMap = new HashMap<>();
            Set<byte[]> conflictSet = new HashSet<>();
            byte[] value = new byte[]{0, 0, 0, 1};
            conflictSet.add(value);
            conflictMap.put(stream, conflictSet);

            Map<UUID, Set<byte[]>> writeConflictParams = new HashMap<>();
            Set<byte[]> writeConflictSet = new HashSet<>();
            byte[] value1 = new byte[]{0, 0, 0, 1};
            writeConflictSet.add(value1);
            writeConflictParams.put(stream, writeConflictSet);

            TxResolutionInfo conflictInfo = new TxResolutionInfo(transactionID,
                    new Token(0, -1),
                    conflictMap,
                    writeConflictParams);
            corfuRuntime.getSequencerView().next(conflictInfo, stream);
        }
    }

    @Test
    public void sequencerPerformanceTest() throws IOException, InterruptedException {
        System.setProperty("corfu.local.metrics.collection", "true");
        System.setProperty("corfu.metrics.csv.interval", "1");
        String metricsPath = "/Users/lidong/metrics/sequencer";
        File logPath = new File(metricsPath);
        if (!logPath.exists()) {
            logPath.mkdir();
        }
        System.setProperty("corfu.metrics.csv.folder", metricsPath);
        Process server = runServer();
        runtime = initRuntime();
        long start = System.currentTimeMillis();
        while (true) {
            if ((System.currentTimeMillis() - start) / milliToSecond > time) {
                break;
            }
            //System.out.println("sequencer query=================");
            tokenQuery(runtime, random.nextInt(randomBoundary));
            tokenTx(runtime, random.nextInt(randomBoundary));
        }
        killServer();
    }
}
