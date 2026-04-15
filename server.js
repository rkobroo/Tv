#!/usr/bin/env node
const http = require('http');
const https = require('https');
const url = require('url');
const path = require('path');
const fs = require('fs');

const PORT = 3000;

const MIME = {
  '.html': 'text/html',
  '.js': 'application/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml'
};

const server = http.createServer(async (req, res) => {
  const parsed = url.parse(req.url, true);
  const pathname = parsed.pathname;

  // CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Range');
  res.setHeader('Access-Control-Expose-Headers', 'Content-Length, Content-Range');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }

  // Proxy endpoint - optimized for streaming
  if (pathname.startsWith('/proxy')) {
    const targetUrl = parsed.query.url;
    if (!targetUrl) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Missing url' }));
      return;
    }

    const headers = {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
      'Accept': '*/*',
      'Accept-Encoding': 'identity', // Don't accept compressed
      'Connection': 'keep-alive'
    };

    const protocol = targetUrl.startsWith('https') ? https : http;
    
    try {
      const proxyReq = protocol.get(targetUrl, { headers, timeout: 15000 }, (proxyRes) => {
        const ct = proxyRes.headers['content-type'] || '';
        const isPlaylist = ct.includes('mpegurl') || ct.includes('text/plain') || targetUrl.endsWith('.m3u') || targetUrl.endsWith('.m3u8');
        
        res.setHeader('Content-Type', isPlaylist ? 'application/vnd.apple.mpegurl' : ct);
        res.setHeader('Cache-Control', 'no-cache, no-store');
        res.setHeader('Connection', 'keep-alive');
        
        // For playlists, rewrite URLs and stream immediately
        if (isPlaylist) {
          const baseUrl = targetUrl.substring(0, targetUrl.lastIndexOf('/') + 1);
          
          proxyRes.on('data', (chunk) => {
            const lines = chunk.toString().split(/\r?\n/);
            const rewritten = lines.map(line => {
              line = line.trim();
              if (!line || line.startsWith('#')) return line;
              if (!line.startsWith('http')) {
                try {
                  const absoluteUrl = new URL(line, baseUrl).toString();
                  return `/proxy?url=${encodeURIComponent(absoluteUrl)}`;
                } catch { return line; }
              }
              return `/proxy?url=${encodeURIComponent(line)}`;
            }).join('\n') + '\n';
            
            res.write(rewritten);
          });
          
          proxyRes.on('end', () => {
            res.end();
          });
        } else {
          // Binary - stream directly
          res.writeHead(proxyRes.statusCode, proxyRes.headers);
          proxyRes.pipe(res);
        }
      });

      proxyReq.on('error', (err) => {
        console.error('Proxy error:', err.message);
        if (!res.headersSent) {
          res.writeHead(502, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: err.message }));
        }
      });

      proxyReq.on('timeout', () => {
        proxyReq.destroy();
        if (!res.headersSent) {
          res.writeHead(504);
          res.end();
        }
      });

    } catch (err) {
      if (!res.headersSent) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: err.message }));
      }
    }
    return;
  }

  // Serve files
  let filePath = pathname === '/' ? '/index.html' : pathname;
  filePath = path.join(__dirname, filePath);

  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(404, { 'Content-Type': 'text/html' });
      res.end('<h1>404 Not Found</h1>');
      return;
    }
    const ext = path.extname(filePath);
    res.writeHead(200, { 'Content-Type': MIME[ext] || 'text/plain' });
    res.end(data);
  });
});

server.listen(PORT, () => {
  console.log('\n===========================================');
  console.log('  LIVE SPORTS TV - Streaming Server');
  console.log('===========================================');
  console.log(`  Open: http://localhost:${PORT}`);
  console.log('===========================================\n');
});
