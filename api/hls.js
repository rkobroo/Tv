export default async function handler(req, res) {
  try {
    const url = req.query.url;
    if (!url) {
      res.status(400).json({ error: 'Missing url param' });
      return;
    }

    const upstreamUrl = new URL(url);
    const referer = `${upstreamUrl.protocol}//${upstreamUrl.host}/`;

    const upstream = await fetch(upstreamUrl.toString(), {
      redirect: 'follow',
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
        'Referer': referer,
        'Origin': referer
      }
    });

    const ct = upstream.headers.get('content-type') || '';
    const isText = ct.includes('application/vnd.apple.mpegurl') || ct.includes('application/x-mpegURL') || ct.includes('audio/mpegurl') || ct.includes('text/plain');

    // Always allow any origin to read
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Cache-Control', 'private, max-age=0, no-cache');

    if (isText) {
      const text = await upstream.text();
      // If it's a playlist, rewrite non-comment lines (URIs) to point back to this proxy
      const baseHref = upstreamUrl.href.replace(/[^/]*$/, '');
      const rewritten = text.split(/\r?\n/).map((line) => {
        const trimmed = line.trim();
        if (!trimmed || trimmed.startsWith('#')) return line; // keep comments, tags
        try {
          const abs = new URL(trimmed, baseHref).toString();
          return `/api/hls?url=${encodeURIComponent(abs)}`;
        } catch {
          return line;
        }
      }).join('\n');

      res.setHeader('Content-Type', 'application/vnd.apple.mpegurl');
      res.status(upstream.status).send(rewritten);
      return;
    }

    // Binary pass-through for segments/keys
    const arrayBuf = await upstream.arrayBuffer();
    res.setHeader('Content-Type', ct || 'application/octet-stream');
    res.status(upstream.status).send(Buffer.from(arrayBuf));
  } catch (e) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.status(502).json({ error: 'Upstream fetch failed' });
  }
}