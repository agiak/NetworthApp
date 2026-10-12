# Backend & Sync: η υποδομή

Η υποδομή που προστέθηκε στο NetworthApp για να γίνει από offline εφαρμογή «online εφαρμογή με χρήστες»: λογαριασμοί, συγχρονισμός, backend και συντήρηση. Ο οδηγός **δεν εξαρτάται από τα δεδομένα** της εφαρμογής. Όπου χρειάζεται παράδειγμα, χρησιμοποιεί έναν γενικό πίνακα `items` / `ItemEntity`, που τον αντικαθιστάς με τους δικούς σου.

> Στοίβα: Android, Kotlin, Room, Hilt, Jetpack Compose, DataStore, OkHttp και Supabase (Postgres, Auth, PostgREST, Edge Functions).

---

## 1. Τι υποδομή μπήκε

**Λογαριασμοί**
- Εγγραφή με email + κωδικό **χωρίς επιβεβαίωση email**.
- Σύνδεση με email + κωδικό.
- Κουμπί εμφάνισης/απόκρυψης σε κάθε πεδίο κωδικού.
- «Ξέχασα τον κωδικό»: email με **προσωρινό κωδικό**.
  - 10 χαρακτήρες, ισχύει 1 ώρα, μία χρήση, έως 5 προσπάθειες.
  - Με τη σύνδεση ο χρήστης **υποχρεώνεται** να ορίσει νέο κωδικό.
  - Ο παλιός κωδικός δουλεύει μέχρι να χρησιμοποιηθεί ο προσωρινός.
- Αλλαγή κωδικού (ζητάει τον τρέχοντα).
- Διαγραφή λογαριασμού με επιβεβαίωση κωδικού.
- Αποσύνδεση: ανεβάζει πρώτα τις εκκρεμείς αλλαγές, μετά **σβήνει όλα τα τοπικά δεδομένα** και γυρνάει στη σελίδα Σύνδεση / Εγγραφή.

**Συγχρονισμός**
- Η τοπική βάση (Room) μένει η «αλήθεια» της εφαρμογής, οπότε **όλα δουλεύουν offline**. Το sync τρέχει από πίσω.
- Αμφίδρομος three-way merge ανά πίνακα: νέες εγγραφές, αλλαγές και διαγραφές πάνε και από τις δύο μεριές.
- Οι αλλαγές που γίνονται χωρίς internet μένουν εκκρεμείς (και μετά από restart) και ανεβαίνουν μόλις επανέλθει η σύνδεση.
- Πότε τρέχει: όταν ανοίγει η εφαρμογή, 1,5" μετά από κάθε τοπική αλλαγή (debounce), όταν επανέρχεται το internet, και με το χέρι.
- Ένδειξη κατάστασης: Online / Συγχρονίζει / Χωρίς internet / Σφάλμα / Offline mode, μαζί με την ώρα τελευταίου sync και τις εκκρεμείς αλλαγές.
- Μπάρα προόδου με στάδια για το πρώτο ανέβασμα ή κατέβασμα.

**Ροές οθόνης**
- **Νέα εγκατάσταση:** Welcome → Σύνδεση / Δημιουργία λογαριασμού / Συνέχεια χωρίς λογαριασμό.
- **Υπάρχων offline χρήστης μετά την αναβάθμιση:** «Φτιάξε λογαριασμό;» → λογαριασμός → ανέβασμα με πρόοδο → «Έτοιμο» → **Συνέχεια**.
- **Αναγκαστική αλλαγή κωδικού** μετά από σύνδεση με προσωρινό κωδικό.
- **Ενότητα Λογαριασμού** στο προφίλ: συνδεδεμένος ως, αλλαγή κωδικού, αποσύνδεση, διαγραφή.

**Backend λειτουργία**
- GitHub Action που κάθε μέρα κρατάει το δωρεάν Supabase project ξύπνιο, ώστε να μη γίνει ποτέ pause.

---

## 2. Αρχιτεκτονική

```
┌────────────────────── Android app ──────────────────────┐        ┌──────────── Supabase ─────────────┐
│ UI (Compose, MVI)                                        │        │ Auth: email + password, JWT        │
│  ├ Welcome · Onboarding λογαριασμού · Λογαριασμός         │        │ PostgREST /rest/v1/…              │
│  └ Αναγκαστική αλλαγή κωδικού                            │ HTTPS  │  └ πίνακες με RLS ανά χρήστη       │
│ CloudRepository   sign in/up/out, κωδικοί, διαγραφή      │◄──────►│ Edge Functions (Deno)              │
│ SyncManager       debounce, mutex, πρόοδος, merge        │ OkHttp │  ├ signup (χωρίς email verify)     │
│ SupabaseClient    μικρός REST client, χωρίς SDK          │        │  ├ password-reset (email μέσω Brevo)│
│ CloudSessionStore DataStore: tokens, flags               │        │  └ delete-account                  │
│ NetworkMonitor    online / offline                       │        │ private.keep_alive ◄─ GitHub Action │
│ Room: οι πίνακές σου (αμετάβλητοι) + sync_links          │        └────────────────────────────────────┘
└──────────────────────────────────────────────────────────┘
```

| Απόφαση | Γιατί |
|---|---|
| **Χωρίς Supabase SDK**, δικός μας REST client με OkHttp + `org.json` | Χρειάζονται λίγες κλήσεις. Δεν μπαίνουν Ktor, serialization ή εξαρτήσεις από εκδόσεις Kotlin. |
| **Οι τοπικοί πίνακες δεν αλλάζουν.** Ένας πίνακας `sync_links` αντιστοιχίζει local id ↔ remote UUID | Το sync μπαίνει «από πάνω» σε υπάρχουσα offline εφαρμογή χωρίς να πειράξει οντότητες ή migrations. |
| Το κινητό φτιάχνει τα UUID των νέων γραμμών | Δημιουργία offline, και retry χωρίς διπλές εγγραφές. |
| **Soft delete** (`deleted_at`) στον server | Οι άλλες συσκευές μαθαίνουν τις διαγραφές. |
| **Hash ανά γραμμή** (SHA-256 των πεδίων σε κανονική μορφή) | Ξέρουμε ποια πλευρά άλλαξε από το τελευταίο sync χωρίς ρολόγια. |
| Σε σύγκρουση **κερδίζει το κινητό** | Απλό και προβλέψιμο. |
| **RLS σε κάθε πίνακα** | Το publishable key είναι μέσα στο APK. Η ασφάλεια ζει μόνο στη βάση. |
| Edge functions για signup, reset και delete | Χρειάζονται service-role δικαιώματα, που δεν μπαίνουν ποτέ στο app. |

---

## 3. Backend (Supabase)

### 3.1 Project και κλειδιά
- Ένα Supabase project ανά εφαρμογή. Χρειάζεσαι το **URL** και το **publishable key** (Dashboard → Project Settings → API Keys). Είναι δημόσια, οπότε μπαίνουν στον κώδικα:

```kotlin
object SupabaseConfig {
    const val URL = "https://<project-ref>.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_..."
}
```

- Το **secret / service_role key** δεν μπαίνει ποτέ σε app ή repo. Υπάρχει αυτόματα μόνο μέσα στις edge functions ως `SUPABASE_SERVICE_ROLE_KEY`.
- Όριο free plan: **2 ενεργά δωρεάν projects** ανά λογαριασμό. Για τρίτο χρειάζεται pause/delete κάποιου ή Pro.
- Τα migrations μένουν στο repo, στο `supabase/migrations/<timestamp>_<name>.sql`, και οι functions στο `supabase/functions/<name>/index.ts`.

### 3.2 Βασικό SQL (κοινό για κάθε εφαρμογή)

**Helpers και schema `private`** (ό,τι δεν πρέπει να φαίνεται στο API):

```sql
create schema if not exists private;
revoke all on schema private from public, anon;
grant usage on schema private to authenticated;

create or replace function public.set_updated_at() returns trigger language plpgsql set search_path = '' as $$
begin new.updated_at := now(); return new; end; $$;
```

**Profile ανά χρήστη** (το όνομα που έδωσε στην εγγραφή):

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
create trigger profiles_updated_at before update on public.profiles for each row execute function public.set_updated_at();

create or replace function public.handle_new_user() returns trigger language plpgsql security definer set search_path = '' as $$
begin
  insert into public.profiles (user_id, display_name)
  values (new.id, coalesce(new.raw_user_meta_data ->> 'display_name', ''))
  on conflict (user_id) do nothing;
  return new;
end; $$;
revoke execute on function public.handle_new_user() from public, anon, authenticated;
create trigger on_auth_user_created after insert on auth.users for each row execute function public.handle_new_user();
```

**Χωρίς επιβεβαίωση email** (δίχτυ ασφαλείας, πέρα από την edge function `signup`):

```sql
create or replace function private.auto_confirm_email() returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if new.email is not null and new.email_confirmed_at is null then new.email_confirmed_at := now(); end if;
  return new;
end; $$;
revoke all on function private.auto_confirm_email() from public, anon, authenticated;
create trigger auto_confirm_email before insert on auth.users for each row execute function private.auto_confirm_email();
```

**Προσωρινοί κωδικοί** (μόνο η edge function τους αγγίζει, με service role):

```sql
create table public.password_resets (
  user_id    uuid primary key references auth.users(id) on delete cascade,
  code_hash  text not null,            -- bcrypt· ποτέ ο κωδικός σε καθαρό κείμενο
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

-- Έλεγχος (5 προσπάθειες, 1 ώρα, μία χρήση). Επιστρέφει τον χρήστη μόλις ταιριάξει.
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

**Keep-alive:**

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

### 3.3 Πρότυπο για κάθε συγχρονιζόμενο πίνακα
Κάθε πίνακας δεδομένων της εφαρμογής ακολουθεί το ίδιο σχήμα. Οι στήλες της υποδομής είναι `id`, `user_id`, `created_at`, `updated_at` και `deleted_at`. Ενδιάμεσα μπαίνουν οι στήλες δεδομένων της εφαρμογής:

```sql
create table public.items (
    id         uuid primary key,                       -- το UUID το φτιάχνει το app
    user_id    uuid not null default auth.uid() references auth.users(id) on delete cascade,
    -- … στήλες δεδομένων, με CHECKs ίδια με τους κανόνες του app …
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    deleted_at timestamptz                              -- soft delete
);
create index items_user_sync_idx on public.items(user_id, updated_at);
create trigger items_updated_at before update on public.items for each row execute function public.set_updated_at();

alter table public.items enable row level security;
create policy "Own rows read"   on public.items for select to authenticated using (user_id = (select auth.uid()));
create policy "Own rows add"    on public.items for insert to authenticated with check (user_id = (select auth.uid()));
create policy "Own rows change" on public.items for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy "Own rows delete" on public.items for delete to authenticated using (user_id = (select auth.uid()));
revoke all on public.items from anon;
```

Κανόνες:
- **Αναφορές σε άλλον πίνακα** αποθηκεύονται ως remote UUID (`parent_id uuid`). Το app τις μεταφράζει από/προς τοπικά ids μέσω των links. Για να ελέγχεται ότι ανήκουν στον ίδιο χρήστη: `unique (id, user_id)` στον γονέα και `foreign key (parent_id, user_id) references parent(id, user_id)`.
- **Πίνακας με φυσικό κλειδί** (όχι id): primary key `(user_id, <key>)` και upsert με `on_conflict=user_id,<key>`.
- Ποσά σε `numeric(p,2)`, ημερομηνίες σε `date`, λίστες σε `uuid[]` ή `text[]`.
- Τα CHECK constraints πρέπει να ταιριάζουν με ό,τι στέλνει το app, γιατί μία λάθος γραμμή απορρίπτει όλη την παρτίδα.
- **Διαγραφή λογαριασμού:** το `on delete cascade` στο `user_id` σβήνει όλα τα δεδομένα του χρήστη.

> **Κοινή χρήση (προαιρετικό).** Στο NetworthApp τα δεδομένα ανήκουν σε **ομάδα** (household) αντί για χρήστη. Αυτό απαιτεί τα εξής:
> - Πίνακες `households`, `household_members(role owner/member)` και `household_invites` (κωδικός 8 χαρακτήρων, ισχύει 7 μέρες).
> - Security-definer helper `private.is_household_member(id)` για τα RLS policies.
> - RPC `create_household_invite` και `join_household`.
> - Trigger `before delete on auth.users`: σβήνει τις ομάδες όπου ο χρήστης ήταν μόνος, και στις κοινές δίνει την ιδιοκτησία στο παλαιότερο μέλος.
> - Στη θέση του `user_id` μπαίνει `household_id`.
>
> Χρειάζεται μόνο αν θέλεις πολλούς χρήστες στα ίδια δεδομένα.

### 3.4 Edge functions
Γίνονται deploy **χωρίς JWT verification στο gateway** (`--no-verify-jwt`), γιατί κάποιες καλούνται χωρίς σύνδεση. Κάθε function ελέγχει μόνη της την είσοδο ή το token.

**`signup`**: δημιουργεί τον χρήστη ήδη επιβεβαιωμένο, χωρίς email.

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

**`delete-account`**: επιβεβαιώνει το access token του καλούντα και σβήνει αυτόν τον χρήστη.

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
  - Φτιάχνει κωδικό 10 χαρακτήρων με `crypto.getRandomValues` από αλφάβητο χωρίς μπερδεμένους χαρακτήρες: `abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789`.
  - Καλεί `rpc("request_password_reset")`. Αν επιστρέψει γραμμή, στέλνει email μέσω **Brevo** (`POST https://api.brevo.com/v3/smtp/email`, header `api-key`, HTML + text, Ελληνικά ή Αγγλικά).
  - **Απαντάει πάντα `{ok:true}`**, για να μη μαθαίνει κανείς ποια email υπάρχουν.
  - Αν αποτύχει η αποστολή: 502 `email_send_failed`.
- `{"action":"redeem","email":"…","password":"…"}`
  - Καλεί `rpc("redeem_password_reset")`.
  - Αν ταιριάξει: `admin.auth.admin.updateUserById(id, { password, user_metadata: { must_change_password: true } })`.
  - Αλλιώς: 400 `invalid_credentials`.

Ο πλήρης κώδικας είναι στο NetworthApp, στο `supabase/functions/password-reset/index.ts`. Αντιγράφεται όπως είναι, αλλάζοντας κείμενα και όνομα εφαρμογής.

**Secrets** (Dashboard → Edge Functions → Secrets):
- `BREVO_API_KEY`.
- `EMAIL_SENDER_ADDRESS`: επαληθευμένος sender στο Brevo.
- `EMAIL_SENDER_NAME`.

### 3.5 Keep-alive workflow
Τα δωρεάν projects γίνονται pause μετά από 7 μέρες χωρίς δραστηριότητα. Αρχείο `.github/workflows/supabase-keep-alive.yml`:

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

Τρέχει μόνο από το default branch (`main`).

---

## 4. Υποδομή στο Android app

### 4.1 Πακέτο `cloud/`

```
cloud/
├─ data/
│  ├─ CloudRepository.kt           sign in/up/out, κωδικοί, διαγραφή λογαριασμού
│  ├─ di/CloudModule.kt            provide SyncLinkDao
│  ├─ local/SyncLinkEntity.kt      + SyncLinkDao.kt (Room πίνακας sync_links)
│  ├─ network/NetworkMonitor.kt    StateFlow<Boolean> isOnline
│  ├─ remote/SupabaseConfig.kt     URL + publishable key
│  ├─ remote/SupabaseClient.kt     REST: Auth, PostgREST, Functions
│  ├─ remote/RemoteRows.kt         enum RemoteTable + JSON ↔ fields
│  ├─ session/CloudSessionStore.kt DataStore "cloud_prefs"
│  └─ sync/SyncManager.kt          η μηχανή του sync
├─ domain/
│  ├─ model/CloudModels.kt         CloudSession, CloudState, SyncStatus, CloudMode, SyncProgress, SyncSummary
│  ├─ model/CloudError.kt          enum λαθών + CloudException
│  ├─ sync/SyncPlanner.kt          planSync() + hashOf()  (καθαρές συναρτήσεις, unit-testable)
│  ├─ sync/SyncFields.kt           κανονικοποίηση τιμών (money, idList)
│  └─ usecase/SignOutUseCase.kt
└─ presentation/
   ├─ CloudScreen / CloudViewModel / CloudStatusCard / CloudErrorMessages
   ├─ account/    AccountSecurity* (αλλαγή/ξέχασα κωδικό, αποσύνδεση, διαγραφή, αναγκαστική αλλαγή)
   └─ onboarding/ Welcome + ροή λογαριασμού/ανεβάσματος + SuccessAnimation
```

Manifest: `INTERNET` και `ACCESS_NETWORK_STATE`. Dependency: `okhttp`.

### 4.2 Room: ένας νέος πίνακας

```kotlin
@Entity(tableName = "sync_links", primaryKeys = ["entity", "localKey"])
data class SyncLinkEntity(
    val entity: String,     // όνομα του RemoteTable
    val localKey: String,   // τοπικό id (ή φυσικό κλειδί)
    val remoteId: String,   // UUID στον server
    val hash: String,       // hash των πεδίων στο τελευταίο sync ("" = δεν επιβεβαιώθηκε)
)

@Dao
interface SyncLinkDao {
    @Query("SELECT * FROM sync_links WHERE entity = :entity") suspend fun getAll(entity: String): List<SyncLinkEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(links: List<SyncLinkEntity>)
    @Query("DELETE FROM sync_links WHERE entity = :entity") suspend fun deleteAll(entity: String)
    @Query("DELETE FROM sync_links") suspend fun clear()
}

val MIGRATION_N_N1 = object : Migration(N, N + 1) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS `sync_links` (
            `entity` TEXT NOT NULL, `localKey` TEXT NOT NULL, `remoteId` TEXT NOT NULL, `hash` TEXT NOT NULL,
            PRIMARY KEY(`entity`, `localKey`))""")
    }
}
```

Κάθε συγχρονιζόμενο DAO χρειάζεται: `getAllOnce()`, `insertReturningId()`, `update()`, `deleteById()` και `deleteAll()`.

### 4.3 SupabaseClient

```kotlin
@Singleton
class SupabaseClient @Inject constructor() {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build()

    // ── Auth ──
    fun signUp(email: String, password: String, displayName: String): CloudSession {
        call("POST", "/functions/v1/signup",
            JSONObject().put("email", email).put("password", password).put("display_name", displayName).toString())
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
        call("POST", "/functions/v1/password-reset",
            JSONObject().put("action", "request").put("email", email).put("lang", language).toString())
    }
    fun redeemTempPassword(email: String, password: String): Boolean = try {
        call("POST", "/functions/v1/password-reset",
            JSONObject().put("action", "redeem").put("email", email).put("password", password).toString()); true
    } catch (e: SupabaseException) { if (e.status == 400) false else throw e }
    fun deleteAccount(session: CloudSession) { call("POST", "/functions/v1/delete-account", "{}", session.accessToken) }
    fun displayName(session: CloudSession): String? { /* GET /rest/v1/profiles?user_id=eq.<id>&select=display_name */ }

    // ── Πίνακες ──
    /** Όλες οι γραμμές του χρήστη (και οι σβησμένες), σε σελίδες των 1000. */
    fun selectAll(session: CloudSession, table: String, orderBy: String): List<JSONObject> {
        // GET /rest/v1/<table>?user_id=eq.<userId>&select=*&order=<orderBy>&limit=1000&offset=<n>
    }
    /** Upsert σε παρτίδες των 200, με πρόοδο. */
    fun upsert(session: CloudSession, table: String, rows: List<JSONObject>, onConflict: String, onProgress: (Int, Int) -> Unit) {
        // POST /rest/v1/<table>?on_conflict=<cols>   Prefer: resolution=merge-duplicates,return=minimal
    }
    /** Soft delete σε παρτίδες των 100. */
    fun softDelete(session: CloudSession, table: String, keyColumn: String, ids: List<String>, deletedAt: String) {
        // PATCH /rest/v1/<table>?user_id=eq.<userId>&<keyColumn>=in.("…","…")  body {"deleted_at": deletedAt}
    }

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
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /** Auth: {msg, error_code} ή {error, error_description}· βάση: {message, code}. */
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

**Λάθη → `CloudError`** (ένα κοινό `Throwable.cloudMessageRes()` δίνει string resource σε κάθε ViewModel):

| code / status | CloudError |
|---|---|
| `invalid_credentials`, `invalid_grant` | INVALID_CREDENTIALS |
| `user_already_exists`, `email_exists` | USER_EXISTS |
| `weak_password` | WEAK_PASSWORD |
| `same_password` | SAME_PASSWORD |
| `email_send_failed` | EMAIL_SEND_FAILED |
| `email_address_invalid`, `validation_failed` | INVALID_EMAIL |
| `over_*` ή 429 | RATE_LIMITED |
| 401 | SESSION_EXPIRED |
| `IOException` | NO_CONNECTION |
| (στην αποσύνδεση) | UNSYNCED_CHANGES |
| άλλο | UNKNOWN |

### 4.4 Session και κατάσταση
- **`CloudSessionStore`** (DataStore `cloud_prefs`): access/refresh token, `expiresAt`, `userId`, `email`, `mustChangePassword`, `lastSyncAt`, `pending_changes`, `links_owner_id` (σε ποιον χρήστη ανήκουν τα links) και `prompt_snoozed_until` ή `account_prompt_answered`.
- **`validSession()`**: αν το token λήγει σε λιγότερο από 60", κάνει refresh. Αν το refresh απαντήσει 4xx, σβήνει το session και επιστρέφει `SESSION_EXPIRED`, οπότε ο χρήστης ξανασυνδέεται.
- **`CloudMode`** για το UI, υπολογισμένο από session, sync status, internet και pending:
  - `OFFLINE_MODE` (χωρίς λογαριασμό)
  - `ONLINE`
  - `SYNCING`
  - `NO_INTERNET`
  - `SYNC_ERROR`
- **`NetworkMonitor`**: `registerDefaultNetworkCallback`. Online μόνο με `NET_CAPABILITY_INTERNET` και `NET_CAPABILITY_VALIDATED`.

### 4.5 Η μηχανή sync

**Κανονική μορφή πεδίων.** Κάθε γραμμή γίνεται `Map<String, String?>` με κλειδιά τα ονόματα στηλών του server. Οι τιμές είναι κανονικοποιημένες:
- ποσά με σταθερά δεκαδικά
- ημερομηνίες ISO
- λίστες ταξινομημένες και χωρισμένες με κόμμα
- κείμενα κομμένα στα όρια των CHECK

Έτσι η ίδια γραμμή δίνει **ίδιο hash** στο κινητό και στον server.

```kotlin
object SyncFields {
    fun money(value: Double): String = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString()
    fun idList(ids: Collection<String>): String = ids.filter { it.isNotBlank() }.distinct().sorted().joinToString(",")
}

fun hashOf(fields: Map<String, String?>): String {
    val canonical = fields.toSortedMap().entries.joinToString("\u001F") { (k, v) -> "$k=${v ?: "\u0000"}" }
    return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
```

**Three-way merge** (`planSync`). Για κάθε γραμμή συγκρίνει την τοπική εκδοχή, του server και το link του τελευταίου sync:

| Τοπικά | Server | Αποτέλεσμα |
|---|---|---|
| ίδιο με link | ίδιο με link | τίποτα |
| άλλαξε | ίδιο | push |
| ίδιο | άλλαξε | update τοπικά |
| άλλαξε | άλλαξε στην ίδια τιμή | μόνο ενημέρωση link |
| άλλαξε | άλλαξε διαφορετικά | **κερδίζει το κινητό** → push |
| σβήστηκε | υπάρχει | soft delete στον server |
| υπάρχει, αμετάβλητο | σβήστηκε | delete τοπικά |
| υπάρχει, αλλαγμένο | σβήστηκε | push (επαναφέρεται) |
| νέο | — | νέο UUID → push |
| — | νέο | insert τοπικά |

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

**Περιγραφή πινάκων** (`RemoteRows.kt`). Είναι το μόνο σημείο που «ξέρει» ποιοι πίνακες υπάρχουν:

```kotlin
enum class RemoteTable(val tableName: String, val keyColumn: String, val fieldNames: List<String>) {
    // Ένα entry ανά συγχρονιζόμενο πίνακα, με σειρά εξάρτησης (γονείς πρώτα)
    ITEMS("items", "id", listOf(/* στήλες δεδομένων */));

    val conflictColumns: String get() = if (keyColumn == "id") "id" else "user_id,$keyColumn"
}

object RemoteRows {
    fun parse(table: RemoteTable, row: JSONObject) = RemoteItem(
        remoteId = row.getString(table.keyColumn),
        fields   = table.fieldNames.associateWith { readField(row, it) },   // κανονικοποίηση όπως στο app
        deleted  = !row.isNull("deleted_at"),
    )
    /** Πλήρης γραμμή για upsert: ζωντανή (deleted_at = null). */
    fun toJson(table: RemoteTable, userId: String, remoteId: String, fields: Map<String, String?>): JSONObject =
        JSONObject().put(table.keyColumn, remoteId).put("user_id", userId).put("deleted_at", JSONObject.NULL)
            .apply { table.fieldNames.forEach { put(it, /* αριθμός, πίνακας ή κείμενο */ fields[it] ?: JSONObject.NULL) } }
}
```

**Ένας γύρος sync** (`SyncManager.sync`), πάντα μέσα σε `Mutex`:
1. Έγκυρο session (με refresh αν χρειάζεται).
2. **Pull πρώτα** όλων των πινάκων. Αν πέσει το δίκτυο, δεν έχει αλλάξει τίποτα τοπικά.
3. Αν είναι `replaceLocal` (σύνδεση σε κενό κινητό), σβήνει τα τοπικά και τα links σε transaction.
4. Για κάθε `RemoteTable`, με τη σειρά, καλεί το γενικό `syncTable`:

```kotlin
private suspend fun syncTable(
    table: RemoteTable, session: CloudSession, remote: List<RemoteItem>, local: List<LocalItem>,
    newRemoteId: (LocalItem) -> String,                // UUID.randomUUID() ή το φυσικό κλειδί
    insert: suspend (RemoteItem) -> String?,           // → νέο local key (null = δεν εφαρμόστηκε)
    update: suspend (String, RemoteItem) -> Boolean,
    delete: suspend (String) -> Unit,
    stage: SyncStage, from: Float, to: Float,
): TableResult {
    report(stage, from)
    val oldLinks = linkDao.getAll(table.name).map { SyncLink(it.localKey, it.remoteId, it.hash) }
    val plan = planSync(local, remote, oldLinks, newRemoteId)

    // Τα links των νέων γραμμών σώζονται ΠΡΙΝ το push: ένα retry μετά από crash ξαναστέλνει ίδιο UUID
    val oldKeys = oldLinks.map { it.localKey }.toSet()
    val newRows = plan.links.filter { it.localKey !in oldKeys }
    if (newRows.isNotEmpty()) linkDao.upsertAll(newRows.map { it.toEntity(table, hash = "") })

    if (plan.push.isNotEmpty()) client.upsert(session, table.tableName,
        plan.push.map { (id, fields) -> RemoteRows.toJson(table, session.userId, id, fields) },
        table.conflictColumns, onProgress = { done, total -> report(stage, from + (to - from) * 0.9f * done / total) })
    if (plan.softDelete.isNotEmpty()) client.softDelete(session, table.tableName, table.keyColumn, plan.softDelete, Instant.now().toString())

    val finalLinks = plan.links.associateBy { it.localKey }.toMutableMap()
    db.withTransaction {
        plan.deleteLocal.forEach { delete(it) }
        plan.updateLocal.forEach { (key, row) ->
            // Δεν εφαρμόστηκε (π.χ. λείπει ο γονέας): κράτα το παλιό link για να ξαναδοκιμαστεί
            if (!update(key, row)) oldLinks.find { it.localKey == key }?.let { finalLinks[key] = it }
        }
        plan.insertLocal.forEach { row -> insert(row)?.let { key -> finalLinks[key] = SyncLink(key, row.remoteId, hashOf(row.fields)) } }
        linkDao.deleteAll(table.name)
        linkDao.upsertAll(finalLinks.values.map { it.toEntity(table, it.hash) })
    }
    report(stage, to)
    return TableResult(pushed = plan.push.size, received = plan.deleteLocal.size + plan.updateLocal.size + plan.insertLocal.size)
}
```

   Παράδειγμα σύνδεσης ενός πίνακα:

```kotlin
syncTable(
    table = RemoteTable.ITEMS, session = session, remote = remote.getValue(RemoteTable.ITEMS),
    local = itemDao.getAllOnce().map { it.toLocalItem() },   // entity → κανονικοποιημένα fields
    newRemoteId = { UUID.randomUUID().toString() },
    insert = { r -> itemDao.insertReturningId(r.toEntity(id = 0)).toString() },
    update = { key, r -> itemDao.update(r.toEntity(id = key.toLong())); true },
    delete = { key -> itemDao.deleteById(key.toLong()) },
    stage = SyncStage.DATA, from = 0.15f, to = 0.90f,
)
```

   Για πίνακα-παιδί, το `toLocalItem` μεταφράζει το τοπικό `parentId` σε remote UUID μέσω των links του γονέα, και το `toEntity` κάνει το αντίστροφο. Αν δεν βρεθεί ο γονέας, η γραμμή παραλείπεται ή επιστρέφει null και ξαναδοκιμάζεται στον επόμενο γύρο.

5. Αν ήρθαν αλλαγές από τον server: hooks μετά το sync (ανανέωση widgets, τοπικού backup κ.λπ.). Μετά `lastSyncAt` και `SyncSummary` (πόσα ανέβηκαν και πόσα ήρθαν).

**Πότε τρέχει:**

```kotlin
fun requestSync(delayMillis: Long = 1_500L, localChange: Boolean = false) {
    if (localChange) {
        changeVersion.incrementAndGet()
        if (!_hasPendingChanges.value) { _hasPendingChanges.value = true; scope.launch { store.savePendingChanges(true) } }
    }
    pending?.cancel()                                    // debounce
    pending = scope.launch {
        delay(delayMillis)
        if (store.current().isSignedIn && networkMonitor.isOnline.value) syncNow()
    }
}

suspend fun syncNow(replaceLocal: Boolean = false): Result<SyncSummary> = withContext(io) {
    mutex.withLock {
        _status.value = SyncStatus.Syncing
        val version = changeVersion.get()
        runCatching { sync(replaceLocal) }
            .onSuccess {
                _status.value = SyncStatus.Idle
                // Αλλαγές που έγιναν όσο έτρεχε μένουν pending για τον επόμενο γύρο
                if (changeVersion.get() == version) { _hasPendingChanges.value = false; store.savePendingChanges(false) }
                else requestSync()
            }
            .onFailure { _status.value = SyncStatus.Failed(it.message ?: "") }
            .recoverCatching { throw toCloudException(it) }
    }
}
```

- **Μετά από κάθε εγγραφή** στα repositories: `requestSync(localChange = true)`. Οι εγγραφές που κάνει το ίδιο το sync δεν το ξανακαλούν.
- **`MainActivity.onStart()`**: `requestSync(delayMillis = 0)`.
- **Επαναφορά internet**, στο `init` του `SyncManager`: `networkMonitor.isOnline.drop(1).filter { it }.collect { requestSync(0) }`.

### 4.6 CloudRepository

**Σύνδεση και εγγραφή**
- `signIn(email, password)` → `signInOrRedeem`:
  - Κανονικό sign-in.
  - Αν αποτύχει με `invalid_credentials`, δοκιμάζει `redeemTempPassword`. Αν πετύχει, ξανακάνει sign-in και το session έχει `mustChangePassword = true`.
- `signUp(email, password, name)` → edge function `signup`, μετά sign-in.

**Κωδικοί και λογαριασμός**
- `requestTempPassword(email, language)`.
- `changePassword(current?, new)`: ελέγχει τον τρέχοντα με sign-in. Παραλείπεται όταν ο χρήστης μπήκε με προσωρινό κωδικό.
- `deleteAccount(password)`: έλεγχος με sign-in → edge function → καθάρισμα session και links.

**`signOut(force)`**
1. Αν υπάρχουν pending αλλαγές, τρέχει `syncNow()`. Αν αποτύχει και δεν είναι `force`, επιστρέφει `UNSYNCED_CHANGES`, και το UI δείχνει «Αποσύνδεση ούτως ή άλλως» σε κόκκινο.
2. Logout και καθάρισμα session.
3. `clearLocalData()`: όλοι οι συγχρονιζόμενοι πίνακες και τα `sync_links`, σε transaction.
4. `SignOutUseCase`: σβήνει τοπικά backups, το κλείδωμα (PIN/biometric) και το τοπικό profile. Το NavHost πάει στη Welcome.
5. Μένουν οι ρυθμίσεις συσκευής (γλώσσα, θέμα).

### 4.7 UI
- **Πεδίο κωδικού:** `OutlinedTextField` με `visualTransformation` που εναλλάσσεται, και trailing `IconButton` (Visibility / VisibilityOff).
- **Αναγκαστική αλλαγή κωδικού:** στο root composable, όταν `mustChangePassword` είναι true (και μετά το lock screen), μια οθόνη καλύπτει όλη την εφαρμογή. Φεύγει μόνο με αλλαγή κωδικού ή αποσύνδεση.
- **Welcome:** start destination όταν δεν υπάρχει τοπικό profile. Τρία αποτελέσματα:
  - Συνδέθηκε και κατέβηκαν δεδομένα → συνέχεια στο app (ή στη ρύθμιση ασφάλειας).
  - Νέος λογαριασμός → κανονικό setup.
  - Χωρίς λογαριασμό → κανονικό setup.
- **Onboarding λογαριασμού/ανεβάσματος:** overlay μετά το lock screen.
  - Βήματα `INTRO → ACCOUNT → SYNCING → SUCCESS | ERROR`.
  - Η μπάρα ακολουθεί την πραγματική πρόοδο, αλλά διαρκεί **τουλάχιστον 4"**, ώστε να διαβάζονται τα στάδια (Σύνδεση → Έλεγχος → … → Ολοκλήρωση).
  - Το SUCCESS δείχνει animation, πόσα ανέβηκαν και **Συνέχεια**. Το ERROR δείχνει **Επανάληψη**.
  - Η απόφαση αν θα εμφανιστεί παίρνεται μία φορά ανά άνοιγμα. Μέχρι τότε δείχνεται κενό frame, ώστε να μη «αναβοσβήνει» η εφαρμογή.
- **Ρυθμίσεις:** κάρτα κατάστασης (`CloudMode`) που ανοίγει την οθόνη λογαριασμού και sync, με κουμπί «Συγχρονισμός τώρα».
- **Προφίλ:** ενότητα Λογαριασμού με συνδεδεμένος ως, αλλαγή κωδικού, αποσύνδεση και διαγραφή (κόκκινο).
- Όλα τα κείμενα σε `strings.xml`, Ελληνικά και Αγγλικά.

---

## 5. Εφαρμογή σε υπάρχουσα offline εφαρμογή

### 5.1 Ροή αναβάθμισης

```
Νέα έκδοση ανοίγει
 ├─ Νέα εγκατάσταση (καμία τοπική χρήση)
 │    → Welcome: [Σύνδεση] [Δημιουργία λογαριασμού] [Συνέχεια χωρίς λογαριασμό]
 │       ├ Σύνδεση  → sync με replaceLocal = true («Φέρνουμε τα δεδομένα σου…») → [Συνέχεια] → app
 │       ├ Εγγραφή  → app (δεν υπάρχει τίποτα για ανέβασμα)
 │       └ Offline  → app
 │
 └─ Υπάρχων χρήστης (υπάρχουν τοπικά δεδομένα), χωρίς session, χωρίς απάντηση ακόμα
      → «Φτιάξε λογαριασμό για να μη χάσεις ποτέ τα δεδομένα σου και να τα έχεις σε κάθε συσκευή»
        [Δημιουργία λογαριασμού]   [Έχω ήδη λογαριασμό]   [Παραμονή offline]
         ├ Εγγραφή: όνομα, email, κωδικός → signup → sign-in
         │    → «Ανεβάζουμε τα δεδομένα σου…» (sync, replaceLocal = false)
         │    → ✓ «Έτοιμο! Είσαι συνδεδεμένος ως x@y»  [Συνέχεια] → app
         ├ Σύνδεση: email, κωδικός, «Ξέχασα τον κωδικό» → sign-in
         │    → ίδια μπάρα· τα δεδομένα του κινητού ενώνονται με του λογαριασμού → ✓ [Συνέχεια]
         ├ Σφάλμα (π.χ. χωρίς internet) → μήνυμα + [Επανάληψη]· το session μένει
         └ Παραμονή offline → αποθηκεύεται η απάντηση, η εφαρμογή συνεχίζει όπως πριν.
              Η είσοδος «Λογαριασμός & συγχρονισμός» μένει στις Ρυθμίσεις.
```

- **«Υπάρχων» ή «νέος»:** ελέγχεις αν υπάρχει τοπικό profile ή γραμμές στη Room.
- **Πρώτο ανέβασμα:** είναι απλώς ένα κανονικό sync με άδειο `sync_links`. Όλες οι γραμμές είναι «νέες», παίρνουν UUID και ανεβαίνουν σε παρτίδες. Δεν χρειάζεται ξεχωριστό export.
- **«Παραμονή offline»:** το NetworthApp ξαναρωτάει σε 7 μέρες (`prompt_snoozed_until`). Για μία μόνο ερώτηση, κράτα ένα `account_prompt_answered = true`.
- **Μετά τη σύνδεση:** ενεργοποιείται το αυτόματο sync.

### 5.2 Βήματα υλοποίησης
1. **Supabase project**: πάρε URL και publishable key.
2. **SQL:** τα βασικά του §3.2 (helpers, profiles, auto-confirm, password resets, keep-alive), και ένας πίνακας ανά οντότητα με το πρότυπο του §3.3.
3. **Edge functions** `signup`, `password-reset`, `delete-account` με `--no-verify-jwt`. Secret `BREVO_API_KEY` και επαληθευμένος sender.
4. **Έλεγχος:** Advisors → Security. Δοκιμή με curl:

```bash
curl -X POST "$URL/functions/v1/signup" -H "apikey: $KEY" -H "Content-Type: application/json" \
     -d '{"email":"test@example.com","password":"secret1","display_name":"Test"}'
curl -X POST "$URL/auth/v1/token?grant_type=password" -H "apikey: $KEY" -H "Content-Type: application/json" \
     -d '{"email":"test@example.com","password":"secret1"}'
```

5. **Το πακέτο `cloud/`** του §4.1. Room migration για `sync_links`, DAO μέθοδοι, permissions στο manifest, `okhttp`.
6. **`RemoteTable` και mappers** για κάθε οντότητα, και `syncTable(...)` με σειρά εξάρτησης στον `SyncManager`. Κανονικοποίηση τιμών στο `toLocalItem()`.
7. **Triggers sync:** `requestSync(localChange = true)` μετά από κάθε εγγραφή, στο `onStart`, και με την επαναφορά του internet.
8. **UI** του §4.7 και η ροή του §5.1.
9. **Keep-alive** workflow (§3.5) με τα στοιχεία του νέου project, και push στο `main`.
10. **Δοκιμές σε κινητό:**
    - (α) αναβάθμιση με δεδομένα → εγγραφή → ανέβασμα → Συνέχεια
    - (β) δεύτερο κινητό → σύνδεση → κατεβαίνουν
    - (γ) αλλαγή offline → επανέρχεται το internet → ανεβαίνει
    - (δ) σβήσιμο σε ένα κινητό → σβήνεται και στο άλλο
    - (ε) ξέχασα τον κωδικό → email → προσωρινός → αναγκαστική αλλαγή
    - (στ) αποσύνδεση → κενή εφαρμογή → σύνδεση → επιστρέφουν
    - (ζ) διαγραφή λογαριασμού

---

## 6. Συντήρηση

| Τι | Πώς |
|---|---|
| **Αλλαγή σχήματος** | Νέο αρχείο στο `supabase/migrations/`. Εφαρμογή με `supabase db push`, με το SQL Editor ή με τον Claude μέσω Supabase MCP (`apply_migration`). Τα παλιά migrations δεν αλλάζουν ποτέ. |
| **Νέα στήλη σε συγχρονιζόμενο πίνακα** | `alter table … add column` (με default) → στο `RemoteTable.fieldNames` → στους mappers → Room migration αν είναι και τοπική. Αλλάζει το hash όλων των γραμμών, οπότε **ξαναστέλνονται μία φορά**. Είναι αναμενόμενο. |
| **Νέος πίνακας** | Πρότυπο §3.3 → νέο `RemoteTable` → `syncTable(...)` στη σωστή σειρά → προσθήκη στο `clearLocalData()`. |
| **Edge functions** | `supabase functions deploy <name> --no-verify-jwt` ή μέσω MCP. Ο κώδικας μένει στο repo. |
| **Secrets** | Dashboard → Edge Functions → Secrets. |
| **Email (Brevo)** | Ο sender πρέπει να είναι επαληθευμένος. Το δωρεάν πλάνο έχει ημερήσιο όριο, περίπου 300 email. |
| **Ασφάλεια** | Advisors → Security μετά από κάθε αλλαγή σχήματος. Αναμενόμενα: «RLS enabled, no policy» σε `password_resets` και `keep_alive` (σκόπιμα κλειστοί), και «anon can execute security definer» στην `keep_alive` (σκόπιμο, ακίνδυνο). |
| **Logs** | Dashboard → Logs (API, Auth, Edge Functions). Στο app: Timber. |
| **Keep-alive** | GitHub → Actions → «Supabase keep-alive», με κουμπί Run workflow. Έλεγχος με `select * from private.keep_alive`. Αν αποτύχει, έρχεται email από το GitHub. |
| **Χρήστες** | Dashboard → Authentication → Users. |
| **Κλειδιά** | Το publishable είναι δημόσιο. Αν αλλάξει, θέλει νέα έκδοση του app και αλλαγή στο workflow. Το secret δεν βγαίνει ποτέ από το Supabase. |

---

## 7. Παγίδες

- **Kotlin:** το `to` δένει πιο σφιχτά από το `?:`. Το `KEY to x.takeIf { … } ?: DEFAULT` γίνεται `(KEY to …) ?: DEFAULT` και βγάζει λάθος τύπου (`Serializable` αντί για `Pair`). Γράψε `KEY to (x.takeIf { … } ?: DEFAULT)`.
- **Compose:** το `Modifier.align(Alignment.End)` δουλεύει μόνο σε `ColumnScope`. Αλλού χρησιμοποίησε `Box(contentAlignment = Alignment.CenterEnd)`.
- **Email verification:** είναι ενεργό από προεπιλογή στο Supabase. Λύση: η function `signup` (με `email_confirm: true`) και το trigger `auto_confirm_email`, ή απενεργοποίηση του «Confirm email» στο Dashboard → Authentication.
- **Hash που δεν ταιριάζει ποτέ → ατελείωτα uploads:** συμβαίνει όταν ο server αποθηκεύει μια τιμή αλλιώς από ό,τι τη στέλνει το app (π.χ. `12.5` ενώ ο server γυρνάει `12.50`, ή αταξινόμητη λίστα). Κανονικοποίησε πάντα με τον ίδιο τρόπο και στο `toLocalItem` και στο `parse`.
- **CHECK constraints:** μία άκυρη γραμμή απορρίπτει όλη την παρτίδα. Καθάρισε τις τιμές πριν το sync.
- **Σειρά πινάκων:** πρώτα οι γονείς και μετά τα παιδιά. Τα foreign keys μεταφράζονται μέσω των links.
- **Αποσύνδεση:** πάντα πρώτα ανέβασμα των pending αλλαγών, αλλιώς χάνονται.
- **`apply_migration` μέσω MCP:** μπορεί να κάνει timeout στα 60". Συνήθως έχει περάσει, οπότε έλεγξε με `list_migrations` πριν το ξανατρέξεις.
- **GitHub Actions:** τα scheduled workflows τρέχουν μόνο από το default branch. Σε public repo απενεργοποιούνται μετά από 60 μέρες χωρίς commit (το workflow ξαναενεργοποιεί τον εαυτό του).
