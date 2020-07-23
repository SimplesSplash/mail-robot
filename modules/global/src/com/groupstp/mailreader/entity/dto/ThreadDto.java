package com.groupstp.mailreader.entity.dto;

import java.util.List;

public class ThreadDto {

    private String id;

    private List<MessageDto> messages;

    public String getId() {
        return id;
    }

    public ThreadDto setId(String id) {
        this.id = id;
        return this;
    }

    public List<MessageDto> getMessages() {
        return messages;
    }

    public ThreadDto setMessages(List<MessageDto> messages) {
        this.messages = messages;
        return this;
    }
}
