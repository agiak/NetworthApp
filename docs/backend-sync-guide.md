# Backend & Sync: οδηγός από το NetworthApp

Τι προσθέσαμε στο NetworthApp για να γίνει από offline εφαρμογή «online εφαρμογή με χρήστες», πώς δουλεύει, πώς συντηρείται, και πώς το ίδιο σύστημα μπαίνει σε άλλη εφαρμογή που σήμερα παίζει μόνο offline.

> Ο οδηγός είναι γραμμένος για να τον δώσεις αυτούσιο σε έναν developer ή στον Claude στο άλλο project. Τα κομμάτια κώδικα είναι τα πραγματικά από το NetworthApp (Kotlin, Room, Hilt, Compose, DataStore, OkHttp, Supabase).

---

## 1. Τι δυνατότητες προστέθηκαν

**Λογαριασμοί χρηστών**
- Εγγραφή με email + κωδικό, **χωρίς επιβεβαίωση email** (ο χρήστης μπαίνει αμέσως).
- Σύνδεση με email + κωδικό.
- Κουμπί εμφάνισης/απόκρυψης κωδικού σε κάθε πεδίο κωδικού.
- «Ξέχασα τον κωδικό»: στέλνεται email με **προσωρινό κωδικό** (10 χαρακτήρες, ισχύει 1 ώρα, μία χρήση, 5 προσπάθειες). Με τη σύνδεση η εφαρμογή **αναγκάζει** τον χρήστη να βάλει νέο κωδικό πριν προχωρήσει. Ο παλιός κωδικός δουλεύει μέχρι να χρησιμοποιηθεί ο προσωρινός.
- Αλλαγή κωδικού (ζητάει τον τρέχοντα).
- Διαγραφή λογαριασμού με επιβεβαίωση κωδικού.
- Αποσύνδεση: ανεβάζει πρώτα ό,τι εκκρεμεί και μετά **σβήνει όλα τα δεδομένα από το κινητό** και γυρνάει στη σελίδα Σύνδεση / Εγγραφή, όπως μια κανονική online εφαρμογή.

**Συγχρονισμός (sync)**
- Τα δεδομένα μένουν στη Room βάση του κινητού. Η εφαρμογή **δουλεύει κανονικά offline** και ο συγχρονισμός γίνεται από πίσω.
- Αμφίδρομος συγχρονισμός (two-way, three-way merge) ανά πίνακα: αλλαγές, νέες εγγραφές και διαγραφές πάνε και από τις δύο μεριές.
- Αλλαγές που έγιναν χωρίς internet μένουν «εκκρεμείς» (επιβιώνουν και σε restart) και ανεβαίνουν μόλις επανέλθει η σύνδεση.
- Συγχρονίζει: όταν ανοίγει η εφαρμογή, 1,5" μετά από κάθε αλλαγή, και όταν επανέρχεται το internet.
- Ένδειξη κατάστασης στις Ρυθμίσεις: Online / Συγχρονίζει / Χωρίς internet / Σφάλμα / Offline mode, μαζί με την ώρα του τελευταίου sync και αν υπάρχουν εκκρεμείς αλλαγές.
- **Νοικοκυριό (household)**: πολλοί χρήστες μοιράζονται τα ίδια δεδομένα, με κωδικό πρόσκλησης 8 χαρακτήρων που ισχύει 7 μέρες. Αυτό είναι ειδικό για το NetworthApp. Στο νέο project μάλλον δεν χρειάζεται (βλ. §6).

**Ροές οθόνης**
- **Νέα εγκατάσταση:** σελίδα «Καλώς ήρθες» με επιλογές Σύνδεση / Δημιουργία λογαριασμού / Συνέχεια χωρίς λογαριασμό. Με σύνδεση σε υπάρχοντα λογαριασμό κατεβαίνουν τα δεδομένα του με μπάρα προόδου.
- **Υπάρχων offline χρήστης:** οθόνη σε όλη την επιφάνεια «Τα δεδομένα σου είναι μόνο σε αυτό το κινητό» → λογαριασμός → ανέβασμα με μπάρα προόδου → επιτυχία → **Συνέχεια**. Αν πατήσει «Όχι τώρα», ξαναρωτάει σε 7 μέρες.

**Backend συντήρηση**
- GitHub Action που «ξυπνάει» τη βάση κάθε μέρα, ώστε το δωρεάν Supabase project να μη γίνει ποτέ pause.

---

## 2. Αρχιτεκτονική με μια ματιά

```
┌──────────────────── Android app ─────────────────────┐        ┌──────────── Supabase ────────────┐
│ UI (Compose)                                          │        │ Auth (email+password, JWT)        │
│  ├ Welcome / SyncOnboarding / Cloud / Account screens │        │ PostgREST  /rest/v1/...           │
│  └ ViewModels (MVI)                                   │ HTTPS  │  ├ πίνακες με RLS ανά χρήστη/νοικ.│
│ CloudRepository  (sign in/up, household, sign out)    │◄──────►│  └ RPC functions                  │
│ SyncManager      (debounce, mutex, progress, merge)   │ OkHttp │ Edge Functions (Deno)             │
│ SupabaseClient   (μικρός REST client, χωρίς SDK)      │        │  ├ signup (χωρίς email verify)    │
│ CloudSessionStore (DataStore: tokens, household, …)   │        │  ├ password-reset (Brevo email)   │
│ NetworkMonitor   (online/offline)                     │        │  └ delete-account                 │
│ Room: υπάρχοντες πίνακες + sync_links                 │        │ private.keep_alive ◄── GitHub     │
└───────────────────────────────────────────────────────┘        │                       Action/μέρα │
                                                                 └───────────────────────────────────┘
```

Βασικές αποφάσεις και γιατί:

| Απόφαση | Γιατί |
|---|---|
| **Χωρίς Supabase SDK**, δικός μας REST client με OkHttp | Χρειαζόμαστε ~10 κλήσεις. Αποφεύγουμε Ktor, serialization και αλλαγές εκδόσεων Kotlin. |
| **Τα τοπικά Room ids δεν αλλάζουν**. Πίνακας `sync_links` αντιστοιχίζει local id ↔ remote UUID | Μηδενικές αλλαγές στις υπάρχουσες οντότητες και migrations. Το sync προστίθεται «από πάνω». |
| Το κινητό φτιάχνει τα UUID των νέων εγγραφών | Δημιουργία offline χωρίς να περιμένει τον server, και ασφαλές retry μετά από crash. |
| **Soft delete** (`deleted_at`) στον server | Οι άλλες συσκευές μαθαίνουν ότι κάτι σβήστηκε. |
| Merge με **hash ανά γραμμή** (SHA-256 των πεδίων) | Ξέρουμε ποια πλευρά άλλαξε από το τελευταίο sync χωρίς ρολόγια ή timestamps. |
| Σε σύγκρουση **κερδίζει το κινητό** | Απλό και προβλέψιμο για 1–2 χρήστες. |
| RLS σε όλους τους πίνακες | Το publishable key είναι μέσα στο APK. Η ασφάλεια είναι αποκλειστικά στη βάση. |
| Edge functions για signup, reset, delete | Χρειάζονται service-role (admin) δικαιώματα, που δεν μπαίνουν ποτέ στο app. |

---

## 3. Backend (Supabase)

### 3.1 Project
- NetworthApp: project `hjobtzpaqxehngjgfhfr`, region eu-central-1 (Frankfurt), organization **WhiteLabel**.
- Στο app χρειάζονται μόνο το **URL** και το **publishable key** (Dashboard → Project Settings → API Keys). Είναι δημόσια, οπότε μπαίνουν στον κώδικα:

```kotlin
object SupabaseConfig {
    const val URL = "https://<project-ref>.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_..."
}
```

- Το **secret / service_role key** δεν μπαίνει ποτέ στο app ή στο repo. Υπάρχει μόνο αυτόματα μέσα στις edge functions (`SUPABASE_SERVICE_ROLE_KEY`).

> ⚠️ **Όριο free plan:** κάθε λογαριασμός έχει 2 ενεργά δωρεάν projects. Αν έχεις ήδη 2 ενεργά (π.χ. WhiteProject + NetworthApp), για τρίτο χρειάζεται να κάνεις pause/delete κάποιο ή να πας σε Pro. Εναλλακτικά, το νέο app μπαίνει στο ίδιο project με δικούς του πίνακες (π.χ. prefix ή ξεχωριστό schema). Ένα project ανά app είναι πιο καθαρό.

### 3.2 Migrations (με τη σειρά)
Βρίσκονται στο `supabase/migrations/` του NetworthApp:

| # | Αρχείο | Τι κάνει |
|---|---|---|
| 1 | `households_and_members` | households, members, invites, profiles, helper functions, triggers, RLS |
| 2 | `finance_data` | οι συγχρονιζόμενοι πίνακες (accounts, net_worth_entries, fixed_expenses, category_feedback) με `updated_at` / `deleted_at` και RLS μελών |
| 3 | `private_helpers_and_indexes` | οι helpers του RLS μεταφέρονται στο μη εκτεθειμένο schema `private`, και προστίθενται indexes στα foreign keys |
| 4 | `auto_confirm_email` | trigger που σημειώνει το email επιβεβαιωμένο τη στιγμή που δημιουργείται ο χρήστης |
| 5 | `account_deletion_and_password_reset` | σωστή διαγραφή χρήστη, πίνακας και functions για προσωρινούς κωδικούς |
| 6 | `keep_alive` | `public.keep_alive()` για το GitHub Action |

#### Μοτίβο συγχρονιζόμενου πίνακα (το σημαντικότερο)

```sql
create table public.accounts (
    id               uuid primary key default gen_random_uuid(),   -- το app στέλνει δικό του UUID
    household_id     uuid not null references public.households(id) on delete cascade,
    name             text not null check (length(trim(name)) between 1 and 100),
    starting_balance numeric(14,2) not null default 0,
    color_hex        text not null default '#76C893' check (color_hex ~ '^#[0-9A-Fa-f]{6}$'),
    monthly_salary   numeric(14,2) not null default 0 check (monthly_salary >= 0),
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now(),
    deleted_at       timestamptz,                                   -- soft delete
    unique (id, household_id)
);
create index accounts_household_sync_idx on public.accounts(household_id, updated_at);

create trigger accounts_updated_at before update on public.accounts
    for each row execute function public.set_updated_at();

alter table public.accounts enable row level security;
create policy "Members read accounts"   on public.accounts for select to authenticated using (private.is_household_member(household_id));
create policy "Members add accounts"    on public.accounts for insert to authenticated with check (private.is_household_member(household_id));
create policy "Members change accounts" on public.accounts for update to authenticated using (private.is_household_member(household_id)) with check (private.is_household_member(household_id));
create policy "Members delete accounts" on public.accounts for delete to authenticated using (private.is_household_member(household_id));
revoke all on public.accounts from anon;
```

Κανόνες:
- Τα CHECK constraints του server πρέπει να ταιριάζουν με ό,τι στέλνει το app. Γι' αυτό το app «καθαρίζει» τις τιμές πριν το sync (κόβει μήκη, βάζει default χρώμα κ.λπ.).
- Τα ποσά είναι `numeric(14,2)`. Το app τα στέλνει πάντα με 2 δεκαδικά, ώστε το hash να βγαίνει ίδιο και στις δύο πλευρές.
- Οι αναφορές σε άλλον πίνακα (π.χ. `account_id`) είναι remote UUIDs, και το app τις μεταφράζει από/προς τοπικά ids.

Βοηθητικές functions (security definer, σε schema `private` ώστε να μην εκτίθενται στο API):

```sql
create schema if not exists private;
revoke all on schema private from public, anon;
grant usage on schema private to authenticated;

create or replace function public.set_updated_at() returns trigger language plpgsql set search_path = '' as $$
begin new.updated_at := now(); return new; end; $$;

create or replace function private.is_household_member(p_household_id uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (select 1 from public.household_members m
                 where m.household_id = p_household_id and m.user_id = (select auth.uid()));
$$;
```

Profile για κάθε νέο χρήστη (κρατάει το όνομα που έδωσε στην εγγραφή):

```sql
create table public.profiles (
    user_id      uuid primary key references auth.users(id) on delete cascade,
    display_name text not null default '' check (length(display_name) <= 100),
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);
alter table public.profiles enable row level security;
create policy "Users read their profile" on public.profiles for select to authenticated using (user_id = (select auth.uid()));
create policy "Users update their profile" on public.profiles for update to authenticated
    using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

create or replace function public.handle_new_user() returns trigger language plpgsql security definer set search_path = '' as $$
begin
  insert into public.profiles (user_id, display_name)
  values (new.id, coalesce(new.raw_user_meta_data ->> 'display_name', ''))
  on conflict (user_id) do nothing;
  return new;
end; $$;
create trigger on_auth_user_created after insert on auth.users for each row execute function public.handle_new_user();
revoke execute on function public.handle_new_user() from public, anon, authenticated;
```

Εγγραφή χωρίς επιβεβαίωση email (δίχτυ ασφαλείας, πέρα από την edge function):

```sql
create or replace function private.auto_confirm_email() returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if new.email is not null and new.email_confirmed_at is null then new.email_confirmed_at := now(); end if;
  return new;
end; $$;
revoke all on function private.auto_confirm_email() from public, anon, authenticated;
create trigger auto_confirm_email before insert on auth.users for each row execute function private.auto_confirm_email();
```

Προσωρινοί κωδικοί για το «Ξέχασα τον κωδικό». Τους αγγίζει μόνο η edge function, με service role:

```sql
create table public.password_resets (
  user_id    uuid primary key references auth.users(id) on delete cascade,
  code_hash  text not null,            -- bcrypt, ποτέ ο κωδικός σε καθαρό κείμενο
  expires_at timestamptz not null,
  attempts   int not null default 0,
  created_at timestamptz not null default now()
);
alter table public.password_resets enable row level security;
revoke all on public.password_resets from anon, authenticated;

-- Νέος προσωρινός κωδικός (το πολύ 1 ανά λεπτό). Επιστρέφει user + όνομα, ή τίποτα αν δεν υπάρχει λογαριασμός.
create or replace function public.request_password_reset(p_email text, p_code text)
returns table (user_id uuid, display_name text) language plpgsql security definer set search_path = '' as $$
declare v_user uuid;
begin
  select u.id into v_user from auth.users u where lower(u.email) = lower(trim(p_email));
  if v_user is null then return; end if;
  if exists (select 1 from public.password_resets r where r.user_id = v_user and r.created_at > now() - interval '1 minute') then return; end if;
  insert into public.password_resets (user_id, code_hash, expires_at)
  values (v_user, extensions.crypt(p_code, extensions.gen_salt('bf')), now() + interval '1 hour')
  on conflict on constraint password_resets_pkey do update
    set code_hash = excluded.code_hash, expires_at = excluded.expires_at, attempts = 0, created_at = now();
  return query select v_user, (select p.display_name from public.profiles p where p.user_id = v_user);
end; $$;

-- Ελέγχει τον προσωρινό κωδικό (5 προσπάθειες, 1 ώρα, μία χρήση).
create or replace function public.redeem_password_reset(p_email text, p_code text)
returns uuid language plpgsql security definer set search_path = '' as $$
declare v_user uuid; v_reset public.password_resets;
begin
  select u.id into v_user from auth.users u where lower(u.email) = lower(trim(p_email));
  if v_user is null then return null; end if;
  select * into v_reset from public.password_resets r where r.user_id = v_user for update;
  if not found or v_reset.expires_at < now() or v_reset.attempts >= 5 then return null; end if;
  if v_reset.code_hash = extensions.crypt(p_code, v_reset.code_hash) then
    delete from public.password_resets r where r.user_id = v_user;
    return v_user;
  end if;
  update public.password_resets r set attempts = r.attempts + 1 where r.user_id = v_user;
  return null;
end; $$;

revoke all on function public.request_password_reset(text, text) from public, anon, authenticated;
revoke all on function public.redeem_password_reset(text, text) from public, anon, authenticated;
grant execute on function public.request_password_reset(text, text) to service_role;
grant execute on function public.redeem_password_reset(text, text) to service_role;
```

Διαγραφή χρήστη στο NetworthApp: ένα trigger `before delete on auth.users` σβήνει τα νοικοκυριά όπου ήταν μόνος, και στα κοινά περνάει την ιδιοκτησία στο παλαιότερο μέλος. **Με δεδομένα ανά χρήστη (§6) δεν χρειάζεται**: αρκεί `on delete cascade` στο `user_id`.

Keep-alive:

```sql
create table private.keep_alive (
  id smallint primary key default 1 check (id = 1),
  last_ping timestamptz not null default now(),
  pings bigint not null default 0
);
alter table private.keep_alive enable row level security;

create or replace function public.keep_alive() returns timestamptz language sql security definer set search_path = '' as $$
  insert into private.keep_alive (id, last_ping, pings) values (1, now(), 1)
  on conflict (id) do update set last_ping = now(), pings = private.keep_alive.pings + 1
  returning last_ping;
$$;
revoke all on function public.keep_alive() from public, authenticated;
grant execute on function public.keep_alive() to anon;
```

### 3.3 Edge functions (Deno, `supabase/functions/<name>/index.ts`)

Όλες γίνονται deploy **χωρίς JWT verification στο gateway** (`--no-verify-jwt`), γιατί καλούνται και από αποσυνδεδεμένους χρήστες. Κάθε μία ελέγχει μόνη της την είσοδο ή το token.

**`signup`**: δημιουργεί τον χρήστη ήδη επιβεβαιωμένο και δεν στέλνει email.

```ts
import { createClient } from "npm:@supabase/supabase-js@2.45.4";

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_PASSWORD = 6;
const MAX_PASSWORD = 72;

function reply(status: number, body: Record<string, unknown>): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return reply(405, { error_code: "method_not_allowed", msg: "Use POST" });
  let body: Record<string, unknown>;
  try { body = await req.json(); } catch { return reply(400, { error_code: "validation_failed", msg: "Invalid JSON" }); }

  const email = String(body.email ?? "").trim().toLowerCase();
  const password = String(body.password ?? "");
  const displayName = String(body.display_name ?? "").trim().slice(0, 100);
  if (email.length > 254 || !EMAIL.test(email)) return reply(400, { error_code: "email_address_invalid", msg: "Invalid email address" });
  if (password.length < MIN_PASSWORD || password.length > MAX_PASSWORD) {
    return reply(400, { error_code: "weak_password", msg: `Password must be ${MIN_PASSWORD}-${MAX_PASSWORD} characters` });
  }

  const url = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !serviceKey) return reply(500, { error_code: "unexpected_failure", msg: "Server is not configured" });
  const admin = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });

  const { data, error } = await admin.auth.admin.createUser({
    email, password, email_confirm: true, user_metadata: { display_name: displayName },
  });
  if (error) {
    const status = error.status && error.status >= 400 && error.status < 600 ? error.status : 500;
    return reply(status, { error_code: error.code ?? "unexpected_failure", msg: error.message });
  }
  return reply(200, { id: data.user?.id });
});
```

**`delete-account`**: ελέγχει το access token του καλούντα και σβήνει τον ίδιο.

```ts
import { createClient } from "npm:@supabase/supabase-js@2.45.4";

function reply(status: number, body: Record<string, unknown>): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return reply(405, { error_code: "method_not_allowed", msg: "Use POST" });
  const token = (req.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "").trim();
  if (!token) return reply(401, { error_code: "no_authorization", msg: "Sign in first" });

  const url = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !serviceKey) return reply(500, { error_code: "unexpected_failure", msg: "Server is not configured" });
  const admin = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });

  const { data, error } = await admin.auth.getUser(token);
  if (error || !data.user) return reply(401, { error_code: "bad_jwt", msg: "Session expired" });

  const { error: deleteError } = await admin.auth.admin.deleteUser(data.user.id);
  if (deleteError) return reply(500, { error_code: "unexpected_failure", msg: "Could not delete the account" });
  return reply(200, { ok: true });
});
```

**`password-reset`**: δύο ενέργειες.
- `{"action":"request","email":"…","lang":"el"}`
  - Παράγει κωδικό 10 χαρακτήρων από αλφάβητο χωρίς μπερδεμένους χαρακτήρες (`abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789`, χωρίς 0/O/1/l/I) με `crypto.getRandomValues`.
  - Καλεί `rpc("request_password_reset")`. Αν επιστρέψει γραμμή, στέλνει email μέσω **Brevo** (`POST https://api.brevo.com/v3/smtp/email`, header `api-key`) σε Ελληνικά ή Αγγλικά, με HTML και text.
  - **Απαντάει πάντα `{ok:true}`**, είτε υπάρχει ο λογαριασμός είτε όχι, για να μη μαθαίνει κανείς ποια email είναι εγγεγραμμένα. Αν αποτύχει η αποστολή απαντάει 502 `email_send_failed`.
- `{"action":"redeem","email":"…","password":"…"}`
  - Καλεί `rpc("redeem_password_reset")`.
  - Αν ταιριάξει: `admin.auth.admin.updateUserById(id, { password, user_metadata: { must_change_password: true } })`. Ο προσωρινός γίνεται ο κωδικός του λογαριασμού, μαρκαρισμένος για αλλαγή.
  - Αλλιώς 400 `invalid_credentials`.

Πλήρης κώδικας: `supabase/functions/password-reset/index.ts` στο NetworthApp. Αντιγράφεται όπως είναι, αλλάζοντας μόνο τα κείμενα και το όνομα της εφαρμογής.

**Secrets των functions** (Dashboard → Edge Functions → Secrets):
- `BREVO_API_KEY` (υποχρεωτικό για το «Ξέχασα τον κωδικό»).
- `EMAIL_SENDER_ADDRESS`: επαληθευμένος sender στο Brevo. Default `agcodinglp@gmail.com`.
- `EMAIL_SENDER_NAME`: default `NetWorth`.

### 3.4 Keep-alive (GitHub Action)
Τα δωρεάν projects γίνονται pause μετά από 7 μέρες χωρίς δραστηριότητα. Το `.github/workflows/supabase-keep-alive.yml` καλεί την `keep_alive()` κάθε μέρα. Η εφαρμογή δεν συμμετέχει.

```yaml
name: Supabase keep-alive
on:
  schedule:
    - cron: "17 6 * * *"   # κάθε μέρα 06:17 UTC
  workflow_dispatch:
permissions:
  actions: write           # για να ξαναενεργοποιεί τον εαυτό του
  contents: read
env:
  SUPABASE_URL: https://<project-ref>.supabase.co
  SUPABASE_PUBLISHABLE_KEY: sb_publishable_...
jobs:
  ping:
    runs-on: ubuntu-latest
    timeout-minutes: 5
    steps:
      - name: Ping the database
        run: |
          last_ping=$(curl --silent --show-error --fail-with-body \
            --retry 5 --retry-delay 30 --retry-all-errors --max-time 60 \
            -X POST "$SUPABASE_URL/rest/v1/rpc/keep_alive" \
            -H "apikey: $SUPABASE_PUBLISHABLE_KEY" -H "Content-Type: application/json" -d '{}')
          echo "Supabase heartbeat: $last_ping"
      # Σε public repo το GitHub σβήνει τα scheduled workflows μετά από 60 μέρες χωρίς commit
      - name: Keep this schedule enabled
        if: github.event_name == 'schedule'
        env:
          GH_TOKEN: ${{ github.token }}
        run: gh api -X PUT "repos/${{ github.repository }}/actions/workflows/supabase-keep-alive.yml/enable"
```

Τρέχει **μόνο από το default branch** (`main`).

---

## 4. Αλλαγές στο Android app

### 4.1 Δομή πακέτου `cloud/`

```
cloud/
├─ data/
│  ├─ CloudRepository.kt          sign in/up/out, household, change/forgot password, delete account
│  ├─ di/CloudModule.kt           provide SyncLinkDao
│  ├─ local/SyncLinkEntity.kt     + SyncLinkDao.kt  (Room πίνακας sync_links)
│  ├─ network/NetworkMonitor.kt   StateFlow<Boolean> isOnline
│  ├─ remote/SupabaseConfig.kt    URL + publishable key
│  ├─ remote/SupabaseClient.kt    REST client (Auth, PostgREST, Functions)
│  ├─ remote/RemoteRows.kt        enum RemoteTable + JSON ↔ fields
│  ├─ session/CloudSessionStore.kt DataStore "cloud_prefs"
│  └─ sync/SyncManager.kt         η μηχανή του sync
├─ domain/
│  ├─ model/CloudModels.kt        CloudSession, CloudState, SyncStatus, CloudMode, SyncProgress, SyncSummary
│  ├─ model/CloudError.kt         enum λαθών + CloudException
│  ├─ sync/SyncPlanner.kt         planSync() (καθαρή συνάρτηση, unit-testable) + hashOf()
│  ├─ sync/SyncFields.kt          ονόματα πεδίων + μορφή τιμών (money, idList)
│  └─ usecase/SignOutUseCase.kt
└─ presentation/
   ├─ CloudScreen / CloudViewModel / CloudStatusCard / CloudErrorMessages
   ├─ account/   AccountSecurity* (αλλαγή/ξέχασα κωδικό, αποσύνδεση, διαγραφή, αναγκαστική αλλαγή)
   └─ onboarding/ SyncOnboarding* (Welcome + ροή «ανέβασε τα δεδομένα σου») + SuccessAnimation
```

Manifest: `INTERNET` και `ACCESS_NETWORK_STATE`. Dependency: `okhttp`. Το `org.json` υπάρχει ήδη στο Android.

### 4.2 Room: ένας νέος πίνακας, καμία αλλαγή στους υπάρχοντες

```kotlin
@Entity(tableName = "sync_links", primaryKeys = ["entity", "localKey"])
data class SyncLinkEntity(
    val entity: String,     // όνομα του RemoteTable, π.χ. "ACCOUNTS"
    val localKey: String,   // το τοπικό id (ή φυσικό κλειδί)
    val remoteId: String,   // το UUID στον server
    val hash: String,       // hash των πεδίων όπως ήταν στο τελευταίο sync ("" = δεν επιβεβαιώθηκε)
)

@Dao
interface SyncLinkDao {
    @Query("SELECT * FROM sync_links WHERE entity = :entity") suspend fun getAll(entity: String): List<SyncLinkEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(links: List<SyncLinkEntity>)
    @Query("DELETE FROM sync_links WHERE entity = :entity") suspend fun deleteAll(entity: String)
    @Query("DELETE FROM sync_links") suspend fun clear()
}

private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS `sync_links` (
            `entity` TEXT NOT NULL, `localKey` TEXT NOT NULL, `remoteId` TEXT NOT NULL, `hash` TEXT NOT NULL,
            PRIMARY KEY(`entity`, `localKey`))""")
    }
}
```

Κάθε DAO χρειάζεται επίσης `getAllOnce()`, `insertReturningId()`, `update…`, `deleteById()` και `deleteAll()`.

### 4.3 SupabaseClient: ο REST client

```kotlin
@Singleton
class SupabaseClient @Inject constructor() {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build()

    // Auth
    fun signUp(email: String, password: String, displayName: String): CloudSession {
        val body = JSONObject().put("email", email).put("password", password).put("display_name", displayName)
        call("POST", "/functions/v1/signup", body.toString())
        return signIn(email, password)
    }
    fun signIn(email: String, password: String): CloudSession =
        parseSession(JSONObject(call("POST", "/auth/v1/token?grant_type=password",
            JSONObject().put("email", email).put("password", password).toString())))
    fun refresh(refreshToken: String): CloudSession =
        parseSession(JSONObject(call("POST", "/auth/v1/token?grant_type=refresh_token",
            JSONObject().put("refresh_token", refreshToken).toString())))
    fun signOut(accessToken: String) { runCatching { call("POST", "/auth/v1/logout", "{}", accessToken) } }
    fun changePassword(session: CloudSession, newPassword: String) {
        val body = JSONObject().put("password", newPassword).put("data", JSONObject().put("must_change_password", false))
        call("PUT", "/auth/v1/user", body.toString(), session.accessToken)
    }
    fun requestTempPassword(email: String, language: String) {
        call("POST", "/functions/v1/password-reset", JSONObject().put("action", "request").put("email", email).put("lang", language).toString())
    }
    fun redeemTempPassword(email: String, password: String): Boolean = try {
        call("POST", "/functions/v1/password-reset", JSONObject().put("action", "redeem").put("email", email).put("password", password).toString()); true
    } catch (e: SupabaseException) { if (e.status == 400) false else throw e }
    fun deleteAccount(session: CloudSession) { call("POST", "/functions/v1/delete-account", "{}", session.accessToken) }

    // Πίνακες
    fun selectAll(session: CloudSession, table: String, ownerFilter: String, orderBy: String): List<JSONObject> { /* GET σε σελίδες των 1000 με limit/offset */ }
    fun upsert(session: CloudSession, table: String, rows: List<JSONObject>, onConflict: String, onProgress: (Int, Int) -> Unit) {
        // POST /rest/v1/<table>?on_conflict=<cols>  σε παρτίδες των 200
        // Prefer: resolution=merge-duplicates,return=minimal
    }
    fun softDelete(/* … */) { /* PATCH /rest/v1/<table>?<key>=in.(…)  body {"deleted_at": now}  σε παρτίδες των 100 */ }

    private fun call(method: String, path: String, body: String? = null, token: String? = null, prefer: String? = null): String {
        val requestBody = (body ?: if (method == "GET" || method == "DELETE") null else "")?.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(SupabaseConfig.URL + path)
            .header("apikey", SupabaseConfig.PUBLISHABLE_KEY)
            .apply {
                if (token != null) header("Authorization", "Bearer $token")
                if (prefer != null) header("Prefer", prefer)
            }
            .method(method, requestBody).build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw parseError(response.code, text)
            return text
        }
    }

    private fun parseSession(json: JSONObject): CloudSession {
        val user = json.getJSONObject("user")
        val expiresAt = json.optLong("expires_at", 0L).takeIf { it > 0L }
            ?: (System.currentTimeMillis() / 1000 + json.optLong("expires_in", 3600L))
        return CloudSession(
            accessToken = json.getString("access_token"), refreshToken = json.getString("refresh_token"),
            expiresAt = expiresAt, userId = user.getString("id"), email = user.optString("email"),
            mustChangePassword = user.optJSONObject("user_metadata")?.optBoolean("must_change_password", false) ?: false,
        )
    }

    companion object {
        /** Auth απαντάει {msg, error_code} ή {error, error_description}· η βάση {message, code}. */
        internal fun parseError(status: Int, body: String): SupabaseException {
            val json = runCatching { JSONObject(body) }.getOrNull() ?: return SupabaseException(status, body.ifBlank { "HTTP $status" })
            val message = listOf("msg", "error_description", "message", "error")
                .firstNotNullOfOrNull { k -> json.optString(k).takeIf { it.isNotBlank() } } ?: "HTTP $status"
            val code = listOf("error_code", "code", "error")
                .firstNotNullOfOrNull { k -> json.opt(k)?.toString()?.takeIf { it.isNotBlank() && it != "null" } }
            return SupabaseException(status, message, code)
        }
    }
}

class SupabaseException(val status: Int, message: String, val code: String? = null) : IOException(message)
```

Τα λάθη μεταφράζονται σε `CloudError` (`SyncManager.toCloudException`):

| code / status | CloudError |
|---|---|
| `invalid_credentials`, `invalid_grant` | INVALID_CREDENTIALS |
| `user_already_exists`, `email_exists` | USER_EXISTS |
| `weak_password` | WEAK_PASSWORD |
| `same_password` | SAME_PASSWORD |
| `email_send_failed` | EMAIL_SEND_FAILED |
| `email_address_invalid`, `validation_failed` | INVALID_EMAIL |
| `over_*` ή 429 | RATE_LIMITED |
| `P0002` | INVALID_INVITE |
| 401 | SESSION_EXPIRED |
| `IOException` | NO_CONNECTION |

Ένα κοινό `Throwable.cloudMessageRes()` δίνει το string resource σε κάθε ViewModel.

### 4.4 Session: DataStore `cloud_prefs`
Αποθηκεύει access/refresh token, expiresAt, userId, email, mustChangePassword, household, lastSyncAt, `pending_changes`, `links_household_id` και `prompt_snoozed_until`.

`validSession()`: αν το token λήγει σε λιγότερο από 60", κάνει refresh. Αν το refresh απαντήσει 4xx, σβήνει το session και επιστρέφει SESSION_EXPIRED, οπότε ο χρήστης ξανασυνδέεται.

### 4.5 Ο αλγόριθμος sync (καρδιά του συστήματος)

**Πεδία:** κάθε εγγραφή μετατρέπεται σε `Map<String, String?>` με τα ονόματα στηλών του server. Οι τιμές είναι σε κανονική μορφή: ποσά με 2 δεκαδικά, ημερομηνίες ISO, λίστες ids ταξινομημένες και χωρισμένες με κόμμα. Έτσι η ίδια εγγραφή δίνει **το ίδιο hash** στο κινητό και στον server.

```kotlin
fun hashOf(fields: Map<String, String?>): String {
    val canonical = fields.toSortedMap().entries.joinToString("\u001F") { (k, v) -> "$k=${v ?: "\u0000"}" }
    return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
```

**Three-way merge ανά πίνακα** (`planSync`). Συγκρίνει την τοπική εκδοχή, την εκδοχή του server και το link του τελευταίου sync:

| Τοπικά | Server | Αποτέλεσμα |
|---|---|---|
| ίδιο με link | ίδιο με link | τίποτα |
| άλλαξε | ίδιο | push |
| ίδιο | άλλαξε | update τοπικά |
| άλλαξε | άλλαξε (ίδια τιμή) | μόνο ενημέρωση link |
| άλλαξε | άλλαξε (διαφορετική) | **κερδίζει το κινητό** → push |
| σβήστηκε | υπάρχει | soft delete στον server |
| υπάρχει, όχι αλλαγμένο | σβήστηκε | delete τοπικά |
| υπάρχει, αλλαγμένο | σβήστηκε | push (το «ανασταίνει») |
| νέο (χωρίς link) | — | νέο UUID, push |
| — | νέο (χωρίς link) | insert τοπικά |

```kotlin
data class LocalItem(val localKey: String, val fields: Map<String, String?>)
data class RemoteItem(val remoteId: String, val fields: Map<String, String?>, val deleted: Boolean = false)
data class SyncLink(val localKey: String, val remoteId: String, val hash: String)

data class EntityPlan(
    val push: Map<String, Map<String, String?>>,   // remoteId → fields
    val softDelete: List<String>,
    val insertLocal: List<RemoteItem>,
    val updateLocal: Map<String, RemoteItem>,      // localKey → remote row
    val deleteLocal: List<String>,
    val links: List<SyncLink>,
)

fun planSync(local: List<LocalItem>, remote: List<RemoteItem>, links: List<SyncLink>, newRemoteId: (LocalItem) -> String): EntityPlan {
    val localByKey = local.associateBy { it.localKey }
    val remoteById = remote.associateBy { it.remoteId }
    val push = linkedMapOf<String, Map<String, String?>>()
    val softDelete = mutableListOf<String>()
    val insertLocal = mutableListOf<RemoteItem>()
    val updateLocal = linkedMapOf<String, RemoteItem>()
    val deleteLocal = mutableListOf<String>()
    val newLinks = mutableListOf<SyncLink>()
    val linkedLocal = mutableSetOf<String>()
    val linkedRemote = mutableSetOf<String>()

    fun pushLocal(item: LocalItem, remoteId: String) {
        push[remoteId] = item.fields
        newLinks += SyncLink(item.localKey, remoteId, hashOf(item.fields))
    }

    links.forEach { link ->
        if (link.localKey in linkedLocal || link.remoteId in linkedRemote) return@forEach
        val l = localByKey[link.localKey]
        val r = remoteById[link.remoteId]
        linkedLocal += link.localKey
        linkedRemote += link.remoteId
        val localChanged = l != null && hashOf(l.fields) != link.hash
        when {
            l == null && r == null -> Unit
            l == null -> if (!r!!.deleted) softDelete += r.remoteId
            r == null || r.deleted -> if (localChanged) pushLocal(l, link.remoteId) else deleteLocal += l.localKey
            else -> {
                val remoteHash = hashOf(r.fields)
                val remoteChanged = remoteHash != link.hash
                when {
                    !localChanged && !remoteChanged -> newLinks += link
                    remoteHash == hashOf(l.fields)  -> newLinks += link.copy(hash = remoteHash)
                    localChanged                    -> pushLocal(l, link.remoteId)
                    else -> { updateLocal[l.localKey] = r; newLinks += link.copy(hash = remoteHash) }
                }
            }
        }
    }
    local.filter { it.localKey !in linkedLocal }.forEach { item ->
        val remoteId = newRemoteId(item)
        linkedLocal += item.localKey; linkedRemote += remoteId
        pushLocal(item, remoteId)
    }
    remote.filter { it.remoteId !in linkedRemote && !it.deleted }.forEach { insertLocal += it }
    return EntityPlan(push, softDelete, insertLocal, updateLocal, deleteLocal, newLinks)
}
```

**Ένας γύρος sync** (`SyncManager.sync`), πάντα μέσα σε `Mutex`:
1. Παίρνει έγκυρο session (με refresh αν χρειάζεται).
2. **Pull πρώτα** όλων των πινάκων. Αν πέσει το δίκτυο εδώ, τίποτα τοπικό δεν έχει αλλάξει.
3. Αν είναι `replaceLocal` (νέα συσκευή ή join), σβήνει τα τοπικά και τα links μέσα σε transaction.
4. Για κάθε πίνακα **με σειρά εξαρτήσεων** (πρώτα αυτοί που αναφέρονται από άλλους):
   - `planSync`.
   - **Αποθηκεύει τα links των νέων γραμμών πριν το push** (με hash ""), ώστε ένα retry μετά από crash να ξαναστείλει το ίδιο UUID και να μη δημιουργήσει διπλό.
   - `upsert` του push σε παρτίδες με πρόοδο, μετά `softDelete`.
   - Σε **ένα Room transaction**: deleteLocal, updateLocal, insertLocal, και αντικατάσταση των links του πίνακα.
   - Αν μια γραμμή δεν μπορεί να εφαρμοστεί (π.χ. λείπει ο λογαριασμός της), κρατάει το παλιό link ώστε να ξαναδοκιμαστεί.
5. Αν ήρθαν αλλαγές: ανανέωση widgets και του τοπικού backup αρχείου. Μετά `lastSyncAt` και `SyncSummary` (πόσα ανέβηκαν και πόσα ήρθαν ανά πίνακα).

**Πότε τρέχει:**

```kotlin
fun requestSync(delayMillis: Long = 1_500L, localChange: Boolean = false) {
    if (localChange) {
        changeVersion.incrementAndGet()
        if (!_hasPendingChanges.value) { _hasPendingChanges.value = true; scope.launch { store.savePendingChanges(true) } }
    }
    pending?.cancel()                       // debounce
    pending = scope.launch {
        delay(delayMillis)
        if (store.current().isSyncing && networkMonitor.isOnline.value) syncNow()
    }
}
```

- Μετά από **κάθε εγγραφή στη βάση** από τα repositories: `requestSync(localChange = true)`. Στο NetworthApp μπαίνει μέσω του ήδη υπάρχοντος `AutoBackupDataSource.trigger()`, που καλούν όλα τα repositories. Οι εγγραφές που κάνει το ίδιο το sync **δεν** το ξανακαλούν (`trigger(requestSync = false)`).
- `MainActivity.onStart()`: `requestSync(delayMillis = 0)`.
- Όταν επανέρχεται το internet: `networkMonitor.isOnline.drop(1).filter { it }.collect { requestSync(0) }`.
- Το `changeVersion` εξασφαλίζει ότι αλλαγές που έγιναν **κατά τη διάρκεια** του sync μένουν pending για τον επόμενο γύρο.

**NetworkMonitor:** `registerDefaultNetworkCallback`. Θεωρεί «online» μόνο αν το δίκτυο έχει `NET_CAPABILITY_INTERNET` και `NET_CAPABILITY_VALIDATED`.

### 4.6 CloudRepository: οι ενέργειες λογαριασμού
- `signIn(email, password)` → `signInOrRedeem`:
  - Κανονικό sign-in.
  - Αν αποτύχει με `invalid_credentials`, δοκιμάζει `redeemTempPassword`. Αν ήταν ο προσωρινός, ξανακάνει sign-in και το session έχει `mustChangePassword = true`.
- `signUp(email, password, name)` → edge function `signup`, μετά sign-in.
- `changePassword(current?, new)`: ελέγχει τον τρέχοντα με sign-in. Παραλείπεται όταν ήρθε από προσωρινό κωδικό.
- `deleteAccount(password)`: έλεγχος με sign-in, μετά edge function, μετά καθάρισμα session και links.
- `signOut(force)`:
  1. Αν υπάρχουν εκκρεμείς αλλαγές, δοκιμάζει `syncNow()`. Αν αποτύχει και δεν είναι `force`, επιστρέφει `UNSYNCED_CHANGES` και το UI δείχνει «Αποσύνδεση ούτως ή άλλως».
  2. Logout και καθάρισμα session.
  3. `clearLocalData()`: σβήνει όλους τους πίνακες και τα links σε transaction.
  4. Το `SignOutUseCase` σβήνει επίσης το backup αρχείο και το PIN/biometric, και καθαρίζει το profile, οπότε το NavHost γυρνάει στη Welcome.
  5. Γλώσσα, θέμα και νόμισμα μένουν.

### 4.7 UI ροές
- **Αναγκαστική αλλαγή κωδικού:** στο `NetWorthApp.kt`, όταν `mustChangePassword` είναι true, μια οθόνη καλύπτει όλη την εφαρμογή και δεν φεύγει μέχρι να αλλάξει ο κωδικός (ή αποσύνδεση).
- **Welcome (νέα εγκατάσταση):** `WelcomeRoute` ως start destination όταν δεν υπάρχει profile. Τα αποτελέσματα:
  - `HOUSEHOLD_DATA`: συνδέθηκε και κατέβηκαν δεδομένα → μόνο ρύθμιση ασφάλειας.
  - `NEW_HOUSEHOLD`: νέος λογαριασμός → συνέχεια με το κανονικό setup.
  - `NO_ACCOUNT`: offline → κανονικό setup.
- **SyncOnboarding (υπάρχων offline χρήστης):** overlay πάνω από την εφαρμογή, μετά το lock screen. Βήματα `INTRO → ACCOUNT → HOUSEHOLD → SYNCING → SUCCESS | ERROR`.
  - Το «Όχι τώρα» κάνει snooze 7 μέρες.
  - Η μπάρα προόδου ακολουθεί την πραγματική πρόοδο, αλλά διαρκεί τουλάχιστον 4" ώστε να διαβάζονται τα μηνύματα των σταδίων: Σύνδεση → Έλεγχος → Λογαριασμοί → Ιστορικό → Πάγια → Κατηγορίες → Ολοκλήρωση.
  - Το SUCCESS δείχνει animation και πόσα ανέβηκαν, και κουμπί **Συνέχεια**.
  - Το ERROR έχει Επανάληψη.
- **Ρυθμίσεις:** κάρτα κατάστασης (`CloudStatusCard`) που ανοίγει την οθόνη Cloud: λογαριασμός, νοικοκυριό, μέλη, πρόσκληση, «Συγχρονισμός τώρα».
- **Προφίλ:** ενότητα Λογαριασμός με τα εξής: συνδεδεμένος ως, αλλαγή κωδικού, αποσύνδεση, διαγραφή (κόκκινο).

---

## 5. Συντήρηση

| Τι | Πώς |
|---|---|
| **Αλλαγή σχήματος** | Νέο αρχείο `supabase/migrations/<timestamp>_<name>.sql`. Εφαρμογή με Supabase CLI (`supabase db push`), με SQL Editor στο Dashboard ή με τον Claude μέσω Supabase MCP (`apply_migration`). Ποτέ αλλαγή σε παλιό migration. |
| **Νέο πεδίο σε συγχρονιζόμενο πίνακα** | 1) `alter table … add column` με default. 2) Σταθερά στο `SyncFields`. 3) Στο `RemoteTable.fieldNames`. 4) Στο `toLocalItem()` και στο `RemoteItem.toX()`. 5) Room migration αν είναι νέο και τοπικά. Αλλάζει το hash όλων των γραμμών, οπότε **την πρώτη φορά ξαναστέλνονται όλες**. Είναι αναμενόμενο και ακίνδυνο. |
| **Νέος πίνακας** | SQL με το μοτίβο του §3.2 (id, owner, updated_at, deleted_at, RLS), νέο `RemoteTable`, νέο `syncTable(...)` στη σωστή σειρά εξάρτησης, και καθάρισμα στο `clearLocalData()`. |
| **Edge functions** | `supabase functions deploy <name> --no-verify-jwt` ή μέσω MCP. Ο κώδικας μένει στο repo, στο `supabase/functions/`. |
| **Secrets** | Dashboard → Edge Functions → Secrets (`BREVO_API_KEY` κ.λπ.). |
| **Email (Brevo)** | Ο sender πρέπει να είναι επαληθευμένος στο Brevo. Το δωρεάν πλάνο έχει ημερήσιο όριο (περίπου 300 email τη μέρα). Αν αποτύχει, η function απαντάει `email_send_failed` και το app το δείχνει. |
| **Έλεγχος ασφάλειας** | Dashboard → Advisors → Security (ή `get_advisors` μέσω MCP) μετά από κάθε αλλαγή σχήματος. Αναμενόμενα: «RLS enabled, no policy» σε `password_resets` και `keep_alive` (σκόπιμα κλειστοί πίνακες), και «anon can execute security definer» στην `keep_alive` (σκόπιμο, ακίνδυνο). |
| **Logs** | Dashboard → Logs (API, Auth, Edge Functions). Στο app: Timber (`Sync failed`). |
| **Keep-alive** | GitHub → Actions → «Supabase keep-alive». Υπάρχει κουμπί Run workflow. Έλεγχος με `select * from private.keep_alive`. Αν αποτύχει, έρχεται email από το GitHub. |
| **Χρήστες** | Dashboard → Authentication → Users: διαγραφή, αλλαγή κωδικού, δημιουργία. |
| **Κλειδιά** | Το publishable είναι δημόσιο. Αν το αλλάξεις, χρειάζεται νέα έκδοση του app και αλλαγή στο workflow. Το secret δεν βγαίνει ποτέ από το Supabase. |
| **Commits** | Όλα ως `agiak <anastas.giakoumatos@gmail.com>`. |

---

## 6. Το ίδιο σε εφαρμογή που σήμερα είναι μόνο offline

### 6.1 Τι αλλάζει σε σχέση με το NetworthApp
- **Δεδομένα ανά χρήστη, όχι ανά νοικοκυριό** (εκτός αν θες κοινή χρήση). Στη θέση του `household_id` μπαίνει `user_id uuid not null default auth.uid() references auth.users(id) on delete cascade`, και το RLS γίνεται `user_id = (select auth.uid())`. Έτσι φεύγουν: πίνακες households, members, invites, οθόνη μελών, βήμα HOUSEHOLD, trigger διαγραφής χρήστη (το cascade αρκεί) και `prepareFor(householdId)` (τα links ανήκουν στον `userId`).
- Ο πίνακας-πρότυπο:

```sql
create table public.<items> (
    id         uuid primary key,
    user_id    uuid not null default auth.uid() references auth.users(id) on delete cascade,
    -- … τα πεδία της εφαρμογής, με CHECKs που ταιριάζουν με το app …
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz
);
create index <items>_user_sync_idx on public.<items>(user_id, updated_at);
create trigger <items>_updated_at before update on public.<items> for each row execute function public.set_updated_at();
alter table public.<items> enable row level security;
create policy "Own rows read"   on public.<items> for select to authenticated using (user_id = (select auth.uid()));
create policy "Own rows add"    on public.<items> for insert to authenticated with check (user_id = (select auth.uid()));
create policy "Own rows change" on public.<items> for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "Own rows delete" on public.<items> for delete to authenticated using (user_id = (select auth.uid()));
revoke all on public.<items> from anon;
```

- Στο app, το `selectAll` φιλτράρει με `user_id=eq.<userId>` και το `RemoteRows.toJson` βάζει `user_id` αντί για `household_id`. Ο έλεγχος «συνδεδεμένος» γίνεται απλώς `session != null`, αντί για `isSyncing = session != null && household != null`.

### 6.2 Η ροή αναβάθμισης που θέλουμε

```
Νέα έκδοση ανοίγει
 ├─ Νέα εγκατάσταση (δεν υπάρχουν δεδομένα / profile)
 │    → Welcome: [Σύνδεση] [Δημιουργία λογαριασμού] [Συνέχεια χωρίς λογαριασμό]
 │       ├ Σύνδεση → κατεβαίνουν τα δεδομένα (replaceLocal = true, μπάρα «Φέρνουμε τα δεδομένα σου…») → Συνέχεια → app
 │       ├ Εγγραφή → (τίποτα για ανέβασμα) → κανονικό setup / app
 │       └ Offline → κανονικό setup / app
 │
 └─ Υπάρχων χρήστης με δεδομένα, χωρίς session, χωρίς απάντηση ακόμα
      → Οθόνη σε όλη την επιφάνεια:
        «Φτιάξε λογαριασμό για να μη χάσεις ποτέ τα δεδομένα σου και να τα έχεις σε κάθε συσκευή»
        [Δημιουργία λογαριασμού]   [Έχω ήδη λογαριασμό]   [Παραμονή offline]
         ├ Εγγραφή: όνομα, email, κωδικός (με 👁) → signup → sign-in
         │    → «Ανεβάζουμε τα δεδομένα σου…» μπάρα προόδου (sync, replaceLocal = false)
         │    → ✓ «Έτοιμο! Ανέβηκαν N εγγραφές. Είσαι συνδεδεμένος ως x@y»  [Συνέχεια] → app
         ├ Σύνδεση: email, κωδικός, «Ξέχασα τον κωδικό» → sign-in
         │    → ίδια μπάρα. Τα δεδομένα του κινητού ενώνονται με του λογαριασμού (merge),
         │      και κερδίζει το κινητό σε σύγκρουση → ✓ [Συνέχεια]
         ├ Σφάλμα (π.χ. χωρίς internet) → μήνυμα + [Επανάληψη] (το session μένει, και το επόμενο
         │    άνοιγμα συνεχίζει από εκεί που έμεινε)
         └ Παραμονή offline → αποθηκεύεται η απάντηση· η εφαρμογή συνεχίζει όπως πριν.
              Η επιλογή «Λογαριασμός & συγχρονισμός» μένει στις Ρυθμίσεις για αργότερα.
```

Λεπτομέρειες που μετράνε:
- **Πότε δείχνεται:** μετά το lock screen (PIN/biometric), όχι πάνω από αυτό, και μόνο αν `session == null`. Στο NetworthApp η απόφαση παίρνεται **μία φορά ανά άνοιγμα** (`decided`) και μέχρι τότε δείχνεται ένα κενό frame, ώστε να μη «αναβοσβήνει» η εφαρμογή.
- **«Παραμονή offline»:** το NetworthApp ξαναρωτάει μετά από 7 μέρες (`prompt_snoozed_until`). Αν θες να ρωτάει **μόνο μία φορά**, κράτα ένα boolean `account_prompt_answered` και μην ξαναδείξεις την οθόνη. Η είσοδος από τις Ρυθμίσεις μένει πάντα.
- **Πώς ξεχωρίζει «υπάρχων» από «νέος»:** υπάρχει ήδη profile ή δεδομένα στη Room (π.χ. `profileCreated == true` ή μη κενή βάση).
- **Το πρώτο upload** είναι απλώς ένα κανονικό sync με κενούς τους πίνακες `sync_links`. Όλες οι γραμμές είναι «νέες», παίρνουν UUID και ανεβαίνουν σε παρτίδες των 200. Δεν χρειάζεται ξεχωριστός κώδικας «export».
- **Μετά τη σύνδεση** ενεργοποιείται το αυτόματο sync (onStart, μετά από αλλαγές, όταν επανέρχεται το internet).
- **Αποσύνδεση:** ανεβάζει ό,τι εκκρεμεί, σβήνει τα τοπικά και πάει στη Welcome. Ο offline χρήστης δεν έχει αποσύνδεση.
- **Ελάχιστο μήκος κωδικού:** 6, ίδιο στο app και στην edge function.

### 6.3 Βήματα υλοποίησης (με σειρά)
1. **Supabase:** νέο project (δες όριο free plan στο §3.1). Πάρε URL και publishable key.
2. **Migrations:**
   - `set_updated_at`, `private` schema, `profiles` και `handle_new_user`, `auto_confirm_email`.
   - Πίνακες της εφαρμογής με το πρότυπο του §6.1.
   - `password_resets` και οι functions του.
   - `keep_alive`.
3. **Edge functions:** `signup`, `password-reset`, `delete-account` (deploy με `--no-verify-jwt`). Secret `BREVO_API_KEY`, και επαλήθευση του sender στο Brevo.
4. **Έλεγχος:** Advisors → Security. Δοκιμή εγγραφής και σύνδεσης με curl:

```bash
curl -X POST "$URL/functions/v1/signup" -H "apikey: $KEY" -H "Content-Type: application/json" \
     -d '{"email":"test@example.com","password":"secret1","display_name":"Test"}'
curl -X POST "$URL/auth/v1/token?grant_type=password" -H "apikey: $KEY" -H "Content-Type: application/json" \
     -d '{"email":"test@example.com","password":"secret1"}'
```

5. **App: data layer:**
   - Πακέτο `cloud/` όπως στο §4.1: `SupabaseConfig`, `SupabaseClient`, `CloudSessionStore`, `NetworkMonitor`, `SyncFields`, `SyncPlanner`, `RemoteRows`, `SyncLinkEntity` και `SyncLinkDao`, `SyncManager`, `CloudRepository`.
   - Room migration για το `sync_links`, και DAO μέθοδοι `getAllOnce`, `insertReturningId`, `deleteById`, `deleteAll`.
   - Manifest permissions και okhttp.
6. **App: triggers:** `requestSync(localChange = true)` μετά από κάθε εγγραφή στα repositories, `requestSync(0)` στο `onStart`, και ο collector για την επαναφορά του internet μέσα στον `SyncManager`.
7. **App: UI:**
   - Οθόνη αναβάθμισης/λογαριασμού (§6.2) με ροή `INTRO → ACCOUNT → SYNCING → SUCCESS | ERROR`.
   - Welcome για νέες εγκαταστάσεις.
   - Πεδία κωδικού με toggle ορατότητας.
   - Σύνδεσμος «Ξέχασα τον κωδικό».
   - Αναγκαστική αλλαγή κωδικού.
   - Ενότητα Λογαριασμού: αλλαγή κωδικού, αποσύνδεση με προειδοποίηση για μη ανεβασμένες αλλαγές, διαγραφή με κωδικό.
   - Κάρτα κατάστασης στις Ρυθμίσεις.
   - Όλα τα κείμενα σε Ελληνικά και Αγγλικά.
8. **Keep-alive:** το workflow του §3.4 με το URL και το key του νέου project. Push στο `main`.
9. **Δοκιμές σε κινητό:**
   - (α) αναβάθμιση με υπάρχοντα δεδομένα → εγγραφή → upload → Συνέχεια.
   - (β) δεύτερο κινητό → σύνδεση → κατεβαίνουν.
   - (γ) αλλαγή offline → επανέρχεται το internet → ανεβαίνει.
   - (δ) σβήσιμο σε ένα κινητό → σβήνεται και στο άλλο.
   - (ε) ξέχασα τον κωδικό → email → προσωρινός → αναγκαστική αλλαγή.
   - (στ) αποσύνδεση → κενό app → σύνδεση → επιστρέφουν.
   - (ζ) διαγραφή λογαριασμού.

---

## 7. Παγίδες που συναντήσαμε

- **Kotlin:** το `to` δένει πιο σφιχτά από το `?:`. Το `KEY to value.takeIf { … } ?: DEFAULT` γίνεται `(KEY to …) ?: DEFAULT` και βγάζει λάθος τύπου (`Serializable` αντί για `Pair`). Βάζε παρενθέσεις: `KEY to (value.takeIf { … } ?: DEFAULT)`.
- **Compose:** το `Modifier.align(Alignment.End)` δουλεύει μόνο μέσα σε `ColumnScope`. Σε custom container τύλιξε με `Box(contentAlignment = Alignment.CenterEnd)`.
- **Email verification:** το Supabase ζητάει επιβεβαίωση email από προεπιλογή. Το λύσαμε με την edge function `signup` (admin `createUser` με `email_confirm: true`) και το trigger `auto_confirm_email`. Εναλλακτικά: Dashboard → Authentication → Providers → Email → απενεργοποίηση του «Confirm email».
- **Hash mismatch → άπειρα uploads:** αν το app στείλει τιμή που ο server αποθηκεύει διαφορετικά (π.χ. `12.5` αντί για `12.50`, ή μη ταξινομημένη λίστα), το hash δεν ταιριάζει ποτέ. Κανονικοποίησε **πάντα** με τις `SyncFields.money()` και `SyncFields.idList()`.
- **CHECK constraints:** τιμή που παραβιάζει constraint απορρίπτει ολόκληρη την παρτίδα. Καθάριζε στο `toLocalItem()` (μήκη, κενά ονόματα, χρώματα).
- **Εξαρτήσεις:** συγχρόνιζε πρώτα τους «γονείς» και μετά τα «παιδιά», και μετάφραζε τα foreign keys μέσω των links.
- **Αποσύνδεση με pending αλλαγές:** πάντα δοκίμασε πρώτα να τις ανεβάσεις. Αλλιώς χάνονται.
- **Το apply_migration μέσω MCP** μπορεί να κάνει timeout στα 60". Ξαναδοκίμασε, γιατί συνήθως έχει ήδη περάσει. Έλεγξε με `list_migrations`.
- **GitHub Actions:** τα scheduled workflows τρέχουν μόνο από το default branch. Σε public repo απενεργοποιούνται μετά από 60 μέρες χωρίς commit (το workflow ξαναενεργοποιεί τον εαυτό του).
