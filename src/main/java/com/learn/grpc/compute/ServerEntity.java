package com.learn.grpc.compute;


import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Data
public class ServerEntity {
    @Id
    @GeneratedValue
    Long id;
    String accessIP;
    String serverName;
    int cpu;
    String host;
    ServerStatus status;


    public static ServerEntity fromProto(Server proto) {
        ServerEntity serverEntity = new ServerEntity();
        serverEntity.setId(proto.getId());
        serverEntity.setAccessIP(proto.getAccessIP());
        serverEntity.setServerName(proto.getServerName());
        serverEntity.setCpu(proto.getCpu());
        serverEntity.setHost(proto.getHost());
        serverEntity.setStatus(proto.getStatus());
        return serverEntity;
    }

    public Server toProto() {
        return Server.newBuilder()
                .setId(getId())
                .setAccessIP(getAccessIP())
                .setServerName(getServerName())
                .setCpu(getCpu())
                .setHost(getHost())
                .setStatus(getStatus())
                .build();
    }
}
