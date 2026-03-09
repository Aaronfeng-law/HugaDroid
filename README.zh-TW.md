# HugaDroid

[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com)
[![Min API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org)

> 基於 Git 的 Hugo 行動內容管理系統。在 Android 裝置上 clone、編輯與發布你的 Hugo 站點。

**語言版本**: [English](README.md) | 繁體中文

---

## 概述

HugaDroid 將你的 Android 裝置轉變為完整的 Hugo 內容管理系統。基於 JGit 與 SSH 協定，提供無需中介服務的直接 Git 整合。

**核心工作流程**：
1. 透過 SSH 克隆你的 Hugo 儲存庫
2. 建立與編輯 Markdown 文章，即時預覽
3. 管理 front matter（YAML/TOML）、圖片與檔案
4. 直接 commit 與 push 變更至 GitHub

⚠️ **重要提醒**：本應用程式直接與你的 Git 儲存庫互動。它會執行檔案修改、commit 與 push 操作，且無復原機制。使用前請務必維護備份，並確保理解 Git 操作原理。

---

## 功能特點

### Git 操作
- 完整的 clone/pull/push 工作流程，支援 SSH 認證
- 即時進度追蹤（百分比與任務描述）
- 網路錯誤自動重試（最多 3 次）
- ahead/behind 狀態偵測

### 內容管理
- 瀏覽 `content/` 目錄下所有文章
- 建立新文章（page bundle 格式：`posts/slug/index.md`）
- 編輯 Markdown 與 front matter（YAML/TOML）
- 刪除文章（整個 bundle 或單一檔案）
- 依草稿/已發布狀態過濾
- 搜尋標題、描述、標籤
- 多種排序選項（日期、標題、字數）
- 釘選常用文章

### Markdown 編輯器
- 三分頁介面：內容 / Front Matter / 預覽
- 自動儲存（500ms 防抖）
- AtomicFile 寫入（防止檔案毀損）
- 圖片壓縮與插入（相機/圖庫）
- 支援 CJK 字數統計
- 即時 Markdown 預覽，含圖片路徑解析

### 媒體處理
- Page Bundle 策略：`slug/image.jpg`（推薦）
- Static Folder 備援：`/static/images/slug/`（非 bundle 文章）
- 自動壓縮至 1920px，JPEG 品質 85%
- 自訂檔名與替代文字

### 檔案管理
- 完整檔案瀏覽器（隱藏 `.git` 等系統資料夾）
- 建立資料夾與 Markdown 檔案
- 重新命名、刪除、複製/貼上操作
- 批次 WebP 轉換（保留 EXIF）
- 在系統應用中開啟非 Markdown 檔案

### 同步與設定
- 手動同步（commit + push）
- 背景自動同步（WorkManager，可調整間隔）
- 作者資訊設定（姓名、Email）
- 主題模式（跟隨系統/淺色/深色）
- 多語言介面（英文、繁體中文）
- 重置設定（清除 repo、SSH 金鑰、憑證）

---

## 快速開始

### 環境需求
- Android 8.0 以上（API 26）
- 託管於 GitHub（或其他 Git 平台）的 Hugo 站點儲存庫
- 基本 Git 操作知識

### 安裝

前往 [Releases](https://github.com/Aaronfeng-law/HugaDroid/releases) 下載最新版 APK，安裝至你的 Android 裝置。

---

### 步驟一：產生 SSH Deploy Key

1. 開啟 HugaDroid → **Setup** 畫面
2. 點擊 **"Generate Ed25519 Key Pair"** 按鈕
3. 應用程式建立：
   - 私鑰：`/data/data/com.soogoino.hugadroid/files/.ssh/id_ed25519`
   - 公鑰：`/data/data/com.soogoino.hugadroid/files/.ssh/id_ed25519.pub`
4. 公鑰顯示於下方卡片中（格式：`ssh-ed25519 AAAAC3...`）
5. 點擊 **"Copy to Clipboard"**

---

### 步驟二：在 GitHub 新增 Deploy Key

1. 前往你的 Hugo 儲存庫頁面
2. 導航至：**Settings** → **Deploy keys** → **Add deploy key**
3. 設定：
   - **Title**：任意描述性名稱（例如 "HugaDroid Mobile"）
   - **Key**：貼上已複製的公鑰
   - ✅ **Allow write access**：**務必勾選**（push 操作必須）
4. 點擊 **"Add key"**

**注意**：GitLab/Gitea 平台請尋找類似的「Deploy Keys」或「SSH Keys」設定。

---

### 步驟三：Clone 儲存庫

1. 在 HugaDroid **Setup** 畫面填入：
   - **Repository URL**： 
     - SSH 格式：`git@github.com:user/repo.git`
     - HTTPS（自動轉換）：`https://github.com/user/repo`
   - **Your Name**：Git commit 作者名稱
   - **Your Email**：Git commit 作者 Email
2. 點擊 **"Clone and Setup"**
3. 等候進度指示器：
   - `Receiving objects...`
   - `Resolving deltas...`
4. 成功後將自動跳轉至 Home 畫面

**疑難排解**：
- **認證錯誤**：確認 deploy key 已正確新增且勾選 write access
- **網路錯誤**：應用程式會自動重試 3 次；請檢查網路連線
- **部分 clone**：若中斷，重啟應用程式 → Setup 畫面會偵測並提示清理

---

### 步驟四：開始編輯

- **Home**：儀錶板顯示統計資訊、最近文章、進行中的草稿
- **Posts**：瀏覽、搜尋、建立與刪除文章
- **Files**：完整儲存庫檔案瀏覽器
- **Sync**：手動 pull/push，含狀態指示器
- **Settings**：設定作者資訊、自動同步、主題

---

## Hugo 專案要求

### 必須的目錄結構

```
your-hugo-repo/
├── content/           ← 必須存在（應用程式掃描此目錄）
│   ├── posts/         ← 預設 section（可建立其他 section）
│   │   ├── my-post/   ← Page bundle（推薦）
│   │   │   └── index.md
│   │   └── single-file.md  ← 單一檔案（使用 static folder）
│   └── about/
├── static/            ← 用於 static image 策略
│   └── images/
├── themes/            ← 若使用 git submodule
│   └── your-theme/
├── config.toml        ← 或 hugo.toml、config.yaml
└── .gitmodules        ← 若 theme 為 submodule
```

### 設定檢查清單

✅ **content/ 資料夾存在**  
   應用程式的 `PostRepository.scanAndRefresh()` 需要此目錄。若不存在，無法偵測任何文章。

✅ **Front matter 格式**  
   - YAML：使用 `---` 包裹
   - TOML：使用 `+++` 包裹
   - 必要欄位：`title`、`date`
   - 可選欄位：`draft`、`tags`、`categories`、`description`、`slug`

✅ **Page Bundle 策略（推薦）**  
   - 文章：`content/posts/my-post/index.md`
   - 圖片：`content/posts/my-post/image.jpg`
   - 引用：`![alt](image.jpg)`

⚠️ **單一檔案備援**  
   - 文章：`content/posts/my-post.md`
   - 圖片自動儲存至：`/static/images/my-post/`
   - 引用：`![alt](/images/my-post/image.jpg)`

⚠️ **Git Submodule 限制**  
   目前實作**不會自動初始化 submodules**。JGit 的 `CloneRepository` 缺少 `.setCloneSubmodules(true)` 設定。
   
   **解決方案**：在應用程式 clone 之前，確保 theme 已 commit：
   ```bash
   git submodule add <theme-url> themes/<name>
   git commit -m "Add theme submodule"
   git push
   ```
   或改用 Hugo Modules 取代 submodules。

✅ **作者設定**  
   在應用程式的 Settings 畫面設定。所有 commit 皆需此資訊。應用程式會在同步前驗證。

---

## 設定參考

| 設定項目 | 類型 | 預設值 | 說明 |
|---------|------|--------|------|
| `repoUrl` | String | - | SSH 儲存庫 URL |
| `authorName` | String | - | Git commit 作者名稱（必填） |
| `authorEmail` | String | - | Git commit 作者 Email（必填） |
| `mediaStrategy` | Enum | `PAGE_BUNDLE` | 圖片儲存策略：Page Bundle / Static Folder |
| `autoSyncEnabled` | Boolean | `true` | 啟用背景同步 |
| `autoSyncInterval` | Int | `30` | 自動同步間隔（分鐘） |
| `themeMode` | Enum | `SYSTEM` | UI 主題：System / Light / Dark |
| `appLanguage` | String | (跟隨系統) | UI 語言：`en` / `zh-TW` |

**儲存位置**：
- 設定：DataStore Preferences (`hugadroid_prefs`)
- SSH 金鑰：`/data/data/com.soogoino.hugadroid/files/.ssh/`
- 本地 repo：`/data/data/com.soogoino.hugadroid/files/repo/`
- 資料庫：Room SQLite (`hugadroid_db`)

---

## 技術堆疊

### 核心框架
- **平台**：Android 8.0 以上（minSdk 26, targetSdk 35）
- **語言**：Kotlin 2.1.0
- **建置**：Gradle 8.8.0, JVM 17

### UI 層
| 函式庫 | 版本 | 用途 |
|--------|------|------|
| Jetpack Compose | 2025.02.00 (BOM) | 宣告式 UI 框架 |
| Material 3 | (BOM) | Material Design 元件 |
| Material Icons Extended | (BOM) | 圖示庫 |
| Navigation Compose | 2.8.8 | 畫面導航 |
| RichEditor Compose | 1.0.0-rc13 | Markdown 編輯器與預覽 |

### 依賴注入
- Hilt 2.55 + Navigation/Work 整合

### 資料層
| 函式庫 | 版本 | 用途 |
|--------|------|------|
| Room | 2.7.0 | 本地資料庫（文章快取） |
| DataStore Preferences | 1.1.3 | 鍵值對設定儲存 |

### Git 與 SSH
| 函式庫 | 版本 | 用途 |
|--------|------|------|
| JGit | 7.5.0 | Git 協定實作 |
| JSch (mwiede) | 0.2.21 | SSH 傳輸層 |
| BouncyCastle | 1.80 | Ed25519 金鑰產生 |

### 解析與序列化
- SnakeYAML Engine 2.9（YAML front matter）
- ktoml-core 0.7.1（TOML front matter）
- Kotlin Serialization 2.1.0（JSON）

### 媒體與網路
- Coil 3（3.1.0）含 OkHttp 整合（圖片載入）

### 背景任務
- WorkManager 2.10.0（自動同步排程）

**完整依賴列表**：參見 [build.gradle.kts](app/build.gradle.kts)

---

## 安全性與可靠性

- **SSH 金鑰**：Ed25519，未加密儲存於應用程式私有空間
- **原子寫入**：使用 `AtomicFile` 防止檔案損毀
- **執行緒安全**：所有檔案 I/O 以 Mutex 序列化
- **網路韌性**：指數退避的自動重試（3 次嘗試）
- **錯誤分類**：網路錯誤與認證錯誤分別處理

---

## 限制與已知問題

1. **無 Git Submodule 支援**：Clone 不會初始化 submodules。解決方案：在應用程式 clone 前手動加入 submodule。
2. **僅支援 SSH**：已移除 Personal Access Token (PAT) 支援。必須使用 Deploy keys。
3. **無衝突解決介面**：Merge conflicts 不在 UI 中處理。請使用桌面 Git 客戶端解決。
4. **Android 分區儲存**：大型 repo（>500MB）可能面臨效能問題。

---

## 從原始碼建置

```bash
git clone https://github.com/Aaronfeng-law/HugaDroid.git
cd HugaDroid
./gradlew assembleDebug
```

若要建置 release 版本，請在 `local.properties` 設定簽章：
```properties
KEYSTORE_PATH=/path/to/release.jks
KEYSTORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

接著執行：
```bash
./gradlew assembleRelease
```

---

## 授權條款

本專案採用 MIT License 授權 - 詳見 [LICENSE](LICENSE) 檔案。

---

## 致謝

本專案使用以下開源專案：
- Eclipse Foundation 的 [JGit](https://github.com/eclipse-jgit/jgit)
- mwiede 的 [JSch](https://github.com/mwiede/jsch) fork
- MohamedRejeb 的 [RichEditor Compose](https://github.com/MohamedRejeb/Compose-Rich-Editor)
- [Coil](https://coil-kt.github.io/coil/) 圖片載入函式庫

特別感謝 Hugo 社群與所有開源貢獻者。
