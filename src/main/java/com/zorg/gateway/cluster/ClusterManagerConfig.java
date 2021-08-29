package com.zorg.gateway.cluster;

import com.zorg.gateway.cluster.zookeeper.ZKClient;
import com.zorg.gateway.cluster.zookeeper.ZKClientImp;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.IOException;

@Configuration
public class ClusterManagerConfig {

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
    private String zkRootLeaderPath ;

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ZooKeeper zooKeeper() throws IOException, InterruptedException {
        ZKClient client = new ZKClientImp();
        return client.connect(zookeeperNodes, sessionTimeout);
    }

    @Bean
    @Scope
    public ClusterManager clusterManager(@Autowired ZooKeeper zooKeeper){
        return new ClusterManagerImp(zooKeeper, zkRootPartitionPath, zkRootLeaderPath);
    }
}
