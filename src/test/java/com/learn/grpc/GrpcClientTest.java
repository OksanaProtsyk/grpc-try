package com.learn.grpc;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class GrpcClientTest {
    //shutdown for the registered servers and channels
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final HelloServiceGrpc.HelloServiceImplBase serviceImpl =
            mock(HelloServiceGrpc.HelloServiceImplBase.class, delegatesTo(
                    new HelloServiceGrpc.HelloServiceImplBase() {
                        // By default the client will receive Status.UNIMPLEMENTED for all RPCs.
                        // You might need to implement necessary behaviors for your test here, like this:
                        //
                        // @Override
                        // public void sayHello(HelloRequest request, StreamObserver<HelloResponse> respObserver) {
                        //   respObserver.onNext(HelloResponse.getDefaultInstance());
                        //   respObserver.onCompleted();
                        // }
                    }));

    private GrpcClient client;

    @Before
    public void setUp() throws Exception {
        // Generate a unique in-process server name.
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(serviceImpl).build().start());

        // Create a client channel and register for automatic graceful shutdown.
        ManagedChannel channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor().build());

        // Create a client using the in-process channel;
        client = new GrpcClient(channel);
    }

    @Test
    public void sayHello_messageDeliveredToServer() {
        ArgumentCaptor<HelloRequest> requestCaptor = ArgumentCaptor.forClass(HelloRequest.class);

        String firstName = "Test";
        String lastName = "Ololo";
        client.sayHello(firstName, lastName);

        verify(serviceImpl)
                .hello(requestCaptor.capture(), ArgumentMatchers.<StreamObserver<HelloResponse>>any());
        assertEquals(firstName, requestCaptor.getValue().getFirstName());
        assertEquals(lastName, requestCaptor.getValue().getLastName());
    }
}