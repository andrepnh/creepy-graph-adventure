package com.github.andrepnh.java8;

import com.fasterxml.jackson.databind.ObjectMapper;
import static com.google.common.base.Preconditions.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ForkJoinPool;
import org.apache.http.HttpResponse;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import rx.Observable;

public class GraphAssembler {

    private static final String CONFIG_FILE_VARIABLE = "CONFIG_FILE";
    
    private static GraphAssembler assembler;
    
    private final Configuration config;
    
    private final ObjectMapper jsonMapper;
    
    private final CloseableHttpClient httpClient;
    
    private final ForkJoinPool ioPool;

    public GraphAssembler(File configFile) {
        checkArgument(checkNotNull(configFile).canRead());
        jsonMapper = new ObjectMapper();
        try {
            this.config = jsonMapper.readValue(configFile, Configuration.class);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        this.httpClient = newHttpClient();
        int ioThreads = Runtime.getRuntime().availableProcessors() * config.getIoThreadPoolMultiplier();
        this.ioPool = new ForkJoinPool(ioThreads - 1);
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        assembler = new GraphAssembler(new File(System.getenv(CONFIG_FILE_VARIABLE)));
        Runtime.getRuntime().addShutdownHook(new Thread(assembler::shutdown));
        assembler.run();
    }
    
    private static CloseableHttpClient newHttpClient() {
        PoolingHttpClientConnectionManager cnmngr = new PoolingHttpClientConnectionManager();
        cnmngr.setDefaultMaxPerRoute(200);
        return HttpClientBuilder.create()
            .setConnectionManager(cnmngr)
            .setRetryHandler(new DefaultHttpRequestRetryHandler(Integer.MAX_VALUE, false))
            .setServiceUnavailableRetryStrategy(new ServiceUnavailableRetryStrategy() {
                @Override public boolean retryRequest(
                    HttpResponse response, int executionCount, HttpContext context) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    return statusCode >= 500;
                }

                @Override public long getRetryInterval() {
                    return 0;
                }
            }).build();
    }
    
    private int getEdgesQuanty() {
        HttpGet getEdgesQuanty = new HttpGet(config.getEdgesQuantyUrl());
        try (CloseableHttpResponse response = httpClient.execute(getEdgesQuanty)) {
            checkArgument(response.getStatusLine().getStatusCode() == 200,
                "bad status %s",
                response.getStatusLine().getStatusCode());
            return Integer.parseInt(EntityUtils
                .toString(response.getEntity(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void run() throws InterruptedException {
        long start, end;
        start = System.nanoTime();
        Graph.Builder builder = new Graph.Builder();
        Observable<Page> pages = Page.getPages(ioPool, httpClient, getEdgesQuanty(), config);
        pages.subscribe(page -> {
            page.getEdges().forEach(builder::edge);
        });
        Graph graph = builder.build();
        end = System.nanoTime();
        System.out.println("Vertices: " + graph.getVertices());
        System.out.println("Edges: " + graph.getAdjacencies().size());
        System.out.println("Milliseconds taken: " + Math.round(((double) (end - start)) / 1000000));
    }
    
    private void shutdown() {
        ioPool.shutdownNow();
        try {
            httpClient.close();
        } catch (IOException ex) {
            // NOOP
        }
    }
    
}
