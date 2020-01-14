package com.learn.grpc;

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import com.learn.grpc.HelloRequest;
import com.learn.grpc.HelloResponse;
import com.learn.grpc.HelloServiceGrpc;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class HelloServiceImpl extends HelloServiceGrpc.HelloServiceImplBase {

    private static Set<StreamObserver<ChatMessageFromServer>> observers = new HashSet<>();

    @Override
    public void hello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {

        String greeting = "Hello, " + request.getFirstName() + " " + request.getLastName();

        HelloResponse helloResponse = HelloResponse.newBuilder().setGreeting(greeting).build();

        responseObserver.onNext(helloResponse);
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessageFromServer> responseObserver) {
        observers.add(responseObserver);

        return new StreamObserver<ChatMessage>() {
            @Override
            public void onNext(ChatMessage chatMessage) {
                log.info("Server reseived message: {}", chatMessage);
                ChatMessageFromServer message = ChatMessageFromServer.newBuilder()
                        .setMessage(chatMessage)
                        .setTimestamp(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000))
                        .build();

                for (StreamObserver<ChatMessageFromServer> observer : observers) {
                    observer.onNext(message);
                }
            }

            @Override
            public void onError(Throwable t) {
                observers.remove(responseObserver);
                log.error("Server error occurred", t);
            }

            @Override
            public void onCompleted() {
                observers.remove(responseObserver);
            }
        };
    }
}