// Email + password sign-up without email verification: the user is created already
// confirmed and no email is sent. The app signs in with the same details right after.
import { createClient } from "npm:@supabase/supabase-js@2.45.4";

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_PASSWORD = 6;
const MAX_PASSWORD = 72;

function reply(status: number, body: Record<string, unknown>): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return reply(405, { error_code: "method_not_allowed", msg: "Use POST" });

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return reply(400, { error_code: "validation_failed", msg: "Invalid JSON" });
  }

  const email = String(body.email ?? "").trim().toLowerCase();
  const password = String(body.password ?? "");
  const displayName = String(body.display_name ?? "").trim().slice(0, 100);

  if (email.length > 254 || !EMAIL.test(email)) {
    return reply(400, { error_code: "email_address_invalid", msg: "Invalid email address" });
  }
  if (password.length < MIN_PASSWORD || password.length > MAX_PASSWORD) {
    return reply(400, { error_code: "weak_password", msg: `Password must be ${MIN_PASSWORD}-${MAX_PASSWORD} characters` });
  }

  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const url = Deno.env.get("SUPABASE_URL");
  if (!serviceKey || !url) return reply(500, { error_code: "unexpected_failure", msg: "Server is not configured" });

  const admin = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });
  const { data, error } = await admin.auth.admin.createUser({
    email,
    password,
    email_confirm: true,
    user_metadata: { display_name: displayName },
  });
  if (error) {
    const status = error.status && error.status >= 400 && error.status < 600 ? error.status : 500;
    return reply(status, { error_code: error.code ?? "unexpected_failure", msg: error.message });
  }
  return reply(200, { id: data.user?.id });
});
