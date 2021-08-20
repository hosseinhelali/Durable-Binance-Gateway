package com.zorg.gateway.cluster;

public interface ClusterMemberChangedEventHandler {

    void handle(Events.ClusterMemberChangedEvent event);

}
