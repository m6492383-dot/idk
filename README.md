# Personal WhatsApp Scheduler (Android APK)

A privacy-focused, 100% offline native Android application for scheduling WhatsApp messages with file attachments (Images, Videos, PDFs, Word documents, Audio, ZIPs) and recurring execution options.

---

## 🌟 Key Features & Principles

- **100% Local & Private**: Stores all schedules and logs locally in Room SQLite database. No cloud backends, user accounts, analytics, or external servers.
- **No Advertisements / No Subscriptions**: Built strictly for personal utility.
- **Hybrid Scheduling Engine**: Combines `AlarmManager.setExactAndAllowWhileIdle()` for exact wakeup timing with `WorkManager` for reliable background execution.
- **Resilient Accessibility Service**: Uses semantic Android Accessibility node discovery (role, content description, text) instead of hardcoded resource IDs for resilient WhatsApp UI automation.
- **Rich Attachment Support**: Attach Images, Videos, PDFs, Word docs, Audio files, or ZIP archives.
- **Recurring Schedules**: Supports `Once`, `Daily`, `Weekly`, and `Monthly` message recurrence.
- **GitHub Actions Integration**: Automated CI pipeline builds the debug APK (`app-debug.apk`) on every push.

---

## 🏗 Architecture & Tech Stack

| Component | Technology |
|---|---|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture Pattern** | MVVM (ViewModel → Repository → Room) |
| **Database** | Room (SQLite) |
| **Scheduling Engine** | Hybrid: `AlarmManager` (Exact Trigger) + `WorkManager` (Execution) |
| **Automation Engine** | Android `AccessibilityService` |
| **Min SDK** | Android 10+ (API 26+; API 31+ recommended) |

### System Flow
```
User Creates Schedule
        │
        ▼
Save to Room Database (Status: PENDING)
        │
        ▼
AlarmManager.setExactAndAllowWhileIdle()
        │
        ▼  <-- Alarm Fires at Exact DateTime
AlarmReceiver (BroadcastReceiver)
        │
        ▼
WorkManager (WhatsAppSendWorker)
        │
        ▼
Validate Schedule & Check Permissions
        │
        ▼
Launch WhatsApp Deep Link Chat Intent
        │
        ▼
WhatsApp Accessibility Service Automation (Paste Text & Click Send)
        │
        ▼
Update Room DB Status (SENT / FAILED) & Trigger Notification
        │
        ▼
Re-arm Next Schedule if Recurring (Daily / Weekly / Monthly)
```

---

## 🗄 Database Schema (Room SQLite)

### Table: `messages`

| Column | Type | Constraints / Description |
|---|---|---|
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` |
| `contact_name` | `TEXT` | Display name of target contact |
| `phone_number` | `TEXT` | International format phone number (e.g., `+1234567890`) |
| `message` | `TEXT` | Message text body |
| `attachment_path` | `TEXT` | Local URI / file path (Nullable) |
| `scheduled_datetime`| `INTEGER` | Epoch timestamp in milliseconds |
| `repeat_type` | `TEXT` | `ONCE`, `DAILY`, `WEEKLY`, `MONTHLY` |
| `status` | `TEXT` | `PENDING`, `SENT`, `FAILED`, `CANCELLED` |
| `failure_reason` | `TEXT` | Failure details if sending failed (Nullable) |
| `created_at` | `INTEGER` | System epoch timestamp |
| `updated_at` | `INTEGER` | System epoch timestamp |

### Supported Status & Failure Reasons

- **Statuses**: `PENDING`, `SENT`, `FAILED`, `CANCELLED`
- **Failure Reasons**:
  - `WhatsApp not installed`
  - `Contact not found`
  - `Attachment missing`
  - `Accessibility disabled`
  - `Permission denied`
  - `User cancelled`
  - `Send button not detected`
  - `Timeout waiting for UI`
  - `Unknown error`

---

## 🔒 Android Permissions Required

- `android.permission.POST_NOTIFICATIONS` (Android 13+)
- `android.permission.SCHEDULE_EXACT_ALARM` (Android 12+)
- `android.permission.USE_EXACT_ALARM` (Android 14+)
- `android.permission.WAKE_LOCK`
- `android.permission.RECEIVE_BOOT_COMPLETED` (Restores pending alarms after reboot)
- `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (Optional, for Doze mode exemption)
- `android.permission.FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_DATA_SYNC`
- `android.permission.READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO` & `READ_EXTERNAL_STORAGE`
- `android.permission.BIND_ACCESSIBILITY_SERVICE` (WhatsApp Auto-Sender Service)

---

## 🚀 Building the APK

### Via GitHub Actions (Automated)
1. Push your repository to GitHub.
2. Go to the **Actions** tab on GitHub.
3. Download the compiled **`WhatsApp-Scheduler-Debug-APK`** artifact once the build completes.

### Via Android Studio / Command Line
1. Open the project in **Android Studio**.
2. Sync Gradle dependencies.
3. Run `./gradlew assembleDebug` or build directly to a connected Android device.
