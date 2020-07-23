package com.groupstp.mailreader.service;

import com.google.api.services.gmail.model.Message;
import com.groupstp.mailreader.entity.dto.ThreadDto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public interface GmailService {
    String NAME = "mailreader_GmailService";


    List<ThreadDto> receive();
}