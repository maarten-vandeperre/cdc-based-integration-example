-- oc port-forward $(oc get pod -n integration-project | grep integration-79f675fdd9-jnj6g  | awk '{print $1}') 5432:5432 -n integration-project

ALTER SYSTEM SET wal_level = 'logical';

CREATE TABLE addresses
(
    id           serial PRIMARY KEY,
    ref          VARCHAR(50) UNIQUE NOT NULL,
    address_line1 VARCHAR(250)       NOT NULL,
    address_line2 VARCHAR(250)       NOT NULL,
    address_line3 VARCHAR(250),
    country_code VARCHAR(10)        NOT NULL
);

INSERT INTO addresses (ref, address_line1, address_line2, address_line3, country_code)
VALUES ('925a7e8e-8b13-429c-80ed-9ae2f788b3dc', 'Leonardo Da Vinci Laan 27', '9000 Gent', null, 'BE');

CREATE TABLE people
(
    id         serial PRIMARY KEY,
    ref        VARCHAR(50) UNIQUE NOT NULL,
    address    INTEGER,
    first_name VARCHAR(50)        NOT NULL,
    last_name  VARCHAR(50)        NOT NULL,
    birth_date VARCHAR(10),
    CONSTRAINT fk_address FOREIGN KEY (address) REFERENCES addresses (id)
);

INSERT INTO people (ref, address, first_name, last_name, birth_date)
VALUES ('13ed6a67-a4c4-4307-85da-2accbcf25aa7',
        (select id from addresses where ref = '925a7e8e-8b13-429c-80ed-9ae2f788b3dc'),
        'Maarten', 'Vandeperre', '17/04/1989');