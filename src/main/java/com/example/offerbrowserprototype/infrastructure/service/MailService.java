package com.example.offerbrowserprototype.infrastructure.service;

import com.example.offerbrowserprototype.infrastructure.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Value("${app.server.base-url}")
    private String serverBaseUrl;

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final UserRepository userRepository;

    public MailService(JavaMailSender mailSender, TemplateEngine templateEngine, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.userRepository = userRepository;
    }

    @Async
    public void sendConfirmationEmail(String to, String subject, String username, String confirmationLink) {
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("confirmationLink", confirmationLink);

            String htmlContent = templateEngine.process("registration-email", context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            log.info("Confirmation email sent to {}", to);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Error sending confirmation email to {}: {}", to, e.getMessage());
            throw new IllegalStateException("Failed to send email", e);
        }
    }
}
