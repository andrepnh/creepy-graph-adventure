package com.github.andrepnh.graph.server;

import ch.qos.logback.classic.AsyncAppender;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.hubspot.dropwizard.guice.GuiceBundle;
import io.dropwizard.Application;
import io.dropwizard.java8.Java8Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends Application<AppConfig> {
    
    private static final Logger LOG = LoggerFactory.getLogger(App.class);
    
    private static final int DEFAULT_VERTICES = 10000;
    
    private static final int DEFAULT_FILL_RATE = 1;
    
    public static final App INSTANCE = new App();
    
    private final Graph graph;

    public App() {
        int vertices = Integer.parseInt(System.getProperty("vertices", String.valueOf(DEFAULT_VERTICES))),
            fillRate = Integer.parseInt(System.getProperty("fillRate", String.valueOf(DEFAULT_FILL_RATE)));
        LOG.info("Building graph with {} vertices and fill rate of {}", vertices, fillRate);
        graph = Graph.random(vertices, fillRate);
        LOG.info("Graph successfully built. Total nodes: {}; total edges: {}",
            graph.getVertices(), graph.getAdjacencies().size());
    }
    
    public static void main(String[] args) throws Exception {
        INSTANCE.run(args);
    }

    public Graph getGraph() {
        return graph;
    }

    @Override
    public String getName() {
        return "graph-server";
    }

    @Override
    public void initialize(Bootstrap<AppConfig> bootstrap) {
        GuiceBundle<AppConfig> guiceBundle = GuiceBundle.<AppConfig>newBuilder()
            .addModule(new AppGuiceModule())
            .enableAutoConfig(getClass().getPackage().getName())
            .setConfigClass(AppConfig.class)
            .build();
        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new Java8Bundle());
    }

    @Override
    public void run(AppConfig appConfig, Environment env) throws Exception {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        AsyncAppender appender = (AsyncAppender) root.getAppender("async-console-appender");
        appender.setIncludeCallerData(true);
        env.getObjectMapper()
                .registerModule(new JSR310Module())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, true);
    }

}
