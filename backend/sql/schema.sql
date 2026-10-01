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
    documentation_url TEXT
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

CREATE TABLE component_tags (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    component_id BIGINT NOT NULL REFERENCES components(id),
    tag_id BIGINT NOT NULL REFERENCES tags(id),
    UNIQUE (component_id, tag_id)
);

CREATE INDEX idx_component_tags_tag_id_component_id ON component_tags (tag_id, component_id);

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