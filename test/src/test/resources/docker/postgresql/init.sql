CREATE USER alice WITH REPLICATION PASSWORD '123456';
CREATE DATABASE foo;
GRANT ALL PRIVILEGES ON DATABASE foo TO alice;

\c foo
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON tables TO alice;

CREATE TABLE public.bar
(
    id              int,
    name            varchar(255),
    cost            decimal,
    additional_info jsonb
);

SELECT *
FROM pg_create_logical_replication_slot('embedded_debezium_slot', 'decoderbufs');
