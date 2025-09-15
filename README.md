## Live Sports TV (Video.js + IPTV-org)

A minimal static web app that fetches and parses the `sports.m3u` playlist from `iptv-org/iptv` and plays HLS (`.m3u8`) live TV channels using Video.js.

- Playlist: `https://raw.githubusercontent.com/iptv-org/iptv/main/categories/sports.m3u`
- Player: Video.js (via CDN)
- Focus: Sports channels; Nepal-first sorting (NP → IN → BD → PK → LK → BT)

### Features
- Fetch and parse M3U playlist (channel name + HLS URL)
- Dropdown to choose channels; play in Video.js
- Basic error handling with alerts
- CORS fallback options (local file or your own proxy)
- Guidance for geo-restricted content (Nepal)

### Legal & License
- Channel lists from `iptv-org/iptv` under the MIT License: `https://github.com/iptv-org/iptv`.
- Only access streams you are legally allowed to watch in your region.

---

## Quick Start (Local)

1. Open `index.html` directly in a modern browser.
2. The app tries to fetch `sports.m3u` from GitHub.
3. If CORS blocks it, download the file and use the file picker in the UI.

---

## Deploy on Vercel (Recommended)

This is a static site—no build step needed.

1. Create a new Vercel project.
   - Drag-and-drop the folder with `index.html`, or
   - Push to GitHub and import the repo in Vercel.
2. Deploy. Open the deployment URL.

### Optional: CORS Proxy via Vercel Serverless Function
Some playlists/streams may require permissive CORS headers. Create `api/proxy.js`:

```js
export default async function handler(req, res) {
  const url = req.query.url;
  if (!url) return res.status(400).json({ error: 'Missing url param' });
  try {
    const upstream = await fetch(url, { headers: { 'User-Agent': 'Mozilla/5.0' } });
    const body = await upstream.text();
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Content-Type', upstream.headers.get('content-type') || 'text/plain');
    res.status(upstream.status).send(body);
  } catch (e) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.status(502).json({ error: 'Upstream fetch failed' });
  }
}
```

Use it like:
```
https://YOUR-APP.vercel.app/api/proxy?url=https://raw.githubusercontent.com/iptv-org/iptv/main/categories/sports.m3u
```

Only proxy legal content you are licensed to access.

---

## Testing Streams

1. Open the app.
2. Keep "Nepal-first sorting" checked.
3. Wait for channels to load (or use the file picker).
4. Select a channel and click Play.
5. If a stream fails, try another—links can go offline or require specific headers.

---

## Geo-Restriction Notes (Nepal)

- Prefer channels with country code NP, then IN/BD/PK/LK/BT.
- Use your regular ISP connection in Nepal for best compatibility.
- If a broadcaster restricts to a region, use official apps/services that provide legal access in your location.
- Do not attempt to bypass restrictions where it violates terms or laws.

---

## MIT License

This project is under the MIT License. Channel listings courtesy of `iptv-org/iptv` (MIT).