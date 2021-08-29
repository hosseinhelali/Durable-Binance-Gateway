package com.zorg.gateway.binance;

import com.zorg.gateway.cluster.ClusterConfigs;
import com.zorg.gateway.cluster.ClusterManager;
import com.zorg.gateway.cluster.Events;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Gateway {

    private static final Logger logger = LoggerFactory.getLogger(Gateway.class);

    public Gateway(@Autowired ClusterManager clusterManager, @Autowired ClusterConfigs configs) {
        try {

            clusterManager.initCluster(configs.getProperty("clusterName"), AllCoins.coins);
            clusterManager.subscribeOnClusterMemberChanged(this::handleClusterMemberChanged);
            clusterManager.subscribeOnLeaderChanged(this::handleLeaderChanged);

            for (String coin : AllCoins.coins) {
                String nodeVersion = clusterManager.joinToCluster(configs.getProperty("nodeName"), coin);
                logger.debug("a new coin add to cluster name {0}", coin);

            }

        } catch (KeeperException | InterruptedException e) {
            logger.error("can not joined to cluster", e);
        }
    }

    private void handleLeaderChanged(Events.LeaderChangedEvent leaderChangedEvent) {
        logger.info("leader Changed.");
    }

    private void handleClusterMemberChanged(Events.ClusterMemberChangedEvent clusterMemberChangedEvent) {
        logger.info("cluster members changed.");
    }
}
