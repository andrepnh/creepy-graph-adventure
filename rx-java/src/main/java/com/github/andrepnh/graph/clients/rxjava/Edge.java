package com.github.andrepnh.graph.clients.rxjava;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;
import rx.Observable;

public class Edge {

    @JsonProperty("i")
    private int source;

    @JsonProperty("j")
    private int target;

    private int weight;

    public static Observable<Edge> getEdges(
        ExecutorService ioPool,
        AsyncHttpClient httpClient,
        Observable<Integer> edgesQuantity,
        Configuration config) {
        
        Observable<Integer> batchAmount = edgesQuantity.single()
            .map(quantity -> (quantity / config.getBatchSize()) + 1);
        return batchAmount.flatMap(batches -> {
            ImmutableList<Observable<Edge>> allObservableEdges = ImmutableList.copyOf(
                IntStream.range(0, batches)
                    .mapToObj(batch -> fetchSingleEdgesBatch(batch, config, httpClient))
                    .iterator()
            );
            return Observable.from(allObservableEdges)
                .flatMap(observable -> observable);
        });
    }

    private static Observable<Edge> fetchSingleEdgesBatch(
        int index, Configuration config, AsyncHttpClient httpClient) {
        ListenableFuture<List<Edge>> future = httpClient
            .prepareGet(config.getGraphUrl(index * config.getBatchSize()))
            .execute(new AsyncCompletionHandler<List<Edge>>() {

                @Override
                public List<Edge> onCompleted(Response response) throws Exception {
                    String json = response.getResponseBody("UTF-8");
                    return config.getObjectMapper()
                    .readerFor(new TypeReference<List<Edge>>() {
                    })
                    .readValue(json);
                }

                @Override
                public AsyncHandler.STATE onStatusReceived(HttpResponseStatus status) throws Exception {
                    boolean retry = status.getStatusCode() == 502;
                    if (retry) {
                        throw new IOException();
                    }
                    return STATE.CONTINUE;
                }

            });
        return Observable.from(future)
            .flatMap(list -> Observable.from(list));
    }

    public int getSource() {
        return source;
    }

    public int getTarget() {
        return target;
    }

    public int getWeight() {
        return weight;
    }

}
