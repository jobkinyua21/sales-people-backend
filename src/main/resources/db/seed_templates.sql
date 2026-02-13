-- Login OTP Template
INSERT INTO pos_core.message_template (mst_name, mst_description, mst_email, mst_sms, mst_subject, mst_type, mst_status, mst_content, created_at, updated_at)
VALUES (
    'Login OTP',
    'OTP verification email sent during login',
    '<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin:0;padding:0;background-color:#f4f7fa;font-family:''Segoe UI'',Tahoma,Geneva,Verdana,sans-serif;">
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#f4f7fa;padding:40px 0;">
        <tr>
            <td align="center">
                <table role="presentation" width="480" cellspacing="0" cellpadding="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.08);">
                    <!-- Header -->
                    <tr>
                        <td style="background:linear-gradient(135deg,#1a73e8,#0d47a1);padding:32px 40px;text-align:center;">
                            <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:600;">POS System</h1>
                        </td>
                    </tr>
                    <!-- Body -->
                    <tr>
                        <td style="padding:40px;">
                            <p style="margin:0 0 16px;color:#333333;font-size:16px;">Hi <strong>#{USER_FIRST_NAME}</strong>,</p>
                            <p style="margin:0 0 24px;color:#555555;font-size:15px;line-height:1.6;">You''re trying to sign in to your account. Use the verification code below to complete your login:</p>
                            <!-- OTP Box -->
                            <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td align="center" style="padding:8px 0 24px;">
                                        <div style="display:inline-block;background-color:#f0f4ff;border:2px dashed #1a73e8;border-radius:12px;padding:20px 48px;">
                                            <span style="font-size:36px;font-weight:700;letter-spacing:12px;color:#1a73e8;font-family:''Courier New'',monospace;">#{OTP_CODE}</span>
                                        </div>
                                    </td>
                                </tr>
                            </table>
                            <p style="margin:0 0 8px;color:#555555;font-size:14px;line-height:1.6;">This code expires in <strong>#{EXPIRY_MINUTES} minutes</strong>.</p>
                            <p style="margin:0 0 24px;color:#888888;font-size:13px;line-height:1.6;">If you didn''t request this code, you can safely ignore this email. Someone may have entered your email address by mistake.</p>
                            <!-- Divider -->
                            <hr style="border:none;border-top:1px solid #e8e8e8;margin:24px 0;">
                            <p style="margin:0;color:#aaaaaa;font-size:12px;line-height:1.5;">For security, never share this code with anyone. Our team will never ask you for this code.</p>
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td style="background-color:#f8f9fa;padding:24px 40px;text-align:center;border-top:1px solid #eeeeee;">
                            <p style="margin:0;color:#999999;font-size:12px;">&copy; 2026 POS System. All rights reserved.</p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>',
    'Your login OTP is #{OTP_CODE}. It expires in #{EXPIRY_MINUTES} minutes. Do not share this code with anyone.',
    'Your POS System Login Code',
    'LOGIN_OTP',
    'ACTIVE',
    NULL,
    NOW(),
    NOW()
)
ON CONFLICT (mst_type) DO NOTHING;

-- Password Reset Template
INSERT INTO pos_core.message_template (mst_name, mst_description, mst_email, mst_sms, mst_subject, mst_type, mst_status, mst_content, created_at, updated_at)
VALUES (
    'Password Reset',
    'Password reset email with token/link',
    '<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin:0;padding:0;background-color:#f4f7fa;font-family:''Segoe UI'',Tahoma,Geneva,Verdana,sans-serif;">
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#f4f7fa;padding:40px 0;">
        <tr>
            <td align="center">
                <table role="presentation" width="480" cellspacing="0" cellpadding="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.08);">
                    <!-- Header -->
                    <tr>
                        <td style="background:linear-gradient(135deg,#1a73e8,#0d47a1);padding:32px 40px;text-align:center;">
                            <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:600;">POS System</h1>
                        </td>
                    </tr>
                    <!-- Body -->
                    <tr>
                        <td style="padding:40px;">
                            <p style="margin:0 0 16px;color:#333333;font-size:16px;">Hi <strong>#{USER_FIRST_NAME}</strong>,</p>
                            <p style="margin:0 0 24px;color:#555555;font-size:15px;line-height:1.6;">We received a request to reset your password. Click the button below to set a new password:</p>
                            <!-- Button -->
                            <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td align="center" style="padding:8px 0 24px;">
                                        <a href="#{RESET_LINK}" style="display:inline-block;background-color:#1a73e8;color:#ffffff;text-decoration:none;padding:14px 40px;border-radius:8px;font-size:16px;font-weight:600;">Reset Password</a>
                                    </td>
                                </tr>
                            </table>
                            <p style="margin:0 0 8px;color:#555555;font-size:14px;line-height:1.6;">This link expires in <strong>60 minutes</strong>.</p>
                            <p style="margin:0 0 24px;color:#888888;font-size:13px;line-height:1.6;">If you didn''t request a password reset, you can safely ignore this email. Your password will remain unchanged.</p>
                            <!-- Divider -->
                            <hr style="border:none;border-top:1px solid #e8e8e8;margin:24px 0;">
                            <p style="margin:0 0 8px;color:#aaaaaa;font-size:12px;line-height:1.5;">If the button doesn''t work, copy and paste this link into your browser:</p>
                            <p style="margin:0;color:#1a73e8;font-size:12px;word-break:break-all;">#{RESET_LINK}</p>
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td style="background-color:#f8f9fa;padding:24px 40px;text-align:center;border-top:1px solid #eeeeee;">
                            <p style="margin:0;color:#999999;font-size:12px;">&copy; 2026 POS System. All rights reserved.</p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>',
    'Reset your POS System password. This link expires in 60 minutes.',
    'Reset Your Password',
    'PASSWORD_RESET',
    'ACTIVE',
    NULL,
    NOW(),
    NOW()
)
ON CONFLICT (mst_type) DO NOTHING;

-- Email Verification Template
INSERT INTO pos_core.message_template (mst_name, mst_description, mst_email, mst_sms, mst_subject, mst_type, mst_status, mst_content, created_at, updated_at)
VALUES (
    'Email Verification',
    'Email verification for new account registration',
    '<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin:0;padding:0;background-color:#f4f7fa;font-family:''Segoe UI'',Tahoma,Geneva,Verdana,sans-serif;">
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#f4f7fa;padding:40px 0;">
        <tr>
            <td align="center">
                <table role="presentation" width="480" cellspacing="0" cellpadding="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.08);">
                    <!-- Header -->
                    <tr>
                        <td style="background:linear-gradient(135deg,#1a73e8,#0d47a1);padding:32px 40px;text-align:center;">
                            <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:600;">POS System</h1>
                        </td>
                    </tr>
                    <!-- Body -->
                    <tr>
                        <td style="padding:40px;">
                            <p style="margin:0 0 16px;color:#333333;font-size:16px;">Hi <strong>#{USER_FIRST_NAME}</strong>,</p>
                            <p style="margin:0 0 24px;color:#555555;font-size:15px;line-height:1.6;">Welcome to POS System! Please verify your email address to activate your account:</p>
                            <!-- Button -->
                            <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td align="center" style="padding:8px 0 24px;">
                                        <a href="#{VERIFICATION_LINK}" style="display:inline-block;background-color:#34a853;color:#ffffff;text-decoration:none;padding:14px 40px;border-radius:8px;font-size:16px;font-weight:600;">Verify Email</a>
                                    </td>
                                </tr>
                            </table>
                            <p style="margin:0 0 8px;color:#555555;font-size:14px;line-height:1.6;">This link expires in <strong>24 hours</strong>.</p>
                            <p style="margin:0 0 24px;color:#888888;font-size:13px;line-height:1.6;">If you didn''t create an account, you can safely ignore this email.</p>
                            <!-- Divider -->
                            <hr style="border:none;border-top:1px solid #e8e8e8;margin:24px 0;">
                            <p style="margin:0 0 8px;color:#aaaaaa;font-size:12px;line-height:1.5;">If the button doesn''t work, copy and paste this link into your browser:</p>
                            <p style="margin:0;color:#1a73e8;font-size:12px;word-break:break-all;">#{VERIFICATION_LINK}</p>
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td style="background-color:#f8f9fa;padding:24px 40px;text-align:center;border-top:1px solid #eeeeee;">
                            <p style="margin:0;color:#999999;font-size:12px;">&copy; 2026 POS System. All rights reserved.</p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>',
    'Verify your email to activate your POS System account.',
    'Verify Your Email Address',
    'EMAIL_VERIFICATION',
    'ACTIVE',
    NULL,
    NOW(),
    NOW()
)
ON CONFLICT (mst_type) DO NOTHING;

-- Welcome Template
INSERT INTO pos_core.message_template (mst_name, mst_description, mst_email, mst_sms, mst_subject, mst_type, mst_status, mst_content, created_at, updated_at)
VALUES (
    'Welcome',
    'Welcome email sent after account activation',
    '<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin:0;padding:0;background-color:#f4f7fa;font-family:''Segoe UI'',Tahoma,Geneva,Verdana,sans-serif;">
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#f4f7fa;padding:40px 0;">
        <tr>
            <td align="center">
                <table role="presentation" width="480" cellspacing="0" cellpadding="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.08);">
                    <!-- Header -->
                    <tr>
                        <td style="background:linear-gradient(135deg,#1a73e8,#0d47a1);padding:32px 40px;text-align:center;">
                            <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:600;">POS System</h1>
                        </td>
                    </tr>
                    <!-- Body -->
                    <tr>
                        <td style="padding:40px;">
                            <p style="margin:0 0 16px;color:#333333;font-size:16px;">Hi <strong>#{USER_FIRST_NAME}</strong>,</p>
                            <p style="margin:0 0 16px;color:#555555;font-size:15px;line-height:1.6;">Welcome to POS System! Your account has been successfully activated.</p>
                            <p style="margin:0 0 24px;color:#555555;font-size:15px;line-height:1.6;">You can now log in and start managing your business. Here are some things to get started:</p>
                            <ul style="margin:0 0 24px;padding-left:20px;color:#555555;font-size:14px;line-height:2;">
                                <li>Set up your shop details</li>
                                <li>Add your products and inventory</li>
                                <li>Invite your team members</li>
                                <li>Configure payment methods</li>
                            </ul>
                            <!-- Button -->
                            <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td align="center" style="padding:8px 0 24px;">
                                        <a href="#{LOGIN_LINK}" style="display:inline-block;background-color:#1a73e8;color:#ffffff;text-decoration:none;padding:14px 40px;border-radius:8px;font-size:16px;font-weight:600;">Go to Dashboard</a>
                                    </td>
                                </tr>
                            </table>
                            <p style="margin:0;color:#888888;font-size:13px;line-height:1.6;">Need help? Reply to this email or visit our support center.</p>
                        </td>
                    </tr>
                    <!-- Footer -->
                    <tr>
                        <td style="background-color:#f8f9fa;padding:24px 40px;text-align:center;border-top:1px solid #eeeeee;">
                            <p style="margin:0;color:#999999;font-size:12px;">&copy; 2026 POS System. All rights reserved.</p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>',
    'Welcome to POS System, #{USER_FIRST_NAME}! Your account is now active.',
    'Welcome to POS System!',
    'WELCOME',
    'ACTIVE',
    NULL,
    NOW(),
    NOW()
)
ON CONFLICT (mst_type) DO NOTHING;
