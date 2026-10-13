-- initial setups
INSERT INTO suppliers (name, web_url) 
VALUES
('Grainger', 'https://www.grainger.com'),
('Digikey', 'https://www.digikey.com'),
('Adafruit', 'https://www.adafruit.com');

INSERT INTO categories(name)
VALUES ('Development Boards')
RETURNING id;

INSERT INTO locations (name, description)
VALUES ('Front Stock', 'Bins near Help Desk')
RETURNING id;

INSERT INTO locations (name, description, parent_id)
VALUES ('Bin 1', 'First bin from the left at Front Stock', 1),
('Bin 2', 'Second bin from the left at Front Stock', 1), 
('Bin 3', 'Third bin from the left at Front Stock', 1),
('Bin 4', 'Fourth bin from the left at Front Stock', 1)
RETURNING id;

INSERT INTO locations (name, description)
VALUES ('Backstock', 'Storage area for excess inventory')
RETURNING id;

INSERT INTO locations (name, description, parent_id)
VALUES ('Bin Shelf', 'Shelf under bins at Frontstock', 6),
       ('Shadow Realm', 'Storage closet near tool wall', 6)
RETURNING id;

INSERT INTO tags (name)
VALUES ('Microcontroller'),
       ('ESP32'),
       ('Wi-Fi'),
       ('Bluetooth'),
       ('Micro-USB')
RETURNING id, name;

-- sample data for a single component (ESP32-DevKitC V4)
INSERT INTO categories (name)
VALUES ('Microcontrollers')
RETURNING id;

INSERT INTO categories(name, parent_id)
VALUES ('ESP32', 2)
RETURNING id;

INSERT INTO brands (name, web_url)
VALUES ('Espressif', 'https://www.espressif.com')
RETURNING id;

INSERT INTO components (name, description, category_id, brand_id, part_number, power_requirements, documentation_url)
VALUES ('ESP32-DevKitC V4', 'Development board for ESP32 microcontroller', 3, 1, 'ESP32-DEVKITC-32UE', '5V via USB', 'https://documentation.espressif.com/esp32-wroom-32e_esp32-wroom-32ue_datasheet_en.pdf')
RETURNING id;

INSERT INTO inventory (component_id, location_id, quantity, stock_level)
VALUES (1, 5, 2, NULL)
RETURNING id;

INSERT INTO supplier_listings (component_id, supplier_id, supplier_part_number, order_url)
VALUES (1, 2, '1965-ESP32-DEVKITC-32UE-ND', 'https://www.digikey.com/en/products/detail/espressif-systems/ESP32-DEVKITC-32UE/12091813')
RETURNING id;

INSERT INTO component_tags (component_id, tag_id)
VALUES (1, 1),
       (1, 2),
       (1, 3),
       (1, 4),
       (1, 5)
RETURNING id;

INSERT INTO aliases (component_id, alias)
VALUES (1, 'ESP32-DevKitC V4'),
       (1, 'ESP32-DEVKITC-32UE'),
       (1, 'ESP32 DevKit'),
       (1, 'ESP32 DevKitC')
RETURNING id;