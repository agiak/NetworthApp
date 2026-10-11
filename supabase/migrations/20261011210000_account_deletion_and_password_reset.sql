-- ── Deleting an account ───────────────────────────────────────────────────
-- A household outlives the person who created it
alter table public.households alter column created_by drop not null;
alter table public.households drop constraint households_created_by_fkey;
alter table public.households
  add constraint households_created_by_fkey
  foreign key (created_by) references auth.users(id) on delete set null;

-- Before a user is deleted: households only they belong to go with them (and their data);
-- in shared ones the longest-standing member becomes the owner if they were the only one
create or replace function private.before_user_delete()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  m record;
begin
  for m in select household_id, role from public.household_members where user_id = old.id loop
    if not exists (
      select 1 from public.household_members
      where household_id = m.household_id and user_id <> old.id
    ) then
      delete from public.households where id = m.household_id;
    elsif m.role = 'owner' and not exists (
      select 1 from public.household_members
      where household_id = m.household_id and user_id <> old.id and role = 'owner'
    ) then
      update public.household_members set role = 'owner'
      where household_id = m.household_id
        and user_id = (
          select user_id from public.household_members
          where household_id = m.household_id and user_id <> old.id
          order by joined_at
          limit 1
        );
    end if;
  end loop;
  return old;
end;
$$;

revoke all on function private.before_user_delete() from public, anon, authenticated;

create trigger before_user_delete
  before delete on auth.users
  for each row execute function private.before_user_delete();

-- ── Forgot password: temporary passwords ──────────────────────────────────
-- Only the password-reset edge function (service role) reaches these
create table public.password_resets (
  user_id    uuid primary key references auth.users(id) on delete cascade,
  code_hash  text not null,
  expires_at timestamptz not null,
  attempts   int not null default 0,
  created_at timestamptz not null default now()
);

alter table public.password_resets enable row level security;
revoke all on public.password_resets from anon, authenticated;

-- Stores a new temporary password for the account with this email (at most one a minute).
-- Returns the user and their name, or nothing when there's no such account.
create or replace function public.request_password_reset(p_email text, p_code text)
returns table (user_id uuid, display_name text)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user uuid;
begin
  select u.id into v_user from auth.users u where lower(u.email) = lower(trim(p_email));
  if v_user is null then
    return;
  end if;
  if exists (
    select 1 from public.password_resets r
    where r.user_id = v_user and r.created_at > now() - interval '1 minute'
  ) then
    return;
  end if;

  insert into public.password_resets (user_id, code_hash, expires_at)
  values (v_user, extensions.crypt(p_code, extensions.gen_salt('bf')), now() + interval '1 hour')
  on conflict on constraint password_resets_pkey do update
    set code_hash = excluded.code_hash,
        expires_at = excluded.expires_at,
        attempts = 0,
        created_at = now();

  return query
    select v_user, (select p.display_name from public.profiles p where p.user_id = v_user);
end;
$$;

-- Checks a temporary password (5 tries, valid for an hour). Returns the user once it matches,
-- and the temporary password can't be used again.
create or replace function public.redeem_password_reset(p_email text, p_code text)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user uuid;
  v_reset public.password_resets;
begin
  select u.id into v_user from auth.users u where lower(u.email) = lower(trim(p_email));
  if v_user is null then
    return null;
  end if;

  select * into v_reset from public.password_resets r where r.user_id = v_user for update;
  if not found or v_reset.expires_at < now() or v_reset.attempts >= 5 then
    return null;
  end if;

  if v_reset.code_hash = extensions.crypt(p_code, v_reset.code_hash) then
    delete from public.password_resets r where r.user_id = v_user;
    return v_user;
  end if;

  update public.password_resets r set attempts = r.attempts + 1 where r.user_id = v_user;
  return null;
end;
$$;

revoke all on function public.request_password_reset(text, text) from public, anon, authenticated;
revoke all on function public.redeem_password_reset(text, text) from public, anon, authenticated;
grant execute on function public.request_password_reset(text, text) to service_role;
grant execute on function public.redeem_password_reset(text, text) to service_role;
