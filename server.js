#!/usr/bin/env node
const http = require('http');
const https = require('https');
const url = require('url');
const path = require('path');
const fs = require('fs');

const PORT = process.env.PORT || 3000;

const MIME_TYPES = {
  '.html': 'text/html',
  '.js': 'application/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml'
};

// CORS headers for all responses
const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, HEAD, OPTIONS',
  'Access-Control-Allow-Headers': 'Range, Content-Type, Origin, Referer'
};

function setHeaders(res, extraHeaders = {}) {
  Object.keys(CORS_HEADERS).forEach(key => res.setHeader(key, CORS_HEADERS[key]));
  Object.keys(extraHeaders).forEach(key => res.setHeader(key, extraHeaders[key]));
}

const server = http.createServer(async (req, res) => {
  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;

  // Handle OPTIONS preflight
  if (req.method === 'OPTIONS') {
    setHeaders(res);
    res.writeHead(200);
    res.end();
    return;
  }

  // HLS Proxy - This is the key fix!
  if (pathname === '/proxy' || pathname.startsWith('/proxy?')) {
    const targetUrl = parsedUrl.query.url;
    
    if (!targetUrl) {
      setHeaders(res);
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Missing url parameter' }));
      return;
    }

    // Validate URL
    if (!targetUrl.startsWith('http://') && !targetUrl.startsWith('https://')) {
      setHeaders(res);
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Invalid URL' }));
      return;
    }

    console.log('Proxying:', targetUrl.substring(0, 80));

    const headers = {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      'Accept': '*/*',
      'Accept-Language': 'en-US,en;q=0.9',
      'Referer': targetUrl.substring(0, targetUrl.indexOf('/', 8)) + '/'
    };

    try {
      const protocol = targetUrl.startsWith('https') ? https : http;
      
      const proxyReq = protocol.get(targetUrl, { headers, timeout: 30000 }, (proxyRes) => {
        const contentType = proxyRes.headers['content-type'] || '';
        setHeaders(res);
        
        // Check if this is a playlist
        const isPlaylist = contentType.includes('mpegurl') || 
                          contentType.includes('application/x-mpegURL') ||
                          contentType.includes('application/vnd.apple.mpegurl') ||
                          contentType.includes('text/plain') ||
                          targetUrl.endsWith('.m3u') ||
                          targetUrl.endsWith('.m3u8');

        if (isPlaylist) {
          res.setHeader('Content-Type', 'application/vnd.apple.mpegurl');
          res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
        } else {
          res.setHeader('Content-Type', contentType || 'application/octet-stream');
          res.setHeader('Cache-Control', 'public, max-age=300');
        }

        res.writeHead(proxyRes.statusCode);

        if (isPlaylist) {
          // For playlists, collect data and rewrite URLs
          let data = '';
          proxyRes.on('data', chunk => data += chunk);
          proxyRes.on('end', () => {
            const baseUrl = targetUrl.substring(0, targetUrl.lastIndexOf('/') + 1);
            const lines = data.split(/\r?\n/);
            const rewritten = lines.map(line => {
              line = line.trim();
              if (!line || line.startsWith('#')) return line;
              
              // Handle relative URLs
              if (!line.startsWith('http')) {
                const absoluteUrl = new URL(line, baseUrl).toString();
                return `/proxy?url=${encodeURIComponent(absoluteUrl)}`;
              }
              
              // Handle absolute URLs - proxy them too
              return `/proxy?url=${encodeURIComponent(line)}`;
            }).join('\n');

            res.end(rewritten);
          });
        } else {
          // Binary content - stream directly
          proxyRes.pipe(res);
        }
      });

      proxyReq.on('error', (err) => {
        console.error('Proxy request error:', err.message);
        setHeaders(res);
        res.writeHead(502, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Upstream fetch failed: ' + err.message }));
      });

      proxyReq.on('timeout', () => {
        proxyReq.destroy();
        setHeaders(res);
        res.writeHead(504, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Request timeout' }));
      });

    } catch (err) {
      console.error('Proxy error:', err);
      setHeaders(res);
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: err.message }));
    }
    return;
  }

  // Legacy /api/hls endpoint
  if (pathname.startsWith('/api/')) {
    const targetUrl = parsedUrl.query.url;
    
    if (!targetUrl) {
      setHeaders(res);
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Missing url' }));
      return;
    }

    console.log('API proxy:', targetUrl.substring(0, 80));

    const headers = {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
      'Accept': '*/*'
    };

    const protocol = targetUrl.startsWith('https') ? https : http;
    
    try {
      protocol.get(targetUrl, { headers, timeout: 30000 }, (proxyRes) => {
        const ct = proxyRes.headers['content-type'] || '';
        const isPlaylist = ct.includes('mpegurl') || ct.includes('text/plain') || targetUrl.endsWith('.m3u');
        
        setHeaders(res);
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
                return `/api/hls?url=${encodeURIComponent(new URL(line, baseUrl).toString())}`;
              }
              return `/api/hls?url=${encodeURIComponent(line)}`;
            }).join('\n');
            res.end(lines.join('\n'));
          });
        } else {
          res.writeHead(proxyRes.statusCode);
          proxyRes.pipe(res);
        }
      }).on('error', (err) => {
        setHeaders(res);
        res.writeHead(502);
        res.end(JSON.stringify({ error: err.message }));
      });
    } catch (err) {
      setHeaders(res);
      res.writeHead(500);
      res.end(JSON.stringify({ error: err.message }));
    }
    return;
  }

  // Serve static files
  let filePath = pathname === '/' ? '/index.html' : pathname;
  filePath = path.join(__dirname, filePath);
  
  const ext = path.extname(filePath);
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';

  fs.readFile(filePath, (err, data) => {
    if (err) {
      setHeaders(res);
      if (err.code === 'ENOENT') {
        res.writeHead(404, { 'Content-Type': 'text/html' });
        res.end('<h1>404 - Not Found</h1>');
      } else {
        res.writeHead(500, { 'Content-Type': 'text/html' });
        res.end('<h1>500 - Server Error</h1>');
      }
      return;
    }
    setHeaders(res);
    res.writeHead(200, { 'Content-Type': contentType });
    res.end(data);
  });
});

server.listen(PORT, () => {
  console.log('\n========================================');
  console.log('  Live Sports TV Server');
  console.log('========================================');
  console.log(`  URL: http://localhost:${PORT}`);
  console.log('========================================\n');
});

process.on('SIGINT', () => {
  console.log('\nServer stopped');
  process.exit(0);
});
