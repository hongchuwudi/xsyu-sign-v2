UPDATE `email_notification_task`
SET `status` = 'CANCELLED',
    `enabled` = 0,
    `updated_at` = CURRENT_TIMESTAMP
WHERE `status` = 'PENDING'
  AND `enabled` = 1
  AND `scheduled_at` IS NOT NULL;
