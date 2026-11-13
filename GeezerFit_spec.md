# GeezerFit -- Android App Specification

**Project:** Personal gym workout tracker for Dave and Lesa Covert
**Platform:** Android (native Kotlin + Jetpack Compose)
**Target:** Personal use; Play Store publication optional in future
**Android target:** API 29+ (Android 10)

---

## Overview

A two-user workout checklist app used at the gym on Android phones. Replaces printed workout plans. Users select their name, select today's workout (A or B), check off exercises as they complete them, and record the weight used for each set. Session history is saved locally so users can see what they lifted last time and apply progressive overload.

No accounts. No cloud sync required. No login. Local SQLite storage. Plans are fetched from a remote manifest at startup and cached locally -- new months are delivered by updating the remote files, with no app rebuild or Play Store update required.

---

## Users

Two hardcoded users at launch:
- Dave
- Lesa

User selection is the home screen. Each user has fully independent workout plans, session history, and saved weights.

---

## Workout Plans -- Remote Delivery

### Concept

Plans live in a GitHub repository as JSON files. The app fetches a manifest on startup, compares it against locally cached versions, and downloads only what has changed. This means new months (January, February, etc.) are delivered by committing a new JSON file and updating the manifest -- zero app changes required.

### Manifest File

A single file at a fixed URL:

    https://raw.githubusercontent.com/davecove/GeezerFit/main/plans/manifest.json

### Update Flow

1. App launches -> fetch manifest from GitHub raw URL
2. Compare each plan's version against locally cached version
3. Download any plan whose version has changed (or is not yet cached)
4. Fall back to cached plans if network is unavailable (no bundled seed plans; first launch needs network)
5. Never block the UI waiting for network -- show cached data immediately, update in background
6. Any version difference (not just higher) triggers a download. A download that fails or fails schema validation is discarded and the cached copy is kept.
7. When a new/changed plan is applied at startup, show an announcement (e.g. "New plan loaded: Dave -- January 2027"). No confirmation prompt.
8. A session in progress keeps the plan version it started with.
9. Month selection: current calendar month by default. If no plan exists for the current month, keep using the latest loaded plan and show a "New plan not yet available" message.

### Adding a New Month

1. Create dave_2027_01.json and lesa_2027_01.json in the plans/ folder
2. Add entries to manifest.json
3. Commit and push
4. Both phones pick up the new plans on next app launch

### Plan File Format

See plans/dave_2026_11.json for a complete example.

Key fields per exercise:
- id: string (stable slug, e.g. goblet_squat; key for history and last weight -- never change it when renaming)
- name: string
- sets: int
- reps: string (e.g. "12" or "10 each side" or "30 sec each")
- rest_sec: int (drives the rest timer, see below)
- weight_entry: boolean (false for bodyweight/stretch exercises)
- notes: string (coaching cue, collapsed by default in UI; no medical/clinical claims)

Workout fields: id (A/B/C), day (weekday name, e.g. "Tuesday"), warmup, exercises, cardio_finisher.
Cardio appears ONLY in cardio_finisher, not in the exercises list.
Plans are not limited to two days/week; add Workout C etc. as needed.

---

## Repository Layout

    GeezerFit/
      plans/
        manifest.json
        dave_2026_11.json
        dave_2026_12.json
        lesa_2026_11.json
        lesa_2026_12.json
      app/
        src/main/
          kotlin/com/geezerfit/
            data/
              db/             -- Room entities, DAOs, AppDatabase
              model/          -- Plan data classes (deserialized from JSON)
              repository/     -- PlanRepository interface
                remote/       -- RemotePlanRepository (GitHub raw fetch + cache)
                local/        -- LocalPlanRepository (cache-only fallback)
            ui/
              home/
              plan/
              session/
              history/
              settings/
            viewmodel/
            di/               -- Hilt modules
      GeezerFit_spec.md
      README.md

---

## Screens

### 1. Home Screen
- App name: GeezerFit
- Two large buttons: Dave / Lesa
- Subtle indicator if a plan update was fetched on this launch

### 2. Plan Screen (per user)
- Tabs or dropdown for available months
- Active month highlighted (current calendar month by default)
- Two workout cards: Workout A -- Tuesday and Workout B -- Thursday
- Tap to begin a session

### 3. Workout Session Screen

Scrollable.

Header: Workout A -- Tuesday * November 2026 * Foundation Phase
Phase note in a subtle info card.

Warm-Up Section: Collapsible. Checkbox list only -- no weight entry.

Exercises -- for each:
  [ ] Romanian Deadlift (RDL)   3 x 10   Rest: 90s
      Weight: [____] lbs
      [v Coaching Notes]  (collapsed, tap to expand)

- One checkbox per exercise (no per-set boxes); marking it done starts the rest timer (see Rest Timer)
- Weight field (one weight per exercise, not per set) pre-filled with last-used weight for this exercise
- Completed exercises visually dimmed
- Notes collapsed by default

Cardio Finisher: Checkbox + label + duration + instruction.

Footer: [Finish Workout] -- enabled once at least one exercise is checked.
Tap -> confirmation dialog -> save session.

#### Draft sessions
In-progress sessions are auto-saved (checks and weights) and resumed on next open if interrupted.

#### Rest Timer
When an exercise is checked complete, a countdown starts from that exercise's rest_sec (skipped if 0). Shows time remaining, skip/dismiss, and alerts (vibrate/sound) at zero. Unchecking cancels it.

### 4. Session Complete Screen
- "Session saved. Good work."
- Summary: date, workout, exercises completed
- Buttons: View History / Back to Plan

### 5. History Screen (per user)
- Sessions listed most-recent first
- Tap to expand: each exercise with recorded weight
- Sessions can be edited (weights, completed exercises) or deleted
- Primary use: check last week's weights before adding load

### 6. Settings Screen
- Units toggle: Imperial (lbs + feet) / Metric (kg + meters). Plan distances are stored in meters and converted for display. Default: Imperial.
- Export history to CSV or JSON (share sheet)
- Plan cache: "Last updated {date}" + manual refresh button
- App version

---

## Data Model (Room / SQLite)

User
  id: Int (1=Dave, 2=Lesa)
  name: String

Session
  id: Long (autoincrement)
  user_id: Int
  date: String (ISO 8601)
  month_key: String
  workout_id: String
  completed_at: Long (epoch ms)

ExerciseLog
  id: Long (autoincrement)
  session_id: Long (FK -> Session)
  exercise_id: String
  exercise_name: String (display snapshot)
  sets_completed: Int
  weight_lbs: Float (nullable)
  notes_shown: Boolean

LastWeight
  user_id: Int
  exercise_id: String
  weight_lbs: Float
  PRIMARY KEY (user_id, exercise_id)
  Updated on session save; pre-fills weight field next session

PlanCache
  user_id: Int
  month_key: String
  version: String
  json_content: String
  cached_at: Long
  PRIMARY KEY (user_id, month_key)

---

## UI / UX Guidelines

- Material Design 3 throughout
- Large touch targets -- gym use, sweaty fingers; minimum 48dp, prefer 56dp+
- High contrast -- readable in bright gym lighting
- User color theming:
    Dave: navy (#1F4E79 as Material seed)
    Lesa: forest green (#375623 as Material seed)
    App visually switches color scheme when switching users
- Font sizes: body minimum 16sp, exercise names 18sp
- Weight input: numeric keyboard opens on tap; "Done" key dismisses

---

## Tech Stack

Language:      Kotlin
UI:            Jetpack Compose
Navigation:    Compose Navigation
Database:      Room (SQLite)
JSON parsing:  kotlinx.serialization
HTTP:          Ktor client or OkHttp
DI:            Hilt
Architecture:  MVVM + Repository pattern
Min SDK:       29 (Android 10)
Target SDK:    latest stable

---

## Initial Plan Data

The four JSON plan files in plans/ are the seed data for the app. All exercises,
coaching notes, sets, reps, and rest times are already filled in.

Files present:
- plans/dave_2026_11.json  -- Dave, November (Foundation Phase, 2 workouts)
- plans/dave_2026_12.json  -- Dave, December (Load Phase, 2 workouts)
- plans/lesa_2026_11.json  -- Lesa, November (Foundation Phase, 2 workouts)
- plans/lesa_2026_12.json  -- Lesa, December (Progressive Load Phase, 2 workouts)
- plans/manifest.json      -- index of all plan files with versions

---

## Backup

Android Auto Backup enabled for the Room database. Manual export to CSV or JSON from Settings.

---

## Out of Scope (for now)

- Exercise GIF/video demonstrations
- Body weight / progress tracking charts
- Social features
- Apple iOS

---

## Future Considerations

- January 2027: add two JSON files + update manifest, push -- done
- 3-day/week plans: same format, add Workout C entry
- Play Store: planned. Clinical language already removed from plan notes. Add privacy policy before submitting
  (local-only data, nothing collected or transmitted beyond plan fetches from GitHub)

---

Spec prepared November 2026. App name: GeezerFit.
Reference: plans/*.json is the source of truth for all exercise data.
