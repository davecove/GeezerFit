# GeezerFit

Android gym workout checklist for Dave and Lesa. Kotlin + Jetpack Compose, Room, Hilt.
Full details: [GeezerFit_spec.md](GeezerFit_spec.md).

## Build

1. Install Android Studio (latest stable) with an Android 10+ (API 29) SDK.
2. Open this folder, let Gradle sync.
3. Run the `app` configuration on a device or emulator (network needed on first launch to fetch plans).

## Plans

Plans live in `plans/` and are fetched from
`https://raw.githubusercontent.com/davecove/GeezerFit/main/plans/manifest.json` at app start.
The repo must stay public.

### Add a new month

1. Create `plans/dave_YYYY_MM.json` and `plans/lesa_YYYY_MM.json` (copy an existing file).
2. Keep each exercise's `id` stable across months so weight history carries over.
3. No clinical or medical claims in `notes`.
4. Add entries to `plans/manifest.json` (`file`, `version`, `user`, `month_key`, `month_label`, `phase_label`).
5. To change an existing plan, edit the file and bump its `version` in the manifest.
6. Commit and push to `main`. Apps pick it up on next launch and announce it.

### Plan format

- Workout: `id` (A/B/C), `day` (weekday name), `warmup`, `exercises`, `cardio_finisher`.
- Exercise: `id`, `name`, `sets`, `reps` (string), `rest_sec`, `weight_entry`, `notes`.
- Cardio goes only in `cardio_finisher`, not in `exercises`.
- Distances are in meters; the app converts for the Imperial setting.
