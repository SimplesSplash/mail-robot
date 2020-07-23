package com.groupstp.mailreader.service;

import com.groupstp.mailreader.entity.dto.MessageDto;
import com.groupstp.mailreader.entity.dto.ThreadDto;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

public interface
ReceiveEmailsService {
    String NAME = "mailreader_ReceiveEmailsService";

    List<MessageDto> receive() throws MessagingException, IOException;

}