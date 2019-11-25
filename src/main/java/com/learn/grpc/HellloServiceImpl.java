package com.learn.grpc;

import io.grpc.stub.StreamObserver;
import com.learn.grpc.HelloRequest;
import com.learn.grpc.HelloResponse;
import com.learn.grpc.HelloServiceGrpc;

public class HellloServiceImpl extends HelloServiceGrpc.HelloServiceImplBase {

    @Override
    public void hello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {

        String greeting = "Hello, " + request.getFirstName() + " " + request.getLastName();

        HelloResponse helloResponse = HelloResponse.newBuilder().setGreeting(greeting).build();

        responseObserver.onNext(helloResponse);
        responseObserver.onCompleted();
    }
}