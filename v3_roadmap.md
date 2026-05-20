# AutoSort V3.0 Roadmap — AI-Powered Premium Features

This document outlines the premium AI features planned for AutoSort V3.0. These features require cloud AI processing and will be monetized as a paid subscription tier ("AutoSort Intelligence").

---

## 1. AI Folder Insights — Deep Content Analysis (Core Premium Feature)

### The Problem
Users sort hundreds of files into destination folders like "Finance", "Work", "Personal", etc. Over time, these folders grow massive and the user has no idea what's actually in them — they forget which invoices they saved, what amounts they paid, and which files are duplicates or junk.

### The Solution
AutoSort Intelligence will **read the actual content** of files inside a destination folder using an AI model and provide **deep, actionable insights** — not just file counts, but real data extracted from the documents themselves.

### How It Works
1. **User Action**: User taps "Analyze with AI" on a destination folder.
2. **Automated Privacy Scanner (Pre-scan)**: Before sending anything to the cloud, the app runs a fast, local scan of all file names in the folder. It looks for sensitive keywords (e.g., "tax", "medical", "bank", "password", "invoice").
3. **Exclusion Prompt**: If sensitive files are detected, the app pauses and shows a prompt: *"We found 3 potentially sensitive files (e.g., 'Tax_Return_2025.pdf'). Do you want to exclude them from the AI analysis?"* The user can easily check/uncheck these specific files.
4. **Selective Content Extraction**: The app extracts metadata for the cleared files. For specifically targeted documents (PDFs, images) that were not excluded, it uses on-device parsers to extract the text content.
5. **AI Processing**: The data is bundled and sent to the AI model. The AI categorizes the folder generally based on file names, but digs deep into the provided text content for "Special Insights" (e.g., spending anomalies).
6. **Rich AI Response Card**: The app displays a beautifully formatted insights screen, clearly separating the general file summary from the deep insights:

#### Example Output — Finance Folder:
```
📊 Finance Folder — AI Insights
Period: January — May 2026
Total Invoices: 15
Total Amount Spent: ₹47,320

┌──────────────────┬───────┬──────────┐
│ Vendor           │ Count │ Total    │
├──────────────────┼───────┼──────────┤
│ Amazon           │ 6     │ ₹12,400  │
│ Swiggy           │ 4     │ ₹3,200   │
│ Airtel           │ 3     │ ₹2,700   │
│ Electricity Board│ 2     │ ₹29,020  │
└──────────────────┴───────┴──────────┘

⚠️ Duplicate: Airtel_Bill_March.pdf and Airtel_Bill_March(1).pdf
💡 Anomaly: Electricity jumped 340% in April (₹18,500 vs ₹5,260)
```

#### Example Output — Work Folder:
```
📁 Work Folder — AI Insights
Total Documents: 8
- 3 project proposals (Project Alpha, Beta, Gamma)
- 2 meeting notes (Q1 Review, Q2 Planning)
- 2 client contracts (signed)
- 1 junk file (.tmp, safe to delete)

💡 Suggestion: Project Gamma proposal is a draft (v0.2).
   A newer version may exist in your Downloads.
```

### Context-Aware Analysis
The AI prompt dynamically adapts based on the folder name:
- **"Finance" / "Bills" / "Invoices"** → Extract amounts, vendors, dates; show spending summary.
- **"Work" / "Projects"** → Identify document types, project names, drafts vs finals.
- **"Medical" / "Health"** → Summarize report types, dates, doctor/hospital names.
- **"Education" / "Study"** → List subjects, assignment names, identify incomplete work.
- **Generic folders** → General file categorization, duplicate detection, junk identification.

### Supported File Types for Deep Analysis
- **PDFs**: Full text extraction → invoice parsing, amount detection, vendor identification.
- **Images (JPG/PNG)**: AI Vision → read receipts, screenshots of transactions, photos of bills.
- **Documents (DOCX/TXT)**: Full text extraction and contextual summarization.
- **Spreadsheets (XLSX/CSV)**: Column header detection and data summary.
- **Other Files**: Fallback to file name, size, and date pattern analysis only.

### Insights History & Offline Access
- Every AI analysis result is cached locally in the `ai_insights` database table.
- Users can browse past insights offline without spending another AI credit.
- Each cached insight shows: *"Last analyzed: 3 days ago. Tap to refresh."*
- Old insights are never auto-deleted — user controls their history.

### Share Insights
- A **"Share as Image"** button at the bottom of every Insights card.
- Captures the summary as a clean PNG and opens the Android Share Sheet.
- Every shared image includes a subtle "Powered by AutoSort ✨" watermark at the bottom — **free marketing** with every share.

---

## 2. Smart Categorization Suggestions (On-Device — NO AI Credits Used)

### The Problem
New users don't know what rules to create. They download the app but stare at the empty Dashboard with no idea where to start.

### The Solution
When a new user opens AutoSort for the first time, the app scans their existing Downloads folder and **suggests rules automatically** using simple on-device pattern matching.

### How It Works (On-Device, Zero Cost)
1. **First Launch Scan**: On first app launch (after permission is granted), the app silently reads all file names in the Downloads folder.
2. **Pattern Matching Engine** (No AI needed — runs locally):
   - Files ending in `.pdf` with "invoice", "bill", "receipt" in name → Suggest **"Finance"** rule
   - Files ending in `.jpg`, `.png`, `.webp` → Suggest **"Images"** rule
   - Files with "assignment", "notes", "chapter" → Suggest **"Study"** rule
   - Files ending in `.apk` → Suggest **"Apps"** rule
   - Files ending in `.zip`, `.rar` → Suggest **"Archives"** rule
3. **Suggestion Cards**: The app displays beautiful suggestion cards on the Dashboard:
   - "📄 We noticed 12 PDF invoices. Create a rule to auto-sort them to a 'Finance' folder?"
   - "🖼️ We found 45 images. Create a rule to move them to 'Images'?"
4. **One-Tap Setup**: User taps "Create Rule" and the rule is instantly created with zero typing required.

> **Why no AI?** Sending file names to a cloud API just to match `.pdf` → "Finance" wastes money. Pattern matching does this instantly, for free, offline.

---

## 3. Monthly Intelligence Report (Hybrid: DB Queries + Minimal AI)

### The Problem
Users don't know how productive AutoSort has been for them. They forget the app is even running.

### The Solution
Once a month, AutoSort generates a beautiful report summarizing everything it did, delivered as a push notification.

### How It Works (90% On-Device, 10% AI)

**On-Device (Free — Database Queries):**
- Total files moved → `SELECT COUNT(*) FROM logs WHERE time > 30_days_ago`
- Most active rule → `GROUP BY rule_name ORDER BY count DESC LIMIT 1`
- Files per destination → `GROUP BY target`
- Time saved estimate → `file_count × 2 minutes`
- Success rate → `success_count / total_count × 100`

**AI-Powered (Uses 1 Credit):**
- Smart suggestion only: *"You downloaded 8 .zip files that weren't matched by any rule. Consider adding a rule for archives."*
- This is the only part that genuinely benefits from AI pattern recognition.

### Notification
Delivered as a rich Android notification with a "View Full Report" button.

---

## 4. Monetization Strategy for AI Features

### Why Subscription (Not One-Time Purchase)
AI API calls cost real money per request. Unlike V2 features which run entirely on-device, every AI analysis requires a server call that costs approximately $0.01-0.05 per request. A subscription model ensures sustainable revenue to cover these costs.

### Pricing Tiers
| Tier | Price | What You Get |
|------|-------|--------------|
| **Free** | $0 | Core AutoSort (3 rules, local + Drive sorting, logs) |
| **Pro** | $4.99 one-time | Unlimited rules, multi-account, CSV backups, cleanup assistant |
| **Intelligence** | $1.99/month | Everything in Pro + AI Folder Insights (10 scans/month), Monthly Reports |

### Credit Transparency
- The "AI Insights" button on each rule card shows remaining credits: **"✨ AI Insights (7/10 remaining)"**
- When credits hit 0: *"You've used all 10 scans this month. Resets on June 1st."*
- No surprise blocks. User always knows where they stand.

### AI Provider — Recommended Approach
Start with **Google Gemini API** because:
1. We already have Google OAuth integrated in the app.
2. Google offers a generous free tier (enough for testing and early users).
3. Future-proof: Google is pushing Gemini Nano for on-device AI, which could eliminate server costs entirely for supported devices.

---

## 5. Privacy Considerations & Cloud AI Approach
- **Cloud AI Decision**: To avoid bloating the user's phone storage with gigabytes of local AI models, AutoSort will use a Cloud-based AI service (e.g., Google Gemini API).
- **Mandatory Consent Dialog**: Before any analysis occurs, users must explicitly opt-in. The app will show a clear warning: *"We are using a secure third-party AI service (Google/OpenAI) to analyze your documents. Your file contents will be sent securely to this model for analysis and not stored by us. Do you consent?"*
- **Future Commitment**: The consent screen will also note: *"We are actively working on future updates to minimize third-party dependencies and bring this analysis entirely on-device when technology allows."*
- **Data Minimization**: Never send full file contents if it's not needed. Only send file names, sizes, dates, and the first 500 characters of text documents.

---

## 6. Seamless UI Integration (Respecting Current App Design)

### The Goal
To avoid breaking the current stable V1 codebase, we will **NOT** rewrite the app to use a Bottom Navigation Bar. Instead, we will seamlessly integrate the new V2 and V3 features directly into the existing `NavGraph` and `Scaffold` layouts.

### 1. Dashboard Screen (`DashboardScreen.kt`)
The current Dashboard remains the main hub.
- **AI Folder Insights Trigger**: We simply add a shiny ✨ **"Analyze Folder"** button directly to the bottom of the **existing Rule Card**. No new screens needed. When tapped, the AI Privacy Bottom Sheet slides up over the current Dashboard.
- **Smart Suggestions**: These will appear as temporary, dismissible cards at the very top of the Dashboard list (above the user's active rules).
- **Existing Elements**: The FAB (Add Rule), Logs button, Settings icon, and Power menu all stay exactly where they are.

### 2. Logs Screen (`LogsScreen.kt`)
The current Logs screen will be upgraded without changing its routing.
- **V2 Audit Details**: The existing `LogCard` simply gets an `expanded` state. When tapped, it drops down to show the Source Folder and Rule Name.
- **AI Insights History**: We add a simple toggle at the top of the screen (like a `TabRow`): 
  - [ File Moves ] (Current logs)
  - [ AI Insights ] (Shows past AI analyses)
  - *This reuses the exact same screen layout.*

### 3. Settings Screen (`SettingsScreen.kt`)
The current Settings screen simply gets new rows added to the existing `LazyColumn`.
- **Subscription Tier**: A new card at the top showing "Current Tier: Free" with an "Upgrade" button.
- **Multi-Account**: A new row under the Google Drive section to manage accounts.
- **Cloud Backup**: A new row to trigger the CSV backup manually.

### 4. Rule Creation (`AddRuleScreen.kt`)
- **Cloud Retention**: We simply add the `[ ] Delete original file after uploading` Checkbox below the destination picker, and only set it to `visible = true` if the Drive destination is selected.

### Why this is better for development:
- **Zero Routing Changes**: We don't touch `NavGraph.kt`.
- **Zero Broken Code**: We don't have to rip out the current `Scaffold`.
- **Faster Development**: We are just adding new components (Bottom Sheets, Buttons) to existing screens.

---

## 7. Technical Architecture

### New Components Required
- `AiAnalysisService.kt` — Handles communication with the Gemini/OpenAI API.
- `FolderInsightsScreen.kt` — New UI screen displaying the AI summary cards.
- `InsightsHistoryScreen.kt` — Scrollable list of cached past analyses.
- `SuggestionEngine.kt` — On-device pattern matching for rule suggestions (no AI).
- `MonthlyReportWorker.kt` — WorkManager task for generating monthly reports.
- `SubscriptionManager.kt` — Google Play Billing integration for Pro/Intelligence tiers.
- `InsightShareHelper.kt` — Captures insight cards as PNG for sharing.
- `BottomNavigation.kt` — New bottom navigation bar component.

### Database Changes
- New `ai_insights` table to cache AI responses (avoid re-analyzing the same folder repeatedly).
- New `subscription_status` field in AppConfig to track the user's current tier.
- New `ai_credits_used` counter in AppConfig, reset monthly.
