package com.learn.grpc.compute;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.grpc.server.GrpcService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

@GrpcService
@Slf4j
public class ComputeServiceGrpcImpl extends ComputeServiceGrpc.ComputeServiceImplBase {

    @Autowired
    ServerRepository serverRepository;

    @Override
    public void createServer(CreateServerRequest request, StreamObserver<Server> responseObserver) {

        ServerEntity serverEntity = ServerEntity.fromProto(request.getServer());
        serverEntity = serverRepository.save(serverEntity);
        log.info("Server with id {} was created", serverEntity.getId());
        responseObserver.onNext(serverEntity.toProto());
        responseObserver.onCompleted();
    }

    @Override
    @SneakyThrows
    public void rebootServer(RebootServerRequest request, StreamObserver<RebootServerResponse> responseObserver) {
        Long serverId = request.getServerId();
        Optional<ServerEntity> res = serverRepository.findById(serverId);
        ServerEntity serverEntity = null;

        if (res.isPresent()) {
            serverEntity = res.get();
        } else {
            log.info("Server with id {} is not found", serverId);
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Server with id:" + serverId + " not found")
                    .asRuntimeException());
            return;
        }
        serverEntity.setStatus(ServerStatus.STOPPED);
        serverRepository.save(serverEntity);
        log.info("Server with id {} has stopped", serverEntity.getId());

        Thread.sleep(10000); // on purpose delay

        serverEntity.setStatus(ServerStatus.RUNNING);
        log.info("Server with id {} has started", serverEntity.getId());
        serverRepository.save(serverEntity);
        responseObserver.onNext(RebootServerResponse.newBuilder().setServer(serverEntity.toProto()).build());
        responseObserver.onCompleted();


    }

    @Override
    public void listServers(GetServersRequest getServersRequest, StreamObserver<Server> responseObserver) {

        serverRepository.findAll().forEach(entity -> responseObserver.onNext(entity.toProto())
        );
        responseObserver.onCompleted();
    }

    @Override
    public void subscribeForServerStatusNotifications(GetServerStatus request, StreamObserver<ServerStatusResponse> responseObserver) {

        Long serverId = request.getServerId();

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(() -> {
            Optional<ServerEntity> res = serverRepository.findById(serverId);
            ServerEntity serverEntity = null;

            if (res.isPresent()) {
                serverEntity = res.get();
                responseObserver.onNext(ServerStatusResponse.newBuilder().setStatus(serverEntity.toProto().getStatus()).build());
            } else {
                log.info("Server with id {} is not found", serverId);
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Server with id:" + serverId + " not found")
                        .asRuntimeException());
                return;
            }
            responseObserver.onNext(ServerStatusResponse.newBuilder().setStatus(serverEntity.toProto().getStatus()).build());
        }, 0, 5, TimeUnit.SECONDS);

    }

    @Override
    public StreamObserver<CreateServerRequest> createServerBulk(StreamObserver<CreateServerBulkResponse> responseObserver) {
        return new StreamObserver<CreateServerRequest>() {
            int serverCount;
            long startTime = System.nanoTime();
            List<Server> createdServers = new ArrayList<>();

            @Override
            public void onNext(CreateServerRequest createServerRequest) {
                long serverId = createServerRequest.getServer().getId();
                if (serverId > 0) {
                    Optional<ServerEntity> res = serverRepository.findById(createServerRequest.getServer().getId());
                    ServerEntity serverEntity = null;

                    if (res.isPresent()) {
                        serverEntity = res.get();
                        log.info("Server with id {} already exists", serverId);
                        return;
                    }
                }
                serverCount++;
                ServerEntity serverEntity = serverRepository.save(ServerEntity.fromProto(createServerRequest.getServer()));
                createdServers.add(serverEntity.toProto());

            }

            @Override
            public void onError(Throwable t) {
                log.warn("Some error occurred", t);
            }

            @Override
            public void onCompleted() {
                long seconds = NANOSECONDS.toSeconds(System.nanoTime() - startTime);
                responseObserver.onNext(CreateServerBulkResponse.newBuilder()
                        .setNumberOfServers(serverCount)
                        .addAllServers(createdServers)
                        .setElapsedSec(seconds)
                        .build());
                responseObserver.onCompleted();
            }
        };
    }
}
