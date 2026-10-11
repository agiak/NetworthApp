# Supabase backend

Project **NetworthApp** (`hjobtzpaqxehngjgfhfr`, eu-central-1). The app talks to it from
`cloud/` (see `SupabaseConfig`, `SupabaseClient`, `SyncManager`).

`migrations/` holds the SQL applied to the project, in order:

1. `households_and_members` — households, members, invite codes, profiles, row level security
2. `finance_data` — accounts, net worth entries, fixed expenses, category feedback (soft deletes,
   `updated_at` triggers, members-only policies)
3. `private_helpers_and_indexes` — membership helpers moved to a non-exposed `private` schema,
   extra indexes on foreign keys
4. `auto_confirm_email` — new users are marked confirmed as they are created
5. `account_deletion_and_password_reset` — deleting a user deletes the households only they
   belong to (shared ones pass ownership on); temporary passwords for "forgot password"

Sign-up has no email verification: the app calls the `signup` edge function
(`functions/signup`, deployed without JWT verification since it is the public sign-up), which
creates the user already confirmed with the admin API and sends no email. The app then signs in
with the same email and password.

Every table has RLS: a signed-in user only reaches the rows of households they belong to, and
anonymous requests see nothing. People join a household with an 8-character invite code
(`create_household_invite` / `join_household`).

Edge functions (`functions/`), all deployed without gateway JWT verification:

- `signup` — public sign-up, see above.
- `password-reset` — "forgot password". `request` emails a temporary password (through Brevo)
  and answers the same whether or not the account exists; `redeem` checks it (5 tries, 1 hour,
  once) and makes it the password, marking the user `must_change_password` so the app asks for
  a new one. The old password works until the temporary one is used. The app calls `redeem`
  only after a normal sign-in fails.
- `delete-account` — checks the caller's access token itself, then deletes the user.

Secrets the functions need (Dashboard → Edge Functions → Secrets): `BREVO_API_KEY`; optionally
`EMAIL_SENDER_ADDRESS` (a verified Brevo sender, default `agcodinglp@gmail.com`) and
`EMAIL_SENDER_NAME` (default `NetWorth`).
