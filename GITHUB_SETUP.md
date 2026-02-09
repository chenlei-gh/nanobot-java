# Nanobot Java - GitHub Repository Setup

## 🚀 Quick Setup (Run these commands)

### 1. Install GitHub CLI
**Windows:**
```powershell
winget install --id GitHub.cli
```

**Or download from:** https://cli.github.com

### 2. Authenticate
```bash
gh auth login
```
Follow the prompts to authenticate with your GitHub account.

### 3. Create Repository & Push
```bash
# Navigate to your project
cd F:\nanobot

# Create repository (replace "your-username" with your GitHub username)
gh repo create nanobot-java --public --description "High-Performance AI Agent - Java 21 Implementation" --source=. --push
```

### 4. Enable Codespaces
```bash
# Enable Codespaces for the repository
gh codespace create --repo your-username/nanobot-java
```

Or via GitHub Web:
1. Go to https://github.com/your-username/nanobot-java
2. Settings → Codespaces → Enable

---

## 📋 Manual Setup (If CLI fails)

### 1. Create Repository on GitHub Web

**Option A: Direct Link**
```
https://github.com/new?name=nanobot-java&description=High-Performance+AI+Agent+-+Java+21+Implementation&public=true
```

**Option B: Manual Steps**
1. Go to: https://github.com/new
2. Repository name: `nanobot-java`
3. Description: `High-Performance AI Agent - Java 21 Implementation`
4. Public: ✅
5. Don't initialize with README
6. Click **"Create repository"**

### 2. Push Local Code

```bash
cd F:\nanobot

# Initialize git (if not done)
git init
git add .
git commit -m "Initial commit: Nanobot Java implementation"

# Push to GitHub (replace USERNAME with your username)
git remote add origin https://github.com/USERNAME/nanobot-java.git
git branch -M main
git push -u origin main
```

---

## 🔗 Useful Links After Creation

| Action | Link |
|--------|------|
| **Repository** | https://github.com/USERNAME/nanobot-java |
| **Codespaces** | https://github.com/codespaces/new?repo=USERNAME/nanobot-java |
| **Settings** | https://github.com/USERNAME/nanobot-java/settings |
| **Actions** | https://github.com/USERNAME/nanobot-java/actions |

---

## 🏷️ Recommended Settings

After creating the repository, enable these features:

### 1. Enable Codespaces
- **URL**: https://github.com/USERNAME/nanobot-java/settings/codespaces
- **Status**: ✅ Enable

### 2. Enable Actions (for CI/CD)
- **URL**: https://github.com/USERNAME/nanobot-java/settings/actions
- **Status**: ✅ Enable all actions

### 3. Add Topics (Optional)
Go to repository **About** section → Topics:
```
java-21 nanobot ai-agent virtual-threads mcp spring-boot
```

### 4. Add Description
```
High-Performance AI Agent built with Java 21 + Virtual Threads. 
Supports OpenAI, Claude, DeepSeek, Qwen, Gemini. 
Features: Tools, Channels, Cron, Skills, Events.
```

---

## 📝 Repository Settings Checklist

- [ ] Repository created
- [ ] Code pushed
- [ ] Codespaces enabled
- [ ] GitHub Actions enabled
- [ ] README updated with real repo URL
- [ ] Topics added
- [ ] Description added

---

## 🔗 Quick Actions

**Create Issues:**
```
https://github.com/USERNAME/nanobot-java/issues/new
```

**View Actions:**
```
https://github.com/USERNAME/nanobot-java/actions
```

**Create Pull Request:**
```
https://github.com/USERNAME/nanobot-java/compare/main
```

---

## 📞 Need Help?

- **GitHub Docs**: https://docs.github.com
- **CLI Docs**: https://cli.github.com/manual
- **Codespaces Docs**: https://docs.github.com/en/codespaces

---

## 🎉 After Setup

1. **Star your repository** ⭐
2. **Enable Codespaces** for instant development
3. **Share** with others!
