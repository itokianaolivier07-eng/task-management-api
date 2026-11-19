package com.taskflow.task_management_api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Envoi d'e-mails. Reçoit uniquement des chaînes (pas d'entités JPA)
 * pour éviter les problèmes de chargement paresseux dans un autre thread.
 */
@Service
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final String from;

    public NotificationService(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    /** Envoi en arrière-plan : une panne SMTP ne casse jamais une requête métier. */
    @Async
    public void send(String to, String subject, String body) {
        sendNow(to, subject, body);
    }

    /** Envoi synchrone : renvoie false si l'envoi a échoué (utilisé par le job de rappels). */
    public boolean sendNow(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.warn("Échec d'envoi de l'e-mail à {} : {}", to, e.getMessage());
            return false;
        }
    }
}
