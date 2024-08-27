-- oc port-forward $(oc get pod | grep integration-database  | awk '{print $1}') 5432:5432

ALTER SYSTEM SET wal_level = 'logical';


-- Step 1: Create Schemas
CREATE SCHEMA tenant_1;
CREATE SCHEMA tenant_2;
CREATE SCHEMA tenant_3;

-- Step 2: Create Users with passwords
CREATE USER tenant_1 WITH PASSWORD 'integration';
CREATE USER tenant_2 WITH PASSWORD 'integration';
CREATE USER tenant_3 WITH PASSWORD 'integration';

-- Step 3: Grant Schema Permissions to Users
GRANT ALL PRIVILEGES ON SCHEMA tenant_1 TO tenant_1;
GRANT ALL PRIVILEGES ON SCHEMA tenant_2 TO tenant_2;
GRANT ALL PRIVILEGES ON SCHEMA tenant_3 TO tenant_3;

-- Step 4: Create a Table in the Public Schema
CREATE TABLE public.identifiers (
                                    id SERIAL PRIMARY KEY,
                                    code VARCHAR(50) NOT NULL UNIQUE,
                                    name VARCHAR(255) NOT NULL,
                                    readable_code VARCHAR(50) NOT NULL,
                                    type VARCHAR(50) NOT NULL
);

-- Step 5: Create Tables in Tenant Schemas
-- Tenant 1 Schema
CREATE TABLE tenant_1.people (
                                 id SERIAL PRIMARY KEY,
                                 code VARCHAR(50) NOT NULL UNIQUE,
                                 first_name VARCHAR(100),
                                 last_name VARCHAR(100),
                                 gender VARCHAR(10),
                                 status VARCHAR(50)
);

CREATE TABLE tenant_1.addresses (
                                    id SERIAL PRIMARY KEY,
                                    code VARCHAR(50) NOT NULL UNIQUE,
                                    type VARCHAR(50),
                                    address_line_1 VARCHAR(255),
                                    address_line_2 VARCHAR(255),
                                    country VARCHAR(100)
);

CREATE TABLE tenant_1.contracts (
                                    id SERIAL PRIMARY KEY,
                                    code VARCHAR(50) NOT NULL UNIQUE,
                                    type VARCHAR(50),
                                    name VARCHAR(255),
                                    owner VARCHAR(50) REFERENCES tenant_1.people(code)
);

CREATE TABLE tenant_1.people_addresses (
                                           people_id INTEGER REFERENCES tenant_1.people(id),
                                           address_id INTEGER REFERENCES tenant_1.addresses(id),
                                           PRIMARY KEY (people_id, address_id)
);

-- Tenant 2 Schema
CREATE TABLE tenant_2.people (
                                 id SERIAL PRIMARY KEY,
                                 code VARCHAR(50) NOT NULL UNIQUE,
                                 first_name VARCHAR(100),
                                 last_name VARCHAR(100),
                                 gender VARCHAR(10),
                                 status VARCHAR(50)
);

CREATE TABLE tenant_2.addresses (
                                    id SERIAL PRIMARY KEY,
                                    code VARCHAR(50) NOT NULL UNIQUE,
                                    type VARCHAR(50),
                                    address_line_1 VARCHAR(255),
                                    address_line_2 VARCHAR(255),
                                    country VARCHAR(100)
);

CREATE TABLE tenant_2.contracts (
                                    id SERIAL PRIMARY KEY,
                                    code VARCHAR(50) NOT NULL UNIQUE,
                                    type VARCHAR(50),
                                    name VARCHAR(255),
                                    owner VARCHAR(50) REFERENCES tenant_1.people(code)
);

CREATE TABLE tenant_2.people_addresses (
                                           people_id INTEGER REFERENCES tenant_2.people(id),
                                           address_id INTEGER REFERENCES tenant_2.addresses(id),
                                           PRIMARY KEY (people_id, address_id)
);

-- Tenant 3 Schema
CREATE TABLE tenant_3.people (
                                 id SERIAL PRIMARY KEY,
                                 code VARCHAR(50) NOT NULL UNIQUE,
                                 first_name VARCHAR(100),
                                 last_name VARCHAR(100),
                                 gender VARCHAR(10),
                                 status VARCHAR(50)
);

CREATE TABLE tenant_3.addresses (
                                    id SERIAL PRIMARY KEY,
                                    code VARCHAR(50) NOT NULL UNIQUE,
                                    type VARCHAR(50),
                                    address_line_1 VARCHAR(255),
                                    address_line_2 VARCHAR(255),
                                    country VARCHAR(100)
);

CREATE TABLE tenant_3.contracts (
                                    id SERIAL PRIMARY KEY,
                                    code VARCHAR(50) NOT NULL UNIQUE,
                                    type VARCHAR(50),
                                    name VARCHAR(255),
                                    owner VARCHAR(50) REFERENCES tenant_3.people(code)
);

CREATE TABLE tenant_3.people_addresses (
                                           people_id INTEGER REFERENCES tenant_3.people(id),
                                           address_id INTEGER REFERENCES tenant_3.addresses(id),
                                           PRIMARY KEY (people_id, address_id)
);

-- Step 6: Insert Sample Records

-- Insert into public.identifiers
INSERT INTO public.identifiers (name, code, readable_code, type) VALUES
                                                               ('ACTIVE', 'ACTIVE', 'PAS_A', 'PERSON_ACTIVITY_STATUS'),
                                                               ('INACTIVE', 'INACTIVE', 'PAS_I', 'PERSON_ACTIVITY_STATUS'),
                                                               ('MALE', 'MALE', 'G_M', 'GENDER'),
                                                               ('FEMALE', 'FEMALE', 'G_F', 'GENDER'),
                                                               ('HOME', 'HOME', 'AT_H', 'ADDRESS_TYPE'),
                                                               ('OFFICE', 'OFFICE', 'AT_O', 'ADDRESS_TYPE'),
                                                               ('LEASE', 'LEASE', 'CT_L', 'CONTRACT_TYPE'),
                                                               ('SERVICE', 'SERVICE', 'CT_S', 'CONTRACT_TYPE')
;

-- Insert into tenant_1.people
INSERT INTO tenant_1.people (code, first_name, last_name, gender, status) VALUES
                                                                        ('urn:person:t1:0001', 'Maarten', 'Tenant 1', 'G_M', 'PAS_A'),
                                                                        ('urn:person:t1:0002', 'Pieter', 'Tenant 1', 'G_M', 'PAS_I'),
                                                                        ('urn:person:t1:0003', 'Alice', 'Tenant 1', 'G_F', 'PAS_A');

-- Insert into tenant_1.addresses
INSERT INTO tenant_1.addresses (code, type, address_line_1, address_line_2, country) VALUES
                                                                                   ('urn:address:t1:1', 'AT_H', '123 Main St', 'Apt 4B', 'USA'),
                                                                                   ('urn:address:t1:2', 'AT_O', '456 Business Rd', 'Suite 101', 'USA'),
                                                                                   ('urn:address:t1:3', 'AT_H', '789 Beach Ave', 'Cottage 7', 'USA');

-- Insert into tenant_1.contracts
INSERT INTO tenant_1.contracts (code, type, name, owner) VALUES
                                                       ('urn:contract:t1:1', 'CT_L', 'Lease Agreement', 'urn:person:t1:0001'),
                                                       ('urn:contract:t1:2', 'CT_L', 'Employment Contract', 'urn:person:t1:0001'),
                                                       ('urn:contract:t1:3', 'CT_S', 'Service Agreement', 'urn:person:t1:0002');

-- Insert into tenant_1.people_addresses (many-to-many relationship)
INSERT INTO tenant_1.people_addresses (people_id, address_id) VALUES
                                                                  (1, 1),
                                                                  (1, 2),
                                                                  (2, 2),
                                                                  (3, 3);

-- Repeat sample data insertion for tenant_2 and tenant_3
-- Tenant 2
INSERT INTO tenant_2.people (code, first_name, last_name, gender, status) VALUES
                                                                        ('urn:person:t2:0001', 'Maarten', 'Tenant 2', 'G_M', 'PAS_A'),
                                                                        ('urn:person:t2:0002', 'Pieter', 'Tenant 2', 'G_M', 'PAS_I'),
                                                                        ('urn:person:t2:0003', 'Alice', 'Tenant 2', 'G_F', 'PAS_A');

INSERT INTO tenant_2.addresses (code, type, address_line_1, address_line_2, country) VALUES
                                                                                         ('urn:address:t2:1', 'AT_H', '123 Main St', 'Apt 4B', 'USA 2'),
                                                                                         ('urn:address:t2:2', 'AT_O', '456 Business Rd', 'Suite 101', 'USA 2'),
                                                                                         ('urn:address:t2:3', 'AT_H', '789 Beach Ave', 'Cottage 7', 'USA 2');

INSERT INTO tenant_2.contracts (code, type, name, owner) VALUES
                                                       ('urn:contract:t2:1', 'CT_L', 'Lease Agreement', 'urn:person:t1:0001'),
                                                       ('urn:contract:t2:2', 'CT_L', 'Employment Contract', 'urn:person:t1:0001'),
                                                       ('urn:contract:t2:3', 'CT_S', 'Service Agreement', 'urn:person:t1:0002');

INSERT INTO tenant_2.people_addresses (people_id, address_id) VALUES
                                                                  (1, 1),
                                                                  (1, 2),
                                                                  (2, 2),
                                                                  (3, 3);

-- Tenant 3
INSERT INTO tenant_3.people (code, first_name, last_name, gender, status) VALUES
                                                                        ('urn:person:t3:0001', 'Maarten', 'Tenant 3', 'G_M', 'PAS_A'),
                                                                        ('urn:person:t3:0002', 'Pieter', 'Tenant 3', 'G_M', 'PAS_I'),
                                                                        ('urn:person:t3:0003', 'Alice', 'Tenant 3', 'G_F', 'PAS_A');

INSERT INTO tenant_3.addresses (code, type, address_line_1, address_line_2, country) VALUES
                                                                                         ('urn:address:t3:1', 'AT_H', '123 Main St', 'Apt 4B', 'USA 3'),
                                                                                         ('urn:address:t3:2', 'AT_O', '456 Business Rd', 'Suite 101', 'USA 3'),
                                                                                         ('urn:address:t3:3', 'AT_H', '789 Beach Ave', 'Cottage 7', 'USA 3');

INSERT INTO tenant_3.contracts (code, type, name, owner) VALUES
                                                       ('urn:contract:t3:1', 'CT_L', 'Lease Agreement', 'urn:person:t3:0001'),
                                                       ('urn:contract:t3:2', 'CT_L', 'Employment Contract', 'urn:person:t3:0001'),
                                                       ('urn:contract:t3:3', 'CT_S', 'Service Agreement', 'urn:person:t3:0002');

INSERT INTO tenant_3.people_addresses (people_id, address_id) VALUES
                                                                  (1, 1),
                                                                  (1, 2),
                                                                  (2, 2),
                                                                  (3, 3);
