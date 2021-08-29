package com.zorg.gateway.cluster;

import org.apache.zookeeper.WatchedEvent;

public class Events {

    public final static class ClusterMemberChangedEvent {

        public WatchedEvent watchedEvent;

        public ClusterMemberChangedEvent(WatchedEvent watchedEvent) {
            this.watchedEvent = watchedEvent;
        }
    }

    public final static class LeaderChangedEvent {
        public String partition;
        public String newLeader;
        public String lastLeader;

        public LeaderChangedEvent(String partition, String newLeader, String lastLeader) {
            this.partition = partition;
            this.newLeader = newLeader;
            this.lastLeader = lastLeader;
        }
    }
}
