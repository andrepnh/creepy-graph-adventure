package com.github.andrepnh.graph.clients.rxjava;

import static com.google.common.base.Preconditions.*;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;
import java.io.IOException;
import java.util.concurrent.Executors;
import rx.Observable;

public class GraphAssembler {

    private static GraphAssembler assembler;

    private final Configuration config;

    private final AsyncHttpClient httpClient;

    public GraphAssembler(Configuration config) {
        this.config = checkNotNull(config);
        this.httpClient = new AsyncHttpClient(new AsyncHttpClientConfig.Builder()
            .build());
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        assembler = new GraphAssembler(new Configuration(1000, 1000, 8, "localhost:8080"));
        Runtime.getRuntime().addShutdownHook(new Thread(assembler::shutdown));
        assembler.run();
    }

    private Observable<Integer> getEdgesQuantity() {
        return Observable
            .from(httpClient.prepareGet(config.getEdgesQuantityUrl())
                .execute(new AsyncCompletionHandler<Integer>() {
                    @Override
                    public Integer onCompleted(Response response) throws Exception {
                        return Integer.parseInt(response.getResponseBody());
                    }
                }));
    }

    private void run() throws InterruptedException {
        try {
            long start, end;
            start = System.nanoTime();
            Graph.Builder builder = new Graph.Builder();
            Observable<Edge> edges = Edge.getEdges(httpClient, getEdgesQuantity(), config);
            edges.toBlocking().forEach(builder::edge);
            Graph graph = builder.build();
            end = System.nanoTime();
            System.out.println("Vertices: " + graph.getVertices());
            System.out.println("Edges: " + graph.getAdjacencies().size());
            System.out.println("Milliseconds taken: " + Math.round(((double) (end - start)) / 1000000));
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        httpClient.close();
    }
}
