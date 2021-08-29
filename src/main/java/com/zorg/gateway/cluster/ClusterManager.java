package com.zorg.gateway.cluster;

import org.apache.zookeeper.KeeperException;

import java.util.List;

public interface ClusterManager {

    void initCluster(String clusterName, List<String> partitions) throws KeeperException, InterruptedException;

    String joinToCluster(String hostName, String partition) throws KeeperException, InterruptedException;

    void remove(String partition, String sequence) throws KeeperException, InterruptedException;

    String getLeader(String partition) throws KeeperException, InterruptedException;

    List<String> getClusterMembers(String partition) throws KeeperException, InterruptedException;

    void subscribeOnLeaderChanged(LeaderChangedEventHandler handler);

    void subscribeOnClusterMemberChanged(ClusterMemberChangedEventHandler handler);

    int getAllChildrenNumber(String partition) throws KeeperException, InterruptedException;
}

