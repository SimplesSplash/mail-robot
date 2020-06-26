package com.groupstp.mailreader.entity;

import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.BaseUuidEntity;
import com.haulmont.cuba.core.entity.FileDescriptor;

import javax.validation.constraints.Email;
import java.util.List;

@MetaClass(name = "mailreader_ResultMessage")
public class ResultMessage extends BaseUuidEntity {
    private static final long serialVersionUID = 4795004548510941647L;

    @MetaProperty
    protected String subject;

    @Email
    @MetaProperty
    protected String from;

    @MetaProperty
    protected String textContent;

    @MetaProperty
    protected List<FileDescriptor> attachments;

    @MetaProperty
    protected String recipient;

    public String getFrom() {
        return from;
    }

    public ResultMessage setFrom(String from) {
        this.from = from;
        return this;
    }

    public String getRecipient() {
        return recipient;
    }

    public ResultMessage setRecipient(String recipient) {
        this.recipient = recipient;
        return this;
    }

    public List<FileDescriptor> getAttachments() {
        return attachments;
    }

    public ResultMessage setAttachments(List<FileDescriptor> attachments) {
        this.attachments = attachments;
        return this;
    }

    public String getTextContent() {
        return textContent;
    }

    public ResultMessage setTextContent(String textContent) {
        this.textContent = textContent;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public ResultMessage setSubject(String subject) {
        this.subject = subject;
        return this;
    }
}