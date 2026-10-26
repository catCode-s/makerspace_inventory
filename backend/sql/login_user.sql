-- find user and return their details
SELECT id, display_name, email, role, password_hash
FROM users
WHERE LOWER(BTRIM(email)) = LOWER(BTRIM($1));
