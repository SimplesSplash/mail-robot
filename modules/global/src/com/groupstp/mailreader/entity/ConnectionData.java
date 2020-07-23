package com.groupstp.mailreader.entity;

import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

@Table(name = "MAILREADER_CONNECTION_DATA")
@Entity(name = "mailreader$ConnectionData")
public class ConnectionData extends StandardEntity {
    private static final long serialVersionUID = 8029068749923194344L;

    @Column(name = "SERVER")
    protected String server;

    @Column(name = "PORT")
    protected Integer port;

    @Column(name = "USERNAME")
    protected String username;

    @Column(name = "PASSWORD")
    protected String password;

    @Column(name = "PROTO")
    protected String proto;

    @Lob
    @Column(name = "CREDENTIALS")
    protected String credentials;

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getProto() {
        return proto;
    }

    public void setProto(String proto) {
        this.proto = proto;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }
}