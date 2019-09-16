package org.corfudb.benchmarks;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.codahale.metrics.MetricRegistry;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.corfudb.runtime.CorfuRuntime;
import org.corfudb.runtime.exceptions.unrecoverable.UnrecoverableCorfuInterruptedError;
import org.corfudb.util.MetricsUtils;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import java.util.concurrent.*;

/**
 * This class is the super class for all performance tests.
 * It set test parameters like how many clients to run, how many threads each client run,
 * and how many request one thread send.
 */
@Slf4j
public class BenchmarkTest {
    private static class Args {
        @Parameter(names = {"-h", "--help"}, description = "Help message", help = true)
        boolean help;

        @Parameter(names = {"--endpoint"}, description = "Cluster endpoint", required = true)
        String endpoint; //ip:portnum

        @Parameter(names = {"--num-clients"}, description = "Number of clients", required = true)
        int numClients;

        @Parameter(names = {"--num-threads"}, description = "Total number of threads", required = true)
        int numThreads;

        @Parameter(names = {"--num-requests"}, description = "Number of requests per thread", required = true)
        int numRequests;

        @Parameter(names = {"--ratio"}, description = "Ratio of put operations among all requests", required = true)
        double ratio;
    }

    /**
     * Number of clients
     */
    protected int numRuntimes = -1;
    /**
     * Number of threads per client.
     */
    protected int numThreads = -1;
    /**
     * Number of requests per thread.
     */
    protected int numRequests = -1;
    /**
     * Server endpoint.
     */
    protected String endpoint = null;

    protected double ratio = 1.0;
    protected CorfuRuntime[] rts = null;
    final BlockingQueue<Operation> operationQueue;
    final ExecutorService taskProducer;
    final ExecutorService workers;

    private final long durationMs = 1000;
    static final int APPLICATION_TIMEOUT_IN_MS = 10000000;
    static final int QUEUE_CAPACITY = 1000;

    BenchmarkTest(String[] args) {
        setArgs(args);
        rts = new CorfuRuntime[numRuntimes];

        for (int x = 0; x < rts.length; x++) {
            rts[x] = new CorfuRuntime(endpoint).connect();
        }
        log.info("Connected {} runtimes...", numRuntimes);

        operationQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        taskProducer = Executors.newSingleThreadExecutor();
        workers = Executors.newFixedThreadPool(numThreads);
        //MetricsUtils.metricsReportingSetup(CorfuRuntime.getDefaultMetrics());
        startPrometheusExporter(CorfuRuntime.getDefaultMetrics(), 1000);
    }

    private void startPrometheusExporter(@NonNull MetricRegistry registry, int metricsPort) {
        if (metricsPort == -1) {
            log.info("Metrics exporting via Prometheus is not enabled.");
            return;
        }

        Server server = new Server(metricsPort);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);

        try {
            server.start();
        } catch (Exception e) {
            log.error("Failed to start the metrics exporter.", e);
            return;
        }

        context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
        CollectorRegistry.defaultRegistry.register(new DropwizardExports(registry));
        log.info("Metrics exporting via Prometheus has been enabled at port {}.", metricsPort);
    }

    private void setArgs(String[] args) {
        Args cmdArgs = new Args();
        JCommander jc = JCommander.newBuilder()
                .addObject(cmdArgs)
                .build();
        jc.parse(args);

        if (cmdArgs.help) {
            jc.usage();
            System.exit(0);
        }
        numRuntimes = cmdArgs.numClients;
        numThreads = cmdArgs.numThreads;
        numRequests = cmdArgs.numRequests;
        endpoint = cmdArgs.endpoint;
        ratio = cmdArgs.ratio;
    }

    void runTaskProducer(Operation operation) {
            taskProducer.execute(() -> {
                try {
                    operationQueue.put(operation);
                } catch (InterruptedException e) {
                    throw new UnrecoverableCorfuInterruptedError(e);
                } catch (Exception e) {
                    log.error("operation error", e);
                }
            });
    }

    void runConsumers() {
        for (int i = 0; i < numThreads; i++) {
            workers.execute(() -> {
                try {
                    long start = System.nanoTime();
                    Operation op = operationQueue.take();
                    op.execute();
                    long latency = System.nanoTime() - start;
                    log.info("nextToken request latency: " + latency);
                } catch (Exception e) {
                    log.error("Operation failed with", e);
                }
            });
        }
    }

    void waitForAppToFinish() {
        workers.shutdown();
        try {
            boolean finishedInTime = workers.
                    awaitTermination(durationMs + APPLICATION_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);

            if (!finishedInTime) {
                log.error("not finished in time.");
                System.exit(1);
            }
        } catch (InterruptedException e) {
            log.error(String.valueOf(e));
            throw new UnrecoverableCorfuInterruptedError(e);
        } finally {
            System.exit(0);
        }
    }
}
