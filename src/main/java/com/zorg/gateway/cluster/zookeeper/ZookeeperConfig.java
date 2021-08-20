package com.zorg.gateway.cluster.zookeeper;

import org.apache.zookeeper.ZooKeeper;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.IOException;

@Configuration
public class ZookeeperConfig {

    @Scope(value = "singleton")
    public ZooKeeper zooKeeper() throws IOException, InterruptedException {
        ZKClient client = new ZKClientImp();
        return client.connect("localhost:2181");
    }
}
