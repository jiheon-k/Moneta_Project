-- 공용 카테고리: user_id가 NULL인 항목.
-- 같은 이름의 공용 카테고리가 있으면 추가하지 않는다.
-- 개인 카테고리는 변경하지 않는다. 대상 DB를 확인한 후 수동 실행한다.
START TRANSACTION;

INSERT INTO category (name, user_id)
SELECT defaults.name, NULL
FROM (
    SELECT '식비' AS name
    UNION ALL SELECT '카페·간식'
    UNION ALL SELECT '교통'
    UNION ALL SELECT '주거·공과금'
    UNION ALL SELECT '생활용품'
    UNION ALL SELECT '쇼핑'
    UNION ALL SELECT '의료·건강'
    UNION ALL SELECT '문화·여가'
    UNION ALL SELECT '교육'
    UNION ALL SELECT '기타'
) AS defaults
WHERE NOT EXISTS (
    SELECT 1 FROM category AS existing
    WHERE existing.user_id IS NULL AND existing.name = defaults.name
);

SELECT ROW_COUNT() AS added_count;

COMMIT;

SELECT id, name, user_id FROM category WHERE user_id IS NULL ORDER BY id;
