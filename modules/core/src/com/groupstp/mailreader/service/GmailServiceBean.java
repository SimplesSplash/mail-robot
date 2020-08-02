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
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;

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


    private Gmail getConnectedService(ConnectionData connectionData) throws GeneralSecurityException, IOException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT, connectionData))
                .setApplicationName(APPLICATION_NAME)
                .build();
        return service;
    }

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

    public List<ThreadDto> getThreads(ConnectionData connectionData) throws GeneralSecurityException, IOException {
        Gmail service = getConnectedService(connectionData);
        String user = "me";
        ListThreadsResponse threadsResponse = service.users().threads().list(user).setQ("label:inbox").setMaxResults(500L).execute();
        List<Thread> threads = threadsResponse.getThreads();
        List<ThreadDto> threadDtos = new ArrayList<>();
        if (threads.isEmpty()) {
            return new ArrayList<>();
        } else {
            for (Thread thread : threads) {
                boolean isThreadCreatedByMe = false;
                Thread fullThread = service.users().threads().get("me", thread.getId()).execute();
               for (MessagePartHeader partHeader : fullThread.getMessages().get(0).getPayload().getHeaders()){
                   if ("From".equals(partHeader.getName()) && connectionData.getUsername().equals(partHeader.getValue())){
                       isThreadCreatedByMe = true;
                   }
               }
               if (!isThreadCreatedByMe){
                   ThreadDto threadDto = new ThreadDto()
                           .setId(fullThread.getId())
                           .setMessages(new ArrayList<>());

                   fullThread.getMessages().forEach(message -> {
                       try {
                           MessageDto messageDto = parseMessage(service, message, user);
                           threadDto.getMessages().add(messageDto);
                       } catch (IOException e) {
                           e.printStackTrace();
                       }
                   });
                   threadDtos.add(threadDto);
               }
            }
        }
        return threadDtos;
    }
    @Override
    public MessageDto sendReplyMessage(@NotNull ConnectionData connectionData,
                                       @NotNull String to,
                                       @NotNull String subject,
                                       @NotNull String bodyText,
                                       String inreplyTo,
                                       String threadId,
                                       String references) throws MessagingException, IOException, GeneralSecurityException {
        Gmail service = getConnectedService(connectionData);
        MimeMessage mesasge = createMesasge(connectionData, to, subject, bodyText);

        if (inreplyTo != null)
            mesasge.addHeader("In-Reply-To", inreplyTo);

        if (references != null)
            mesasge.addHeader("References", references);

        mesasge.addHeader("Subject",subject);
        Message sentMsg = sendMessage(service, mesasge, threadId);
        sentMsg = service.users().messages().get("me", sentMsg.getId()).execute();
        return parseMessage(service, sentMsg, "me");

    }
    @Override
    public void sendMessage(ConnectionData connectionData,
                            String to,
                            String subject,
                            String bodyText) throws GeneralSecurityException, MessagingException, IOException {
        sendReplyMessage(connectionData,to,subject,bodyText,null,null,null);
    }

    private MimeMessage createMesasge(ConnectionData connectionData,
                                   String to,
                                   String subject,
                                   String bodyText) throws MessagingException {

            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            MimeMessage email = new MimeMessage(session);

            email.setFrom(new InternetAddress(connectionData.getUsername()));
            email.addRecipient(javax.mail.Message.RecipientType.TO,
                    new InternetAddress(to));
            email.setSubject(subject);
            email.setText(bodyText);
            return email;
    }

//    private MimeMessage createEmailWithAttachment(ConnectionData connectionData,
//                String to,
//                String subject,
//                String bodyText,
//                FileDescriptor file)
//            throws MessagingException {
//            Properties props = new Properties();
//            Session session = Session.getDefaultInstance(props, null);
//
//            MimeMessage email = new MimeMessage(session);
//
//            email.setFrom(new InternetAddress(connectionData.getUsername()));
//            email.addRecipient(javax.mail.Message.RecipientType.TO,
//                    new InternetAddress(to));
//            email.setSubject(subject);
//
//            MimeBodyPart mimeBodyPart = new MimeBodyPart();
//            mimeBodyPart.setContent(bodyText, "text/plain");
//
//            Multipart multipart = new MimeMultipart();
//            multipart.addBodyPart(mimeBodyPart);
//
//            mimeBodyPart = new MimeBodyPart();
//            DataSource source = new FileDataSource(file);
//
//            mimeBodyPart.setDataHandler(new DataHandler(source));
//            mimeBodyPart.setFileName(file.getName());
//
//            multipart.addBodyPart(mimeBodyPart);
//            email.setContent(multipart);
//
//            return email;
//        }

    private Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private Message sendMessage(Gmail service, MimeMessage emailContent, String threadId)
            throws MessagingException, IOException {
        Message message = createMessageWithEmail(emailContent);
        if (threadId !=null)
            message.setThreadId(threadId);
        message = service.users().messages().send("me", message).execute();
        return message;
    }

    private MessageDto parseMessage(Gmail service, Message message, String user) throws IOException {
        List<FileDescriptor> attachments = new ArrayList<>();
        String body = getContent(service, message, attachments, user);
        return new MessageDto()
                .setRecipient(getHeaderValue(message, "To"))
                .setSubject(getHeaderValue(message, "Subject"))
                .setAttachments(attachments)
                .setTextContent(body)
                .setFrom(getHeaderValue(message, "From"))
                .setReceiptTime(new Date(message.getInternalDate()))
                .setMessageExtId(getHeaderValue(message, "Message-ID"))
                .setReferences(getHeaderValue(message, "References"))
                .setInReplyTo(getHeaderValue(message, "In-Reply-To"));
    }

    private String getHeaderValue(Message message, String from) {
        for (MessagePartHeader partHeader : message.getPayload().getHeaders()) {
            if (from.equals(partHeader.getName()))
                return partHeader.getValue();
        }
        return null;
    }

    public String getContent(Gmail service, Message message, List<FileDescriptor> attachments, String user) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        if (message.getPayload().getParts() == null){
            stringBuilder.append(message.getPayload().getBody().getData());
        }else {
            ParseMultiParted(service, message.getPayload().getParts(),message.getId(), user, stringBuilder, attachments);
        }
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
    public Map<ConnectionData, List<ThreadDto>> receive() {

        List<ConnectionData> allConnectionData = connectionService.getAllConnectionData();
        Map<ConnectionData, List<ThreadDto>> result = new HashMap<>();
        List<ThreadDto> threadDtos = new ArrayList<>();
        allConnectionData.forEach(connectionData -> {
            try {
               threadDtos.addAll(getThreads(connectionData));
               result.put(connectionData, new ArrayList<>());
               result.get(connectionData).addAll(threadDtos);
               threadDtos.clear();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
        });
        return result;

    }
}