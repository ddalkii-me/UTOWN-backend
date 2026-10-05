UPDATE restaurant r
SET r.average_rating = (
    SELECT COALESCE(AVG(rt.score), 0)
    FROM rating rt
    WHERE rt.restaurant_id = r.id AND rt.deleted_at IS NULL
);
