export default async function handler(req, res) {
  try {
    const url = req.query.url;
    if (!url) {
      res.status(400).json({ error: 'Missing url param' });
      return;
    }

    const upstreamUrl = new URL(url);
    const referer = `${upstreamUrl.protocol}//${upstreamUrl.host}/`;

    // Set up timeout and abort controller
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 10000); // 10s timeout

    const upstream = await fetch(upstreamUrl.toString(), {
      redirect: 'follow',
      signal: controller.signal,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
        'Referer': referer,
        'Origin': referer,
        'Accept': 'application/vnd.apple.mpegurl,application/x-mpegURL,application/json,text/plain,*/*',
        'Accept-Encoding': 'gzip, deflate, br',
        'Connection': 'keep-alive',
        'Cache-Control': 'no-cache',
        'Accept-Language': 'en-US,en;q=0.9',
        'Sec-Fetch-Dest': 'empty',
        'Sec-Fetch-Mode': 'cors',
        'Sec-Fetch-Site': 'cross-site'
      }
    });

    clearTimeout(timeoutId);

    const ct = upstream.headers.get('content-type') || '';
    const isPlaylist = ct.includes('application/vnd.apple.mpegurl') || ct.includes('application/x-mpegURL') || ct.includes('audio/mpegurl') || ct.includes('text/plain');

    // Always allow any origin to read
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Range, Content-Type');
    
    // Optimized caching based on content type
    if (isPlaylist) {
      res.setHeader('Cache-Control', 'public, max-age=30, s-maxage=60'); // Short cache for playlists
    } else {
      res.setHeader('Cache-Control', 'public, max-age=300, s-maxage=600'); // Longer cache for segments
    }

    if (isPlaylist) {
      const text = await upstream.text();
      const baseHref = upstreamUrl.href.replace(/[^/]*$/, '');

      // helper to absolutize URLs
      const absolutize = (u) => {
        try { return new URL(u, baseHref).toString(); } catch { return u; }
      };
      const proxify = (u) => `/api/hls?url=${encodeURIComponent(absolutize(u))}`;

      const rewritten = text.split(/\r?\n/).map((line, idx, arr) => {
        const t = line.trim();
        if (!t) return line;

        // Rewrite URI="..." attributes in LL-HLS and related tags
        if (t.startsWith('#EXT-X-KEY') || t.startsWith('#EXT-X-MAP') || t.startsWith('#EXT-X-PART') || t.startsWith('#EXT-X-PRELOAD-HINT') || t.startsWith('#EXT-X-I-FRAME-STREAM-INF')) {
          return line.replace(/URI="(.*?)"/g, (m, g1) => `URI="${proxify(g1)}"`);
        }

        // Handle EXT-X-SESSION-KEY
        if (t.startsWith('#EXT-X-SESSION-KEY')) {
          return line.replace(/URI="(.*?)"/g, (m, g1) => `URI="${proxify(g1)}"`);
        }

        // Variant playlists: #EXT-X-STREAM-INF next line is a URI
        if (t.startsWith('#EXT-X-STREAM-INF') || t.startsWith('#EXT-X-MEDIA')) {
          return line; // keep tag; next non-tag line will be handled below
        }

        // Handle EXT-X-DISCONTINUITY-SEQUENCE and other metadata
        if (t.startsWith('#EXT-X-DISCONTINUITY-SEQUENCE') || t.startsWith('#EXT-X-TARGETDURATION') || t.startsWith('#EXT-X-VERSION')) {
          return line;
        }

        // Segments or child playlist URIs (non-tag lines)
        if (!t.startsWith('#')) {
          // Only proxy if it looks like a URL
          if (t.startsWith('http://') || t.startsWith('https://') || t.startsWith('/')) {
            return proxify(t);
          }
          return line;
        }

        return line;
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
    res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Range, Content-Type');
    
    if (e.name === 'AbortError') {
      res.status(504).json({ error: 'Request timeout' });
    } else {
      res.status(502).json({ error: 'Upstream fetch failed' });
    }
  }
}