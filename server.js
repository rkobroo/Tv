#!/usr/bin/env node

const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');

const PORT = process.env.PORT || 3000;

const server = http.createServer((req, res) => {
  // Handle API routes
  if (req.url.startsWith('/api/')) {
    res.setHeader('Content-Type', 'application/json');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Range, Content-Type');
    
    if (req.method === 'OPTIONS') {
      res.writeHead(200);
      res.end();
      return;
    }

    // Parse query parameters
    const queryParams = new URL(req.url, `http://localhost:${PORT}`).searchParams;
    const targetUrl = queryParams.get('url');

    if (!targetUrl) {
      res.writeHead(400);
      res.end(JSON.stringify({ error: 'Missing url param' }));
      return;
    }

    // Fetch the upstream URL
    const protocol = targetUrl.startsWith('https') ? require('https') : require('http');
    
    protocol.get(targetUrl, { timeout: 10000 }, (upstreamRes) => {
      const contentType = upstreamRes.headers['content-type'] || '';
      const isPlaylist = contentType.includes('application/vnd.apple.mpegurl') || 
                        contentType.includes('application/x-mpegURL') || 
                        contentType.includes('audio/mpegurl') || 
                        contentType.includes('text/plain');

      res.setHeader('Content-Type', contentType || 'application/octet-stream');
      
      if (isPlaylist) {
        res.setHeader('Cache-Control', 'public, max-age=30, s-maxage=60');
      } else {
        res.setHeader('Cache-Control', 'public, max-age=300, s-maxage=600');
      }

      let data = '';
      upstreamRes.on('data', chunk => data += chunk);
      
      upstreamRes.on('end', () => {
        if (isPlaylist) {
          // Rewrite playlist URLs to proxy through our server
          const baseHref = targetUrl.replace(/[^/]*$/, '');
          const rewritten = data.split(/\r?\n/).map(line => {
            const t = line.trim();
            if (!t || t.startsWith('#')) return line;
            if (t.startsWith('http://') || t.startsWith('https://') || t.startsWith('/')) {
              try {
                const absoluteUrl = new URL(t, baseHref).toString();
                return `/api/hls?url=${encodeURIComponent(absoluteUrl)}`;
              } catch {
                return line;
              }
            }
            return line;
          }).join('\n');
          
          res.setHeader('Content-Type', 'application/vnd.apple.mpegurl');
          res.writeHead(upstreamRes.statusCode);
          res.end(rewritten);
        } else {
          res.writeHead(upstreamRes.statusCode);
          res.end(data);
        }
      });
    }).on('error', (err) => {
      console.error('Upstream fetch error:', err);
      res.writeHead(502);
      res.end(JSON.stringify({ error: 'Upstream fetch failed' }));
    });
    return;
  }

  // Serve static files
  let filePath = path.join(__dirname, req.url === '/' ? 'index.html' : req.url);
  
  fs.readFile(filePath, 'utf8', (err, content) => {
    if (err) {
      res.writeHead(404, { 'Content-Type': 'text/html' });
      res.end('<h1>404 - Not Found</h1>');
      return;
    }

    const ext = path.extname(filePath);
    const contentType = {
      '.html': 'text/html',
      '.css': 'text/css',
      '.js': 'application/javascript',
      '.json': 'application/json',
      '.png': 'image/png',
      '.jpg': 'image/jpeg',
      '.svg': 'image/svg+xml'
    }[ext] || 'text/plain';

    res.writeHead(200, { 'Content-Type': contentType });
    res.end(content);
  });
});

server.listen(PORT, () => {
  console.log(`🎬 Live Sports TV server running at http://localhost:${PORT}`);
  console.log(`📺 Open your browser to http://localhost:${PORT}`);
  console.log(`Press Ctrl+C to stop the server`);
});

process.on('SIGINT', () => {
  console.log('\n\n👋 Server stopped');
  process.exit(0);
});
