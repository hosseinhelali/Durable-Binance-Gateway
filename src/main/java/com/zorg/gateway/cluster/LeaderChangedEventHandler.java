package com.zorg.gateway.cluster;

public interface LeaderChangedEventHandler {

    void handle(Events.LeaderChangedEvent event);
}
