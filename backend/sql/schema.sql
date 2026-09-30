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
    part_number TEXT,
    brand_id BIGINT REFERENCES brands(id),
    category_id BIGINT REFERENCES categories(id),
    description TEXT,
    documentation_url TEXT
);

CREATE TABLE locations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    parent_id BIGINT REFERENCES locations(id) CHECK (parent_id <> id),
    UNIQUE NULLS NOT DISTINCT (parent_id, name)
);

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