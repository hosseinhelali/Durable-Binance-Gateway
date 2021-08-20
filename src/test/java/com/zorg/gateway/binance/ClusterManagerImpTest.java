package com.zorg.gateway.binance;

import com.zorg.gateway.cluster.ClusterManager;
import com.zorg.gateway.cluster.ClusterManagerImp;
import com.zorg.gateway.cluster.zookeeper.ZKClient;
import com.zorg.gateway.cluster.zookeeper.ZKClientImp;
import org.apache.zookeeper.KeeperException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

class ClusterManagerImpTest {

    private final static String path = "/election";

    @Test
    void connect() {

        try {

            ZKClient zkClient = new ZKClientImp();
            ClusterManager client = new ClusterManagerImp(zkClient.connect("localhost:2181"));

            client.initCluster("zorg_cluster", Arrays.asList("UTC", "BNB"));

            client.subscribeOnClusterMemberChanged(event -> {
                System.out.println(event.watchedEvent.toString());
            });

            String Node1 = client.joinToCluster("node1.zorg.com", "UTC");
            String Node2 = client.joinToCluster("node2.zorg.com", "UTC");
            String Node3 = client.joinToCluster("node3.zorg.com", "UTC");

            List<String> nodes = client.getClusterMembers("UTC");
            String leader = client.getLeader("UTC");

            client.remove("UTC", Node1);

            List<String> nodes2 = client.getClusterMembers("UTC");
            String leader2 = client.getLeader("UTC");

            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    @Test
    void close() {
    }

}