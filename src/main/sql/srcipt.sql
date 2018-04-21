-- Table: public.recepients

-- DROP TABLE public.recepients;

CREATE TABLE public.recepients
(
    id integer NOT NULL,
    name character varying(255) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT recepients_pkey PRIMARY KEY (id)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.recepients
    OWNER to postgres;
	
-- Table: public.users

-- DROP TABLE public.users;

CREATE TABLE public.users
(
    id integer NOT NULL,
    name character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT users_pkey PRIMARY KEY (id)
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.users
    OWNER to postgres;
	
-- Table: public.subscriptions

-- DROP TABLE public.subscriptions;

CREATE TABLE public.subscriptions
(
    uid integer NOT NULL,
    subid integer NOT NULL,
    CONSTRAINT subscriptions_pkey PRIMARY KEY (subid, uid),
    CONSTRAINT subid FOREIGN KEY (subid)
        REFERENCES public.recepients (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION,
    CONSTRAINT uid FOREIGN KEY (uid)
        REFERENCES public.users (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)
WITH (
    OIDS = FALSE
)
TABLESPACE pg_default;

ALTER TABLE public.subscriptions
    OWNER to postgres;