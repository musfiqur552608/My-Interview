# Interview Tracker

An **offline-first Android app** that helps job-seekers track their entire interview
process — from first application to final offer — across every company they apply to.

Built with **Kotlin + Jetpack Compose (Material 3)**, following **MVVM + Clean
Architecture**. Your data never leaves the device: Room is the single source of
truth, and every smart feature (match scoring, email parsing, coaching questions,
gamification) runs **on-device**.

---

## ✨ Features

### Core tracking
- **Company → Application → Interview Round** nested hierarchy with full CRUD.
- **Kanban pipeline board** (`Applied → Screening → Interviewing → Offer → Rejected → Accepted/Withdrawn`) with **long-press drag-and-drop** between columns (plus an accessible move dialog), plus a list view, global search, status filters and 4 sort orders.
- **Company detail** with Overview / Applications tabs, dossier (size, funding, interview difficulty, rating) and per-company accent colors.
- **Application detail** with Rounds / Prep / Docs / Offer / **Journey timeline** tabs.

### Calendar & reminders
- Month calendar with round indicators, conflict (overlap) warnings and per-day schedule.
- **WorkManager reminders**: X-hours-before, day-before, and post-round thank-you nudges.
- **Daily follow-up detector** (`FollowUpWorker`): flags applications gone quiet past your threshold, in-app + notification.
- **Interview-day mode**: today's rounds on one screen — times, join links, checklist progress, prep notes.
- **Device-calendar sync** (opt-in): rounds mirrored into a local “Interview Tracker” system calendar with 1-hour + 1-day alarms; auto-removed when rounds are cancelled or deleted.

### Preparation hub
- Per-round prep checklists, notes and resource links.
- Reusable prep templates (seeded: Technical / System Design / Behavioral).
- Taggable **question bank** reused across companies.
- **AI interview coach**: on-device mock questions tailored to role + JD, self-rated answers, 0–100 session scores with history. Optional OpenAI-compatible API key (Settings) upgrades feedback to a real model call — failures always fall back to offline coaching.

### Post-interview reflection
- Questions asked / your answers, 1–5 self-rating, went-well / improve journal.
- **Voice memos** (mic recording + playback, stored app-private).
- Thank-you-note tracking.

### Documents, contacts & offers
- Resume versions / cover letters / offer letters attached per application (Storage Access Framework picker).
- Recruiter/interviewer contacts cross-linked to companies.
- Offer recording with deadline countdown, negotiation notes, **annualized total-comp calculator** (base + bonus + equity/4yr) and side-by-side **offer comparison table**.
- Indicative market salary bands per role (offline, labeled as estimates).

### Analytics dashboard
- Pipeline funnel chart + Applied→Offer conversion (Compose-canvas, no chart dependency).
- Pass rate by round type, active/upcoming counts, weekly activity.
- **Gamification**: XP, levels (Scout → Legend), weekly application streaks and goal progress.
- **JD match score**: resume-vs-description keyword overlap with missing-skill list.

### Productivity extras
- Global quick-add bottom sheet from any tab; **Smart Import** (paste a recruiter email → company + application + round auto-created by an offline parser).
- Home-screen widgets: next interview + countdown, and quick-log (quick-add / day-mode deep links).
- CSV backup/restore, per-application PDF reports, full-portfolio PDF export.
- **Google Drive backup** (opt-in): CSV snapshots in the app's private Drive folder, backup/restore from Settings — OAuth via AccountManager, no API keys.
- **Gmail import** (read-only): list interview mail and decode one tap into Smart Import — same keyless OAuth, revocable anytime.
- Dark / Light / **Focus (AMOLED-black)** themes + Material You dynamic color.
- Biometric/device-credential app lock, multi-profile pipelines, onboarding flow.
- Empty / loading states throughout, edge-to-edge Material 3 UI.

---

## 🛠 Tech stack

| Area | Choice |
|---|---|
| Language / UI | Kotlin 2.2.10, Jetpack Compose (Material 3, BOM 2026.02.01) |
| Architecture | MVVM + Clean (UI → ViewModel → UseCase → Repository) |
| DI | Hilt 2.60.1 (+ `hilt-navigation-compose` 1.2.0) |
| Database | Room 2.8.3 (KSP 2.2.10-2.0.2), offline-first |
| Navigation | Navigation-Compose 2.9.4 |
| Settings | DataStore Preferences 1.2.0 |
| Background work | WorkManager 2.10.5 |
| Privacy | Biometric 1.1.0 app lock |
| Charts / export | Compose-canvas charts, framework `PdfDocument`, manual CSV |
| Build | AGP 9.4.0, compile/target SDK 37, min SDK 24, Java 11 |

No analytics SDKs, no ads, no required network calls. (`INTERNET` is declared
only for the optional user-supplied AI-coach key.)

---

## 🏗 Architecture & project structure

```
app/src/main/java/com/freedu/myinterviews/
├── ai/                  # AiCoach: offline question banks + optional LLM call
├── data/
│   ├── local/           # Room: TrackerDatabase, entities, DAOs, Mappers
│   ├── preferences/     # SettingsDataStore (DataStore) + AppSettings
│   └── repository/      # TrackerRepositoryImpl (Flow joins live here)
├── di/                  # AppModule (Hilt: DB, repo, settings, seed templates)
├── domain/
│   ├── model/           # Pure data classes + pipeline/round enums
│   ├── repository/      # TrackerRepository interface (+ SearchResults)
│   └── usecase/         # ObservePipeline, MoveApplication, ObserveDashboard, LogReflection
├── presentation/
│   ├── navigation/      # Routes + AppNavGraph (bottom bar, onboarding/lock gates)
│   ├── components/      # StatusChip, EmptyState, charts, QuickAddSheet, VoiceMemo…
│   ├── dashboard/ pipeline/ company/ calendar/ contacts/
│   ├── prep/ offers/ search/ settings/ today/ simport/ coach/ onboarding/
├── ui/theme/            # Material 3 theme (+ amoled Focus variant)
├── util/                # MatchScore, Gamification, CompCalc, Insights,
│                        #   EmailParser, CsvBackup, PdfReport, DateUtils, StatusUi
├── widget/              # NextInterviewWidget (+countdown), QuickLogWidget
├── worker/              # ReminderWorker, FollowUpWorker + schedulers
├── MainActivity.kt      # FragmentActivity (BiometricPrompt), theme + deep links
└── TrackerApp.kt        # @HiltAndroidApp
```

**Key conventions**
- ViewModels never touch DAOs — only `TrackerRepository` / use cases.
- Pure, unit-testable logic lives in `util/` and `ai/` (no Android dependencies in signatures where avoidable).
- Joins (e.g. resolving `companyName` for applications/rounds) happen in the repository, so UI code stays flat.
- Process death is handled via `SavedStateHandle` (pipeline search query) and cold Flow re-collection everywhere else.

---

## 🗄 Database

Room `tracker.db`, currently **version 2** with a lossless `AutoMigration(1 → 2)`
(schemas exported under `app/schemas/` — keep them, future migrations need them).

| Table | Contents |
|---|---|
| `companies` | name, website, industry, notes, source, tags + dossier (`rating, size, funding, difficulty`) + `accentColor` |
| `applications` | company FK (logical), title, JD/link, dates, status, salary, location, work mode, resume version + **`resumeText`** |
| `rounds` | type, schedule, duration, mode, interviewers, link, status/outcome, reflection fields, prep notes/checklist + **`voiceMemoUri`** |
| `contacts` | recruiter/interviewer details, optional company link |
| `documents` | name, content URI, type, sent-version flag |
| `offers` | base, bonus, equity, benefits, deadline, notes, decision |
| `question_bank` | reusable Q&A + tags |
| `prep_templates` | named checklists (seeded on first run) |
| `practice_sessions` | mock-interview Q&A, score, date |

---

## 🚀 Getting started

**Prerequisites:** JDK 17+, Android Studio Otter (2025.2.3+) or newer for AGP 9,
Android SDK 37.

```bash
# debug APK
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# unit tests (27 tests: use cases, CSV, match/gamification/comp/overlap/email/AI)
./gradlew :app:testDebugUnitTest

# compile on-device UI tests (run on an emulator/device — no Hilt, pure components)
./gradlew :app:compileDebugAndroidTestKotlin
# ./gradlew :app:connectedDebugAndroidTest
```

Open the project in Android Studio and run the `app` configuration on an
emulator or device (min SDK 24).

### Permissions (all requested only when the feature is used)

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | Interview reminders, thank-you nudges, follow-up alerts (asked once on launch, Android 13+) |
| `RECORD_AUDIO` | Voice memos in reflections (asked when recording) |
| `USE_BIOMETRIC` | Optional app lock (Settings → Privacy) |
| `READ/WRITE_CALENDAR` | Optional device-calendar sync (asked when enabling) |
| `GET_ACCOUNTS` | Finding your on-device Google account for Gmail/Drive import |
| `INTERNET` | Gmail/Drive REST calls and the optional user-supplied AI-coach key |

### Settings reference (DataStore)

Theme mode (`SYSTEM/LIGHT/DARK/FOCUS`), dynamic color, reminder lead times,
day-before + thank-you toggles, follow-up days, daily summary, app lock, active
profile id, onboarding flag, weekly goal, AI-coach API key.

### CSV backup format

Two sections — `[COMPANIES]` and `[APPLICATIONS]` — each with a header row.
Import matches applications to companies by name (creating missing companies)
and always assigns fresh ids, so re-importing never collides.

---

## 🧭 Navigation map

Bottom bar: **Dashboard · Pipeline · Calendar · Contacts · Settings**.
Secondary destinations: Prep Hub, Offer compare, Interview-day mode, Smart
Import, global Search, `company/{id}`, `application/{id}`, `coach/{appId}`.
Widget buttons deep-link via `MainActivity.EXTRA_DEST` (`quick_add`, `today`).

---

## ⚠️ AGP 9 notes / known tech debt

- Kotlin support is **built into AGP 9**: the `kotlin-android` plugin and
  `kotlinOptions` are intentionally absent.
- Hilt must be **≥ 2.59** for AGP 9 (this project uses 2.60.1).
- `android.disallowKotlinSourceSets=false` is set in `gradle.properties` because
  KSP 2.2.x still registers sources via the legacy DSL. **Upgrade KSP to ≥ 2.3.1
  and remove the flag** — it stops working in AGP 10.

---

### Google integrations (no SDKs, no keys)

Gmail import and Drive backup share `google/GoogleAuth.kt`: OAuth tokens are
minted by the on-device Google authenticator via `AccountManager`
(`oauth2:` scopes `gmail.readonly` / `drive.appdata`), with the system consent
screen on first use. REST calls use `HttpURLConnection` + `org.json`. Users
revoke access at myaccount.google.com/permissions. Requires a Google account on
the device; every network failure surfaces as a status message, never a crash.

## 🗺 Possible next steps

Server-driven salary data, interviewer-side scheduling links, Wear OS
complications, iOS/desktop ports.
