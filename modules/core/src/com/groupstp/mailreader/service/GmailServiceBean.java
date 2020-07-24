package com.groupstp.mailreader.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Thread;
import com.google.api.services.gmail.model.*;
import com.groupstp.mailreader.entity.ConnectionData;
import com.groupstp.mailreader.entity.dto.MessageDto;
import com.groupstp.mailreader.entity.dto.ThreadDto;
import com.haulmont.cuba.core.app.FileStorageAPI;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileStorageException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service(GmailService.NAME)
public class GmailServiceBean implements GmailService {
    private static final Logger log = LoggerFactory.getLogger(GmailServiceBean.class);


    private static final String APPLICATION_NAME = "Scrumit";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_MODIFY);
    @Inject
    private ConnectionService connectionService;
    @Inject
    private FileStorageAPI fileStorageAPI;

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private  Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT, ConnectionData connectionData) throws IOException {
        if (StringUtils.isAnyBlank(connectionData.getCredentials(), connectionData.getRefreshToken())){
            log.error("Некорректные данные подключения");
            return null;
        }
//        Подключение без запроса у пользователя
//                гайд:(https://stackoverflow.com/questions/19766912/how-do-i-authorise-an-app-web-or-installed-without-user-intervention)
        GoogleCredential credential;
        try( StringReader stringReader = new StringReader(connectionData.getCredentials())) {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, stringReader);
            credential = new GoogleCredential.Builder()
                    .setTransport(HTTP_TRANSPORT)
                    .setJsonFactory(JSON_FACTORY)
                    .setClientSecrets(clientSecrets)
                    .build();
            credential.setRefreshToken(connectionData.getRefreshToken());
        }
        return  credential;
    }

    public List<ThreadDto> getThreads(ConnectionData connectionData) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT, connectionData))
                .setApplicationName(APPLICATION_NAME)
                .build();

        String user = "me";
        ListThreadsResponse threadsResponse = service.users().threads().list(user).execute();
        List<Thread> threads = threadsResponse.getThreads();
        List<ThreadDto> threadDtos = new ArrayList<>();
        if (threads.isEmpty()) {
            return new ArrayList<>();
        } else {
            for (Thread thread : threads) {
                Thread fullThread = service.users().threads().get("me", thread.getId()).execute();
                ThreadDto threadDto = new ThreadDto()
                        .setId(fullThread.getId())
                        .setMessages(new ArrayList<>());

                fullThread.getMessages().forEach(message -> {
                    try {
                        MessageDto messageDto = parseMessage(connectionData, service, message, user);
                        threadDto.getMessages().add(messageDto);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                threadDtos.add(threadDto);
            }
        }
        return threadDtos;
    }

    private MessageDto parseMessage(ConnectionData connectionData, Gmail service, Message message, String user) throws IOException {
        List<FileDescriptor> attachments = new ArrayList<>();
        String body = getContent(service, message, attachments, user);
        String subject = null;
        String from = null;
        for (MessagePartHeader partHeader : message.getPayload().getHeaders()) {
            if ("From".equals(partHeader.getName()))
                from = partHeader.getValue();
        }
        for (MessagePartHeader messagePartHeader : message.getPayload().getHeaders()) {
            if ("Subject".equals(messagePartHeader.getName()))
                subject = messagePartHeader.getValue();
        }
       return new MessageDto()
                .setRecipient(connectionData.getUsername())
                .setSubject(subject)
                .setAttachments(attachments)
                .setTextContent(body)
                .setFrom(from);
    }

    public String getContent(Gmail service, Message message, List<FileDescriptor> attachments, String user) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        ParseMultiParted(service, message.getPayload().getParts(),message.getId(), user, stringBuilder, attachments);
        byte[] bodyBytes = Base64.decodeBase64(stringBuilder.toString());
        String text = new String(bodyBytes, StandardCharsets.UTF_8);
        int indexOfBeginOfForwardedDiv = text.indexOf("<div class=\"gmail_quote\">");
        if (indexOfBeginOfForwardedDiv>0)
            return text.substring(0,indexOfBeginOfForwardedDiv);
        else
            return text;
    }

    private void ParseMultiParted(Gmail service,
                                  List<MessagePart> messageParts,
                                  String msgId,
                                  String user,
                                  StringBuilder stringBuilder,
                                  List<com.haulmont.cuba.core.entity.FileDescriptor> attachments) throws IOException {
        for (MessagePart messagePart : messageParts) {
            if (messagePart.getMimeType().equals("text/html")) {
                stringBuilder.append(messagePart.getBody().getData());
            }
            if (messagePart.getFilename() != null && messagePart.getFilename().length() > 0) {
                String filename = messagePart.getFilename();
                String attId = messagePart.getBody().getAttachmentId();
                MessagePartBody attachPart = service.users().messages().attachments().
                        get(user, msgId, attId).execute();

                byte[] fileByteArray = Base64.decodeBase64(attachPart.getData());
                com.haulmont.cuba.core.entity.FileDescriptor fd = new com.haulmont.cuba.core.entity.FileDescriptor();
                fd.setExtension("png");
                fd.setName(filename);
                fd.setCreateDate(new Date());
                try {
                    fileStorageAPI.saveFile(fd, fileByteArray);
                } catch (FileStorageException e) {
                    e.printStackTrace();
                }
                attachments.add(fd);
            }
            if (messagePart.getParts() != null) {
                ParseMultiParted(service,messagePart.getParts(), msgId, user, stringBuilder, attachments);
            }
        }
    }

    @Override
    public List<ThreadDto> receive() {

        List<ConnectionData> allConnectionData = connectionService.getAllConnectionData();
        List<ThreadDto> threadDtos = new ArrayList<>();
        allConnectionData.forEach(connectionData -> {
            try {
               threadDtos.addAll(getThreads(connectionData));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
        });
        return threadDtos;

    }
}