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

Sign-up has no email verification: the app calls the `signup` edge function
(`functions/signup`, deployed without JWT verification since it is the public sign-up), which
creates the user already confirmed with the admin API and sends no email. The app then signs in
with the same email and password.

Every table has RLS: a signed-in user only reaches the rows of households they belong to, and
anonymous requests see nothing. People join a household with an 8-character invite code
(`create_household_invite` / `join_household`).
