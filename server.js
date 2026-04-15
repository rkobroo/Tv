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
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }

  // Proxy endpoint
  if (pathname.startsWith('/proxy')) {
    const targetUrl = parsed.query.url;
    if (!targetUrl) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Missing url' }));
      return;
    }

    console.log('Proxy:', targetUrl.substring(0, 60));

    const headers = {
      'User-Agent': 'Mozilla/5.0',
      'Accept': '*/*'
    };

    const protocol = targetUrl.startsWith('https') ? https : http;
    
    try {
      protocol.get(targetUrl, { headers, timeout: 30000 }, (proxyRes) => {
        const ct = proxyRes.headers['content-type'] || '';
        const isPlaylist = ct.includes('mpegurl') || ct.includes('text/plain') || targetUrl.endsWith('.m3u') || targetUrl.endsWith('.m3u8');
        
        res.setHeader('Content-Type', isPlaylist ? 'application/vnd.apple.mpegurl' : ct);
        
        if (isPlaylist) {
          let data = '';
          proxyRes.on('data', chunk => data += chunk);
          proxyRes.on('end', () => {
            const baseUrl = targetUrl.substring(0, targetUrl.lastIndexOf('/') + 1);
            const lines = data.split(/\r?\n/).map(line => {
              line = line.trim();
              if (!line || line.startsWith('#')) return line;
              if (!line.startsWith('http')) {
                return `/proxy?url=${encodeURIComponent(new URL(line, baseUrl).toString())}`;
              }
              return `/proxy?url=${encodeURIComponent(line)}`;
            }).join('\n');
            res.end(lines);
          });
        } else {
          res.writeHead(proxyRes.statusCode);
          proxyRes.pipe(res);
        }
      }).on('error', (err) => {
        res.writeHead(502, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: err.message }));
      });
    } catch (err) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: err.message }));
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
  console.log('  LIVE SPORTS TV SERVER');
  console.log('===========================================');
  console.log(`  Open: http://localhost:${PORT}`);
  console.log('===========================================\n');
});
