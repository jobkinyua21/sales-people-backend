-- Seed message templates (only insert if not already present)

INSERT INTO pos_core.message_template (mst_name, mst_description, mst_email, mst_sms, mst_subject, mst_type, mst_status, mst_content, created_at, updated_at)
SELECT 'Login OTP', 'OTP sent during login for verification',
'<html><body style="font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;">
<div style="max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
<div style="background-color: #2563eb; padding: 30px; text-align: center;">
<h1 style="color: #ffffff; margin: 0; font-size: 24px;">POS Management System</h1>
</div>
<div style="padding: 30px;">
<h2 style="color: #333333; margin-top: 0;">Hello #{USER_FIRST_NAME},</h2>
<p style="color: #555555; font-size: 16px; line-height: 1.5;">Your one-time verification code is:</p>
<div style="text-align: center; margin: 30px 0;">
<span style="font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #2563eb; background-color: #eff6ff; padding: 15px 30px; border-radius: 8px; border: 2px dashed #2563eb;">#{OTP_CODE}</span>
</div>
<p style="color: #555555; font-size: 14px; line-height: 1.5;">This code expires in <strong>#{EXPIRY_MINUTES} minutes</strong>. Do not share this code with anyone.</p>
<p style="color: #999999; font-size: 12px; margin-top: 30px;">If you did not request this code, please ignore this email.</p>
</div>
<div style="background-color: #f8f9fa; padding: 15px; text-align: center;">
<p style="color: #999999; font-size: 11px; margin: 0;">POS Management System. All rights reserved.</p>
</div>
</div>
</body></html>',
'Your OTP code is #{OTP_CODE}. It expires in #{EXPIRY_MINUTES} minutes.',
'Your Verification Code',
'LOGIN_OTP', 'ACTIVE', NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM pos_core.message_template WHERE mst_type = 'LOGIN_OTP');

INSERT INTO pos_core.message_template (mst_name, mst_description, mst_email, mst_sms, mst_subject, mst_type, mst_status, mst_content, created_at, updated_at)
SELECT 'Password Reset', 'OTP sent during forgot password flow',
'<html><body style="font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;">
<div style="max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
<div style="background-color: #2563eb; padding: 30px; text-align: center;">
<h1 style="color: #ffffff; margin: 0; font-size: 24px;">POS Management System</h1>
</div>
<div style="padding: 30px;">
<h2 style="color: #333333; margin-top: 0;">Hello #{USER_FIRST_NAME},</h2>
<p style="color: #555555; font-size: 16px; line-height: 1.5;">We received a request to reset your password. Your verification code is:</p>
<div style="text-align: center; margin: 30px 0;">
<span style="font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #2563eb; background-color: #eff6ff; padding: 15px 30px; border-radius: 8px; border: 2px dashed #2563eb;">#{OTP_CODE}</span>
</div>
<p style="color: #555555; font-size: 14px; line-height: 1.5;">This code expires in <strong>#{EXPIRY_MINUTES} minutes</strong>.</p>
<p style="color: #999999; font-size: 12px; margin-top: 30px;">If you did not request a password reset, please ignore this email.</p>
</div>
<div style="background-color: #f8f9fa; padding: 15px; text-align: center;">
<p style="color: #999999; font-size: 11px; margin: 0;">POS Management System. All rights reserved.</p>
</div>
</div>
</body></html>',
'Your password reset code is #{OTP_CODE}. It expires in #{EXPIRY_MINUTES} minutes.',
'Password Reset - Verification Code',
'PASSWORD_RESET', 'ACTIVE', NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM pos_core.message_template WHERE mst_type = 'PASSWORD_RESET');

 