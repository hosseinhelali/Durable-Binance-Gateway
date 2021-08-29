package com.zorg.gateway.cluster;

import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public final class ClusterManagerImp implements ClusterManager {

    private static final Logger logger = LoggerFactory.getLogger(ClusterManagerImp.class);
    private static List<String> partitions;

    private final String rootPartitionPath;
    private final String rootLeaderPath;

    private static String clusterName;
    private final ZooKeeper zooKeeper;

    private final List<LeaderChangedEventHandler> leaderChangedEventHandlers = new ArrayList<>();
    private final List<ClusterMemberChangedEventHandler> clusterMemberChangedEventHandlers =
            new ArrayList<>();

    private final HashMap<String, String> leaders = new HashMap<>();


    public ClusterManagerImp(ZooKeeper zooKeeper,
                             String rootPartitionPath,
                             String rootLeaderPath) {
        this.zooKeeper = zooKeeper;
        this.rootPartitionPath = rootPartitionPath;
        this.rootLeaderPath = rootLeaderPath;
    }

    @Override
    public void initCluster(String clusterName, List<String> partitions) throws KeeperException, InterruptedException {

        ClusterManagerImp.clusterName = clusterName;

        ClusterManagerImp.partitions = partitions;

        //create root path
        this.createZNode(rootPartitionPath, clusterName);

        //create partitions path
        for (String partition : partitions) {
            String partitionPath = getPartitionPath(partition);
            this.createZNode(partitionPath, partition);
        }
    }

    @Override
    public String joinToCluster(String hostName, String partition)
            throws KeeperException, InterruptedException {

        String leaderPath = getLeaderPath(partition);

        this.addWatch(rootPartitionPath.concat("/").concat(partition), this::handleEvent);

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

        String targetNodePath = getNodePath(partition, sequence);

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

        String partitionPath = getPartitionPath(partition);

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

        String partitionPath = getPartitionPath(partition);
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

    @Override
    public int getAllChildrenNumber(String partition) throws KeeperException, InterruptedException {
        String path = getPartitionPath(partition);
        return zooKeeper.getAllChildrenNumber(path);
    }

    private String getPartitionPath(String partition) {
        return rootPartitionPath.concat("/").concat(partition);
    }

    private String getLeaderPath(String partition) {
        return getPartitionPath(partition).concat(rootLeaderPath);
    }

    private String getNodePath(String partition, String sequence) {
        return getNodePath(partition, sequence).concat(sequence);
    }

    private String getPartitionName(String nodePath){
        return nodePath.substring(rootPartitionPath.concat("/").length());
    }


    private List<String> getZNode(String partition) throws KeeperException, InterruptedException {
        String partitionPath = getPartitionPath(partition);
        return zooKeeper.getChildren(partitionPath, true);
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
        String partition = getPartitionName(watchedEvent.getPath());
        switch (watchedEvent.getType()) {
            case NodeChildrenChanged:
                for (ClusterMemberChangedEventHandler handler : clusterMemberChangedEventHandlers) {
                    handler.handle(new Events.ClusterMemberChangedEvent(watchedEvent));
                    this.scanLeaders(partition);
                }
            case NodeDeleted:
                for (ClusterMemberChangedEventHandler handler : clusterMemberChangedEventHandlers) {
                    handler.handle(new Events.ClusterMemberChangedEvent(watchedEvent));
                    this.scanLeaders(partition);
                }
        }
    }

    private void scanLeaders(String partition) throws RuntimeException {
        StopWatch latency = new StopWatch();

        latency.start();
        try {
            String newLeader = this.getLeader(partition);
            String lastLeader = leaders.get(partition);

            if (newLeader != null && lastLeader != null) {
                if (!lastLeader.equals(newLeader)) {
                    leaders.remove(partition);
                    leaders.put(partition, newLeader);
                    if (!newLeader.equals(clusterName)) {
                        for (LeaderChangedEventHandler handler : leaderChangedEventHandlers) {
                            handler.handle(new Events.LeaderChangedEvent(partition, newLeader, lastLeader));
                        }
                    }
                }
            } else {
                //first node add to cluster or the node joining to cluster
                if (newLeader != null)
                    leaders.put(partition, newLeader);
                else
                    leaders.put(partition, clusterName);
            }

        } catch (InterruptedException e) {
            logger.error("can not scan leaders", e);
            throw new RuntimeException(e);
        } catch (KeeperException e) {
            logger.error("can not scan leaders", e);
            throw new RuntimeException(e);
        }

        latency.stop();
        System.out.println(">>>>>>>>>>>>>>>>>>>>>" + latency.getLastTaskTimeMillis());
    }

}
