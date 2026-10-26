-- categories can nest: Input devices -> Sensors -> Distance sensors
CREATE TABLE categories (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL,
    parent_id BIGINT REFERENCES categories(id) CHECK (parent_id <> id),
    UNIQUE NULLS NOT DISTINCT (parent_id, name)
);

CREATE TABLE brands (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT UNIQUE NOT NULL,
    web_url TEXT
);

CREATE TABLE components (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    category_id BIGINT REFERENCES categories(id),
    brand_id BIGINT REFERENCES brands(id),
    part_number TEXT,
    power_requirements TEXT,
    documentation_url TEXT,
    low_stock_threshold INT CHECK (low_stock_threshold >= 0),
    -- does this component require a formal checkout to be used? (e.g. tools, wires)
    requires_checkout BOOLEAN NOT NULL DEFAULT TRUE
);

-- locations can nest: Front Stock -> Bin 1
CREATE TABLE locations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    parent_id BIGINT REFERENCES locations(id) CHECK (parent_id <> id),
    UNIQUE NULLS NOT DISTINCT (parent_id, name)
);

-- inventory of components, tracks either quantity or stock level (many, some, few, none)
CREATE TABLE inventory (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    component_id BIGINT NOT NULL REFERENCES components(id),
    location_id BIGINT NOT NULL REFERENCES locations(id),
    quantity INTEGER CHECK (quantity >= 0),
    stock_level TEXT CHECK (stock_level IN ('many', 'some', 'few', 'none')),
    UNIQUE (component_id, location_id),
    CHECK (
        (quantity IS NOT NULL AND stock_level IS NULL)
        OR
        (quantity IS NULL AND stock_level IS NOT NULL)
    )
);

CREATE TABLE aliases (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    component_id BIGINT NOT NULL REFERENCES components(id),
    alias TEXT NOT NULL,
    UNIQUE (component_id, alias)
);
 
CREATE TABLE tags (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

-- associates components with tags, allowing for many-to-many relationships
CREATE TABLE component_tags (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    component_id BIGINT NOT NULL REFERENCES components(id),
    tag_id BIGINT NOT NULL REFERENCES tags(id),
    UNIQUE (component_id, tag_id)
);

CREATE INDEX idx_component_tags_tag_id_component_id ON component_tags (tag_id, component_id);

-- tracks components that depend on other components 
CREATE TABLE dependencies (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    component_id BIGINT NOT NULL REFERENCES components(id),
    depends_on_component_id BIGINT NOT NULL REFERENCES components(id),
    CHECK (component_id <> depends_on_component_id),
    UNIQUE (component_id, depends_on_component_id)
);

-- suppliers and their listings, each component can have multiple supplier listings
CREATE TABLE suppliers (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT UNIQUE NOT NULL,
    web_url TEXT
);

CREATE TABLE supplier_listings (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    component_id BIGINT NOT NULL REFERENCES components(id),
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    supplier_part_number TEXT,
    order_url TEXT NOT NULL,
    UNIQUE (component_id, supplier_id)
);

CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    display_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'user'  
        CHECK (role IN ('admin', 'staff', 'user')),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE checkouts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    borrower_id BIGINT NOT NULL REFERENCES users(id),
    inventory_id BIGINT NOT NULL REFERENCES inventory(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    checked_out_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- tracks returns against checkouts, allowing for partial returns
CREATE TABLE checkout_returns (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    checkout_id BIGINT NOT NULL REFERENCES checkouts(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    returned_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- tracks inventory changes, either by quantity or stock level
CREATE TABLE stock_adjustments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inventory_id BIGINT NOT NULL REFERENCES inventory(id),
    performed_by BIGINT NOT NULL REFERENCES users(id),
    quantity_change INT CHECK (quantity_change <> 0),
    old_stock_level TEXT CHECK (old_stock_level IN ('many', 'some', 'few', 'none')),
    new_stock_level TEXT CHECK (new_stock_level IN ('many', 'some', 'few', 'none')),
    reason TEXT NOT NULL,
    adjusted_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CHECK (
        (
            quantity_change IS NOT NULL
            AND old_stock_level IS NULL
            AND new_stock_level IS NULL
        )
        OR
        (
            quantity_change IS NULL
            AND old_stock_level IS NOT NULL
            AND new_stock_level IS NOT NULL
            AND old_stock_level <> new_stock_level
        )
    )
);

CREATE UNIQUE INDEX users_email_normalized_unique
ON users (LOWER(BTRIM(email)));