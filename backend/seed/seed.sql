--
-- PostgreSQL database dump
--

\restrict c06j9K8PITaeBVIU7jgD3MQvY7dBIrKKdl5xlLPNxrZ4UBl51Ft718vxR7v6IMI

-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: auth_attempts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.auth_attempts (
    id bigint NOT NULL,
    identifier character varying(255) NOT NULL,
    attempt_type character varying(30) NOT NULL,
    created_at bigint NOT NULL
);


--
-- Name: auth_attempts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.auth_attempts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: auth_attempts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.auth_attempts_id_seq OWNED BY public.auth_attempts.id;


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    order_id bigint,
    type character varying(50) NOT NULL,
    title character varying(200) NOT NULL,
    message character varying(500) NOT NULL,
    is_read boolean DEFAULT false NOT NULL,
    created_at bigint NOT NULL
);


--
-- Name: notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.notifications_id_seq OWNED BY public.notifications.id;


--
-- Name: order_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_items (
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    product_id bigint NOT NULL,
    quantity numeric(12,2) NOT NULL,
    unit_price numeric(12,2) NOT NULL,
    subtotal numeric(12,2) NOT NULL,
    created_at bigint NOT NULL
);


--
-- Name: order_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.order_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: order_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.order_items_id_seq OWNED BY public.order_items.id;


--
-- Name: orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orders (
    id bigint NOT NULL,
    customer_id bigint NOT NULL,
    telephone character varying(20) NOT NULL,
    location character varying(255) NOT NULL,
    needed_date bigint NOT NULL,
    status character varying(30) DEFAULT 'PENDING'::character varying NOT NULL,
    notes text,
    total_amount numeric(12,2) DEFAULT 0 NOT NULL,
    created_at bigint NOT NULL,
    updated_at bigint NOT NULL,
    assigned_dealer_id bigint,
    order_type character varying(20) DEFAULT 'RETAIL'::character varying NOT NULL
);


--
-- Name: orders_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.orders_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: orders_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.orders_id_seq OWNED BY public.orders.id;


--
-- Name: products; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.products (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    category character varying(100) NOT NULL,
    unit character varying(30) NOT NULL,
    price numeric(12,2) NOT NULL,
    stock_quantity numeric(12,2) DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at bigint NOT NULL,
    updated_at bigint NOT NULL
);


--
-- Name: products_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.products_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: products_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.products_id_seq OWNED BY public.products.id;


--
-- Name: role_audit_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.role_audit_log (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    old_role character varying(30) NOT NULL,
    new_role character varying(30) NOT NULL,
    changed_by_user_id bigint NOT NULL,
    reason character varying(500),
    created_at bigint NOT NULL
);


--
-- Name: role_audit_log_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.role_audit_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: role_audit_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.role_audit_log_id_seq OWNED BY public.role_audit_log.id;


--
-- Name: saved_locations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.saved_locations (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    label character varying(50) NOT NULL,
    address character varying(500) NOT NULL,
    is_default boolean DEFAULT false NOT NULL,
    created_at bigint NOT NULL,
    updated_at bigint NOT NULL
);


--
-- Name: saved_locations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.saved_locations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: saved_locations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.saved_locations_id_seq OWNED BY public.saved_locations.id;


--
-- Name: settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.settings (
    id bigint NOT NULL,
    key character varying(100) NOT NULL,
    value text NOT NULL,
    value_type character varying(20) NOT NULL,
    category character varying(50) NOT NULL,
    description character varying(500) NOT NULL,
    updated_at bigint NOT NULL,
    updated_by_user_id bigint
);


--
-- Name: settings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.settings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: settings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.settings_id_seq OWNED BY public.settings.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    phone character varying(20),
    email character varying(150),
    password_hash character varying(255) NOT NULL,
    role character varying(30) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at bigint NOT NULL,
    updated_at bigint NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    dealer_status character varying(20),
    business_name character varying(200),
    business_region character varying(100),
    account_manager_name character varying(150),
    account_manager_email character varying(200),
    credit_limit numeric(14,2) DEFAULT 0,
    credit_used numeric(14,2) DEFAULT 0,
    payment_terms character varying(20) DEFAULT 'PREPAID'::character varying,
    lifetime_bulk_orders integer DEFAULT 0
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: verification_codes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.verification_codes (
    id bigint NOT NULL,
    user_id bigint,
    purpose character varying(30) NOT NULL,
    channel character varying(10) NOT NULL,
    destination character varying(255) NOT NULL,
    code_hash character varying(255) NOT NULL,
    expires_at bigint NOT NULL,
    verified_at bigint,
    attempts integer DEFAULT 0 NOT NULL,
    created_at bigint NOT NULL
);


--
-- Name: verification_codes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.verification_codes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: verification_codes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.verification_codes_id_seq OWNED BY public.verification_codes.id;


--
-- Name: auth_attempts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_attempts ALTER COLUMN id SET DEFAULT nextval('public.auth_attempts_id_seq'::regclass);


--
-- Name: notifications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications ALTER COLUMN id SET DEFAULT nextval('public.notifications_id_seq'::regclass);


--
-- Name: order_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items ALTER COLUMN id SET DEFAULT nextval('public.order_items_id_seq'::regclass);


--
-- Name: orders id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders ALTER COLUMN id SET DEFAULT nextval('public.orders_id_seq'::regclass);


--
-- Name: products id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products ALTER COLUMN id SET DEFAULT nextval('public.products_id_seq'::regclass);


--
-- Name: role_audit_log id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_audit_log ALTER COLUMN id SET DEFAULT nextval('public.role_audit_log_id_seq'::regclass);


--
-- Name: saved_locations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saved_locations ALTER COLUMN id SET DEFAULT nextval('public.saved_locations_id_seq'::regclass);


--
-- Name: settings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.settings ALTER COLUMN id SET DEFAULT nextval('public.settings_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: verification_codes id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.verification_codes ALTER COLUMN id SET DEFAULT nextval('public.verification_codes_id_seq'::regclass);


--
-- Data for Name: auth_attempts; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (1, 'email:test@example.com', 'OTP_REQUEST', 1790261872519);
INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (2, 'email:test@example.com', 'OTP_REQUEST', 1790261913178);
INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (3, 'email:test@example.com', 'OTP_REQUEST', 1790261985560);
INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (4, 'email:newuser@gmail.com', 'OTP_REQUEST', 1790264410984);
INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (5, 'email:newuser@gmail.com', 'OTP_REQUEST', 1790265207195);
INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (6, 'email:movinoscar45@gmail.com', 'OTP_REQUEST', 1790274975588);
INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (7, 'phone:+254111966397', 'OTP_REQUEST', 1790275197297);
INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (8, 'phone:+254111966397', 'OTP_REQUEST', 1790278520810);
INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (9, 'email:movinoscar45@gmail.com', 'OTP_REQUEST', 1790318470205);
INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (10, 'email:movinoscar45@gmail.com', 'OTP_REQUEST', 1790326017947);
INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (11, 'email:victoriandune01@gmail.com', 'OTP_REQUEST', 1790329230871);
INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (12, 'email:victoriandune01@gmail.com', 'OTP_REQUEST', 1790329256813);
INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (13, 'email:movinoscar45@gmail.com', 'OTP_REQUEST', 1790329389572);
INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (14, 'email:movinoscar45@gmail.com', 'OTP_REQUEST', 1790331936028);
INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (15, 'email:palmer@gmail.com', 'OTP_REQUEST', 1790411868321);
INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (16, 'email:palmer@gmail.com', 'OTP_REQUEST', 1790411890874);
INSERT INTO public.auth_attempts (id, identifier, attempt_type, created_at) VALUES (17, 'email:victoriandune01@gmail.com', 'OTP_REQUEST', 1790411917298);


--
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.notifications (id, user_id, order_id, type, title, message, is_read, created_at) VALUES (3, 4, 6, 'ORDER_ASSIGNED', 'New Order Assigned', 'You have been assigned order #6.', false, 1789226406760);
INSERT INTO public.notifications (id, user_id, order_id, type, title, message, is_read, created_at) VALUES (4, 4, 6, 'ORDER_ASSIGNED', 'New Order Assigned', 'You have been assigned order #6.', false, 1789226427720);
INSERT INTO public.notifications (id, user_id, order_id, type, title, message, is_read, created_at) VALUES (5, 4, 6, 'ORDER_ASSIGNED', 'New Order Assigned', 'You have been assigned order #6.', false, 1789226734155);
INSERT INTO public.notifications (id, user_id, order_id, type, title, message, is_read, created_at) VALUES (6, 5, 8, 'ORDER_ASSIGNED', 'New Order Assigned', 'You have been assigned order #8.', true, 1789226901493);
INSERT INTO public.notifications (id, user_id, order_id, type, title, message, is_read, created_at) VALUES (2, 5, 6, 'ORDER_ASSIGNED', 'New Order Assigned', 'You have been assigned order #6.', true, 1789194954351);
INSERT INTO public.notifications (id, user_id, order_id, type, title, message, is_read, created_at) VALUES (1, 5, 7, 'ORDER_ASSIGNED', 'New Order Assigned', 'You have been assigned order #7.', true, 1789194894244);
INSERT INTO public.notifications (id, user_id, order_id, type, title, message, is_read, created_at) VALUES (8, 2, 11, 'ORDER_STATUS_UPDATE', 'Order Status Updated', 'Your order #11 is now CONFIRMED.', true, 1789495608128);
INSERT INTO public.notifications (id, user_id, order_id, type, title, message, is_read, created_at) VALUES (9, 2, 11, 'ORDER_STATUS_UPDATE', 'Order Status Updated', 'Your order #11 is now PROCESSING.', false, 1789503431065);
INSERT INTO public.notifications (id, user_id, order_id, type, title, message, is_read, created_at) VALUES (10, 2, 11, 'ORDER_STATUS_UPDATE', 'Order Status Updated', 'Your order #11 is now READY.', false, 1789503688372);
INSERT INTO public.notifications (id, user_id, order_id, type, title, message, is_read, created_at) VALUES (11, 2, 11, 'ORDER_STATUS_UPDATE', 'Order Status Updated', 'Your order #11 is now COMPLETED.', false, 1789503718662);
INSERT INTO public.notifications (id, user_id, order_id, type, title, message, is_read, created_at) VALUES (7, 5, 11, 'ORDER_ASSIGNED', 'New Order Assigned', 'You have been assigned order #11.', true, 1789490589623);
INSERT INTO public.notifications (id, user_id, order_id, type, title, message, is_read, created_at) VALUES (12, 18, 12, 'ORDER_STATUS_UPDATE', 'Order Status Updated', 'Your order #12 is now CONFIRMED.', false, 1790408539052);
INSERT INTO public.notifications (id, user_id, order_id, type, title, message, is_read, created_at) VALUES (13, 18, 12, 'ORDER_STATUS_UPDATE', 'Order Status Updated', 'Your order #12 is now PROCESSING.', false, 1790408544119);
INSERT INTO public.notifications (id, user_id, order_id, type, title, message, is_read, created_at) VALUES (14, 18, 12, 'ORDER_STATUS_UPDATE', 'Order Status Updated', 'Your order #12 is now READY.', false, 1790408546868);
INSERT INTO public.notifications (id, user_id, order_id, type, title, message, is_read, created_at) VALUES (15, 18, 12, 'ORDER_STATUS_UPDATE', 'Order Status Updated', 'Your order #12 is now COMPLETED.', false, 1790408554139);


--
-- Data for Name: order_items; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.order_items (id, order_id, product_id, quantity, unit_price, subtotal, created_at) VALUES (1, 1, 1, 5.00, 2500.00, 12500.00, 1787214681378);
INSERT INTO public.order_items (id, order_id, product_id, quantity, unit_price, subtotal, created_at) VALUES (2, 2, 1, 2.00, 2500.00, 5000.00, 1787238142554);
INSERT INTO public.order_items (id, order_id, product_id, quantity, unit_price, subtotal, created_at) VALUES (3, 3, 1, 10.00, 2500.00, 25000.00, 1787295622762);
INSERT INTO public.order_items (id, order_id, product_id, quantity, unit_price, subtotal, created_at) VALUES (4, 4, 1, 10.00, 2500.00, 25000.00, 1787295935102);
INSERT INTO public.order_items (id, order_id, product_id, quantity, unit_price, subtotal, created_at) VALUES (5, 5, 1, 3.00, 2500.00, 7500.00, 1787333133330);
INSERT INTO public.order_items (id, order_id, product_id, quantity, unit_price, subtotal, created_at) VALUES (6, 6, 1, 101.00, 2500.00, 252500.00, 1787354170889);
INSERT INTO public.order_items (id, order_id, product_id, quantity, unit_price, subtotal, created_at) VALUES (7, 7, 1, 5.00, 2500.00, 12500.00, 1787396324481);
INSERT INTO public.order_items (id, order_id, product_id, quantity, unit_price, subtotal, created_at) VALUES (8, 8, 1, 5.00, 2500.00, 12500.00, 1787431211068);
INSERT INTO public.order_items (id, order_id, product_id, quantity, unit_price, subtotal, created_at) VALUES (9, 9, 1, 5.00, 2500.00, 12500.00, 1787432202668);
INSERT INTO public.order_items (id, order_id, product_id, quantity, unit_price, subtotal, created_at) VALUES (10, 10, 1, 5.00, 2500.00, 12500.00, 1787432422071);
INSERT INTO public.order_items (id, order_id, product_id, quantity, unit_price, subtotal, created_at) VALUES (11, 11, 1, 2.00, 2500.00, 5000.00, 1789489648657);
INSERT INTO public.order_items (id, order_id, product_id, quantity, unit_price, subtotal, created_at) VALUES (12, 12, 1, 1.00, 2500.00, 2500.00, 1790408422552);
INSERT INTO public.order_items (id, order_id, product_id, quantity, unit_price, subtotal, created_at) VALUES (13, 12, 2, 1.00, 2200.00, 2200.00, 1790408422552);


--
-- Data for Name: orders; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.orders (id, customer_id, telephone, location, needed_date, status, notes, total_amount, created_at, updated_at, assigned_dealer_id, order_type) VALUES (2, 3, '0722222222', 'Nairobi', 1787497296976, 'CANCELLED', 'Second customer''s test order', 5000.00, 1787238142554, 1787259354821, NULL, 'RETAIL');
INSERT INTO public.orders (id, customer_id, telephone, location, needed_date, status, notes, total_amount, created_at, updated_at, assigned_dealer_id, order_type) VALUES (1, 2, '0712345678', 'Nyeri', 1787387466681, 'COMPLETED', 'First test order', 12500.00, 1787214681378, 1787277386457, NULL, 'RETAIL');
INSERT INTO public.orders (id, customer_id, telephone, location, needed_date, status, notes, total_amount, created_at, updated_at, assigned_dealer_id, order_type) VALUES (12, 18, '759288183', 'Blum, Kangemi,Nyeri', 1790715600000, 'COMPLETED', NULL, 4700.00, 1790408422552, 1790408554134, NULL, 'RETAIL');
INSERT INTO public.orders (id, customer_id, telephone, location, needed_date, status, notes, total_amount, created_at, updated_at, assigned_dealer_id, order_type) VALUES (4, 2, '0712345678', 'Nyeri', 1787727934893, 'READY', 'Inventory deduction test', 25000.00, 1787295935102, 1787311951538, NULL, 'RETAIL');
INSERT INTO public.orders (id, customer_id, telephone, location, needed_date, status, notes, total_amount, created_at, updated_at, assigned_dealer_id, order_type) VALUES (5, 2, '0712345678', 'Nyeri', 1787765131260, 'CANCELLED', 'Stock deduction diagnostic test', 7500.00, 1787333133330, 1787353397387, NULL, 'RETAIL');
INSERT INTO public.orders (id, customer_id, telephone, location, needed_date, status, notes, total_amount, created_at, updated_at, assigned_dealer_id, order_type) VALUES (9, 2, '0712345678', 'Nyeri', 1787864201698, 'COMPLETED', 'Full order lifecycle test', 12500.00, 1787432202668, 1788809533959, 4, 'RETAIL');
INSERT INTO public.orders (id, customer_id, telephone, location, needed_date, status, notes, total_amount, created_at, updated_at, assigned_dealer_id, order_type) VALUES (10, 2, '0712345678', 'Nyeri', 1787864421800, 'COMPLETED', 'Full order lifecycle test', 12500.00, 1787432422071, 1788845058286, 4, 'RETAIL');
INSERT INTO public.orders (id, customer_id, telephone, location, needed_date, status, notes, total_amount, created_at, updated_at, assigned_dealer_id, order_type) VALUES (3, 2, '0712345678', 'Nyeri', 1787727622434, 'COMPLETED', 'Inventory deduction test', 25000.00, 1787295622762, 1789131297678, 5, 'RETAIL');
INSERT INTO public.orders (id, customer_id, telephone, location, needed_date, status, notes, total_amount, created_at, updated_at, assigned_dealer_id, order_type) VALUES (7, 2, '0712345678', 'Nyeri', 1787828322825, 'CANCELLED', 'Final inventory restoration test', 12500.00, 1787396324481, 1789194894064, 5, 'RETAIL');
INSERT INTO public.orders (id, customer_id, telephone, location, needed_date, status, notes, total_amount, created_at, updated_at, assigned_dealer_id, order_type) VALUES (6, 2, '0712345678', 'Nyeri', 1787786127121, 'PENDING', 'Insufficient stock test', 252500.00, 1787354170889, 1789226734122, 4, 'RETAIL');
INSERT INTO public.orders (id, customer_id, telephone, location, needed_date, status, notes, total_amount, created_at, updated_at, assigned_dealer_id, order_type) VALUES (8, 2, '0712345678', 'Nyeri', 1787863208933, 'CANCELLED', 'Full order lifecycle test', 12500.00, 1787431211068, 1789226901451, 5, 'RETAIL');
INSERT INTO public.orders (id, customer_id, telephone, location, needed_date, status, notes, total_amount, created_at, updated_at, assigned_dealer_id, order_type) VALUES (11, 2, '0111966397', 'jukiwa', 1789592400000, 'COMPLETED', NULL, 5000.00, 1789489648657, 1789503718579, 5, 'RETAIL');


--
-- Data for Name: products; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (71, 'GOAT PELLET', 'It is a nutritionally balanced concentrated feeds made from grains, proteins, vitamins and minerals that prevent selective feeding and reduce waste.
It delivers steady protein (14% to 20%) energy and trace minerals.
Support faster growth in kids, higher fertility in breeding stock and increased milk production.', 'GOAT FEEDS', '50KG', 3300.00, 50.00, true, 1790662701261, 1790662701261);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (3, 'BROILER STARTER MASH PREMIUM', 'Premium complete starter feed for broiler chickens', 'POULTRY', 'KG', 2900.00, 175.00, false, 1787148331786, 1787167480911);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (72, 'PIG FINISHER FEED', 'It is a high energy,lower protein diet designed to maximize lean muscle growth and frame maturity in pigs weighing 70 kg.
Feed a maximum of 2.5kg per pig daily,paired with constant supply of drinking water.
Aim for roughly 13% to 16% crude protein and 0.65% to 0.7% lysine to prevent excessive fat deposition and encourage lean meat', 'PIG FEEDS', '50KG', 3300.00, 100.00, true, 1790663296376, 1790663296376);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (73, 'KIENYEJI MASH', 'This is a formulated, balanced supplementary feed designed for indigenous chickens raised in free range or semi intensive systems.
Provides moderate protein and energy tailored to the active lifestyle and foraging habits of indigenous breeds.
Works alongside natural scavenging to supply extra vitamins, minerals and amino acids needed for steady growth and immunity.
Supports healthy bone structure,consistent body development and quality egg production with strong shells and rich yellow yolks.', 'POULTRY FEEDS', '50KG', 2300.00, 100.00, true, 1790663971919, 1790663971919);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (74, 'LAYERS MASH', 'This is a complete,loose,coarse-ground feed specially formulated for laying hens around 18 to 20 weeks of age to support high quality egg production and strong shells.
It contains high levels of protein, energy, vitamins and extra calcium for eggshell formation.
Each laying hen eats about 140grams of feed per day', 'LAYERS', '50KG', 2750.00, 100.00, true, 1790664484320, 1790664484320);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (75, 'BROILER FINISHER FEEDS', 'This is a high energy,lower protein diet fed to chickens from about 3 to 4 weeks of age until market weight.
Feed to birds starting from day 22 or week 4-5 until slaughter .
Each bird consumes roughly 2.85 kg of feed during this final finishing period.
Feed dry,ad libitum, with continuous access to clean water.
Higher energy levels promote rapid weight gain and proper fat deposition', 'BROILER FEEDS', '50KG', 3500.00, 100.00, true, 1790665197441, 1790665197441);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (76, 'CHICK MASH', 'This is a specialised, finely ground starter feed formulated for chicks from day one upto 8 weeks of age.
It contains coccidiostat to prevent and manage coccidiosis.
It is enriched with phosphorus and  highly digestible to promote a strong body framework.
Each chick eats an average of 2kg during this 8 -week window', 'LAYERS FEEDS', '50KG', 2800.00, 100.00, true, 1790665769260, 1790665769260);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (4, 'Dairy Mineral', 'Mineral supplement for dairy cattle', 'Supplements', 'piece', 1000.00, 25.00, false, 1788372775399, 1788421877107);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (5, 'Layers Mash', 'Complete feed for laying birds', 'Poultry', 'KG', 2500.00, 100.00, true, 1790024722292, 1790024722292);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (6, 'Tester', 'My tester product', 'POULTRY', '1KG', 200.00, 20.00, true, 1790029697004, 1790029882641);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (7, 'Tester 2', 'Testedr 2 products', 'POULTRY', '1kg', 500.00, 50.00, true, 1790030552547, 1790030552547);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (40, 'Tester3', 'Testing 3', 'Poultry', '1kg', 150.00, 250.00, true, 1790054229405, 1790054229405);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (41, 'Tester 4', 'Testing 4', 'POULTRY', 'KG', 500.00, 50.00, true, 1790056819546, 1790056837048);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (42, 'Zx', 'zban', 'Poultry', 'Kg', 2000.00, 500.00, true, 1790057175330, 1790057175330);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (43, 'Product Z', 'Testing', 'LAYERS', '5kg', 150.00, 200.00, true, 1790104393076, 1790104393076);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (44, 'Product Z', 'Testing', 'LAYERS', '5kg', 150.00, 200.00, true, 1790104399655, 1790104399655);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (45, 'Product Z', 'Testing', 'LAYERS', '5kg', 150.00, 200.00, true, 1790104428448, 1790104428448);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (46, 'Product Z', 'Testing', 'LAYERS', '5kg', 150.00, 200.00, true, 1790104435583, 1790104435583);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (47, 'Product Z', 'Testing', 'LAYERS', '5kg', 150.00, 200.00, true, 1790104710588, 1790104710588);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (48, 'Product Z', 'Testing', 'LAYERS', '5kg', 150.00, 200.00, true, 1790104737910, 1790104737910);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (49, 'Product Z', 'Testing', 'LAYERS', '5kg', 150.00, 200.00, true, 1790104759390, 1790104759390);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (50, 'Product Z', 'Testing', 'Layers', '5kg', 200.00, 200.00, true, 1790104860517, 1790104860517);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (51, 'Product Z', 'Testing', 'Layers', '5kg', 200.00, 200.00, true, 1790104867318, 1790104867318);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (52, 'Product Z', 'Testing', 'Layers', '5kg', 200.00, 200.00, true, 1790104879486, 1790104879486);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (53, 'Product Z', 'Testing', 'Layers', '5kg', 200.00, 200.00, true, 1790104954318, 1790104954318);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (54, 'Product Z', 'Testing', 'Layers', '5kg', 200.00, 200.00, true, 1790104969348, 1790104969348);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (55, 'Product Z', 'Testing', 'Layers', '5kg', 200.00, 200.00, true, 1790104982188, 1790104982188);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (56, 'Product Z', 'Testing', 'Layers', '5kg', 200.00, 200.00, true, 1790105015010, 1790105015010);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (57, 'Image', 'Image Tester', 'Pig', '10KG', 999.00, 900.00, true, 1790121276161, 1790121276161);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (59, 'Zz', '🏆🏆', 'Supplements,', '190Kg', 999.00, 900.00, false, 1790127067409, 1790127484476);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (58, 'Mm', '💙💙💙💙💙', 'Layers', '20kh', 999.00, 999.00, false, 1790123052614, 1790127492577);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (60, 'MO', '💙💙💙', 'New', '70kg', 9999.00, 99.00, true, 1790149216566, 1790149216566);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (61, 'L', '🔵', 'Feeds', '50kg', 99.00, 369.00, true, 1790151125552, 1790151125552);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (62, 'Zx', '🏈🏈🏈', 'Vitamins', '10KG', 980.00, 560.00, true, 1790154347660, 1790154347660);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (63, 'Mx', '🥳🥳', 'Pig', '10KG', 936.00, 693.00, true, 1790155788590, 1790155788590);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (64, 'Oo', '😃😃😃', 'Pig', '5KG', 693.00, 600.00, true, 1790161146918, 1790161146918);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (65, 'Movo', '🤞🤞🤞', 'Layers', '9Kg', 999.00, 999.00, true, 1790161445128, 1790161445128);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (66, 'AA', '🎉', 'Pig', '10kg', 999.00, 99.00, true, 1790188673036, 1790188673036);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (2, 'GROWERS MASH', 'Complete feed for growing chickens', 'POULTRY', 'KG', 2200.00, 99.00, true, 1787146215684, 1790408539035);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (1, 'LAYERS MASH', 'Complete feed for laying chickens', 'POULTRY', 'KG', 2500.00, 77.00, true, 1787122032915, 1790408539043);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (67, 'Calf Pellets', 'A specialised dry feeds designed to help young calves transition smoothly from milk based diet to solid forage.
Always feed the pellets dry-do not mix them into water or milk.
Introduce small amounts of starter pellets(about 25g to 50g)starting from day 4 to 7 of the calf''s life', 'CALF FEEDS', 'KG', 2560.00, 100.00, true, 1790617557361, 1790617557361);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (68, 'CALF STARTER', 'The textured feed introduced to young calves around 3 days to stimulate rumen development and bridge the nutritional hap weaning.
Contains 18% to 20% protein to support rapid muscle and skeletal growth.
Offer a small handful in a clean, shallow bucket starting on day 3 to day 5 alongside colostrum and milk.
Supply free-choice,clean fresh water at all times from week one as water is essential for rumen fermentation and solid feed digestion', 'CALF FEEDS', '10KG', 825.00, 100.00, true, 1790644762599, 1790644762599);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (69, 'CALF EARLY WEANER PELLETS', 'It is a specialised,high protein solid feeds formulated transition young calves from purely liquid diet to solid feeds.
Fortified with essential trace elements, calcium,phosphorus and vitamins to boost immunity and overall health.
The pelleted form ensures an even distribution of nutrients and prevents selective feeding or separation of ingredients.
Usually introduced around 7 to 14 days of age in small,fresh amounts alongside milk feeding.', 'CALF FEEDS', '10KG', 850.00, 50.00, true, 1790646334439, 1790646334439);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (70, 'RABBIT PELLET', 'It is a compressed, cylindrical animal feeds made from ground plants,grains and added vitamins and minerals to supplement a rabbit''s primary diet of fresh hay.
High amount of crude fiber,18% to 22%+, Moderate crude protein, ideally around 13% to 14% and low amount of crude fat roughly less than 3%', 'RABBIT FEEDS', '1KG', 65.00, 100.00, true, 1790662080142, 1790662080142);
INSERT INTO public.products (id, name, description, category, unit, price, stock_quantity, is_active, created_at, updated_at) VALUES (77, 'BROILER STARTER FEEDS', 'This is a high-protein, nutrient dense fed to chicks from day-old up to 3 weeks of age to support rapid early growth,bone structure and muscle development.
It is highly rich in crude protein 20% to 24% to fuel rapid tissue and feather development .
Calcium and Phosphorus:Vital for strong skeletal frames capable supporting fast weight gain', 'BROILER FEEDS', '50KG', 4500.00, 100.00, true, 1790666203011, 1790666203011);


--
-- Data for Name: role_audit_log; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.role_audit_log (id, user_id, old_role, new_role, changed_by_user_id, reason, created_at) VALUES (1, 18, 'CUSTOMER', 'DEALER', 1, 'Dealer application approved', 1790432759128);


--
-- Data for Name: saved_locations; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.saved_locations (id, user_id, label, address, is_default, created_at, updated_at) VALUES (1, 18, 'Kmtc', 'Kangemi,Riara', false, 1790408055168, 1790408055168);
INSERT INTO public.saved_locations (id, user_id, label, address, is_default, created_at, updated_at) VALUES (2, 2, 'Kikuyu', 'Rware', false, 1790910643490, 1790910643490);


--
-- Data for Name: settings; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (1, 'bulk_order.min_value', '25000', 'DECIMAL', 'BULK_ORDER', 'Minimum total order value in KES for bulk orders', 1790358812865, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (2, 'bulk_order.min_qty_per_item', '100', 'INT', 'BULK_ORDER', 'Minimum quantity per product line for bulk orders', 1790358812865, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (3, 'bulk_order.max_qty_per_item', '5000', 'INT', 'BULK_ORDER', 'Maximum quantity per product line for bulk orders', 1790358812865, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (4, 'bulk_order.lead_time_days', '3', 'INT', 'BULK_ORDER', 'Minimum days between order and requested delivery date', 1790358812865, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (5, 'bulk_order.payment_terms', 'PREPAID', 'ENUM', 'BULK_ORDER', 'Payment terms: PREPAID, NET_30, or NET_60', 1790358812865, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (6, 'dealer.auto_approve', 'false', 'BOOLEAN', 'DEALER', 'Auto-approve self-registered dealer applications', 1790358812865, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (7, 'dealer.min_completed_orders', '0', 'INT', 'DEALER', 'Minimum completed retail orders before a customer can be promoted', 1790358812865, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (8, 'dealer.min_account_age_days', '0', 'INT', 'DEALER', 'Minimum account age in days before a customer can be promoted', 1790358812865, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (9, 'dealer.requires_admin_approval', 'true', 'BOOLEAN', 'DEALER', 'Whether self-registered dealer applications require admin approval', 1790358812865, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (10, 'dealer.tier_bronze_threshold', '0', 'INT', 'DEALER', 'Minimum completed bulk orders for BRONZE tier', 1790503374022, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (11, 'dealer.tier_silver_threshold', '100', 'INT', 'DEALER', 'Minimum completed bulk orders for SILVER tier', 1790503374022, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (12, 'dealer.tier_gold_threshold', '500', 'INT', 'DEALER', 'Minimum completed bulk orders for GOLD tier', 1790503374022, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (13, 'dealer.tier_platinum_threshold', '2000', 'INT', 'DEALER', 'Minimum completed bulk orders for PLATINUM tier', 1790503374022, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (14, 'dealer.tier_bronze_label', 'BRONZE', 'STRING', 'DEALER', 'Display label for bronze tier', 1790503374022, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (15, 'dealer.tier_silver_label', 'SILVER', 'STRING', 'DEALER', 'Display label for silver tier', 1790503374022, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (16, 'dealer.tier_gold_label', 'GOLD', 'STRING', 'DEALER', 'Display label for gold tier', 1790503374022, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (17, 'dealer.tier_platinum_label', 'PLATINUM', 'STRING', 'DEALER', 'Display label for platinum tier', 1790503374022, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (18, 'branding.app_name', 'MkulimaFeeds', 'STRING', 'BRANDING', 'Display name of the app', 1790532931876, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (19, 'branding.company_name', 'MkulimaFeeds Ltd', 'STRING', 'BRANDING', 'Legal company name', 1790532931876, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (20, 'branding.support_email', 'support@mkulimafeeds.co.ke', 'STRING', 'BRANDING', 'Public support email address', 1790532931876, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (21, 'branding.support_phone', '+254700000000', 'STRING', 'BRANDING', 'Public support phone number', 1790532931876, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (22, 'branding.whatsapp_number', '254700000000', 'STRING', 'BRANDING', 'WhatsApp contact number', 1790532931876, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (23, 'branding.website_url', 'https://mkulimafeeds.co.ke', 'STRING', 'BRANDING', 'Company website', 1790532931876, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (24, 'branding.terms_url', 'https://mkulimafeeds.co.ke/terms', 'STRING', 'BRANDING', 'Terms of Service URL', 1790532931876, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (25, 'branding.privacy_url', 'https://mkulimafeeds.co.ke/privacy', 'STRING', 'BRANDING', 'Privacy Policy URL', 1790532931876, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (26, 'branding.about_text', 'MkulimaFeeds is Kenya''s premier supplier of high-quality animal feeds, supplements, and agricultural supplies. We serve farmers, dealers, and agripreneurs nationwide.', 'STRING', 'BRANDING', 'About text shown on the About screen', 1790532931876, NULL);
INSERT INTO public.settings (id, key, value, value_type, category, description, updated_at, updated_by_user_id) VALUES (27, 'branding.version_label', '1.0.0', 'STRING', 'BRANDING', 'App version shown on the About screen', 1790532931876, NULL);


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.users (id, name, phone, email, password_hash, role, is_active, created_at, updated_at, status, dealer_status, business_name, business_region, account_manager_name, account_manager_email, credit_limit, credit_used, payment_terms, lifetime_bulk_orders) VALUES (2, 'Test Customer', '0712345678', 'testcustomer@movofeeds.com', '$2a$12$8JeOpyFv0TQENx6NOXx7Aehpq19pl/VrRMkvcSyRLCa05qMdYXlGe', 'CUSTOMER', true, 1786910728269, 1786910728269, 'ACTIVE', NULL, NULL, NULL, NULL, NULL, 0.00, 0.00, 'PREPAID', 0);
INSERT INTO public.users (id, name, phone, email, password_hash, role, is_active, created_at, updated_at, status, dealer_status, business_name, business_region, account_manager_name, account_manager_email, credit_limit, credit_used, payment_terms, lifetime_bulk_orders) VALUES (1, 'Movin Onyango', '0759288382', 'movin@example.com', '$2a$12$vb6Lkxtc.CoKeVX8rCeFpu.KuPPIJdmq9jKbzjVSSa3eZjhvH1M.e', 'ADMIN', true, 1786885082057, 1787147946740, 'ACTIVE', NULL, NULL, NULL, NULL, NULL, 0.00, 0.00, 'PREPAID', 0);
INSERT INTO public.users (id, name, phone, email, password_hash, role, is_active, created_at, updated_at, status, dealer_status, business_name, business_region, account_manager_name, account_manager_email, credit_limit, credit_used, payment_terms, lifetime_bulk_orders) VALUES (3, 'Second Customer', '0722222222', 'secondcustomer@movofeeds.com', '$2a$12$h9cZS5v.q9gBGrNPd05/3.38xc0Ht7hFWXvzeyIlHym6ZqUHEndsu', 'CUSTOMER', true, 1787216052255, 1787216052255, 'ACTIVE', NULL, NULL, NULL, NULL, NULL, 0.00, 0.00, 'PREPAID', 0);
INSERT INTO public.users (id, name, phone, email, password_hash, role, is_active, created_at, updated_at, status, dealer_status, business_name, business_region, account_manager_name, account_manager_email, credit_limit, credit_used, payment_terms, lifetime_bulk_orders) VALUES (6, 'Updated Test Customer', '0790000002', 'updatedcustomer@movofeeds.com', '$2a$12$nGsLXWpEZSUDaYzC92VAn.7EHs7d87YkgVDZeGCyjr7c4W/FO4zQO', 'CUSTOMER', false, 1789583574556, 1789627385960, 'ACTIVE', NULL, NULL, NULL, NULL, NULL, 0.00, 0.00, 'PREPAID', 0);
INSERT INTO public.users (id, name, phone, email, password_hash, role, is_active, created_at, updated_at, status, dealer_status, business_name, business_region, account_manager_name, account_manager_email, credit_limit, credit_used, payment_terms, lifetime_bulk_orders) VALUES (7, 'Test User', NULL, 'newuser@gmail.com', '$2a$12$tr49pGkXKKxSrPSNO0NVZ.Jglho.sCu0H3QIbfFF8CcUg/Ab5NM9K', 'CUSTOMER', true, 1790264930545, 1790265767696, 'ACTIVE', NULL, NULL, NULL, NULL, NULL, 0.00, 0.00, 'PREPAID', 0);
INSERT INTO public.users (id, name, phone, email, password_hash, role, is_active, created_at, updated_at, status, dealer_status, business_name, business_region, account_manager_name, account_manager_email, credit_limit, credit_used, payment_terms, lifetime_bulk_orders) VALUES (11, 'Movo', '+254111966397', NULL, '$2a$12$s9mLFyohv8s2S9WN3CDdHe.xi8IxhowiFtHSfl81MueFePeBiCsvm', 'CUSTOMER', true, 1790275223613, 1790278594316, 'ACTIVE', NULL, NULL, NULL, NULL, NULL, 0.00, 0.00, 'PREPAID', 0);
INSERT INTO public.users (id, name, phone, email, password_hash, role, is_active, created_at, updated_at, status, dealer_status, business_name, business_region, account_manager_name, account_manager_email, credit_limit, credit_used, payment_terms, lifetime_bulk_orders) VALUES (4, 'Test Dealer', '0700000004', 'dealer@movofeeds.com', 'TEMPORARY', 'DEALER', true, 1788667237927, 1788667237927, 'ACTIVE', 'APPROVED', NULL, NULL, NULL, NULL, 0.00, 0.00, 'PREPAID', 0);
INSERT INTO public.users (id, name, phone, email, password_hash, role, is_active, created_at, updated_at, status, dealer_status, business_name, business_region, account_manager_name, account_manager_email, credit_limit, credit_used, payment_terms, lifetime_bulk_orders) VALUES (5, 'Test Dealer 2', '0712345679', 'dealer2@movofeeds.com', '$2a$12$hiwdNO7QynLmBxJs/afwh.T0Cx/OKjVKo1hXSeU8XbYtYGJ87vKRS', 'DEALER', true, 1789019410180, 1789447043416, 'ACTIVE', 'APPROVED', NULL, NULL, NULL, NULL, 0.00, 0.00, 'PREPAID', 0);
INSERT INTO public.users (id, name, phone, email, password_hash, role, is_active, created_at, updated_at, status, dealer_status, business_name, business_region, account_manager_name, account_manager_email, credit_limit, credit_used, payment_terms, lifetime_bulk_orders) VALUES (18, 'Test User', '0700044423', 'movinoscar45@gmail.com', '$2a$12$EwDNqIekilc2WNBBvkAPsuj90BDX/4UWuR3CMIhIbQPEJNlD2OWR6', 'DEALER', true, 1790329517630, 1790432759030, 'ACTIVE', 'APPROVED', 'Movo Distributors Ltd', 'Nairobi, Kenya', 'Sarah W.', 'sarah@mkulimafeeds.co.ke', 500000.00, 150000.00, 'NET_30', 340);


--
-- Data for Name: verification_codes; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (1, NULL, 'REGISTER', 'EMAIL', 'test@example.com', '$2a$12$XnhOiMAVNzESSDzyrY9vmej.3/sCgfezib1V3UBoKKKjtRjcYHoja', 1790262472276, 1790261911931, 0, 1790261872283);
INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (2, NULL, 'REGISTER', 'EMAIL', 'test@example.com', '$2a$12$qRWKV0NYm4nUxM4rt01EvuDYeELsHen2bhLgBrTbjJ4As3XP0Z6l6', 1790262512845, 1790261984829, 0, 1790261912861);
INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (3, NULL, 'REGISTER', 'EMAIL', 'test@example.com', '$2a$12$VoWzRMMt50rHmKK.5FaX6ephwIu5evW/CePal794FtU1hjoZKY6a6', 1790262585396, NULL, 0, 1790261985396);
INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (4, NULL, 'REGISTER', 'EMAIL', 'newuser@gmail.com', '$2a$12$NkhiMDneZuQNGkM5T251neBYkmJtFJsVb36cWjXX.8Ep8lo0UIqdy', 1790265010494, 1790264930268, 1, 1790264410509);
INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (5, 7, 'PASSWORD_RESET', 'EMAIL', 'newuser@gmail.com', '$2a$12$JKIpL9Z3.5tpc1VYC3uLVOPAmpKRufa1APA22F80/nUHOFNV9kAea', 1790265807066, 1790265717770, 0, 1790265207068);
INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (6, NULL, 'REGISTER', 'EMAIL', 'movinoscar45@gmail.com', '$2a$12$Q9Hn5zWjBEnXe2MsEtoGiOQ/.MGFrBAFK9iymtP73rYA0y6QkphZ.', 1790275575399, 1790275049445, 0, 1790274975430);
INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (7, NULL, 'REGISTER', 'PHONE', '+254111966397', '$2a$12$bLNyfbPMisDlLU.u0wuur.o3rfRYNURhjUYyAtDmoLO7JnOat9rw6', 1790275797094, 1790275223457, 0, 1790275197110);
INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (8, 11, 'PASSWORD_RESET', 'PHONE', '+254111966397', '$2a$12$9ljMef9qis6s8lpBiB27a.kRCe3Aji49rlDZox9da69DF8aru0pb.', 1790279120581, 1790278549730, 0, 1790278520591);
INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (9, NULL, 'REGISTER', 'EMAIL', 'movinoscar45@gmail.com', '$2a$12$5qEQWfRFzwhafoA03hL3GuwUWLLUw7BM2euZJehf.D5RQZF6GLhkS', 1790319070062, 1790318638616, 0, 1790318470069);
INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (10, NULL, 'REGISTER', 'EMAIL', 'movinoscar45@gmail.com', '$2a$12$FRFS9PELqLsVZKPl46cIxua3USH2dwCtcedEwIDA9sra8FMCwqcKC', 1790326617829, 1790326475113, 0, 1790326017837);
INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (11, NULL, 'REGISTER', 'EMAIL', 'victoriandune01@gmail.com', '$2a$12$42FtSYVWfhkiJWq/0k1ydOzUAX0fAWvqPmijWY0P3HOy4fstTW/.q', 1790329830730, 1790329256152, 0, 1790329230730);
INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (13, NULL, 'REGISTER', 'EMAIL', 'movinoscar45@gmail.com', '$2a$12$VCqpw9ednVloPMbPNDNeWek/.RCGEp6/5GcESZBGhC7R3yd7E1MAC', 1790329989416, 1790329517412, 0, 1790329389416);
INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (14, 18, 'PASSWORD_RESET', 'EMAIL', 'movinoscar45@gmail.com', '$2a$12$/rLeDoK5hj/kH.Tn9W2YxOlmrL2RR/0hO21.Z8MVnmndX1dG5n0zi', 1790332535934, 1790332021537, 0, 1790331935934);
INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (15, NULL, 'REGISTER', 'EMAIL', 'palmer@gmail.com', '$2a$12$MFdq.E8ZM/ytHAn8zb3ZBujM4Fi4.RAGAsRrqQ3soDbf05fipBadu', 1790412468174, 1790411890363, 0, 1790411868183);
INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (16, NULL, 'REGISTER', 'EMAIL', 'palmer@gmail.com', '$2a$12$LJnq2IglhufxvNk7bDqJROZL0z/q3vjMr7U5KmfiVuMqXd.zXXNwa', 1790412490801, NULL, 0, 1790411890801);
INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (12, NULL, 'REGISTER', 'EMAIL', 'victoriandune01@gmail.com', '$2a$12$0M5.GHd/7O6P6UmgRievUet26..vswQxnxRlfo9ev2E5dhygMZsxq', 1790329856689, 1790411916661, 0, 1790329256689);
INSERT INTO public.verification_codes (id, user_id, purpose, channel, destination, code_hash, expires_at, verified_at, attempts, created_at) VALUES (17, NULL, 'REGISTER', 'EMAIL', 'victoriandune01@gmail.com', '$2a$12$WAbD.eyNUNOY9rVvN0GpTOMBeoybBqT8Q2R6EgSLW.kK641rALMsW', 1790412517114, NULL, 0, 1790411917114);


--
-- Name: auth_attempts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.auth_attempts_id_seq', 17, true);


--
-- Name: notifications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.notifications_id_seq', 15, true);


--
-- Name: order_items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.order_items_id_seq', 13, true);


--
-- Name: orders_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.orders_id_seq', 12, true);


--
-- Name: products_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.products_id_seq', 77, true);


--
-- Name: role_audit_log_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.role_audit_log_id_seq', 1, true);


--
-- Name: saved_locations_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.saved_locations_id_seq', 2, true);


--
-- Name: settings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.settings_id_seq', 27, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.users_id_seq', 18, true);


--
-- Name: verification_codes_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.verification_codes_id_seq', 17, true);


--
-- Name: auth_attempts auth_attempts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.auth_attempts
    ADD CONSTRAINT auth_attempts_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- Name: role_audit_log role_audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_audit_log
    ADD CONSTRAINT role_audit_log_pkey PRIMARY KEY (id);


--
-- Name: saved_locations saved_locations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saved_locations
    ADD CONSTRAINT saved_locations_pkey PRIMARY KEY (id);


--
-- Name: settings settings_key_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.settings
    ADD CONSTRAINT settings_key_key UNIQUE (key);


--
-- Name: settings settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.settings
    ADD CONSTRAINT settings_pkey PRIMARY KEY (id);


--
-- Name: users users_email_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_unique UNIQUE (email);


--
-- Name: users users_phone_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_phone_unique UNIQUE (phone);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: verification_codes verification_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.verification_codes
    ADD CONSTRAINT verification_codes_pkey PRIMARY KEY (id);


--
-- Name: auth_attempts_attempt_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_attempts_attempt_type ON public.auth_attempts USING btree (attempt_type);


--
-- Name: auth_attempts_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_attempts_created_at ON public.auth_attempts USING btree (created_at);


--
-- Name: auth_attempts_identifier; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX auth_attempts_identifier ON public.auth_attempts USING btree (identifier);


--
-- Name: idx_orders_order_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orders_order_type ON public.orders USING btree (order_type);


--
-- Name: idx_role_audit_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_role_audit_created ON public.role_audit_log USING btree (created_at DESC);


--
-- Name: idx_role_audit_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_role_audit_user ON public.role_audit_log USING btree (user_id);


--
-- Name: idx_settings_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_settings_category ON public.settings USING btree (category);


--
-- Name: notifications_order_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX notifications_order_id ON public.notifications USING btree (order_id);


--
-- Name: notifications_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX notifications_user_id ON public.notifications USING btree (user_id);


--
-- Name: saved_locations_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX saved_locations_user_id ON public.saved_locations USING btree (user_id);


--
-- Name: verification_codes_destination; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX verification_codes_destination ON public.verification_codes USING btree (destination);


--
-- Name: verification_codes_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX verification_codes_user_id ON public.verification_codes USING btree (user_id);


--
-- Name: order_items fk_order_items_order_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fk_order_items_order_id__id FOREIGN KEY (order_id) REFERENCES public.orders(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: order_items fk_order_items_product_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fk_order_items_product_id__id FOREIGN KEY (product_id) REFERENCES public.products(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: orders fk_orders_assigned_dealer; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk_orders_assigned_dealer FOREIGN KEY (assigned_dealer_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE SET NULL;


--
-- Name: orders fk_orders_customer_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT fk_orders_customer_id__id FOREIGN KEY (customer_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: saved_locations fk_saved_locations_user_id__id; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.saved_locations
    ADD CONSTRAINT fk_saved_locations_user_id__id FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: role_audit_log role_audit_log_changed_by_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_audit_log
    ADD CONSTRAINT role_audit_log_changed_by_user_id_fkey FOREIGN KEY (changed_by_user_id) REFERENCES public.users(id);


--
-- Name: role_audit_log role_audit_log_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.role_audit_log
    ADD CONSTRAINT role_audit_log_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- PostgreSQL database dump complete
--

\unrestrict c06j9K8PITaeBVIU7jgD3MQvY7dBIrKKdl5xlLPNxrZ4UBl51Ft718vxR7v6IMI

