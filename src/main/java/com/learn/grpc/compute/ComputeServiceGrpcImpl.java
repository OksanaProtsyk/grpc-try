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

@GrpcService
@Slf4j
public class ComputeServiceGrpcImpl extends ComputeServiceGrpc.ComputeServiceImplBase {

    @Autowired
    ServerRepository serverRepository;


    @Override
    public void createServer(CreateServerRequest request, StreamObserver<Server> responseObserver) {

        //TODO add fields validation

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

        Thread.sleep(2000); // on purpose delay

        serverEntity.setStatus(ServerStatus.RUNNING);
        log.info("Server with id {} has started", serverEntity.getId());
        serverRepository.save(serverEntity);
        responseObserver.onNext(RebootServerResponse.newBuilder().setServer(serverEntity.toProto()).build());
        responseObserver.onCompleted();


    }

    @Override
    public void listServers(GetServersRequest getServersRequest, StreamObserver<ServersList> responseObserver) {

        List<Server> servers = new ArrayList<>();
        serverRepository.findAll().forEach(entity -> servers.add(entity.toProto()));
        responseObserver.onNext(ServersList.newBuilder().addAllServers(servers).build());
        responseObserver.onCompleted();
    }


    @Override
    public void listServersStreaming(GetServersRequest getServersRequest, StreamObserver<Server> responseObserver) {

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
        }, 0, 10, TimeUnit.SECONDS);

    }
}
