-- Sign-ups don't need to confirm their email: mark it confirmed as the user is created
create or replace function private.auto_confirm_email()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if new.email is not null and new.email_confirmed_at is null then
    new.email_confirmed_at := now();
  end if;
  return new;
end;
$$;

revoke all on function private.auto_confirm_email() from public, anon, authenticated;

create trigger auto_confirm_email
  before insert on auth.users
  for each row execute function private.auto_confirm_email();
