package com.zorg.gateway.cluster.zookeeper;

import com.zorg.gateway.cluster.ClusterManagerImp;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ZKClientImp implements ZKClient {

    private static final Logger logger = LoggerFactory.getLogger(ClusterManagerImp.class);
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    private ZooKeeper zooKeeper;


    public ZooKeeper connect(String zNode, int sessionTimeout) throws InterruptedException, IOException {
        logger.debug("Starting to connect zookeeper nodes : {}", zNode);
        zooKeeper = new ZooKeeper(zNode, sessionTimeout, this::handleZKEvents);

        countDownLatch.await();
        return zooKeeper;
    }

    private void handleZKEvents(WatchedEvent watchedEvent) {
        if (Watcher.Event.KeeperState.SyncConnected == watchedEvent.getState()) {
            logger.info("Connected to zookeeper successfully.");
            countDownLatch.countDown();
        } else {
            logger.debug("Zookeeper connection : {}", watchedEvent.getState());
        }
    }

    public void close() throws InterruptedException {
        logger.debug("Tray to close zookeeper connection.");
        zooKeeper.close();
    }

}
