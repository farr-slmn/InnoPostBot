-- Table: public.recepients

-- DROP TABLE public.recepients CASCADE;

CREATE TABLE public.recepients
(
    uid integer NOT NULL,
    CONSTRAINT recepients_pkey PRIMARY KEY (uid)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.recepients
    OWNER to postgres;

-- Table: public.users

-- DROP  TABLE public.users CASCADE;

CREATE TABLE public.users
(
    subname character varying(255) NOT NULL COLLATE pg_catalog."default",
    CONSTRAINT users_pkey PRIMARY KEY (subname)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.users
    OWNER to postgres;

-- Table: public.subscriptions

-- DROP TABLE public.subscriptions CASCADE;

CREATE TABLE public.subscriptions
(
    uid integer NOT NULL,
    subname character varying(255) NOT NULL COLLATE pg_catalog."default",
    CONSTRAINT subscriptions_pkey PRIMARY KEY (subname, uid),
    CONSTRAINT subname FOREIGN KEY (subname)
        REFERENCES public.users (subname) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT uid FOREIGN KEY (uid)
        REFERENCES public.recepients (uid) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.subscriptions
    OWNER to postgres;