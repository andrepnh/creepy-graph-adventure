package com.github.andrepnh.java8;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.javatuples.Triplet;

public class Graph {
    
    private final int vertices;
    
    private final ImmutableList<Triplet<Integer, Integer, Integer>> adjacencies;

    private Graph(Builder builder) {
        this.adjacencies = builder.adjacencies.entrySet()
            .parallelStream()
            .flatMap(entry -> {
                int source = entry.getKey();
                return entry.getValue().entrySet().stream()
                    .map(nestedEntry -> Triplet.with(
                        source, nestedEntry.getKey(), nestedEntry.getValue()));
            }).collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
        this.vertices = builder.adjacencies.size();
    }

    public int getVertices() {
        return vertices;
    }

    public ImmutableList<Triplet<Integer, Integer, Integer>> getAdjacencies() {
        return adjacencies;
    }
    
    public static class Builder {
        
        private final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> adjacencies
                = new ConcurrentHashMap<>(
                        100000, 0.75F, Runtime.getRuntime().availableProcessors());
        
        public Builder edge(Page.Edge edge) {
            checkNotNull(edge);
            adjacencies.putIfAbsent(edge.getSource(), new ConcurrentHashMap<>());
            adjacencies.get(edge.getSource()).put(edge.getTarget(), edge.getWeight());
            return this;
        }
        
        public Graph build() {
            return new Graph(this);
        }
    }
    
}
