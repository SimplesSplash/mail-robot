package com.groupstp.mailreader.service;

import com.google.api.services.gmail.model.Message;
import com.groupstp.mailreader.entity.ConnectionData;
import com.groupstp.mailreader.entity.dto.MessageDto;
import com.groupstp.mailreader.entity.dto.ThreadDto;

import javax.mail.MessagingException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

public interface GmailService {
    String NAME = "mailreader_GmailService";


    MessageDto sendReplyMessage(ConnectionData connectionData,
                                String to,
                                String subject,
                                String bodyText,
                                String inreplyTo,
                                String threadId,
                                String references) throws MessagingException, IOException, GeneralSecurityException;

    void sendMessage(ConnectionData connectionData,
                     String to,
                     String subject,
                     String bodyText) throws GeneralSecurityException, MessagingException, IOException;

    Map<ConnectionData, List<ThreadDto>> receive();
}