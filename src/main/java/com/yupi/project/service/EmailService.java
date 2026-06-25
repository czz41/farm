package com.yupi.project.service;

/**
 * 邮件发送服务（QQ SMTP）
 */
public interface EmailService {

    /**
     * 发送邮件
     *
     * @param to      收件人邮箱
     * @param subject 主题
     * @param content 正文（纯文本）
     */
    void sendMail(String to, String subject, String content);
}
