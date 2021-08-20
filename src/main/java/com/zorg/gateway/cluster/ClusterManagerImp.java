package com.zorg.gateway.cluster;

import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public final class ClusterManagerImp implements ClusterManager {

    private static final Logger logger = LoggerFactory.getLogger(ClusterManagerImp.class);

    private final static String root_partition_path = "/partitions";
    private final static String root_leader_path = "/leader";

    private final List<LeaderChangedEventHandler> leaderChangedEventHandlers = new ArrayList<>();
    private final List<ClusterMemberChangedEventHandler> clusterMemberChangedEventHandlers =
            new ArrayList<>();

    private static String clusterName;
    private final ZooKeeper zooKeeper;

    public ClusterManagerImp(@Autowired ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    @Override
    public void initCluster(String clusterName, List<String> partitions) throws KeeperException, InterruptedException {

        this.clusterName = clusterName;

        //create root path
        this.createZNode(root_partition_path, clusterName);

        //create partitions path
        for (String partition : partitions) {
            String partitionPath = root_partition_path.concat("/").concat(partition);
            this.createZNode(partitionPath, partition);
        }
    }

    @Override
    public String joinToCluster(String hostName, String partition) throws KeeperException, InterruptedException {

        String leaderPath = root_partition_path.concat("/").concat(partition).concat(root_leader_path);

        this.addWatch(root_partition_path.concat("/").concat(partition), this::handleEvent);

        String nodePath = zooKeeper.create(
                leaderPath,
                hostName.getBytes(StandardCharsets.UTF_8),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);

        return nodePath.substring(leaderPath.length());
    }

    @Override
    public void remove(String partition, String sequence) throws
            KeeperException, InterruptedException {

        String targetNodePath = root_partition_path
                .concat("/").concat(partition)
                .concat(root_leader_path)
                .concat(sequence);

        zooKeeper.delete(targetNodePath, -1);
    }

    @Override
    public void subscribeOnLeaderChanged(LeaderChangedEventHandler handler) {
        leaderChangedEventHandlers.add(handler);
    }

    @Override
    public void subscribeOnClusterMemberChanged(ClusterMemberChangedEventHandler handler) {
        clusterMemberChangedEventHandlers.add(handler);
    }

    @Override
    public String getLeader(String partition) throws KeeperException, InterruptedException {

        String partitionPath = root_partition_path.concat("/").concat(partition);

        List<String> nodes = this.getZNode(partition);

        if (!nodes.isEmpty()) {

            Collections.sort(nodes);

            String leaderNodePath = partitionPath.concat("/").concat(nodes.get(0));

            byte[] data = zooKeeper.getData(leaderNodePath, true, null);

            if (data != null) {
                return new String(data, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @Override
    public List<String> getClusterMembers(String partition) throws KeeperException, InterruptedException {

        List<String> clusterNodes = new ArrayList<>();

        String partitionPath = root_partition_path.concat("/").concat(partition);
        List<String> zNodes = zooKeeper.getChildren(partitionPath, true);

        for (String node : zNodes) {
            byte[] data = zooKeeper.getData(
                    partitionPath.concat("/").concat(node),
                    true,
                    null);
            clusterNodes.add(new String(data, StandardCharsets.UTF_8));
        }
        return clusterNodes;
    }

    private List<String> getZNode(String partition) throws KeeperException, InterruptedException {
        String partitionPath = root_partition_path.concat("/").concat(partition);
        List<String> nodes = zooKeeper.getChildren(partitionPath, true);
        return nodes;
    }

    private void addWatch(String path, Watcher watcher) throws KeeperException, InterruptedException {
        zooKeeper.addWatch(path, watcher, AddWatchMode.PERSISTENT);
    }

    private void createZNode(String path, String data) throws KeeperException, InterruptedException {
        if (zooKeeper.exists(path, true) == null) {
            zooKeeper.create(
                    path,
                    data.getBytes(StandardCharsets.UTF_8),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT);
        }
    }

    private void handleEvent(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case NodeChildrenChanged:
                for (ClusterMemberChangedEventHandler handler : clusterMemberChangedEventHandlers)
                    handler.handle(new Events.ClusterMemberChangedEvent(watchedEvent));
            case NodeDeleted:
                for (ClusterMemberChangedEventHandler handler : clusterMemberChangedEventHandlers)
                    handler.handle(new Events.ClusterMemberChangedEvent(watchedEvent));
        }
    }
}
