package com.courshare.identity.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@courshare.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(String to, String code) {
        logger.info("\n==================================================\n" +
                "VERIFICATION CODE FOR {}: {}\n" +
                "==================================================", to, code);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("CourShare - Xác nhận đăng ký tài khoản");
            message.setText("Mã xác thực đăng ký tài khoản CourShare của bạn là: " + code + "\nMã này có hiệu lực trong vòng 5 phút.");
            mailSender.send(message);
            logger.info("Sent verification email successfully to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}. If you are in local development, please use the OTP printed in logs above.", to, e.getMessage());
        }
    }
}
