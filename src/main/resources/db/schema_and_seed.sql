-- Drop if exists (for development/testing convenience)
DROP TABLE IF EXISTS note;
DROP TABLE IF EXISTS user_entity;

-- Create User Table
CREATE TABLE user_entity (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    access_code VARCHAR(100) UNIQUE NOT NULL
);

-- Create Note Table
CREATE TABLE note (
    id SERIAL PRIMARY KEY,
    message TEXT NOT NULL,
    user_id INTEGER REFERENCES user_entity(id) ON DELETE CASCADE
);

-- Insert test user
INSERT INTO user_entity (name, access_code)
VALUES ('Kavitha', 'kavi2025');

-- Insert test notes for the user
INSERT INTO note (message, user_id)
VALUES
('Two good days don’t disappear just because today feels hard.', 1),
('You’re still in the game — even if it’s a quiet round.', 1),
('Not showing up perfectly is still showing up.', 1);
