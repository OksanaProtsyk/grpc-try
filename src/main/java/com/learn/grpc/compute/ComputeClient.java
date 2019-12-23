package com.learn.grpc.compute;


import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ComputeClient {
    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();
        //Synchronous server creation
        Server createdServerResponse = createFirstServer(channel);
        Server secondCreatedServer = createSecondServer(channel);

        // Reboot server with unknownID
        rebootServerWithUnknownId(channel);

        bulkCreateServers(channel, 5);

        listAllServers(channel);
        //TODO subscribe for server status

        rebootExistingService(channel, createdServerResponse);

        channel.shutdown();
    }


    private static void listAllServers(ManagedChannel channel) {
        ComputeServiceGrpc.ComputeServiceBlockingStub blockingStub = ComputeServiceGrpc.newBlockingStub(channel);
        Iterator<Server> serversList = blockingStub.listServers(GetServersRequest.newBuilder().build());
        log.info("Servers retrieved blocking: ");
        serversList.forEachRemaining(server -> log.info("Server {}", server));

    }

    private static void rebootExistingService(ManagedChannel channel, Server createdServerResponse) {
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
    }

    private static void rebootServerWithUnknownId(ManagedChannel channel) {
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
    }

    private static Server createFirstServer(ManagedChannel channel) {
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
        return createdServerResponse;
    }


    private static Server createSecondServer(ManagedChannel channel) {
        ComputeServiceGrpc.ComputeServiceBlockingStub blockingStub = ComputeServiceGrpc.newBlockingStub(channel);


        Server serverToCreate = Server.newBuilder()
                .setCpu(4)
                .setStatus(ServerStatus.RUNNING)
                .setServerName("My Server 2")
                .setAccessIP("192.168.0.2")
                .setHost("my host2")
                .build();
        Server createdServerResponse = blockingStub.createServer(CreateServerRequest.newBuilder()
                .setServer(serverToCreate)
                .build());

        log.info("Server was successfully created " + createdServerResponse.toString());
        return createdServerResponse;
    }

    public static void bulkCreateServers(ManagedChannel channel, int numberOfServers) throws InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<CreateServerBulkResponse> responseObserver = new StreamObserver<CreateServerBulkResponse>() {
            @Override
            public void onNext(CreateServerBulkResponse summary) {
                log.info("Finished server creation. Took {} seconds. Number of created servers {}, Servers created: ", summary.getElapsedSec(), summary.getNumberOfServers());
                summary.getServersList().forEach(server -> log.info("Server: {} ", server));
            }

            @Override
            public void onError(Throwable t) {
                Status status = Status.fromThrowable(t);
                log.warn("Bulk server creation Failed: {}", status);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info(" Bulk server creation finished");
                finishLatch.countDown();
            }
        };

        ComputeServiceGrpc.ComputeServiceStub asyncStub = ComputeServiceGrpc.newStub(channel);

        StreamObserver<CreateServerRequest> requestObserver = asyncStub.createServerBulk(responseObserver);
        try {
            for (int i = 0; i < numberOfServers; i++) {
                Server serverToCreate = Server.newBuilder()
                        .setCpu(4)
                        .setStatus(ServerStatus.RUNNING)
                        .setServerName("My Server " + i)
                        .setAccessIP("192.168.0." + i)
                        .build();
                requestObserver.onNext(CreateServerRequest.newBuilder().setServer(serverToCreate).build());
                Random rand = new Random();
                Thread.sleep(rand.nextInt(1000) + 500);
                if (finishLatch.getCount() == 0) {
                    // RPC completed or errored before finished sending.
                    return;
                }
            }
        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }
        //end of requests
        requestObserver.onCompleted();

        finishLatch.await(1, TimeUnit.MINUTES);
    }
}
