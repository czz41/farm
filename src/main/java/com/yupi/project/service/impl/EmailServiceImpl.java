package com.yupi.project.service.impl;

import com.yupi.project.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 邮件发送服务实现（QQ SMTP）
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${spring.mail.username:}")
    private String from;

    @Resource
    private JavaMailSender mailSender;

    @Override
    public void sendMail(String to, String subject, String content) {
        if (to == null || to.trim().isEmpty()) {
            log.warn("收件人地址为空，跳过发送邮件：{}", subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("邮件发送成功 to={} subject={}", to, subject);
        } catch (Exception e) {
            log.error("邮件发送失败 to={} subject={}", to, subject, e);
        }
    }
}
