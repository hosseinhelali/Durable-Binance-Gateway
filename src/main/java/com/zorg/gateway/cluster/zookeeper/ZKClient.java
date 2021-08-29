package com.zorg.gateway.cluster.zookeeper;

import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

public interface ZKClient {

    ZooKeeper connect(String zNode, int sessionTimeout) throws InterruptedException, IOException;

    void close() throws InterruptedException;

}
