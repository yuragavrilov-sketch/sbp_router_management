create table routing_config (
    version    bigint primary key,
    payload    text not null,
    created_at timestamptz not null default now()
);
