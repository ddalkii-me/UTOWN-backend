ALTER TABLE ratings
    ADD COLUMN deleted_at DATETIME(6) NULL;

ALTER TABLE ratings
DROP FOREIGN KEY FKrgl02m10esvjulx3lfwlu993m;

ALTER TABLE ratings
DROP COLUMN dish_id;