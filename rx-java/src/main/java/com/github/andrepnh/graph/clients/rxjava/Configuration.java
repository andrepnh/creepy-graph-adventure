package com.github.andrepnh.graph.clients.rxjava;

public class Configuration {

    private final int batchSize;
    
    private final int limit;
    
    private final int ioThreadPoolMultiplier;
    
    private final String graphServerHost;

    public Configuration(int batchSize, int limit, int ioThreadPoolMultiplier, String graphServerHost) {
        this.batchSize = batchSize;
        this.limit = limit;
        this.ioThreadPoolMultiplier = ioThreadPoolMultiplier;
        this.graphServerHost = graphServerHost;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getLimit() {
        return limit;
    }

    public String getGraphServerHost() {
        return graphServerHost;
    }
    
    public String getEdgesQuantityUrl() {
        return String.format("http://%s/api/graph/edges-quantity", graphServerHost);
    }
    
    public String getGraphUrl(int offset) {
        return String.format("http://%s/api/graph?offset=%d&limit=%d", graphServerHost, offset, limit);
    }

    public int getIoThreadPoolMultiplier() {
        return ioThreadPoolMultiplier;
    }
    
}
