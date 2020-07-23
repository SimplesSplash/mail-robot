package com.groupstp.mailreader.entity.dto;

import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.BaseUuidEntity;
import com.haulmont.cuba.core.entity.FileDescriptor;

import javax.validation.constraints.Email;
import java.util.List;

@MetaClass(name = "mailreader_ResultMessage")
public class MessageDto extends BaseUuidEntity {
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

    public MessageDto setFrom(String from) {
        this.from = from;
        return this;
    }

    public String getRecipient() {
        return recipient;
    }

    public MessageDto setRecipient(String recipient) {
        this.recipient = recipient;
        return this;
    }

    public List<FileDescriptor> getAttachments() {
        return attachments;
    }

    public MessageDto setAttachments(List<FileDescriptor> attachments) {
        this.attachments = attachments;
        return this;
    }

    public String getTextContent() {
        return textContent;
    }

    public MessageDto setTextContent(String textContent) {
        this.textContent = textContent;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public MessageDto setSubject(String subject) {
        this.subject = subject;
        return this;
    }
}