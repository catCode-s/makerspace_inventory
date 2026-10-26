-- search componets names, part numbers, aliases, and tags
-- and return one row per stock location
SELECT
    c.id,
    c.name,
    c.description,
    c.part_number,
    c.requires_checkout,
    c.power_requirements,
    c.documentation_url,
    b.name AS brand,
    cat.name AS category,
    i.id AS inventory_id,
    i.quantity,
    i.stock_level,
    l.id AS location_id,
    CONCAT_WS(', ', p.name, l.name) AS location
FROM components AS c
LEFT JOIN brands AS b ON c.brand_id = b.id
LEFT JOIN categories AS cat
    ON c.category_id = cat.id
LEFT JOIN inventory AS i ON i.component_id = c.id
LEFT JOIN locations AS l ON i.location_id = l.id
LEFT JOIN locations AS p ON l.parent_id = p.id
WHERE c.name ILIKE $1
    OR c.part_number ILIKE $1
    OR cat.name ILIKE $1
    OR EXISTS (
        SELECT 1
        FROM aliases AS a
        WHERE a.component_id = c.id
            AND a.alias ILIKE $1      
    )
    OR EXISTS (
        SELECT 1
        FROM component_tags AS ct
        JOIN tags AS t ON ct.tag_id = t.id
        WHERE ct.component_id = c.id
            AND t.name ILIKE $1
    )
ORDER BY c.name, c.id, l.id