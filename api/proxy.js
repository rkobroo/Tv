const http = require('http');
const https = require('https');

module.exports = async (req, res) => {
  const { url } = req.query;
  
  if (!url) {
    return res.status(400).json({ error: 'Missing url parameter' });
  }

  const headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': '*/*',
    'Origin': '*',
    'Referer': url
  };

  const protocol = url.startsWith('https') ? https : http;
  
  try {
    const proxyReq = protocol.get(url, { headers, timeout: 15000 }, (proxyRes) => {
      const ct = proxyRes.headers['content-type'] || '';
      const isPlaylist = ct.includes('mpegurl') || ct.includes('text/plain') || url.endsWith('.m3u') || url.endsWith('.m3u8');
      
      res.setHeader('Access-Control-Allow-Origin', '*');
      res.setHeader('Content-Type', isPlaylist ? 'application/vnd.apple.mpegurl' : ct);
      
      if (isPlaylist) {
        let data = '';
        proxyRes.on('data', chunk => data += chunk);
        proxyRes.on('end', () => {
          const baseUrl = url.substring(0, url.lastIndexOf('/') + 1);
          const lines = data.split(/\r?\n/).map(line => {
            line = line.trim();
            if (!line || line.startsWith('#')) return line;
            if (!line.startsWith('http')) {
              const absoluteUrl = new URL(line, baseUrl).toString();
              return `/api/proxy?url=${encodeURIComponent(absoluteUrl)}`;
            }
            return `/api/proxy?url=${encodeURIComponent(line)}`;
          }).join('\n');
          res.send(lines);
        });
      } else {
        res.setHeader('Cache-Control', 'public, max-age=300');
        res.send(proxyRes);
      }
    });

    proxyReq.on('error', (err) => {
      res.status(502).json({ error: err.message });
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};
