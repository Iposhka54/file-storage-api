ALTER TABLE user_action_audit
ADD COLUMN action_type INTEGER;

-- Обновляем существующие записи: устанавливаем action_type на основе action (если нужно)
-- Для новых записей action_type будет заполняться автоматически

