// Ticked AI proxy (Cloudflare Worker).
//
// The apps never see the Groq API key. They send their Firebase ID token plus the task lines to
// summarise; this Worker verifies the token, rate-limits per user and calls Groq.
//
//   POST /v1/ai   Authorization: Bearer <Firebase ID token>
//   { kind: "weekly" | "standup", period: string, lines: string[], plan?: string, blockers?: string }
//   → { text: string }

import { createRemoteJWKSet, jwtVerify } from 'jose';

const GOOGLE_KEYS = createRemoteJWKSet(
  new URL('https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com')
);

const MAX_LINES = 300;
const MAX_LINE = 300;
const MAX_EXTRA = 1000;

const PROMPTS = {
  weekly: `You are the weekly review assistant inside Ticked, an app where people log tasks they have COMPLETED.
Write a short, warm and specific review of the user's week using ONLY the tasks provided.
Never invent tasks, numbers, people or details. Each task line is "Day date time · title [category] ★ — notes" (★ = starred, important to the user).

Reply in Markdown, under 220 words, with exactly these sections:
## Highlights
- 3 to 5 bullets naming the most meaningful accomplishments (prefer starred tasks).
## Where your time went
- One bullet per category: the category in bold, how many tasks, and a few words about them.
## Patterns
- 1 or 2 observations grounded in the dates (busiest day, consistency, quiet days).
## Next week
- 1 or 2 concrete, encouraging suggestions.

If there are no tasks, reply with one or two kind sentences suggesting they log what they finish.`,

  standup: `You write daily standup updates for the Ticked app from tasks the user COMPLETED.
Use ONLY the tasks, plans and blockers provided. Never invent work.

Reply in plain text ready to paste into Slack (no Markdown headings), under 120 words, in exactly this shape:
*Yesterday*
• ...
*Today*
• ...
*Blockers*
• ...

Rules:
- Merge closely related tasks into one bullet. Keep each bullet under 15 words.
- Yesterday bullets start with a past-tense verb ("Shipped", "Fixed", "Reviewed").
- Today: use the plans given. If none are given, write "• To be planned".
- Blockers: use the blockers given, otherwise "• None".
- If there are no completed tasks, write "• No tasks logged" under Yesterday.`,
};

export default {
  async fetch(request, env) {
    const cors = corsHeaders(request.headers.get('Origin'), env);
    if (request.method === 'OPTIONS') return new Response(null, { status: 204, headers: cors });

    const { pathname } = new URL(request.url);
    if (pathname === '/health') return json({ ok: true }, 200, cors);
    if (request.method !== 'POST' || pathname !== '/v1/ai') return json({ error: 'Not found' }, 404, cors);

    // 1. Only signed-in Ticked users (valid Firebase ID token for our project).
    const token = (request.headers.get('Authorization') || '').replace(/^Bearer\s+/i, '');
    let uid;
    try {
      const { payload } = await jwtVerify(token, GOOGLE_KEYS, {
        issuer: `https://securetoken.google.com/${env.FIREBASE_PROJECT_ID}`,
        audience: env.FIREBASE_PROJECT_ID,
      });
      uid = payload.sub;
      if (!uid) throw new Error('no subject');
    } catch {
      return json({ error: 'Please sign in again.' }, 401, cors);
    }

    // 2. Per-user rate limit (Cloudflare rate-limiting binding; skipped if not configured).
    if (env.AI_LIMITER) {
      const { success } = await env.AI_LIMITER.limit({ key: uid });
      if (!success) return json({ error: 'Too many requests. Try again in a minute.' }, 429, cors);
    }

    // 3. Validate and bound the input.
    let body;
    try {
      body = await request.json();
    } catch {
      return json({ error: 'Invalid request.' }, 400, cors);
    }
    const system = PROMPTS[body?.kind];
    if (!system) return json({ error: 'Unknown request type.' }, 400, cors);
    const clip = (s, n = MAX_EXTRA) => String(s ?? '').trim().slice(0, n);
    const lines = (Array.isArray(body.lines) ? body.lines : []).slice(0, MAX_LINES).map(l => clip(l, MAX_LINE)).filter(Boolean);

    let user = `Period: ${clip(body.period, 120) || 'unspecified'}\n\nCompleted tasks:\n` +
      (lines.length ? lines.map(l => `- ${l}`).join('\n') : '(none)');
    if (body.kind === 'standup') {
      user += `\n\nPlans for today: ${clip(body.plan) || '(none given)'}\nBlockers: ${clip(body.blockers) || '(none given)'}`;
    }

    // 4. Ask Groq.
    if (!env.GROQ_API_KEY) return json({ error: 'The AI service is not configured yet.' }, 503, cors);
    let res;
    try {
      res = await fetch('https://api.groq.com/openai/v1/chat/completions', {
        method: 'POST',
        headers: { Authorization: `Bearer ${env.GROQ_API_KEY}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({
          model: env.GROQ_MODEL,
          temperature: 0.4,
          max_tokens: 900,
          messages: [{ role: 'system', content: system }, { role: 'user', content: user }],
        }),
      });
    } catch {
      return json({ error: "Couldn't reach the AI service. Please try again." }, 502, cors);
    }
    if (!res.ok) {
      console.error('Groq error', res.status, await res.text().catch(() => ''));
      const msg = res.status === 429 ? 'The AI is busy right now. Try again in a moment.' : 'The AI service failed. Please try again.';
      return json({ error: msg }, 502, cors);
    }
    const data = await res.json();
    const text = data?.choices?.[0]?.message?.content?.trim();
    if (!text) return json({ error: 'The AI returned an empty answer. Please try again.' }, 502, cors);
    return json({ text }, 200, cors);
  },
};

function corsHeaders(origin, env) {
  const allowed = (env.ALLOWED_ORIGINS || '').split(',').map(s => s.trim()).filter(Boolean);
  const headers = {
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Authorization, Content-Type',
    'Access-Control-Max-Age': '86400',
    Vary: 'Origin',
  };
  if (origin && allowed.includes(origin)) headers['Access-Control-Allow-Origin'] = origin;
  return headers;
}

function json(data, status, headers) {
  return new Response(JSON.stringify(data), { status, headers: { ...headers, 'Content-Type': 'application/json' } });
}
