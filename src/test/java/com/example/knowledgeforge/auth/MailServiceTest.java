package com.example.knowledgeforge.auth;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailServiceTest {

    JavaMailSender sender = mock(JavaMailSender.class);
    TemplateEngine templates = mock(TemplateEngine.class);

    @Test
    void sends_rendered_template_when_smtp_configured() throws Exception {
        MimeMessage msg = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(msg);
        when(templates.process(eq("registration-email"), any(IContext.class))).thenReturn("<p>link</p>");

        new MailService(sender, templates, "kf@x.pl").sendConfirmationEmail("ala@x.pl", "Temat", "ala", "http://l");

        verify(sender).send(msg);
        assertThat(msg.getSubject()).isEqualTo("Temat");
        assertThat(msg.getAllRecipients()[0].toString()).isEqualTo("ala@x.pl");
    }

    @Test
    void without_smtp_only_logs_the_link_and_send_failures_do_not_propagate() {
        new MailService(sender, templates, "").sendConfirmationEmail("a@x.pl", "T", "a", "http://l");
        verify(sender, never()).send(any(MimeMessage.class));

        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        when(templates.process(eq("registration-email"), any(IContext.class))).thenReturn("x");
        doThrow(new MailSendException("smtp down")).when(sender).send(any(MimeMessage.class));
        new MailService(sender, templates, "kf@x.pl").sendConfirmationEmail("a@x.pl", "T", "a", "http://l");
    }
}
