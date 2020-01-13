package com.learn.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatClient extends Application {
    private ObservableList<String> messages = FXCollections.observableArrayList();
    private ListView<String> messagesView = new ListView<>();
    private TextField name = new TextField("name");
    private TextField message = new TextField();
    private Button send = new Button();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        messagesView.setItems(messages);

        send.setText("Send");

        BorderPane pane = new BorderPane();
        pane.setLeft(name);
        pane.setCenter(message);
        pane.setRight(send);

        BorderPane root = new BorderPane();
        root.setCenter(messagesView);
        root.setBottom(pane);

        primaryStage.setTitle("gRPC Chat");
        primaryStage.setScene(new Scene(root, 480, 320));

        primaryStage.show();

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8081).usePlaintext().build();
        HelloServiceGrpc.HelloServiceStub chatService = HelloServiceGrpc.newStub(channel);
        StreamObserver<ChatMessage> chat = chatService.chat(new StreamObserver<ChatMessageFromServer>() {
            @Override
            public void onNext(ChatMessageFromServer value) {
                Platform.runLater(() -> {
                    messages.add(value.getMessage().getFrom() + ": " + value.getMessage().getMessage());
                    messagesView.scrollTo(messages.size());
                });
            }

            @Override
            public void onError(Throwable t) {
                log.error("Disconnected...", t);
            }

            @Override
            public void onCompleted() {
               log.info("Disconnected");
            }
        });

        send.setOnAction(e -> {
            chat.onNext(ChatMessage.newBuilder().setFrom(name.getText()).setMessage(message.getText()).build());
            message.setText("");
        });
        primaryStage.setOnCloseRequest(e -> {
            chat.onCompleted();
            channel.shutdown();
        });
    }
}
