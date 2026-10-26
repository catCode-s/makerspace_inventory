-- make account
INSERT INTO users (display_name, email, password_hash)
VALUES ($1, LOWER(BTRIM($2)), $3)
ON CONFLICT DO NOTHING
RETURNING id;