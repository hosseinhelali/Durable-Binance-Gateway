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
    }
}
