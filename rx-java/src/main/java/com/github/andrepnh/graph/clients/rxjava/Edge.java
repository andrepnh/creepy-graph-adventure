package com.github.andrepnh.graph.clients.rxjava;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public class Edge {
    
    @JsonProperty("i")
    private int source;

    @JsonProperty("j")
    private int target;

    private int weight;

    public static Observable<Edge> getEdges(
        AsyncHttpClient httpClient, Observable<Integer> edgesQuantity, Configuration config) {
        final Scheduler scheduler = Schedulers.from(Executors.newFixedThreadPool(config.getIoThreadPoolMultiplier() * Runtime.getRuntime().availableProcessors(), 
            runnable -> {
                Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                return thread;
            }));
        return edgesQuantity.single()
            .map(quantity -> {
                int div = quantity / config.getBatchSize(), mod = quantity % config.getBatchSize();
                int batches = div + ((mod > 0) ? 1 : 0);
                return batches;
            }).flatMap(batches -> Observable.range(0, batches))
            .flatMap(batch -> fetchSingleEdgesBatch(batch, config, httpClient, scheduler));
    }

    private static Observable<Edge> fetchSingleEdgesBatch(
        int index, Configuration config, AsyncHttpClient httpClient, Scheduler scheduler) {
        return fetchSingleEdgesBatchHelper(index, config, httpClient, scheduler)
            .flatMap(list -> Observable.from(list));
    }
    
    private static Observable<List<Edge>> fetchSingleEdgesBatchHelper(
        int index, Configuration config, AsyncHttpClient httpClient, Scheduler scheduler) {
        ListenableFuture<List<Edge>> future = httpClient
            .prepareGet(config.getGraphUrl(index * config.getBatchSize()))
            .execute(new AsyncCompletionHandler<List<Edge>>() {
                @Override
                public List<Edge> onCompleted(Response response) throws Exception {
                    if (response.getStatusCode() == 502) {
                        return Collections.emptyList();
                    }
                    String json = response.getResponseBody("UTF-8");
                    return config.getObjectMapper()
                        .readerFor(new TypeReference<List<Edge>>() {})
                        .readValue(json);
                }
            });
        return Observable.from(future)
            .subscribeOn(scheduler)
            .switchMap(list -> list.isEmpty()
                ? fetchSingleEdgesBatchHelper(index, config, httpClient, scheduler) 
                : Observable.from(Collections.singletonList(list)));
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
