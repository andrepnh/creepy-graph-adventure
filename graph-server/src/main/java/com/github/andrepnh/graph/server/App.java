package com.github.andrepnh.graph.server;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.hubspot.dropwizard.guice.GuiceBundle;
import io.dropwizard.Application;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class App extends Application<AppConfig> {

    public static void main(String[] args) throws Exception {
        long before, after;
        before = System.nanoTime();
        Graph random = Graph.random(10000, 1);
        after = System.nanoTime();
        System.out.println((double) (after - before) / 1000000);
        System.out.println(random.getVertices());
        System.out.println(random.getAdjacencies().size());
//        random.getAdjacencies().forEach(System.out::println);
//        new App().run(args);
    }

    @Override
    public String getName() {
        return "graph-server";
    }

    @Override
    public void initialize(Bootstrap<AppConfig> bootstrap) {
        GuiceBundle<AppConfig> guiceBundle = GuiceBundle.<AppConfig>newBuilder()
                .enableAutoConfig(getClass().getPackage().getName())
                .setConfigClass(AppConfig.class)
                .build();
        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new Java8Bundle());
    }

    @Override
    public void run(AppConfig appConfig, Environment env) throws Exception {
        env.getObjectMapper()
                .registerModule(new JSR310Module())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, true);
    }

}
