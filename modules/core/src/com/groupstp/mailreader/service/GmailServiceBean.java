package com.groupstp.mailreader.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.api.services.gmail.model.Thread;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.groupstp.mailreader.entity.ConnectionData;
import com.groupstp.mailreader.entity.dto.MessageDto;
import com.groupstp.mailreader.entity.dto.ThreadDto;
import com.haulmont.cuba.core.app.FileStorageAPI;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileStorageException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;

@Service(GmailService.NAME)
public class GmailServiceBean implements GmailService {
    private static final String APPLICATION_NAME = "Scrumit";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

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

        JsonParser jsonParser = new JsonParser();
        JsonElement credentialsJson = jsonParser.parse(connectionData.getCredentials());
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
        JsonObject installed = credentialsJson.getAsJsonObject().get("installed").getAsJsonObject();
        details.setClientId(installed.get("client_id").getAsString())
                .setAuthUri(installed.get("auth_uri").getAsString())
                .setTokenUri(installed.get("token_uri").getAsString())
                .setClientSecret(installed.get("client_secret").getAsString())
                .setRedirectUris(new ArrayList<>());
                installed.get("redirect_uris").getAsJsonArray().forEach(jsonElement -> {
                    details.getRedirectUris().add(jsonElement.getAsString());
                });
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
        clientSecrets.setInstalled(details);
        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = null;
        try {
            flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
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