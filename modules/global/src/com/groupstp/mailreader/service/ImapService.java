package com.groupstp.mailreader.service;


import com.groupstp.mailreader.entity.ConnectionData;

import javax.mail.*;
import java.util.List;
import java.util.Map;

public interface ImapService {


    String NAME = "mailreader_ImapService";

    Map<String, Message> getUnreadMessages(List<ConnectionData> connectionDataList) throws MessagingException;
    void closeConnection(List<ConnectionData> connectionDataList) throws MessagingException;

}