package com.learn.grpc.compute;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.grpc.server.GrpcService;

import java.util.Optional;

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

        try {
            Thread.sleep(2000); // on purpose delay
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        serverEntity.setStatus(ServerStatus.RUNNING);
        log.info("Server with id {} has started", serverEntity.getId());
        serverRepository.save(serverEntity);
        responseObserver.onNext(RebootServerResponse.newBuilder().setServer(serverEntity.toProto()).build());
        responseObserver.onCompleted();


    }
}
