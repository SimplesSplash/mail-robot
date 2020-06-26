package com.groupstp.mailreader.service;

import com.groupstp.mailreader.entity.ConnectionData;

import java.util.List;

public interface ConnectionService {
    String NAME = "mailreader_ConnectionService";

    List<ConnectionData> getAllConnectionData();
}