package com.learn.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GrpcClient {

    private final ManagedChannel channel;
    private final HelloServiceGrpc.HelloServiceBlockingStub blockingStub;

    public static void main(String[] args) {

        Map<String, Object> retryPolicy = new HashMap<>();
        retryPolicy.put("maxAttempts", 5D);
        retryPolicy.put("initialBackoff", "5s");
        retryPolicy.put("maxBackoff", "30s");
        retryPolicy.put("backoffMultiplier", 2D);
        retryPolicy.put("retryableStatusCodes", Arrays.<Object>asList("UNAVAILABLE", "UNAUTHENTICATED"));
        Map<String, Object> methodConfig = new HashMap<>();
        Map<String, Object> name = new HashMap<>();
        name.put("service", "com.learn.grpc.HelloService");
        methodConfig.put("name", Collections.<Object>singletonList(name));
        methodConfig.put("retryPolicy", retryPolicy);
        Map<String, Object> serviceConfig = new HashMap<>();
        serviceConfig.put("methodConfig", Collections.<Object>singletonList(methodConfig));

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8081)
                .enableRetry()
                .defaultServiceConfig(serviceConfig)
                .usePlaintext()
                .build();

        HelloServiceGrpc.HelloServiceBlockingStub stub = HelloServiceGrpc.newBlockingStub(channel);
        HelloResponse response = stub.hello(HelloRequest.newBuilder().setFirstName("Test").setLastName("Testing").build());

        System.out.println(" Welcome, " + response.getGreeting());
        channel.shutdown();
    }

    public GrpcClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build());
    }

    public GrpcClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = HelloServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void sayHello(String firstName, String lastName) {
        log.info("Trying to say hello to " + firstName + " " + lastName + "...");
        HelloRequest request = HelloRequest.newBuilder().setFirstName(firstName).setLastName(lastName).build();
        HelloResponse response;
        try {
            response = blockingStub.hello(request);
        } catch (StatusRuntimeException e) {
            log.warn("Something went wrong: {}", e.getStatus());
            return;
        }
        log.info("Greeting: " + response.getGreeting());
    }
}


