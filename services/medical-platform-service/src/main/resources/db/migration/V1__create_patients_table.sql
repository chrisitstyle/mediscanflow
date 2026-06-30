CREATE TABLE patients (

id UUID PRIMARY KEY,
first_name VARCHAR(100) NOT NULL,
last_name VARCHAR(100) NOT NULL,
date_of_birth DATE NOT NULL,
medical_record_number VARCHAR(50) NOT NULL UNIQUE,
created_at TIMESTAMP NOT NULL
);