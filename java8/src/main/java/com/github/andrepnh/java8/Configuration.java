package com.github.andrepnh.java8;

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
    
    public String getEdgesQuantyUrl() {
        return String.format("http://%s/api/graph/edges-quanty", graphServerHost);
    }
    
    public String getGraphUrl(int offset) {
        return String.format("http://%s/api/graph?offset=%d&limit=%d", graphServerHost, offset, limit);
    }

    public int getIoThreadPoolMultiplier() {
        return ioThreadPoolMultiplier;
    }
    
}
