package com.finance.tracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Sends a password reset email with a secure link containing the token.
     *
     * @param toEmail   recipient email
     * @param userName  recipient's display name
     * @param token     UUID reset token
     */
    public void sendPasswordResetEmail(String toEmail, String userName, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>Reset Your Password</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:'Segoe UI',Arial,sans-serif;">
                  <div style="max-width:600px;margin:40px auto;background:#ffffff;border-radius:16px;
                              overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);">
                    <!-- Header -->
                    <div style="background:linear-gradient(135deg,#8b5cf6,#d946ef);padding:36px 40px;text-align:center;">
                      <div style="width:64px;height:64px;background:rgba(255,255,255,0.2);border-radius:50%;
                                  display:inline-flex;align-items:center;justify-content:center;margin-bottom:16px;">
                        <span style="font-size:28px;">🔐</span>
                      </div>
                      <h1 style="color:#ffffff;margin:0;font-size:24px;font-weight:700;letter-spacing:-0.5px;">
                        Reset Your Password
                      </h1>
                      <p style="color:rgba(255,255,255,0.85);margin:8px 0 0;font-size:15px;">
                        FinTrack AI — Smart Finance Tracker
                      </p>
                    </div>
                    <!-- Body -->
                    <div style="padding:40px;">
                      <p style="color:#374151;font-size:16px;margin:0 0 8px;">
                        Hi <strong>%s</strong>,
                      </p>
                      <p style="color:#6b7280;font-size:15px;line-height:1.6;margin:0 0 28px;">
                        We received a request to reset the password for your FinTrack AI account.
                        Click the button below to set a new password. This link will expire in
                        <strong>15 minutes</strong>.
                      </p>
                      <!-- CTA Button -->
                      <div style="text-align:center;margin-bottom:32px;">
                        <a href="%s"
                           style="display:inline-block;background:linear-gradient(135deg,#8b5cf6,#d946ef);
                                  color:#ffffff;text-decoration:none;padding:16px 40px;
                                  border-radius:12px;font-size:16px;font-weight:700;
                                  letter-spacing:0.3px;box-shadow:0 4px 15px rgba(139,92,246,0.4);">
                          Reset Password
                        </a>
                      </div>
                      <!-- Security note -->
                      <div style="background:#fef3c7;border:1px solid #fde68a;border-radius:10px;
                                  padding:16px 20px;margin-bottom:28px;">
                        <p style="color:#92400e;font-size:13px;margin:0;line-height:1.5;">
                          ⚠️ <strong>Security notice:</strong> If you did not request a password reset,
                          please ignore this email. Your account remains secure.
                        </p>
                      </div>
                      <!-- Link fallback -->
                      <p style="color:#9ca3af;font-size:13px;margin:0 0 4px;">
                        Or copy and paste this link into your browser:
                      </p>
                      <p style="color:#8b5cf6;font-size:13px;word-break:break-all;margin:0;">
                        %s
                      </p>
                    </div>
                    <!-- Footer -->
                    <div style="background:#f9fafb;border-top:1px solid #f3f4f6;padding:24px 40px;text-align:center;">
                      <p style="color:#9ca3af;font-size:12px;margin:0;">
                        © 2025 FinTrack AI. All rights reserved.<br/>
                        This is an automated message — please do not reply.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(userName, resetLink, resetLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset Your FinTrack AI Password");
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            // Re-throw so the controller can return a 500 if needed
            throw new RuntimeException("Failed to send reset email. Please try again later.");
        }
    }
}
