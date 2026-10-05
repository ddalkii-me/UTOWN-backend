ALTER TABLE restaurant
ADD COLUMN average_rating DECIMAL(3,2) DEFAULT 0.00,
ADD COLUMN delivery_time_minutes INT DEFAULT 30;
