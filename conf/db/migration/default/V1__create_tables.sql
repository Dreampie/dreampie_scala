------dreampie-------
DROP TABLE IF EXISTS sec_user;
DROP SEQUENCE IF EXISTS sec_user_id_seq;
CREATE SEQUENCE sec_user_id_seq START WITH 1;
CREATE TABLE sec_user (
  id           BIGINT       NOT NULL DEFAULT nextval('sec_user_id_seq') PRIMARY KEY,
  username     VARCHAR(50)  NOT NULL,
  providername VARCHAR(50)  NOT NULL,
  email        VARCHAR(200),
  mobile       VARCHAR(50),
  password     VARCHAR(200) NOT NULL,
  hasher       VARCHAR(200) NOT NULL,
  salt         VARCHAR(200) NOT NULL,
  avatar_url   VARCHAR(255),
  first_name   VARCHAR(10),
  last_name    VARCHAR(10),
  full_name    VARCHAR(20),
  created_at   TIMESTAMP    NOT NULL,
  updated_at   TIMESTAMP,
  deleted_at   TIMESTAMP
);

DROP TABLE IF EXISTS sec_user_info;
DROP SEQUENCE IF EXISTS sec_user_info_id_seq;
CREATE SEQUENCE sec_user_info_id_seq START WITH 1;
CREATE TABLE sec_user_info (
  id         BIGINT    NOT NULL DEFAULT nextval('sec_user_info_id_seq') PRIMARY KEY,
  user_id    BIGINT    NOT NULL,
  creator_id BIGINT,
  gender     INT DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,
  deleted_at TIMESTAMP
);

DROP TABLE IF EXISTS sec_role;
DROP SEQUENCE IF EXISTS sec_role_id_seq;
CREATE SEQUENCE sec_role_id_seq START WITH 1;
CREATE TABLE sec_role (
  id         BIGINT      NOT NULL DEFAULT nextval('sec_role_id_seq') PRIMARY KEY,
  name       VARCHAR(50) NOT NULL,
  value      VARCHAR(50) NOT NULL,
  intro      VARCHAR(255),
  created_at TIMESTAMP   NOT NULL,
  updated_at TIMESTAMP,
  deleted_at TIMESTAMP
);

DROP TABLE IF EXISTS sec_user_role;
DROP SEQUENCE IF EXISTS sec_user_role_id_seq;
CREATE SEQUENCE sec_user_role_id_seq START WITH 1;
CREATE TABLE sec_user_role (
  id      BIGINT NOT NULL DEFAULT nextval('sec_user_role_id_seq') PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL
);


DROP TABLE IF EXISTS sec_permission;
DROP SEQUENCE IF EXISTS sec_permission_id_seq;
CREATE SEQUENCE sec_permission_id_seq START WITH 1;
CREATE TABLE sec_permission (
  id         BIGINT      NOT NULL DEFAULT nextval('sec_permission_id_seq') PRIMARY KEY,
  name       VARCHAR(50) NOT NULL,
  value      VARCHAR(50) NOT NULL,
  url        VARCHAR(255),
  intro      VARCHAR(255),
  created_at TIMESTAMP   NOT NULL,
  updated_at TIMESTAMP,
  deleted_at TIMESTAMP
);

DROP TABLE IF EXISTS sec_role_permission;
DROP SEQUENCE IF EXISTS sec_role_permission_id_seq;
CREATE SEQUENCE sec_role_permission_id_seq START WITH 1;
CREATE TABLE sec_role_permission (
  id            BIGINT NOT NULL DEFAULT nextval('sec_role_permission_id_seq') PRIMARY KEY,
  role_id       BIGINT NOT NULL,
  permission_id BIGINT NOT NULL
);

DROP TABLE IF EXISTS sec_token;
CREATE TABLE sec_token (
  uuid          VARCHAR(255) NOT NULL,
  username      VARCHAR(50)  NOT NULL,
  created_at    TIMESTAMP    NOT NULL,
  expiration_at TIMESTAMP    NOT NULL,
  is_sign_up    BOOLEAN DEFAULT TRUE
);

-- DROP TABLE IF EXISTS sec_user_provider;
-- DROP SEQUENCE IF EXISTS sec_user_provider_id_seq;
-- CREATE SEQUENCE sec_user_provider_id_seq START WITH 1;
-- CREATE TABLE sec_user_provider (
--   id            BIGINT      NOT NULL DEFAULT nextval('sec_user_provider_id_seq') PRIMARY KEY,
--   user_id       BIGINT      NOT NULL,
--   provider_name VARCHAR(50) NOT NULL,
--   email         VARCHAR(200),
--   auth_method   VARCHAR(50) NOT NULL,
--   avatar_url    VARCHAR(255),
--   first_name    VARCHAR(10),
--   last_name     VARCHAR(10),
--   full_name     VARCHAR(20),
--   created_at    TIMESTAMP   NOT NULL,
--   updated_at    TIMESTAMP,
--   deleted_at    TIMESTAMP,
-- );

--INSERT INTO sec_user ( creator_id, username, email, mobile, password, hasher, salt, picture, first_name, last_name, full_name,gender, created_at, updated_at, deleted_at)
--VALUES (0,'admin',null,null ,'$2a$10$q5IvwSTS4XNA025F9ScCt.tTaavvdN6BgLjqDxZssxXhDP4YU/Tpu','BCryptHasher','$2a$10$q5IvwSTS4XNA025F9ScCt',null,'','','',0,current_timestamp,null,null);
--
--INSERT INTO sec_role(name,value,intro, created_at, updated_at, deleted_at)
--VALUES('admin','admin','',current_timestamp,null,null)
--
--INSERT INTO sec_user_role(user_id,role_id)VALUES(1,1)
--
--INSERT INTO sec_permission(name,value,url,intro, created_at, updated_at, deleted_at)
--VALUES('base','base','/**','',current_timestamp,null,null)
--
--INSERT INTO sec_role_permission(role_id,permission_id)VALUES(1,1)
------demo--------
DROP TABLE IF EXISTS programmer;
DROP SEQUENCE IF EXISTS programmer_id_seq;
CREATE SEQUENCE programmer_id_seq START WITH 1;
CREATE TABLE programmer (
  id                BIGINT       NOT NULL DEFAULT nextval('programmer_id_seq') PRIMARY KEY,
  name              VARCHAR(255) NOT NULL,
  company_id        BIGINT,
  created_timestamp TIMESTAMP    NOT NULL,
  deleted_timestamp TIMESTAMP
);

DROP TABLE IF EXISTS company;
DROP SEQUENCE IF EXISTS company_id_seq;
CREATE SEQUENCE company_id_seq START WITH 1;
CREATE TABLE company (
  id         BIGINT       NOT NULL DEFAULT nextval('company_id_seq') PRIMARY KEY,
  name       VARCHAR(255) NOT NULL,
  url        VARCHAR(255),
  created_at TIMESTAMP    NOT NULL,
  deleted_at TIMESTAMP
);

DROP TABLE IF EXISTS skill;
DROP SEQUENCE IF EXISTS skill_id_seq;
CREATE SEQUENCE skill_id_seq START WITH 1;
CREATE TABLE skill (
  id         BIGINT       NOT NULL DEFAULT nextval('skill_id_seq') PRIMARY KEY,
  name       VARCHAR(255) NOT NULL,
  created_at TIMESTAMP    NOT NULL,
  deleted_at TIMESTAMP
);

DROP TABLE IF EXISTS programmer_skill;
CREATE TABLE programmer_skill (
  programmer_id BIGINT NOT NULL,
  skill_id      BIGINT NOT NULL,
  PRIMARY KEY (programmer_id, skill_id)
);

INSERT INTO company (name, url, created_at) VALUES ('Typesafe', 'http://typesafe.com/', current_timestamp);
INSERT INTO company (name, url, created_at) VALUES ('Oracle', 'http://www.oracle.com/', current_timestamp);
INSERT INTO company (name, url, created_at) VALUES ('Google', 'http://www.google.com/', current_timestamp);
INSERT INTO company (name, url, created_at) VALUES ('Microsoft', 'http://www.microsoft.com/', current_timestamp);

INSERT INTO skill (name, created_at) VALUES ('Scala', current_timestamp);
INSERT INTO skill (name, created_at) VALUES ('Java', current_timestamp);
INSERT INTO skill (name, created_at) VALUES ('Ruby', current_timestamp);
INSERT INTO skill (name, created_at) VALUES ('MySQL', current_timestamp);
INSERT INTO skill (name, created_at) VALUES ('PostgreSQL', current_timestamp);


INSERT INTO programmer (name, company_id, created_timestamp) VALUES ('Alice', 1, current_timestamp);
INSERT INTO programmer (name, company_id, created_timestamp) VALUES ('Bob', 2, current_timestamp);
INSERT INTO programmer (name, company_id, created_timestamp) VALUES ('Chris', 1, current_timestamp);

INSERT INTO programmer_skill VALUES (1, 1);
INSERT INTO programmer_skill VALUES (1, 2);
INSERT INTO programmer_skill VALUES (2, 2);

