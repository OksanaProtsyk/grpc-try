package com.learn.grpc.compute;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.grpc.server.GrpcService;

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
        log.info(" Server with id {} was created", serverEntity.getId());
        responseObserver.onNext(serverEntity.toProto());
        responseObserver.onCompleted();
    }
}
