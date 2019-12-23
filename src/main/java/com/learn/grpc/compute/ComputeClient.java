package com.learn.grpc.compute;


import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ComputeClient {
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        ComputeServiceGrpc.ComputeServiceBlockingStub blockingStub = ComputeServiceGrpc.newBlockingStub(channel);


        Server serverToCreate = Server.newBuilder()
                .setCpu(4)
                .setStatus(ServerStatus.RUNNING)
                .setServerName("My Server")
                .setAccessIP("192.168.0.1")
                .setHost("ololo")
                .build();
        Server createdServerResponse = blockingStub.createServer(CreateServerRequest.newBuilder()
                .setServer(serverToCreate)
                .build());

        System.out.println("OLOLO "+ createdServerResponse.toString());

        //TODO subscribe for server status
        ComputeServiceGrpc.ComputeServiceFutureStub nonBlockingStub = ComputeServiceGrpc.newFutureStub(channel);

        nonBlockingStub.rebootServer(RebootServerRequest.newBuilder().build());

        channel.shutdown();
    }
}
