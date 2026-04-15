module.exports = async (req, res) => {
  const url = req.query.url;
  
  if (!url) {
    return res.status(400).json({ error: 'Missing url' });
  }

  const isPlaylist = url.endsWith('.m3u') || url.endsWith('.m3u8');

  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', '*');
  
  if (isPlaylist) {
    res.setHeader('Content-Type', 'application/vnd.apple.mpegurl');
    res.setHeader('Cache-Control', 'no-cache');
  } else {
    res.setHeader('Content-Type', 'video/mp2t');
    res.setHeader('Cache-Control', 'public, max-age=300');
  }

  const protocol = url.startsWith('https') ? require('https') : require('http');
  
  const proxyReq = protocol.get(url, {
    headers: {
      'User-Agent': 'Mozilla/5.0',
      'Accept': '*/*'
    },
    timeout: 20000
  }, (proxyRes) => {
    res.writeHead(proxyRes.statusCode);
    
    // For playlists, transform URLs
    if (isPlaylist) {
      let data = '';
      proxyRes.on('data', chunk => data += chunk);
      proxyRes.on('end', () => {
        const baseUrl = url.substring(0, url.lastIndexOf('/') + 1);
        const lines = data.split(/\r?\n/).map(line => {
          line = line.trim();
          if (!line || line.startsWith('#')) return line;
          if (!line.startsWith('http')) {
            try {
              const abs = new URL(line, baseUrl).toString();
              return abs;
            } catch { return line; }
          }
          return line;
        }).join('\n');
        res.end(lines);
      });
    } else {
      proxyRes.pipe(res);
    }
  });

  proxyReq.on('error', (err) => {
    res.status(502).end('Proxy error: ' + err.message);
  });
  
  proxyReq.on('timeout', () => {
    proxyReq.destroy();
    res.status(504).end('Timeout');
  });
};
