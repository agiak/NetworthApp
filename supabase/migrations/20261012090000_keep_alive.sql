-- ── Keep the project awake ────────────────────────────────────────────────
-- Free projects are paused after a week without activity. A scheduled GitHub Action
-- (.github/workflows/supabase-keep-alive.yml) calls keep_alive() every day; the app never does.

create table private.keep_alive (
  id        smallint primary key default 1 check (id = 1),
  last_ping timestamptz not null default now(),
  pings     bigint not null default 0
);
alter table private.keep_alive enable row level security;

-- Takes no input and touches only its own row, so it is safe to expose with the publishable key
create or replace function public.keep_alive()
returns timestamptz
language sql
security definer
set search_path = ''
as $$
  insert into private.keep_alive (id, last_ping, pings) values (1, now(), 1)
  on conflict (id) do update set last_ping = now(), pings = private.keep_alive.pings + 1
  returning last_ping;
$$;

revoke all on function public.keep_alive() from public, authenticated;
grant execute on function public.keep_alive() to anon;
