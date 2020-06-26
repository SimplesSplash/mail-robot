package com.groupstp.mailreader.service;

import com.groupstp.mailreader.entity.ConnectionData;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.search.FlagTerm;
import java.util.*;

@Service(ImapService.NAME)
public class ImapServiceBean implements ImapService {

    private Map<ConnectionData, ConnectionInfo> connectionDataMap = new HashMap<>();

    private final Session session;

    public ImapServiceBean() {
        Properties props = new Properties();
        props.put("mail.pop3.host", "imap.gmail.com");
        props.put("mail.pop3.port", "993");
        props.put("mail.imap.ssl.enable", "true");


        session = Session.getDefaultInstance(props);


    }

    /**
     * Получение непрочитанных писем с сервера
     *
     * @return список непрочитанных писем
     * @throws MessagingException ошибка работы с почтовым сервером
     */
    public List<Message> getUnreadMessages(List<ConnectionData> connections) {
        List<Message> result = new ArrayList<>();
        connections.forEach(connectionData -> {
            try {
                ConnectionInfo info = validateConnection(connectionData);
                Message[] unreadMessages = info.getInbox().search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
                for (Message message : unreadMessages) {
                    result.add(message);
                    message.setFlag(Flags.Flag.SEEN, true);
                }
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        });

        return result;
    }

    /**
     * Закрытие соединения
     *
     * @throws MessagingException ошибка работы с почтовым сервером
     */
    public void closeConnection(List<ConnectionData> connectionDataList) {
        connectionDataList.forEach(connectionData -> {
            ConnectionInfo connectionInfo = connectionDataMap.get(connectionData);

            if (connectionInfo!= null && connectionInfo.getStore() != null && connectionInfo.getStore().isConnected()) {
                try {
                    connectionInfo.getStore().close();
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
                connectionInfo.setStore(null);
            }
        });

    }

    private ConnectionInfo validateConnection(ConnectionData connectionData) throws MessagingException {
        ConnectionInfo info = connectionDataMap.get(connectionData);

        if (info == null) {
             info = new ConnectionInfo();
        }

        if (info.getStore() == null){
            info.setStore(session.getStore(connectionData.getProto()));
        }

        if (!info.getStore().isConnected()){
            info.getStore().connect(
                    connectionData.getServer(),
                    connectionData.getPort(),
                    connectionData.getUsername(),
                    connectionData.getPassword());
        }

        if (info.getInbox() == null){
            info.setInbox(info.getStore().getFolder("inbox"));
        }

        if (!info.getInbox().isOpen()){
            info.getInbox().open(Folder.READ_WRITE);
        }
        connectionDataMap.put(connectionData,info);

        return info;
    }


    public class ConnectionInfo{
        private Store store;
        private Folder inbox;

        public Store getStore() {
            return store;
        }

        public void setStore(Store store) {
            this.store = store;
        }

        public Folder getInbox() {
            return inbox;
        }

        public void setInbox(Folder inbox) {
            this.inbox = inbox;
        }
    }

}