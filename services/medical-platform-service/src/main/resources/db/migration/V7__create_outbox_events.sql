create table outbox_events (

id uuid primary key,
event_type varchar(100) not null,
aggregate_id uuid not null,
payload text not null,
status varchar(30) not null,
attempts integer not null,
last_error text,
created_at timestamptz not null,
updated_at timestamptz not null,
published_at timestamptz
);

create index idx_outbox_events_status_created_at
on outbox_events(status, created_at);
