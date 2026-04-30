const https = require('https');

module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  res.setHeader('Content-Type', 'application/json');

  if (req.method === 'OPTIONS') {
    res.status(200).end();
    return;
  }

  const defaultMatches = [
    { id: 'ipl', name: 'IPL 2026', team1: 'RCB', team2: 'SRH', time: 'LIVE', league: 'Indian Premier League', stadium: 'Rajiv Gandhi Stadium' },
    { id: 'psl', name: 'PSL 2026', team1: 'Islamabad United', team2: 'Lahore Qalandars', time: 'LIVE', league: 'Pakistan Super League', stadium: 'Rawalpindi' },
    { id: 'bbl', name: 'BBL 2026', team1: 'Sydney Sixers', team2: 'Sydney Thunder', time: '19:30', league: 'Big Bash League', stadium: 'SCG' },
    { id: 'cpl', name: 'CPL 2026', team1: 'Trinbago', team2: 'St Kitts', time: '20:00', league: 'Caribbean Premier League', stadium: 'Trinidad' },
    { id: 'wpl', name: 'WPL 2026', team1: 'Mumbai Indians', team2: 'Delhi Capitals', time: '14:00', league: 'Womens Premier League', stadium: 'Mumbai' }
  ];

  const extractStreamLinks = (html) => {
    const links = {};
    try {
      const yonoMatch = html.match(/yonotv\.pages\.dev[^"'\s<>,)]+/gi);
      if (yonoMatch) {
        links['yono'] = [...new Set(yonoMatch)].map(u => ({
          name: 'YonoTV Stream',
          quality: 'HD',
          url: 'https://' + u
        }));
      }
    } catch (e) { }
    return links;
  };

  const fetchUrl = (url) => {
    return new Promise((resolve, reject) => {
      https.get(url, { timeout: 10000 }, (res) => {
        let data = '';
        res.on('data', chunk => data += chunk);
        res.on('end', () => resolve(data));
      }).on('error', reject);
    });
  };

  try {
    const matchData = await fetchUrl('https://www.newsecrettips.com/p/match-data.html?id=ipl');
    const streamLinks = extractStreamLinks(matchData);

    res.status(200).json({
      matches: defaultMatches,
      streamLinks: streamLinks,
      fetchedAt: new Date().toISOString()
    });
  } catch (err) {
    res.status(200).json({
      matches: defaultMatches,
      streamLinks: {},
      fetchedAt: new Date().toISOString()
    });
  }
};