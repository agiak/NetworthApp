-- Households: the unit that owns all data. Anastasis and Christina share one.
create table public.households (
    id          uuid primary key default gen_random_uuid(),
    name        text not null check (length(trim(name)) between 1 and 100),
    created_by  uuid not null default auth.uid() references auth.users(id) on delete restrict,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create table public.household_members (
    household_id uuid not null references public.households(id) on delete cascade,
    user_id      uuid not null references auth.users(id) on delete cascade,
    role         text not null default 'member' check (role in ('owner', 'member')),
    joined_at    timestamptz not null default now(),
    primary key (household_id, user_id)
);
create index household_members_user_idx on public.household_members(user_id);

-- One-time codes to invite another person into a household
create table public.household_invites (
    code         text primary key,
    household_id uuid not null references public.households(id) on delete cascade,
    created_by   uuid not null references auth.users(id) on delete cascade,
    created_at   timestamptz not null default now(),
    expires_at   timestamptz not null default now() + interval '7 days',
    used_by      uuid references auth.users(id) on delete set null,
    used_at      timestamptz
);
create index household_invites_household_idx on public.household_invites(household_id);

-- Per-user profile (name, net worth target)
create table public.profiles (
    user_id       uuid primary key references auth.users(id) on delete cascade,
    display_name  text not null default '' check (length(display_name) <= 100),
    target_amount numeric(14,2) not null default 0 check (target_amount >= 0),
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
);

-- ── Helpers ─────────────────────────────────────────────────────────────────

create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

-- Security definer so policies can check membership without recursing into RLS
create or replace function public.is_household_member(p_household_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1 from public.household_members m
        where m.household_id = p_household_id and m.user_id = (select auth.uid())
    );
$$;

create or replace function public.is_household_owner(p_household_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1 from public.household_members m
        where m.household_id = p_household_id and m.user_id = (select auth.uid()) and m.role = 'owner'
    );
$$;

-- The creator of a household becomes its owner
create or replace function public.add_household_owner()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.household_members (household_id, user_id, role)
    values (new.id, new.created_by, 'owner');
    return new;
end;
$$;

create trigger households_add_owner
    after insert on public.households
    for each row execute function public.add_household_owner();

create trigger households_updated_at
    before update on public.households
    for each row execute function public.set_updated_at();

create trigger profiles_updated_at
    before update on public.profiles
    for each row execute function public.set_updated_at();

-- A profile row for every new user
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.profiles (user_id, display_name)
    values (new.id, coalesce(new.raw_user_meta_data ->> 'display_name', ''))
    on conflict (user_id) do nothing;
    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- ── Invites ─────────────────────────────────────────────────────────────────

-- Any member can create an 8-character code valid for 7 days
create or replace function public.create_household_invite(p_household_id uuid)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_code text;
begin
    if not public.is_household_member(p_household_id) then
        raise exception 'Not a member of this household' using errcode = '42501';
    end if;
    v_code := upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8));
    insert into public.household_invites (code, household_id, created_by)
    values (v_code, p_household_id, (select auth.uid()));
    return v_code;
end;
$$;

-- Joins the household of a valid, unused invite and returns its id
create or replace function public.join_household(p_code text)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_invite public.household_invites;
begin
    if (select auth.uid()) is null then
        raise exception 'Not signed in' using errcode = '42501';
    end if;

    select * into v_invite
    from public.household_invites
    where code = upper(trim(p_code))
    for update;

    if v_invite.code is null or v_invite.used_by is not null or v_invite.expires_at < now() then
        raise exception 'Invite code is invalid or expired' using errcode = 'P0002';
    end if;

    insert into public.household_members (household_id, user_id, role)
    values (v_invite.household_id, (select auth.uid()), 'member')
    on conflict do nothing;

    update public.household_invites
    set used_by = (select auth.uid()), used_at = now()
    where code = v_invite.code;

    return v_invite.household_id;
end;
$$;

revoke execute on function public.is_household_member(uuid) from public, anon;
revoke execute on function public.is_household_owner(uuid) from public, anon;
revoke execute on function public.create_household_invite(uuid) from public, anon;
revoke execute on function public.join_household(text) from public, anon;
revoke execute on function public.add_household_owner() from public, anon, authenticated;
revoke execute on function public.handle_new_user() from public, anon, authenticated;
grant execute on function public.is_household_member(uuid) to authenticated;
grant execute on function public.is_household_owner(uuid) to authenticated;
grant execute on function public.create_household_invite(uuid) to authenticated;
grant execute on function public.join_household(text) to authenticated;

-- ── Row level security ──────────────────────────────────────────────────────

alter table public.households enable row level security;
alter table public.household_members enable row level security;
alter table public.household_invites enable row level security;
alter table public.profiles enable row level security;

create policy "Members read their households" on public.households
    for select to authenticated using (public.is_household_member(id));
create policy "Users create households" on public.households
    for insert to authenticated with check (created_by = (select auth.uid()));
create policy "Owners rename households" on public.households
    for update to authenticated using (public.is_household_owner(id)) with check (public.is_household_owner(id));
create policy "Owners delete households" on public.households
    for delete to authenticated using (public.is_household_owner(id));

create policy "Members see who is in their households" on public.household_members
    for select to authenticated using (public.is_household_member(household_id));
-- Leave a household yourself, or the owner removes someone
create policy "Leave or remove members" on public.household_members
    for delete to authenticated using (user_id = (select auth.uid()) or public.is_household_owner(household_id));

create policy "Members see their household invites" on public.household_invites
    for select to authenticated using (public.is_household_member(household_id));
create policy "Members revoke invites" on public.household_invites
    for delete to authenticated using (public.is_household_member(household_id));

create policy "Users read their profile" on public.profiles
    for select to authenticated using (user_id = (select auth.uid()));
create policy "Users update their profile" on public.profiles
    for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

revoke all on public.households, public.household_members, public.household_invites, public.profiles from anon;
