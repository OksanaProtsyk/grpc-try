package com.learn.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import com.learn.grpc.HelloServiceGrpc;
import com.learn.grpc.HelloRequest;
import com.learn.grpc.HelloResponse;

public class GrpcClient {
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8081)
                .usePlaintext()
                .build();

        HelloServiceGrpc.HelloServiceBlockingStub stub = HelloServiceGrpc.newBlockingStub(channel);
        HelloResponse response = stub.hello(HelloRequest.newBuilder().setFirstName("Oksana").setLastName("Protsyk").build());

        System.out.println("OLOLO "+ response.getGreeting());
        channel.shutdown();
    }
}
