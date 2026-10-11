// Deletes the signed-in user's account. Households only they belong to are deleted with their
// data; in shared ones they just leave (see the before_user_delete trigger).
import { createClient } from "npm:@supabase/supabase-js@2.45.4";

function reply(status: number, body: Record<string, unknown>): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return reply(405, { error_code: "method_not_allowed", msg: "Use POST" });

  const token = (req.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "").trim();
  if (!token) return reply(401, { error_code: "no_authorization", msg: "Sign in first" });

  const url = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !serviceKey) return reply(500, { error_code: "unexpected_failure", msg: "Server is not configured" });
  const admin = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });

  // Only a valid session of the user themselves can delete the account
  const { data, error } = await admin.auth.getUser(token);
  if (error || !data.user) return reply(401, { error_code: "bad_jwt", msg: "Session expired" });

  const { error: deleteError } = await admin.auth.admin.deleteUser(data.user.id);
  if (deleteError) {
    console.error("deleteUser", deleteError);
    return reply(500, { error_code: "unexpected_failure", msg: "Could not delete the account" });
  }
  return reply(200, { ok: true });
});
