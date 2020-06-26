package com.groupstp.mailreader.service;

import com.groupstp.mailreader.entity.ConnectionData;
import com.haulmont.cuba.core.global.DataManager;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Service(ConnectionService.NAME)
public class ConnectionServiceBean implements ConnectionService {

    @Inject
    private DataManager dataManager;

    @Override
    public List<ConnectionData> getAllConnectionData() {
        return  dataManager.load(ConnectionData.class)
                .query("select f from mailreader$ConnectionData f ")
                .view("connectionData-full")
                .list();
    }
}