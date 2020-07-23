package com.groupstp.mailreader.service;

import com.groupstp.mailreader.entity.ConnectionData;
import com.groupstp.mailreader.entity.dto.MessageDto;
import com.groupstp.mailreader.entity.dto.ThreadDto;
import com.haulmont.cuba.core.app.FileStorageAPI;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileStorageException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service(ReceiveEmailsService.NAME)
public class EmailsServiceBean implements ReceiveEmailsService {

    @Inject
    private ImapService imapService;
    @Inject
    private ConnectionService connectionService;
    @Inject
    private FileStorageAPI fileStorageAPI;


    @Override
    public List<MessageDto> receive() throws MessagingException, IOException {

        List<ConnectionData> allConnectionData = connectionService.getAllConnectionData();

        if (allConnectionData.isEmpty())
            return null;

        Map<String, List<Message>> unreadMessages = imapService.getUnreadMessages(allConnectionData);

            List<MessageDto> messageDtos = new ArrayList<>();

            for (Map.Entry<String, List<Message>> messageSet : unreadMessages.entrySet()) {
                for (Message message: messageSet.getValue()){
                    String subject = message.getSubject();
                    String recipient = messageSet.getKey();
                    MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
                    StringBuilder result = new StringBuilder();
                    List<FileDescriptor> fileDescriptors = new ArrayList<>();
                    parseMultiparted(mimeMultipart,result,fileDescriptors);
                    MessageDto messageDto = new MessageDto()
                            .setRecipient(recipient)
                            .setSubject(subject)
                            .setAttachments(fileDescriptors)
                            .setTextContent(result.toString())
                            .setFrom(message.getFrom()[0].toString());
                    messageDtos.add(messageDto);
                }

            }
            imapService.closeConnection(allConnectionData);
            return messageDtos;
    }


    private FileDescriptor saveFile(String filename, InputStream input) {
        try {
            byte[] attachment = new byte[16 * 1024 * 1024];
            input.read(attachment);
            FileDescriptor fd = new FileDescriptor();
            fd.setName(filename);
            fd.setExtension("png");
            fd.setCreateDate(new Date());
            fileStorageAPI.saveFile(fd, attachment);
            return fd;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FileStorageException e) {
            e.printStackTrace();
        }
        return null;
    }
private void parseMultiparted(Multipart part, StringBuilder body, List<FileDescriptor> attachments) throws MessagingException, IOException {
    for(int i = 0; i < part.getCount(); i ++)
        parsePart(part.getBodyPart(i), body, attachments);
}

    private void parsePart(BodyPart part, StringBuilder body, List<FileDescriptor> attachments) throws MessagingException, IOException {
        String type = part.getContentType();

        if(type.contains("multipart/"))
            parseMultiparted((Multipart)part.getContent(),body, attachments);

        else if(type.contains("TEXT/HTML"))
            body.append("\n").append(part.getContent());

        else if((Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) ||
                (Part.INLINE.equalsIgnoreCase(part.getDisposition()) && !StringUtils.isEmpty(part.getFileName())))
            attachments.add(saveFile(MimeUtility.decodeText(part.getFileName()),part.getInputStream()));
    }
}


