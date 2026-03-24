package com.example.myapplication.utils;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Email Sender - Gửi email qua SMTP
 * 
 * QUAN TRỌNG: Để sử dụng Gmail SMTP, bạn cần:
 * 1. Bật 2-Step Verification trong Google Account
 * 2. Tạo App Password tại: https://myaccount.google.com/apppasswords
 * 3. Thay thế SENDER_EMAIL và SENDER_PASSWORD bên dưới
 * 
 * Hoặc sử dụng email service khác như SendGrid, Mailgun, AWS SES
 */
public class EmailSender {

    private static final String TAG = "EmailSender";
    
    // ⚠️ THAY ĐỔI THÔNG TIN NÀY
    // Tạo App Password: Google Account → Security → 2-Step Verification → App passwords
    private static final String SENDER_EMAIL = "your.email@gmail.com";  // Email của bạn
    private static final String SENDER_PASSWORD = "your-app-password";   // App Password (16 ký tự)
    
    // SMTP Configuration for Gmail
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    public interface EmailCallback {
        void onSuccess();
        void onError(String error);
    }

    /**
     * Gửi email OTP
     */
    public static void sendOTPEmail(String recipientEmail, String otp, EmailCallback callback) {
        new SendEmailTask(recipientEmail, otp, callback).execute();
    }

    private static class SendEmailTask extends AsyncTask<Void, Void, Boolean> {
        private final String recipientEmail;
        private final String otp;
        private final EmailCallback callback;
        private String errorMessage;

        SendEmailTask(String recipientEmail, String otp, EmailCallback callback) {
            this.recipientEmail = recipientEmail;
            this.otp = otp;
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                // Cấu hình SMTP properties
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_PORT);
                props.put("mail.smtp.ssl.protocols", "TLSv1.2");

                // Tạo session với authentication
                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                // Tạo email message
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL, "TDT Bank"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                message.setSubject("🔐 Mã OTP xác thực - TDT Bank");
                
                // HTML content
                String htmlContent = buildEmailContent(otp);
                message.setContent(htmlContent, "text/html; charset=utf-8");

                // Gửi email
                Transport.send(message);
                
                Log.d(TAG, "Email sent successfully to: " + recipientEmail);
                return true;

            } catch (MessagingException e) {
                Log.e(TAG, "Failed to send email: " + e.getMessage());
                errorMessage = e.getMessage();
                return false;
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
                errorMessage = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                callback.onSuccess();
            } else {
                callback.onError(errorMessage != null ? errorMessage : "Không thể gửi email");
            }
        }

        private String buildEmailContent(String otp) {
            return "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                    "<meta charset='UTF-8'>" +
                    "<style>" +
                    "body { font-family: Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }" +
                    ".container { max-width: 500px; margin: 0 auto; background: white; border-radius: 16px; padding: 30px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }" +
                    ".header { text-align: center; margin-bottom: 30px; }" +
                    ".logo { font-size: 28px; font-weight: bold; color: #1E88E5; }" +
                    ".otp-box { background: linear-gradient(135deg, #1E88E5, #1565C0); border-radius: 12px; padding: 25px; text-align: center; margin: 20px 0; }" +
                    ".otp-code { font-size: 36px; font-weight: bold; color: white; letter-spacing: 8px; }" +
                    ".message { color: #666; line-height: 1.6; }" +
                    ".warning { background: #FFF3E0; border-left: 4px solid #FF9800; padding: 12px; margin-top: 20px; border-radius: 4px; }" +
                    ".footer { text-align: center; margin-top: 30px; color: #999; font-size: 12px; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='container'>" +
                    "<div class='header'>" +
                    "<div class='logo'>🏦 TDT Bank</div>" +
                    "<p style='color: #666;'>Xác thực giao dịch</p>" +
                    "</div>" +
                    "<p class='message'>Xin chào,</p>" +
                    "<p class='message'>Bạn vừa yêu cầu mã OTP để xác thực giao dịch tại TDT Bank. Vui lòng sử dụng mã dưới đây:</p>" +
                    "<div class='otp-box'>" +
                    "<div class='otp-code'>" + otp + "</div>" +
                    "</div>" +
                    "<p class='message'>Mã OTP có hiệu lực trong <strong>5 phút</strong>.</p>" +
                    "<div class='warning'>" +
                    "⚠️ <strong>Lưu ý:</strong> Không chia sẻ mã này với bất kỳ ai. Nhân viên TDT Bank không bao giờ yêu cầu mã OTP của bạn." +
                    "</div>" +
                    "<div class='footer'>" +
                    "<p>Email này được gửi tự động, vui lòng không trả lời.</p>" +
                    "<p>© 2024 TDT Bank - Mobile Banking</p>" +
                    "</div>" +
                    "</div>" +
                    "</body>" +
                    "</html>";
        }
    }

    /**
     * Kiểm tra email configuration
     */
    public static boolean isConfigured() {
        return !SENDER_EMAIL.equals("your.email@gmail.com") && 
               !SENDER_PASSWORD.equals("your-app-password");
    }
}
