INSERT INTO users (username, password)
VALUES ('admin', '$2a$12$ImLsrqJFXzty2d22bI/EKO5LreopF2LfLs4bCHhobhlbdFtN2JJMO')
ON CONFLICT (username) DO NOTHING;