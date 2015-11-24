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

public class Edge {

    @JsonProperty("i")
    private int source;

    @JsonProperty("j")
    private int target;

    private int weight;

    public static Observable<Edge> getEdges(
        AsyncHttpClient httpClient, Observable<Integer> edgesQuantity, Configuration config) {
        
        return edgesQuantity.single()
            .map(quantity -> {
                int div = quantity / config.getBatchSize(), mod = quantity % config.getBatchSize();
                int batches = div + ((mod > 0) ? 1 : 0);
                return batches;
            }).flatMap(batches -> Observable.range(0, batches))
            .flatMap(batch -> fetchSingleEdgesBatch(batch, config, httpClient));
    }

    private static Observable<Edge> fetchSingleEdgesBatch(
        int index, Configuration config, AsyncHttpClient httpClient) {
        return fetchSingleEdgesBatchHelper(index, config, httpClient)
            .flatMap(list -> Observable.from(list));
    }
    
    private static Observable<List<Edge>> fetchSingleEdgesBatchHelper(
        int index, Configuration config, AsyncHttpClient httpClient) {
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
            .switchMap(list -> list.isEmpty()
                ? fetchSingleEdgesBatchHelper(index, config, httpClient) 
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
