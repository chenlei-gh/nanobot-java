package com.nanobot.channel;

import java.util.*;

/**
 * Channel Interface - Base for all communication channels
 */
public interface Channel {
    String getName();
    String getId();
    void start();
    void stop();
    void send(String chatId, String message);
    void setHandler(ChannelHandler handler);

    interface ChannelHandler {
        void onMessage(String channelId, String chatId, String senderId, String message);
        void onConnect(String channelId, String chatId);
        void onDisconnect(String channelId, String chatId);
        void onError(String channelId, String error);
    }
}
