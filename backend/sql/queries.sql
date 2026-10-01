SELECT component_id, location_id, quantity
FROM inventory;

SELECT c.name AS component, l.name AS location, i.quantity
FROM inventory AS i
JOIN components AS c ON i.component_id = c.id
JOIN locations AS l ON i.location_id = l.id;

SELECT
    c.id,
    c.name,
    c.part_number,
    b.name AS brand,
    cat.name AS category,
    c.description,
    c.power_requirements,
    c.documentation_url,
    (
        SELECT STRING_AGG(
            CONCAT_WS(', ', p.name, l.name)
            || ': ' || COALESCE(i.quantity::TEXT, i.stock_level),
            '; ' ORDER BY l.id
        )
        FROM inventory AS i
        JOIN locations AS l ON i.location_id = l.id
        LEFT JOIN locations AS p ON l.parent_id = p.id
        WHERE i.component_id = c.id
    ) AS stock,
    (
        SELECT STRING_AGG(t.name, ', ' ORDER BY t.name)
        FROM component_tags AS ct
        JOIN tags AS t ON ct.tag_id = t.id
        WHERE ct.component_id = c.id
    ) AS tags,
    (
        SELECT STRING_AGG(a.alias, ', ' ORDER BY a.alias)
        FROM aliases AS a
        WHERE a.component_id = c.id
    ) AS aliases,
    (
        SELECT STRING_AGG(
            s.name || ': ' || sl.order_url,
            E'\n' ORDER BY s.name
        )
        FROM supplier_listings AS sl
        JOIN suppliers AS s ON sl.supplier_id = s.id
        WHERE sl.component_id = c.id
    ) AS order_links
FROM components AS c
LEFT JOIN brands AS b ON c.brand_id = b.id
LEFT JOIN categories AS cat ON c.category_id = cat.id
WHERE c.id = 1;

-- RESULT
-- -[ RECORD 1 ]------+--------------------------------------------------------------------------------------------------
-- id                 | 1
-- name               | ESP32-DevKitC V4
-- part_number        | ESP32-DEVKITC-32UE
-- brand              | Espressif
-- category           | ESP32
-- description        | Development board for ESP32 microcontroller
-- power_requirements | 5V via USB
-- documentation_url  | https://documentation.espressif.com/esp32-wroom-32e_esp32-wroom-32ue_datasheet_en.pdf
-- stock              | Front Stock, Bin 4: 2
-- tags               | Bluetooth, ESP32, Micro-USB, Microcontroller, Wi-Fi
-- aliases            | ESP32-DEVKITC-32UE, ESP32-DevKitC V4, ESP32 DevKit, ESP32 DevKitC                                                           
-- order_links        | Digikey: https://www.digikey.com/en/products/detail/espressif-systems/ESP32-DEVKITC-32UE/12091813                           
