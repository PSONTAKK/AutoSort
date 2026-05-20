# AutoSort V2.0 Roadmap

This document outlines the major feature upgrades planned for the next major release of AutoSort, as discussed with the development team.

## 1. Multiple Google Accounts Support
Currently, AutoSort authenticates a single, global Google Account via `GoogleAuthManager`. The V2.0 upgrade will allow users to:
- Sign into multiple Google Accounts simultaneously (e.g., Personal and Work).
- Select a specific target account (dropdown picker) when creating a new rule with the `CLOUD_GDRIVE` destination.
- `GoogleDriveDestination` will dynamically fetch the correct OAuth token based on the rule's target email.

## 2. Weekly Cloud Log Backup (CSV)
To prevent local database bloat while preserving long-term sorting history for free, the app will utilize the user's Google Drive storage:
- **Worker**: A background script (likely via Android `WorkManager`) will run automatically once a week.
- **CSV Export**: It will query all local logs from the past 7 days and convert them into a structured `.csv` file (e.g., `AutoSort_Logs_Week_4.csv`).
- **Upload**: Utilizing the existing `GoogleDriveDestination` logic, the CSV will be silently uploaded to a designated "AutoSort Logs" folder in the user's Drive.
- **Cleanup**: Once the upload is verified successful, the local SQLite database will be wiped clean to free up device storage.

## 3. Cloud Drive File Retention Control
Instead of forcefully deleting or keeping local files after a Drive upload, give users explicit control per rule:
- **UI Checkbox**: Add `[ ] Delete original file after uploading` to the Rule Creation screen. 
- **Constraint**: This checkbox will **only** appear when the user selects `Google Drive` as the destination. Local folder destinations inherently perform a standard file move.
- **Engine Logic**: `GoogleDriveDestination.kt` will read this flag and conditionally skip `sourceFile.delete()` if unchecked.

## 4. Enhanced Audit Logs
Upgrade the `SortLog` database and `LogsScreen` UI to provide a true audit trail for debugging:
- **New Data**: Log the exact `Source Folder` path, the precise `Destination Folder`, and the `Rule Name` that triggered the action.
- **Expandable UI**: Keep the log cards compact by default. When a user taps a card, expand it to reveal the rich audit data, ensuring the screen remains clean and readable.

## 5. Smart Storage Cleanup Assistant
To prevent local storage bloat for users who choose *not* to delete files after a Drive upload:
- **Background Scanner**: Implement a `WorkManager` task that runs daily.
- **Logic**: It will query the logs for files uploaded to Google Drive > 7 days ago where the local file still exists.
- **Actionable Notification**: Send a smart reminder: *"You have X files safely backed up to Google Drive. Tap to clear local storage."*
- **Cleanup UI**: Tapping the notification opens a "Storage Assistant" screen allowing users to delete all old, backed-up files with a single tap.
