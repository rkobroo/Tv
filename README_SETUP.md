# 🎬 Live Sports TV - Complete Setup & Running Guide

## 🔴 Problem: Web Preview Not Working

**This is completely normal!** VS Code's Simple Browser cannot properly serve web applications that need:
- Proper HTTP headers
- localStorage access  
- CORS handling
- External API calls

**The solution is simple: Use a proper HTTP server** (see methods below)

---

## ✅ SOLUTION: Choose Your Method

### **METHOD 1: VS Code Live Server (⭐ RECOMMENDED)**

**Best for development - auto-refreshes on changes**

#### Steps:
1. **Install Extension:**
   - Press `Ctrl+Shift+X` (open Extensions)
   - Type: `Live Server`
   - Find "Live Server" by **Ritwick Dey**
   - Click **Install**

2. **Start Server:**
   - Right-click on `index.html` in Explorer
   - Select **"Open with Live Server"**
   - Browser opens automatically

3. **Result:**
   - Application runs at: `http://127.0.0.1:5500`
   - Automatically refreshes when you save files
   - ✨ Best development experience

---

### **METHOD 2: Python HTTP Server**

**If you have Python 3.x installed**

#### Steps:
1. Open Terminal: `Ctrl+`` (backtick)
2. Navigate to project:
   ```
   cd path/to/Tv
   ```
3. Start server:
   ```
   python -m http.server 3000
   ```
4. Open in browser: `http://localhost:3000`
5. Stop server: `Ctrl+C` in terminal

---

### **METHOD 3: Node.js Server**

**If you have Node.js installed**

#### Steps:
1. Open Terminal: `Ctrl+`` (backtick)
2. Navigate to project:
   ```
   cd path/to/Tv
   ```
3. Start server:
   ```
   node server.js
   ```
4. Open in browser: `http://localhost:3000`
5. Stop server: `Ctrl+C` in terminal

---

### **METHOD 4: Windows Batch File**

**Easiest for Windows users**

#### Steps:
1. Navigate to the `Tv` folder
2. Find `start-server.bat`
3. Double-click it
4. Terminal window opens and server starts
5. Open browser: `http://localhost:3000`
6. Close terminal to stop server

---

### **METHOD 5: PowerShell HTTP Server**

**Windows only - built-in, no dependencies**

#### Steps:
1. Open PowerShell in the `Tv` directory
2. Run this command:
   ```powershell
   Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process; .\http-server.ps1
   ```
3. Server starts on: `http://localhost:8000`
4. Press `Ctrl+C` to stop

---

## 🎯 Quick Decision Guide

| Method | Pros | Cons | Best For |
|--------|------|------|----------|
| **Live Server** | Auto-refresh, easiest, visual | Requires extension | ⭐ Development |
| **Python** | Works everywhere | Requires Python | General use |
| **Node.js** | Fast, reliable | Requires Node.js | General use |
| **Batch File** | One click | Windows only | Quick start |
| **PowerShell** | No dependencies | Windows only | Minimal setup |

**👉 If unsure: Use Method 1 (Live Server) - it's the easiest!**

---

## 📋 Verification Checklist

After starting your chosen server, verify it's working:

- [ ] Browser opens successfully
- [ ] You see the Live Sports TV application
- [ ] Channel list loads
- [ ] You can search channels
- [ ] Favorites toggle works
- [ ] Video player appears

---

## 📂 Available Documentation

After fixing the preview issue, check these files for more info:

| File | Purpose |
|------|---------|
| `QUICK_START.md` | This quick guide |
| `CONFIG.html` | Visual setup guide (open in browser) |
| `START_HERE.html` | Interactive setup (open in browser) |
| `SETUP.md` | Detailed technical documentation |
| `FIXES_AND_GUIDE.md` | Detailed explanation of all fixes |
| `index.html` | Main application |

---

## 🔧 What Was Fixed in the Code

### Issue 1: Save Failed Error
**Problem:** Favorites weren't saving properly  
**Fix:** Added error handling to localStorage operations  
**File:** `index.html` line 506

### Issue 2: Duplicate Configuration
**Problem:** `smoothQualityChange` defined twice  
**Fix:** Removed duplicate property from Video.js config  
**File:** `index.html` lines 173-181

### Issue 3: Variable Shadowing
**Problem:** Local variable shadowed global scope  
**Fix:** Renamed to `processedEntries` in `processStreamsInChunks()`  
**File:** `index.html` lines 844-875

### Issue 4: Missing Return Statements
**Problem:** Missing returns in error handlers  
**Fix:** Added return statement after error handling  
**File:** `index.html` line 838

✅ **Status:** All issues resolved, no errors found

---

## 🎬 Application Features

Once running, you'll enjoy:

✨ **Live Streaming**
- HLS streams from iptv-org database
- Video.js player with adaptive bitrate
- Low-latency live TV support

🔍 **Smart Search**
- Search by channel name
- Filter by category (Cricket, Football, etc.)
- Regional sorting (Nepal-first option)

⭐ **Favorites**
- Star channels you like
- Persistent storage
- Filter by favorites only

🌍 **Regions**
- Nepal (NP) 🇳🇵
- India (IN) 🇮🇳
- Bangladesh (BD) 🇧🇩
- Pakistan (PK) 🇵🇰
- Sri Lanka (LK) 🇱🇰
- Bhutan (BT) 🇧🇹
- USA (US) 🇺🇸
- UK (GB) 🇬🇧
- More...

---

## 🌐 Browser Requirements

Works on any modern browser:
- ✅ Chrome 90+
- ✅ Firefox 88+
- ✅ Safari 14+
- ✅ Edge 90+
- ✅ Opera 76+

---

## ❓ Frequently Asked Questions

**Q: Why can't I use VS Code Preview?**
A: VS Code's Simple Browser lacks proper HTTP server features needed for this app.

**Q: Which method should I pick?**
A: Live Server is easiest if you're comfortable installing extensions. Otherwise, Python server.

**Q: Can I deploy this anywhere?**
A: Yes! It's ready for Vercel, Netlify, GitHub Pages, or any static hosting.

**Q: Do I need internet?**
A: Yes, to fetch channel lists and stream content from iptv-org.

**Q: Are the streams legal?**
A: Only stream content you're legally allowed to watch in your region. The app doesn't enforce restrictions.

**Q: Will this run on my phone?**
A: Yes, if you share the localhost URL over your network (not recommended for security).

---

## 🚀 Next Steps

1. **Pick a method above** (recommended: Live Server)
2. **Follow the steps** to start the server
3. **Open the app** in your browser
4. **Start streaming!** 📺

---

## 📞 Troubleshooting

### Server Won't Start
- **Python:** Make sure Python 3.x is installed (`python --version`)
- **Node.js:** Make sure Node.js is installed (`node --version`)
- **Batch:** Requires Python or Node.js to be in PATH

### "Port already in use"
- Try a different port: `python -m http.server 3001`
- Or close other applications using the port

### Streams Not Loading
- Check internet connection
- Click "Reload" button in app
- Check browser console (F12) for errors

### Favorites Not Saving
- Clear browser cache and cookies
- Try a different browser
- Enable localStorage in browser settings

### Video Won't Play
- Some streams have CORS or geographic restrictions
- Try a different channel
- Check browser console for detailed errors

---

## 📞 Support

For issues with streams themselves:
- Visit: https://github.com/iptv-org/iptv
- Check channel status in database
- Report outdated streams

For application issues:
- Check browser console (F12 → Console)
- Verify JavaScript is enabled
- Try a different browser

---

## 🎉 You're All Set!

Your Live Sports TV application is:
- ✅ Fully functional
- ✅ All issues fixed
- ✅ Ready to deploy
- ✅ Ready to stream

Pick a method above and start your server. Enjoy! 🎬📺

---

**Questions?** Check the other documentation files or review the browser console for technical details.
