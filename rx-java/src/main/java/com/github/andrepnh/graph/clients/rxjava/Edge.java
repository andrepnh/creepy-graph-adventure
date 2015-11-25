package com.github.andrepnh.graph.clients.rxjava;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import java.util.Collections;
import java.util.List;
import rx.Observable;
import rx.Scheduler;

public class Edge {
    
    @JsonProperty("i")
    private int source;

    @JsonProperty("j")
    private int target;

    private int weight;

    public static Observable<Edge> getEdgesBatch(
        int index, Configuration config, AsyncHttpClient httpClient, Scheduler scheduler) {
        return fireEdgesRequest(index, config, httpClient, scheduler)
            .flatMap(list -> Observable.from(list));
    }
    
    private static Observable<List<Edge>> fireEdgesRequest(
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
        // Hacking request retry with rx
        return Observable.from(future)
            .subscribeOn(scheduler)
            .flatMap(list -> (list.isEmpty())
                ? fireEdgesRequest(index, config, httpClient, scheduler) 
                : Observable.just(list));
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
