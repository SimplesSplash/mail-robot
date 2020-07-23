package com.groupstp.mailreader.web.screens;

import com.groupstp.mailreader.service.GmailService;
import com.groupstp.mailreader.service.ReceiveEmailsService;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import javax.mail.MessagingException;
import java.io.IOException;
import java.security.GeneralSecurityException;

@UiController("mailreader_NewScreen")
@UiDescriptor("new-screen.xml")
public class NewScreen extends Screen {

    @Inject
    private GmailService gmailService;

    public void onGetMessagesBtnClick() {
        gmailService.receive();

    }
}