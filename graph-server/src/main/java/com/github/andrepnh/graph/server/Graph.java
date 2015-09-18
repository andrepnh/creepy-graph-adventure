
package com.github.andrepnh.graph.server;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.javatuples.Triplet;

public class Graph {
    
    private final int vertices;
    
    private final ImmutableList<Triplet<Integer, Integer, Integer>> adjacencies;

    private Graph(Builder builder) {
        this.vertices = builder.vertices;
        this.adjacencies = builder.adjacencies.entrySet()
            .parallelStream()
            .flatMap(entry -> {
                int source = entry.getKey();
                return entry.getValue().entrySet().stream()
                    .map(nestedEntry -> Triplet.with(
                        source, nestedEntry.getKey(), nestedEntry.getValue()));
            }).collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }
    
    public static Graph random(int vertices, int desiredFillRate) {
        checkArgument(0 < desiredFillRate && desiredFillRate <= 100,
            "0 < fillRate <= 100, got %s",
            desiredFillRate);
        final int lowerRate = Math.max(1, (int) Math.floor(desiredFillRate * 0.75)), 
            higherRate = (int) Math.ceil(desiredFillRate * 1.25);
        final Random random = new Random();
        final Builder builder = new Builder(vertices);                          
        IntStream.range(0, vertices)
            .parallel()
            .forEach(source -> {
                int neighbours = getNeighbours(vertices, 
                    () -> random.nextInt(higherRate - lowerRate) + lowerRate);
                for (int i = 0; i < neighbours; i++) {
                    int destination = safeRandom(() -> random.nextInt(vertices), source);
                    builder.edge(source, destination, Math.abs(random.nextInt()));
                }
            });
        return builder.build();
    }

    public int getVertices() {
        return vertices;
    }

    public ImmutableList<Triplet<Integer, Integer, Integer>> getAdjacencies() {
        return adjacencies;
    }

    private static int getNeighbours(int vertices, IntSupplier fillRateSupplier) {
        double fillRate = ((double) fillRateSupplier.getAsInt()) / 100;
        return (int) Math.ceil(fillRate * (vertices - 1));
    }
    
    private static int safeRandom(IntSupplier supplier, int exclusion) {
        int next = supplier.getAsInt();
        if (next != exclusion) {
            return next;
        }
        return safeRandom(supplier, exclusion);
    }
    
    public static class Builder {
        
        private final int vertices;
        
        private final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> adjacencies
                = new ConcurrentHashMap<>(
                        100000, 0.75F, Runtime.getRuntime().availableProcessors());

        public Builder(int vertices) {
            checkArgument(vertices > 0, 
                    "Must have at least one vertex, got %s", 
                    vertices);
            this.vertices = vertices;
        }
        
        public Builder edge(int source, int destination, int weight) {
            checkBoundaries(source);
            checkBoundaries(destination);
            checkArgument(weight > 0, 
                    "Edge weight should be positive, got %s",
                    weight);
            adjacencies.putIfAbsent(source, new ConcurrentHashMap<>());
            adjacencies.get(source).put(destination, weight);
            return this;
        }

        private void checkBoundaries(int vertex) {
            checkArgument(vertex < vertices,
                    "Vertex (%s) outside graph boundaries: %s",
                    vertex, vertices);
        }
        
        public Graph build() {
            return new Graph(this);
        }
    }
    
}
