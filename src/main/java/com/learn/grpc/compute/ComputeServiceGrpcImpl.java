package com.learn.grpc.compute;

import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.grpc.server.GrpcService;

@GrpcService
public class ComputeServiceGrpcImpl extends ComputeServiceGrpc.ComputeServiceImplBase {

    @Autowired
    ServerRepository serverRepository;


    @Override
    public void createServer(CreateServerRequest request, StreamObserver<Server> responseObserver) {

        //TODO add fields validation

        ServerEntity serverEntity = ServerEntity.fromProto(request.getServer());
        serverEntity = serverRepository.save(serverEntity);
        responseObserver.onNext(serverEntity.toProto());
        responseObserver.onCompleted();
    }
}
