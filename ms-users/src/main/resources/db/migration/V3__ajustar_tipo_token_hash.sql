-- El hash SHA-256 en hexadecimal ocupa siempre 64 caracteres; VARCHAR coincide con la entidad RefreshToken.
ALTER TABLE refresh_tokens
    ALTER COLUMN token_hash TYPE VARCHAR(64);
