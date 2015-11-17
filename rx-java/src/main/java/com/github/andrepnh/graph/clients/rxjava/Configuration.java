package com.github.andrepnh.graph.clients.rxjava;

public class Configuration {

    private int batchSize;
    
    private int limit;
    
    private int ioThreadPoolMultiplier;
    
    private String graphServerHost;

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
