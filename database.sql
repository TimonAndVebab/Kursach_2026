-- Повторний імпорт повністю замінює вміст навчальної бази kpp25_approval_demo.
DROP DATABASE IF EXISTS kpp25_approval_demo;
CREATE DATABASE kpp25_approval_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kpp25_approval_demo;
SET NAMES utf8mb4;

CREATE TABLE author (
    id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE reviewer (
    id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE document (
    id INT AUTO_INCREMENT PRIMARY KEY,
    registration_number VARCHAR(40) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    author_id INT NOT NULL,
    criticality_score INT NOT NULL CHECK (criticality_score BETWEEN 0 AND 100),
    dependent_process_count INT NOT NULL CHECK (dependent_process_count >= 0),
    FOREIGN KEY (author_id) REFERENCES author(id)
) ENGINE=InnoDB;

CREATE TABLE approval_request (
    id INT AUTO_INCREMENT PRIMARY KEY,
    document_id INT NOT NULL,
    reviewer_id INT NOT NULL,
    created_on DATE NOT NULL,
    due_on DATE NOT NULL,
    status ENUM('DRAFT','SUBMITTED','IN_REVIEW','APPROVED') NOT NULL DEFAULT 'DRAFT',
    CHECK (due_on >= created_on),
    FOREIGN KEY (document_id) REFERENCES document(id),
    FOREIGN KEY (reviewer_id) REFERENCES reviewer(id)
) ENGINE=InnoDB;

INSERT INTO author (full_name) VALUES ('Ірина Савчук'), ('Богдан Коваль');
INSERT INTO reviewer (full_name) VALUES ('Олена Кравець'), ('Андрій Мельник');
INSERT INTO document (registration_number, title, author_id, criticality_score, dependent_process_count)
VALUES ('ВН-001', 'План закупівель', 1, 80, 3),
       ('ВН-002', 'Графік інвентаризації', 2, 40, 1);
-- Перші два запити стосуються ВН-001. Так видно, хто вже розглядає документ, а хто ще має його взяти.
INSERT INTO approval_request (document_id, reviewer_id, created_on, due_on, status)
VALUES (1, 1, '2026-11-18', '2026-11-20', 'IN_REVIEW'),
       (1, 2, '2026-11-18', '2026-11-24', 'SUBMITTED'),
       (2, 1, '2026-11-18', '2026-11-25', 'DRAFT');
