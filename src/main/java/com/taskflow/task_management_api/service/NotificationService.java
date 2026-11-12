package com.taskflow.task_management_api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Envoi d'e-mails en arrière-plan. Reçoit uniquement des chaînes (pas d'entités JPA)
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

    @Async
    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            // Une panne SMTP ne doit jamais casser une requête métier.
            log.warn("Échec d'envoi de l'e-mail à {} : {}", to, e.getMessage());
        }
    }
}
