package com.groupstp.mailreader.entity.dto;

import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.BaseUuidEntity;
import com.haulmont.cuba.core.entity.FileDescriptor;

import javax.validation.constraints.Email;
import java.util.Date;
import java.util.List;

@MetaClass(name = "mailreader_ResultMessage")
public class MessageDto extends BaseUuidEntity {
    private static final long serialVersionUID = 4795004548510941647L;

    @MetaProperty
    protected String subject;

    @MetaProperty
    protected Date receiptTime;

    @Email
    @MetaProperty
    protected String from;

    @MetaProperty
    protected String textContent;

    @MetaProperty
    protected List<FileDescriptor> attachments;

    @MetaProperty
    protected String recipient;

    @MetaProperty
    protected String references;

    @MetaProperty
    protected String inReplyTo;

    @MetaProperty
    protected String messageExtId;

    public String getMessageExtId() {
        return messageExtId;
    }

    public MessageDto setMessageExtId(String messageExtId) {
        this.messageExtId = messageExtId;
        return this;
    }

    public String getInReplyTo() {
        return inReplyTo;
    }

    public MessageDto setInReplyTo(String inReplyTo) {
        this.inReplyTo = inReplyTo;
        return this;
    }

    public String getReferences() {
        return references;
    }

    public MessageDto setReferences(String references) {
        this.references = references;
        return this;
    }

    public Date getReceiptTime() {
        return receiptTime;
    }

    public MessageDto setReceiptTime(Date receiptTime) {
        this.receiptTime = receiptTime;
        return this;
    }

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