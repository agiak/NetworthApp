-- Membership helpers are only for RLS policies: keep them out of the exposed API schema
create schema if not exists private;
revoke all on schema private from public, anon;
grant usage on schema private to authenticated;

alter function public.is_household_member(uuid) set schema private;
alter function public.is_household_owner(uuid) set schema private;

-- Functions that call the helpers by name need the new schema
create or replace function public.create_household_invite(p_household_id uuid)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_code text;
begin
    if not private.is_household_member(p_household_id) then
        raise exception 'Not a member of this household' using errcode = '42501';
    end if;
    v_code := upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8));
    insert into public.household_invites (code, household_id, created_by)
    values (v_code, p_household_id, (select auth.uid()));
    return v_code;
end;
$$;

create index household_invites_created_by_idx on public.household_invites(created_by);
create index household_invites_used_by_idx on public.household_invites(used_by);
create index households_created_by_idx on public.households(created_by);
create index net_worth_entries_account_household_idx on public.net_worth_entries(account_id, household_id);
