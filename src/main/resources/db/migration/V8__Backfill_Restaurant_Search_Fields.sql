UPDATE restaurant r
SET r.average_rating = (
    SELECT COALESCE(AVG(rt.score), 0)
    FROM ratings rt
    WHERE rt.restaurant_id = r.id AND rt.deleted_at IS NULL
);
