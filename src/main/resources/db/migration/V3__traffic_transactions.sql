create table traffic_transactions (
    correlation_id varchar(128) primary key,
    tx_id varchar(128),
    request_type varchar(64),
    terminal_owner varchar(128),
    route varchar(128),
    upstream varchar(128),
    outcome varchar(128),
    status varchar(16) not null,
    request_at timestamptz,
    response_at timestamptz,
    latency_ms bigint,
    env varchar(32),
    request_xml text,
    response_xml text,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint traffic_transactions_status_chk check (status in ('PENDING', 'RESPONDED')),
    constraint traffic_transactions_latency_non_negative check (latency_ms is null or latency_ms >= 0)
);

create index traffic_transactions_created_at_idx on traffic_transactions (created_at desc);
create index traffic_transactions_request_type_idx on traffic_transactions (request_type);
create index traffic_transactions_upstream_idx on traffic_transactions (upstream);
create index traffic_transactions_outcome_idx on traffic_transactions (outcome);
create index traffic_transactions_terminal_owner_idx on traffic_transactions (terminal_owner);
create index traffic_transactions_request_at_idx on traffic_transactions (request_at);
