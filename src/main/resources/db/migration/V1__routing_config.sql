create table upstreams (
    id uuid primary key,
    name varchar(128) not null,
    url varchar(1024),
    timeout_ms integer,
    retry_max_attempts integer,
    retry_backoff_ms integer,
    status varchar(16) not null,
    removal boolean not null default false,
    version integer not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint upstreams_status_chk check (status in ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    constraint upstreams_version_positive check (version >= 1)
);

create table extraction_rules (
    id uuid primary key,
    message_type varchar(64) not null,
    routing_fields_json jsonb not null,
    extra_fields_json jsonb not null,
    status varchar(16) not null,
    removal boolean not null default false,
    version integer not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint extraction_rules_status_chk check (status in ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    constraint extraction_rules_version_positive check (version >= 1)
);

create table terminal_routing_config (
    id uuid primary key,
    c2b_field_name varchar(64),
    b2c_field_name varchar(64),
    tkb_pay_prefix varchar(32),
    status varchar(16) not null,
    removal boolean not null default false,
    version integer not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint terminal_config_status_chk check (status in ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    constraint terminal_config_version_positive check (version >= 1)
);

-- At most one ACTIVE terminal routing config at a time.
create unique index terminal_config_single_active_uk
    on terminal_routing_config ((1))
    where status = 'ACTIVE';

create table tkb_pay_list_entries (
    id uuid primary key,
    rcv_tsp_id varchar(64) not null,
    status varchar(16) not null,
    removal boolean not null default false,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint tkb_pay_entries_status_chk check (status in ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

create table routing_flags (
    id uuid primary key,
    key varchar(128) not null,
    value varchar(512),
    status varchar(16) not null,
    removal boolean not null default false,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint routing_flags_status_chk check (status in ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

create unique index upstreams_active_name_uk on upstreams (name) where status = 'ACTIVE';
create unique index extraction_rules_active_type_uk on extraction_rules (message_type) where status = 'ACTIVE';
create unique index tkb_pay_entries_active_uk on tkb_pay_list_entries (rcv_tsp_id) where status = 'ACTIVE';
create unique index routing_flags_active_key_uk on routing_flags (key) where status = 'ACTIVE';
