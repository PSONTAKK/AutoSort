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
