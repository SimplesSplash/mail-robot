package com.groupstp.mailreader.service;

import com.groupstp.mailreader.entity.ResultMessage;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

public interface
ReceiveEmailsService {
    String NAME = "mailreader_ReceiveEmailsService";

    List<ResultMessage> receive() throws MessagingException, IOException;

}