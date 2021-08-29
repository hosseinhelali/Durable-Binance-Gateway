package com.zorg.gateway.cluster;

import com.zorg.gateway.cluster.zookeeper.ZKClient;
import com.zorg.gateway.cluster.zookeeper.ZKClientImp;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Properties;

@Configuration
public class ClusterManagerConfig {

    private static final Logger logger = LoggerFactory.getLogger(ClusterManagerConfig.class);

    @Value("${zorg.gateway.cluster.name:}")
    private String clusterName;

    @Value("${zorg.gateway.cluster.node.name:}")
    private String nodeName;

    @Value("${zorg.gateway.cluster.zk.sessionTimeout:2000}")
    private int sessionTimeout;

    @Value("${zorg.gateway.cluster.zk.nodes:}")
    private String zookeeperNodes;


    @Value("${zorg.gateway.cluster.zk.partition.path:/partitions}")
    private String zkRootPartitionPath;

    @Value("${zorg.gateway.cluster.zk.partition.leader:/leader}")
    private String zkRootLeaderPath;

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ClusterConfigs clusterConfigs() {
        ClusterConfigs properties = new ClusterConfigs();
        properties.put("clusterName", clusterName);
        properties.put("nodeName", nodeName);
        properties.put("sessionTimeout", sessionTimeout);
        properties.put("zookeeperNodes", zookeeperNodes);
        properties.put("zkRootPartitionPath", zkRootPartitionPath);
        properties.put("zkRootLeaderPath", zkRootLeaderPath);

        printConfigs(properties);

        return properties;
    }

    private void printConfigs(ClusterConfigs configs) {
        logger.info("\n --------------------------Cluster configs-------------------------------- \n "
                + configs.toString().replace(',', '\n') +
                "\n ------------------------------------------------------------------------ \n ");
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ZooKeeper zooKeeper() throws IOException, InterruptedException {
        ZKClient client = new ZKClientImp();
        return client.connect(zookeeperNodes, sessionTimeout);
    }

    @Bean
    @Scope
    public ClusterManager clusterManager(@Autowired ZooKeeper zooKeeper) {
        return new ClusterManagerImp(zooKeeper, zkRootPartitionPath, zkRootLeaderPath);
    }
}
