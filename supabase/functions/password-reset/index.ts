// Forgot password with a temporary password sent by email.
//
//   {"action": "request", "email": "...", "lang": "el"}  → emails a temporary password
//   {"action": "redeem",  "email": "...", "password": "..."} → if it's the temporary password,
//       it becomes the account's password and the app asks for a new one right after signing in
//
// "request" answers the same whether or not the account exists. The current password keeps
// working until the temporary one is used.
import { createClient } from "npm:@supabase/supabase-js@2.45.4";

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
// No 0/O, 1/l/I: easy to read from an email and type on a phone
const ALPHABET = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const TEMP_LENGTH = 10;

function reply(status: number, body: Record<string, unknown>): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function tempPassword(): string {
  const bytes = new Uint8Array(TEMP_LENGTH);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, (b) => ALPHABET[b % ALPHABET.length]).join("");
}

function escapeHtml(text: string): string {
  return text.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]!);
}

function emailContent(lang: string, name: string, code: string) {
  const greek = lang === "el";
  const hello = name ? (greek ? `Γεια σου ${escapeHtml(name)},` : `Hi ${escapeHtml(name)},`) : (greek ? "Γεια σου," : "Hi,");
  const subject = greek ? "Ο προσωρινός σου κωδικός για το NetWorth" : "Your NetWorth temporary password";
  const intro = greek
    ? "Ζήτησες νέο κωδικό. Συνδέσου στην εφαρμογή με αυτόν τον προσωρινό κωδικό:"
    : "You asked for a new password. Sign in to the app with this temporary password:";
  const after = greek
    ? "Μόλις συνδεθείς θα σου ζητηθεί να βάλεις νέο κωδικό. Ο προσωρινός ισχύει για 1 ώρα και μόνο μία φορά. Ο παλιός σου κωδικός λειτουργεί μέχρι να τον χρησιμοποιήσεις."
    : "Once you're in, you'll be asked to choose a new password. It works once, within 1 hour. Your old password keeps working until you use it.";
  const ignore = greek
    ? "Αν δεν το ζήτησες εσύ, αγνόησε αυτό το email."
    : "If you didn't ask for this, you can ignore this email.";
  const html = `<!doctype html><html><body style="margin:0;padding:24px;background:#f4f6f5;font-family:Arial,Helvetica,sans-serif;color:#1d1d1f">
<div style="max-width:440px;margin:0 auto;background:#ffffff;border-radius:16px;padding:28px">
<p style="font-size:24px;margin:0 0 16px">💰 NetWorth</p>
<p style="margin:0 0 12px">${hello}</p>
<p style="margin:0 0 16px">${intro}</p>
<p style="font-family:'Courier New',monospace;font-size:26px;letter-spacing:3px;font-weight:bold;background:#eef7f1;border-radius:12px;padding:16px;text-align:center;margin:0 0 16px">${code}</p>
<p style="margin:0 0 12px;color:#555">${after}</p>
<p style="margin:0;color:#888;font-size:13px">${ignore}</p>
</div></body></html>`;
  const text = `${hello}\n\n${intro}\n\n${code}\n\n${after}\n\n${ignore}`;
  return { subject, html, text };
}

async function sendEmail(to: string, name: string, lang: string, code: string): Promise<boolean> {
  const apiKey = Deno.env.get("BREVO_API_KEY");
  if (!apiKey) {
    console.error("BREVO_API_KEY is not set");
    return false;
  }
  const { subject, html, text } = emailContent(lang, name, code);
  const response = await fetch("https://api.brevo.com/v3/smtp/email", {
    method: "POST",
    headers: { "api-key": apiKey, "Content-Type": "application/json", "Accept": "application/json" },
    body: JSON.stringify({
      sender: {
        name: Deno.env.get("EMAIL_SENDER_NAME") ?? "NetWorth",
        email: Deno.env.get("EMAIL_SENDER_ADDRESS") ?? "agcodinglp@gmail.com",
      },
      to: [name ? { email: to, name } : { email: to }],
      subject,
      htmlContent: html,
      textContent: text,
    }),
  });
  if (!response.ok) console.error("Brevo error", response.status, await response.text());
  return response.ok;
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return reply(405, { error_code: "method_not_allowed", msg: "Use POST" });

  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return reply(400, { error_code: "validation_failed", msg: "Invalid JSON" });
  }

  const action = String(body.action ?? "");
  const email = String(body.email ?? "").trim().toLowerCase();
  if (email.length > 254 || !EMAIL.test(email)) {
    return reply(400, { error_code: "email_address_invalid", msg: "Invalid email address" });
  }

  const url = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !serviceKey) return reply(500, { error_code: "unexpected_failure", msg: "Server is not configured" });
  const admin = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });

  if (action === "request") {
    const code = tempPassword();
    const { data, error } = await admin.rpc("request_password_reset", { p_email: email, p_code: code });
    if (error) {
      console.error("request_password_reset", error);
      return reply(500, { error_code: "unexpected_failure", msg: "Could not create a temporary password" });
    }
    const row = Array.isArray(data) ? data[0] : null;
    if (row) {
      const lang = String(body.lang ?? "en") === "el" ? "el" : "en";
      const sent = await sendEmail(email, String(row.display_name ?? "").slice(0, 100), lang, code);
      if (!sent) return reply(502, { error_code: "email_send_failed", msg: "Could not send the email" });
    }
    // Same answer whether or not the account exists
    return reply(200, { ok: true });
  }

  if (action === "redeem") {
    const password = String(body.password ?? "");
    if (password.length === 0 || password.length > 72) {
      return reply(400, { error_code: "invalid_credentials", msg: "Invalid login credentials" });
    }
    const { data: userId, error } = await admin.rpc("redeem_password_reset", { p_email: email, p_code: password });
    if (error) {
      console.error("redeem_password_reset", error);
      return reply(500, { error_code: "unexpected_failure", msg: "Could not check the temporary password" });
    }
    if (!userId) return reply(400, { error_code: "invalid_credentials", msg: "Invalid login credentials" });

    const { error: updateError } = await admin.auth.admin.updateUserById(String(userId), {
      password,
      user_metadata: { must_change_password: true },
    });
    if (updateError) {
      console.error("updateUserById", updateError);
      return reply(500, { error_code: "unexpected_failure", msg: "Could not apply the temporary password" });
    }
    return reply(200, { ok: true });
  }

  return reply(400, { error_code: "validation_failed", msg: "Unknown action" });
});
