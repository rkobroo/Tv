# 🎬 Live Sports TV - Quick Start Guide

## Problem: Web Preview Not Working

VS Code's Simple Browser has limitations with this application. **This is normal and expected.**

The application requires a proper local HTTP server to run. Here's how to fix it:

---

## ✅ SOLUTION - Choose One Method Below

### **🌟 METHOD 1: VS Code Live Server (EASIEST)**

**Best for development - auto-refreshes on changes**

1. **Install Extension:**
   - Press `Ctrl+Shift+X` to open Extensions
   - Search: `Live Server`
   - Install the one by "Ritwick Dey"

2. **Run the Application:**
   - Right-click on `index.html` in the file explorer
   - Select **"Open with Live Server"**
   - Browser opens automatically at `http://127.0.0.1:5500`

✨ **Done!** The application is now running.

---

### **🐍 METHOD 2: Python HTTP Server**

**Requires: Python 3.x installed**

1. Open Terminal in VS Code (Ctrl + `)
2. Make sure you're in the project directory:
   ```bash
   cd path/to/Tv
   ```
3. Start the server:
   ```bash
   python -m http.server 3000
   ```
4. Open browser: **http://localhost:3000**

✨ **Done!** Press Ctrl+C to stop the server.

---

### **📦 METHOD 3: Node.js Server**

**Requires: Node.js installed**

1. Open Terminal in VS Code (Ctrl + `)
2. Make sure you're in the project directory
3. Start the server:
   ```bash
   node server.js
   ```
4. Open browser: **http://localhost:3000**

✨ **Done!** Press Ctrl+C to stop the server.

---

### **🪟 METHOD 4: Windows Batch File (Easiest for Windows)**

**Requires: Python or Node.js installed**

1. Navigate to the `Tv` folder
2. Double-click: **`start-server.bat`**
3. A terminal opens and the server starts
4. Open browser: **http://localhost:3000**

✨ **Done!** Close the terminal to stop.

---

### **⚙️ METHOD 5: PowerShell HTTP Server**

**Windows only - no other requirements**

1. Open PowerShell in the project directory
2. Run this command:
   ```powershell
   Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process; .\http-server.ps1
   ```
3. Server starts on **http://localhost:8000**
4. Open in browser

✨ **Done!** Press Ctrl+C to stop.

---

## 📋 Quick Checklist

- [ ] Choose a method above (Method 1 is recommended)
- [ ] Follow the steps exactly
- [ ] Open the URL in your browser
- [ ] Start streaming!

---

## 🎯 What You'll Get

Once running, you'll have access to:

✅ **Live HLS streaming** with Video.js  
✅ **Search & filter** channels by name  
✅ **Regional sorting** (Nepal first!)  
✅ **Favorites** management  
✅ **Error recovery** with auto-retry  
✅ **Dark theme UI**  

---

## 🔧 Technical Details

### Why VS Code Preview Doesn't Work

VS Code's built-in Simple Browser has limitations:
- ❌ No proper CORS handling
- ❌ Limited localStorage access
- ❌ Restricted external API calls
- ❌ No HTTP headers support

A proper HTTP server solves all these issues.

### What's Fixed in the App

✅ **Save Failed Error** - localStorage error handling  
✅ **Duplicate Config** - Removed redundant properties  
✅ **Variable Shadowing** - Fixed scope conflicts  
✅ **Missing Returns** - Added proper control flow  

### System Requirements

- Modern browser (Chrome, Firefox, Safari, Edge)
- Internet connection (to fetch streams)
- One of: Python, Node.js, or Windows

---

## 📞 Troubleshooting

**"Port already in use"**
- Change port: `python -m http.server 3001`
- Or close other servers running on that port

**"Streams not loading"**
- Check internet connection
- Try clicking "Reload" button
- Check browser console (F12) for errors

**"Favorites not saving"**
- Clear browser cache
- Try different browser
- Check localStorage is enabled

**"Video won't play"**
- Some streams have CORS restrictions
- Try different channel
- Check browser console for detailed error

---

## 🎬 Ready to Start?

Pick a method above and follow the steps. Within 1-2 minutes, you'll be streaming live TV! 🚀

Recommended: **Method 1 (Live Server)** - Easiest and best for development!
