# Running Live Sports TV Locally

## Quick Start

### Option 1: Using Python (Recommended)
If you have Python installed, run:
```bash
python -m http.server 3000
```

Then open your browser to: **http://localhost:3000**

### Option 2: Using Node.js
If you have Node.js installed, run:
```bash
node server.js
```

Then open your browser to: **http://localhost:3000**

### Option 3: Windows Batch File
Double-click: **start-server.bat**

---

## Project Structure

```
Tv/
├── index.html          # Main application file
├── api/
│   └── hls.js         # HLS proxy handler (for Vercel deployment)
├── package.json       # NPM configuration
├── server.js          # Node.js development server
├── run-server.ps1     # PowerShell server script
├── start-server.bat   # Windows batch file
└── README.md          # Original documentation
```

---

## Features

✅ **Fetch and parse M3U playlist** from iptv-org/iptv
✅ **Play HLS streams** with Video.js player
✅ **Nepal-first sorting** (NP → IN → BD → PK → LK → BT)
✅ **Favorites management** with localStorage
✅ **Channel search and filtering** by category
✅ **Responsive UI** with dark theme
✅ **CORS proxy** for HLS streams
✅ **Error handling and retry logic**

---

## Fixed Issues

1. ✅ Removed duplicate `smoothQualityChange` property in Video.js config
2. ✅ Fixed variable shadowing in `processStreamsInChunks()` function
3. ✅ Added missing `return` statement in error handler
4. ✅ Added error handling for `setFavorites()` localStorage operations

---

## Requirements

- **Browser**: Modern browser with ES6+ support (Chrome, Firefox, Safari, Edge)
- **Internet**: Required to fetch channel lists from iptv-org API
- **Optional**: Python or Node.js for running development server

---

## Deployment

This project is designed for **Vercel serverless deployment**:

```bash
# Deploy with Vercel CLI
vercel deploy
```

The `/api/hls.js` file will be deployed as a serverless function.

---

## Legal Notice

- Only use streams you are legally allowed to watch in your region
- Data source: [iptv-org/iptv](https://github.com/iptv-org/iptv) (MIT License)
- You are responsible for complying with all applicable laws and content rights

---

## Troubleshooting

### "Cannot find module" or "Server won't start"
- Install Node.js from https://nodejs.org/
- Or install Python from https://www.python.org/

### "Streams not loading"
- Check your internet connection
- Try enabling "Nepal-first" toggle
- Click the "Reload" button to refresh stream list

### "Favorites not saving"
- Clear browser cache and localStorage
- Try a different browser
- Check if browser allows localStorage

### "Video won't play"
- Some streams may have CORS restrictions
- Try another channel
- Check browser console for detailed error messages

---

## Technology Stack

- **Frontend**: HTML5, CSS3, Vanilla JavaScript (ES6+)
- **Video Player**: [Video.js](https://videojs.com/) 8.x
- **Data Source**: [iptv-org/iptv](https://github.com/iptv-org/iptv)
- **Server**: Node.js / Python HTTP Server
- **Deployment**: Vercel (serverless)

---

## License

MIT License - See [iptv-org/iptv](https://github.com/iptv-org/iptv) for details.
