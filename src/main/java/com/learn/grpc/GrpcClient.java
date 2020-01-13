package com.learn.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GrpcClient {
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
}
