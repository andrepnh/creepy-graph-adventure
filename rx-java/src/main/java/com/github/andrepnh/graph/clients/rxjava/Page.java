package com.github.andrepnh.graph.clients.rxjava;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import static com.google.common.base.Preconditions.checkArgument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import rx.Observable;

public class Page {

    private int index;
    
    private List<Edge> edges;
    
    public static Observable<Page> getPages(
            ExecutorService ioPool, 
            CloseableHttpClient httpClient, 
            int edgesQuantity, 
            Configuration config) {
        int batches = (edgesQuantity / config.getBatchSize()) + 1;
        return Observable.<Page>create(subscriber -> {
            IntStream.range(0, batches)
                .parallel()
                .forEach(index -> {
                    Future<Page> ftPage = ioPool.submit(() -> fetchPage(index, config, httpClient));
                    try {
                        subscriber.onNext(ftPage.get());
                    } catch (InterruptedException | ExecutionException ex) {
                        throw new IllegalStateException(ex);
                    }
                });
        });
    }
    
    private static Page fetchPage(int index, Configuration config, CloseableHttpClient httpClient) {
        HttpGet get = new HttpGet(config.getGraphUrl(index * config.getBatchSize()));
        try (CloseableHttpResponse response = httpClient.execute(get)) {
            checkArgument(response.getStatusLine().getStatusCode() == 200,
                "bad status %s",
                response.getStatusLine().getStatusCode());
            String json = EntityUtils
                .toString(response.getEntity(), StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            JavaType type = mapper.getTypeFactory().
                constructCollectionType(List.class, Page.Edge.class);
            List<Edge> edges = mapper.readValue(json, type);
            Page page = new Page();
            page.setEdges(edges);
            page.setIndex(index);
            return page;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }
    
    public static class Edge {
        
        @JsonProperty("i")
        private int source;
        
        @JsonProperty("j")
        private int target;
        
        private int weight;

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
}
