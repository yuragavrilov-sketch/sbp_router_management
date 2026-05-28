create table routing_manifests (
    id uuid primary key,
    version integer not null,
    status varchar(16) not null,
    checksum varchar(128) not null,
    payload_json jsonb not null,
    created_at timestamptz not null,
    constraint routing_manifests_version_positive check (version >= 1),
    constraint routing_manifests_version_uk unique (version),
    constraint routing_manifests_checksum_uk unique (checksum),
    constraint routing_manifests_status_chk check (status = 'VALID'),
    constraint routing_manifests_checksum_shape_chk check (checksum like 'sha256:%')
);

create table routing_manifest_entities (
    manifest_id uuid not null references routing_manifests (id) on delete cascade,
    entity_kind varchar(32) not null,
    entity_id uuid not null,
    entity_version integer not null,
    position integer not null,
    payload_json jsonb not null,
    primary key (manifest_id, entity_id),
    constraint routing_manifest_entities_kind_chk check (
        entity_kind in ('UPSTREAM', 'EXTRACTION_RULE', 'TERMINAL_CONFIG', 'TKB_PAY_ENTRY', 'ROUTING_FLAG')
    ),
    constraint routing_manifest_entities_position_positive check (position >= 0),
    constraint routing_manifest_entities_position_uk unique (manifest_id, position)
);

create index routing_manifests_created_at_idx on routing_manifests (created_at desc);
