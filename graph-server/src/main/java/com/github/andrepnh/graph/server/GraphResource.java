package com.github.andrepnh.graph.server;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import org.javatuples.Triplet;

/**
 *
 * @author André Pinheiro de Melo
 */

@Path("/graph")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GraphResource {
    
    private final Random random = new Random();

    @Path("/edges-quanty")
    @GET
    public int getSize() {
        return App.INSTANCE.getGraph().getAdjacencies().size();
    }
    
    @GET
    @Timed
    @ExceptionMetered
    public List<Adjancency> getAdjacencyList(
        @Min(0) @QueryParam("offset") int offset, @Min(1) @QueryParam("limit") int limit) 
        throws InterruptedException {
        boolean fail = random.nextInt(100) < 25;
        if (fail) {
            throw new WebApplicationException(502);
        }
        TimeUnit.MILLISECONDS.sleep(random.nextInt(300 - 200) + 200);
        return App.INSTANCE.getGraph().getAdjacencies()
            .stream()
            .skip(offset)
            .limit(limit)
            .map(Adjancency::new)
            .collect(Collectors.toList());
    }
    
    public static class Adjancency implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        private final int i;
        
        private final int j;
        
        private final int weight;

        public Adjancency(Triplet<Integer, Integer, Integer> triplet) {
            this.i = triplet.getValue0();
            this.j = triplet.getValue1();
            this.weight = triplet.getValue2();
        }

        public int getI() {
            return i;
        }

        public int getJ() {
            return j;
        }

        public int getWeight() {
            return weight;
        }
        
    }
    
}
