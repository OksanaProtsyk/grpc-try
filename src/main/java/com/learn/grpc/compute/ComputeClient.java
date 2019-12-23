package com.learn.grpc.compute;


import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;

@Slf4j
public class ComputeClient {
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();
        //Synchronous server creation
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

        log.info("Server was successfully created " + createdServerResponse.toString());

        // Reboot server with unknownID
        ComputeServiceGrpc.ComputeServiceBlockingStub serviceStub = ComputeServiceGrpc.newBlockingStub(channel);

        try {
            serviceStub.rebootServer(RebootServerRequest.newBuilder().setServerId(1000L).build());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                log.error("Server threw NOT Found exception... !", e);
            } else {
                log.error("Server threw some other exception... !", e);

            }
        }


        //TODO subscribe for server status
        ComputeServiceGrpc.ComputeServiceFutureStub nonBlockingStub = ComputeServiceGrpc.newFutureStub(channel);

        ListenableFuture<RebootServerResponse> rebootServerResponseFeature = nonBlockingStub.rebootServer(RebootServerRequest.newBuilder().setServerId(createdServerResponse.getId()).build());
        try {
            RebootServerResponse rebootServerResponse = rebootServerResponseFeature.get();
            log.info("Server with id {} was rebooted, server info after reboot {}", createdServerResponse.getId(), rebootServerResponse.getServer());
            log.info("Server status is {} ", rebootServerResponse.getServer().getStatus().toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        channel.shutdown();
    }
}
