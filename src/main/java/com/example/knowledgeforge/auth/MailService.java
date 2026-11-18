package com.example.knowledgeforge.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Mail aktywacyjny (szablon templates/registration-email.html) — wysyłany asynchronicznie, jak na masterze. */
@Service
public class MailService {

    private static final Logger log = Logger.getLogger(MailService.class.getName());

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String mailUsername;

    public MailService(JavaMailSender mailSender, TemplateEngine templateEngine,
                       @Value("${spring.mail.username:}") String mailUsername) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.mailUsername = mailUsername;
    }

    @Async
    public void sendPasswordResetEmail(String to, String link) {
        if (mailUsername == null || mailUsername.isBlank()) {
            log.warning("SMTP not configured — password reset email not sent");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(mailUsername);
            helper.setTo(to);
            helper.setSubject("Ustaw nowe hasło w Knowledge Forge");
            helper.setText("""
                    Otrzymaliśmy prośbę o zresetowanie hasła w Knowledge Forge.

                    Ustaw nowe hasło za pomocą tego jednorazowego linku:
                    %s

                    Link jest ważny przez 30 minut. Jeśli nie proszono o reset, zignoruj wiadomość.
                    """.formatted(link));
            mailSender.send(message);
        } catch (MessagingException | RuntimeException e) {
            log.severe("Password reset email failed: " + e.getClass().getSimpleName());
        }
    }

    @Async
    public void sendConfirmationEmail(String to, String subject, String username, String confirmationLink) {
        if (mailUsername == null || mailUsername.isBlank()) {
            // Bez skonfigurowanego SMTP (MAIL_USERNAME/MAIL_PASSWORD) nie ma jak wysłać maila — link
            // trafia do logu, żeby dało się aktywować konto lokalnie.
            log.warning("SMTP not configured (spring.mail.username is empty) — confirmation email NOT sent to "
                    + to + ". Activation link: " + confirmationLink);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("confirmationLink", confirmationLink);
            String htmlContent = templateEngine.process("registration-email", context);

            helper.setFrom(mailUsername);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Confirmation email sent to " + to);
        } catch (MessagingException | RuntimeException e) {
            log.log(Level.SEVERE, "Error sending confirmation email to " + to + ": " + e.getMessage(), e);
        }
    }
}
