package com.zorg.gateway.binance;

import com.zorg.gateway.cluster.ClusterManager;
import com.zorg.gateway.cluster.Events;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class Gateway {

    private HashMap<String, String> supportedCoins;

    public Gateway(@Autowired ClusterManager clusterManager) {
        try {

            clusterManager.initCluster("zorg_cluster", AllCoins.coins);
            clusterManager.subscribeOnClusterMemberChanged(this::handleClusterMemberChanged);
            clusterManager.subscribeOnLeaderChanged(this::handleLeaderChanged);

            for (String coin : AllCoins.coins) {
                 String nodeVersion = clusterManager.joinToCluster("node1.zorg.name", coin);
                 System.out.println("++++++++++++++++++++++" + nodeVersion);

            }

        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleLeaderChanged(Events.LeaderChangedEvent leaderChangedEvent) {
        System.out.println("leader Changed");
    }

    private void handleClusterMemberChanged(Events.ClusterMemberChangedEvent clusterMemberChangedEvent) {

    }
}
