# HugaDroid

[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com)
[![Min API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org)

> Git-powered Hugo CMS for Android. Clone, edit, and publish your Hugo site from mobile.

**Languages**: English | [繁體中文](README.zh-TW.md)

---

## Overview

HugaDroid transforms your Android device into a complete Hugo content management system. Built on JGit and SSH, it provides direct Git integration without intermediary services.

**Core workflow**:
1. Clone your Hugo repository via SSH
2. Create and edit Markdown posts with live preview
3. Manage front matter (YAML/TOML), images, and files
4. Commit and push changes directly to GitHub

⚠️ **Important**: This app interacts directly with your Git repository. It performs file modifications, commits, and pushes without undo mechanisms. Always maintain backups and understand Git operations before use.

---

## Features

### Git Operations
- Full clone/pull/push workflow with SSH authentication
- Real-time progress tracking (percentage + task description)
- Automatic network retry (up to 3 attempts)
- Ahead/behind status detection

### Content Management
- Browse all posts from `content/` directory
- Create new posts (page bundle: `posts/slug/index.md`)
- Edit Markdown and front matter (YAML/TOML)
- Delete posts (entire bundle or single file)
- Filter by draft/published status
- Search by title, description, tags
- Multiple sort options (date, title, word count)
- Pin favorite posts

### Markdown Editor
- Three-tab interface: Content / Front Matter / Preview
- Auto-save with 500ms debounce
- AtomicFile writes (prevents corruption)
- Image compression and insertion (camera/gallery)
- CJK word count support
- Live Markdown preview with image path resolution

### Media Handling
- Page Bundle strategy: `slug/image.jpg` (recommended)
- Static Folder fallback: `/static/images/slug/` (for non-bundle posts)
- Auto-compression to 1920px, JPEG quality 85%
- Custom filename and alt text

### File Management
- Complete file browser (hides `.git`, system folders)
- Create folders and Markdown files
- Rename, delete, copy/paste operations
- Batch WebP conversion (preserves EXIF)
- Open non-Markdown files in system apps

### Sync & Settings
- Manual sync (commit + push)
- Background auto-sync (WorkManager, configurable interval)
- Author info configuration (name, email)
- Theme mode (System/Light/Dark)
- Multi-language UI (English, Traditional Chinese)
- Reset settings (clear repo, SSH keys, credentials)

---

## Getting Started

### Prerequisites
- Android 8.0+ (API 26)
- Hugo site repository on GitHub (or other Git hosting)
- Basic understanding of Git operations

### Installation

Download the latest APK from [Releases](https://github.com/Aaronfeng-law/HugaDroid/releases) and install on your Android device.

---

### Step 1: Generate SSH Deploy Key

1. Open HugaDroid → **Setup** screen
2. Tap **"Generate Ed25519 Key Pair"** button
3. The app creates:
   - Private key: `/data/data/com.soogoino.hugadroid/files/.ssh/id_ed25519`
   - Public key: `/data/data/com.soogoino.hugadroid/files/.ssh/id_ed25519.pub`
4. Your public key appears in a card below (format: `ssh-ed25519 AAAAC3...`)
5. Tap **"Copy to Clipboard"**

---

### Step 2: Add Deploy Key to GitHub

1. Go to your Hugo repository on GitHub
2. Navigate: **Settings** → **Deploy keys** → **Add deploy key**
3. Configure:
   - **Title**: Any descriptive name (e.g., "HugaDroid Mobile")
   - **Key**: Paste the copied public key
   - ✅ **Allow write access**: **MUST be checked** (required for push)
4. Click **"Add key"**

**Note**: For GitLab/Gitea, find similar "Deploy Keys" or "SSH Keys" settings.

---

### Step 3: Clone Repository

1. In HugaDroid **Setup** screen, fill:
   - **Repository URL**: 
     - SSH format: `git@github.com:user/repo.git`
     - HTTPS (auto-converted): `https://github.com/user/repo`
   - **Your Name**: Git commit author name
   - **Your Email**: Git commit author email
2. Tap **"Clone and Setup"**
3. Wait for progress indicators:
   - `Receiving objects...`
   - `Resolving deltas...`
4. On success, you're redirected to the Home screen

**Troubleshooting**:
- **Auth error**: Verify deploy key exists and has write access enabled
- **Network error**: App auto-retries 3 times; check connection
- **Partial clone**: If interrupted, restart app → Setup screen detects and prompts cleanup

---

### Step 4: Start Editing

- **Home**: Dashboard with stats, recent posts, drafts in progress
- **Posts**: Browse, search, create, and delete articles
- **Files**: Full repository file browser
- **Sync**: Manual pull/push with status indicators
- **Settings**: Configure author info, auto-sync, theme

---

## Hugo Project Requirements

### Mandatory Directory Structure

```
your-hugo-repo/
├── content/           ← REQUIRED (app scans this)
│   ├── posts/         ← Default section (can create others)
│   │   ├── my-post/   ← Page bundle (recommended)
│   │   │   └── index.md
│   │   └── single-file.md  ← Single file (uses static folder)
│   └── about/
├── static/            ← For static image strategy
│   └── images/
├── themes/            ← If using git submodule
│   └── your-theme/
├── config.toml        ← Or hugo.toml, config.yaml
└── .gitmodules        ← If theme is submodule
```

### Configuration Checklist

✅ **content/ folder exists**  
   App's `PostRepository.scanAndRefresh()` requires this. Without it, no posts will be detected.

✅ **Front matter format**  
   - YAML: Wrapped in `---`
   - TOML: Wrapped in `+++`
   - Required fields: `title`, `date`
   - Optional: `draft`, `tags`, `categories`, `description`, `slug`

✅ **Page Bundle strategy (recommended)**  
   - Post: `content/posts/my-post/index.md`
   - Images: `content/posts/my-post/image.jpg`
   - Reference: `![alt](image.jpg)`

⚠️ **Single file fallback**  
   - Post: `content/posts/my-post.md`
   - Images auto-saved to: `/static/images/my-post/`
   - Reference: `![alt](/images/my-post/image.jpg)`

⚠️ **Git Submodule Limitation**  
   Current implementation **does not auto-initialize submodules**. JGit's `CloneRepository` lacks `.setCloneSubmodules(true)`.
   
   **Workaround**: Before cloning in app, ensure theme is committed:
   ```bash
   git submodule add <theme-url> themes/<name>
   git commit -m "Add theme submodule"
   git push
   ```
   Or use Hugo Modules instead of submodules.

✅ **Author configuration**  
   Set in app's Settings screen. Required for all commits. App validates before allowing sync.

---

## Configuration Reference

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `repoUrl` | String | - | SSH repository URL |
| `authorName` | String | - | Git commit author name (required) |
| `authorEmail` | String | - | Git commit author email (required) |
| `mediaStrategy` | Enum | `PAGE_BUNDLE` | Image storage: Page Bundle / Static Folder |
| `autoSyncEnabled` | Boolean | `true` | Enable background sync |
| `autoSyncInterval` | Int | `30` | Auto-sync interval (minutes) |
| `themeMode` | Enum | `SYSTEM` | UI theme: System / Light / Dark |
| `appLanguage` | String | (system) | UI language: `en` / `zh-TW` |

**Storage Locations**:
- Settings: DataStore Preferences (`hugadroid_prefs`)
- SSH keys: `/data/data/com.soogoino.hugadroid/files/.ssh/`
- Local repo: `/data/data/com.soogoino.hugadroid/files/repo/`
- Database: Room SQLite (`hugadroid_db`)

---

## Tech Stack

### Core Framework
- **Platform**: Android 8.0+ (minSdk 26, targetSdk 35)
- **Language**: Kotlin 2.1.0
- **Build**: Gradle 8.8.0, JVM 17

### UI Layer
| Library | Version | Purpose |
|---------|---------|---------|
| Jetpack Compose | 2025.02.00 (BOM) | Declarative UI framework |
| Material 3 | (BOM) | Material Design components |
| Material Icons Extended | (BOM) | Icon library |
| Navigation Compose | 2.8.8 | Screen navigation |
| RichEditor Compose | 1.0.0-rc13 | Markdown editor & preview |

### Dependency Injection
- Hilt 2.55 + Navigation/Work integration

### Data Layer
| Library | Version | Purpose |
|---------|---------|---------|
| Room | 2.7.0 | Local database (post cache) |
| DataStore Preferences | 1.1.3 | Key-value settings storage |

### Git & SSH
| Library | Version | Purpose |
|---------|---------|---------|
| JGit | 7.5.0 | Git protocol implementation |
| JSch (mwiede) | 0.2.21 | SSH transport layer |
| BouncyCastle | 1.80 | Ed25519 key generation |

### Parsing & Serialization
- SnakeYAML Engine 2.9 (YAML front matter)
- ktoml-core 0.7.1 (TOML front matter)
- Kotlin Serialization 2.1.0 (JSON)

### Media & Networking
- Coil 3 (3.1.0) with OkHttp integration (image loading)

### Background Tasks
- WorkManager 2.10.0 (auto-sync scheduling)

**Full dependency list**: See [build.gradle.kts](app/build.gradle.kts)

---

## Security & Reliability

- **SSH Keys**: Ed25519, stored unencrypted in app-private storage
- **Atomic Writes**: Uses `AtomicFile` to prevent file corruption
- **Thread Safety**: All file I/O serialized with Mutex
- **Network Resilience**: Auto-retry with exponential backoff (3 attempts)
- **Error Classification**: Distinct handling for network vs auth errors

---

## Limitations & Known Issues

1. **No Git Submodule Support**: Clone does not initialize submodules. Workaround: manually add submodule before cloning in app.
2. **SSH Only**: Personal Access Token (PAT) support removed. Deploy keys required.
3. **No Conflict Resolution**: Merge conflicts not handled in UI. Use desktop Git client to resolve.
4. **Android Scoped Storage**: Large repos (>500MB) may face performance issues.

---

## Building from Source

```bash
git clone https://github.com/Aaronfeng-law/HugaDroid.git
cd HugaDroid
./gradlew assembleDebug
```

For release builds, configure signing in `local.properties`:
```properties
KEYSTORE_PATH=/path/to/release.jks
KEYSTORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

Then run:
```bash
./gradlew assembleRelease
```

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

Built with:
- [JGit](https://github.com/eclipse-jgit/jgit) by Eclipse Foundation
- [JSch](https://github.com/mwiede/jsch) fork by mwiede
- [RichEditor Compose](https://github.com/MohamedRejeb/Compose-Rich-Editor) by MohamedRejeb
- [Coil](https://coil-kt.github.io/coil/) image loading library

Special thanks to the Hugo community and all open-source contributors.
