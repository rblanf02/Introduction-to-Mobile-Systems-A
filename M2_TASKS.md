# M2 Task Board — Steppy

M2 (Week 9) is a **working demo**: the main journey running on a real phone, data kept after restart, and the real step sensor. **Steppy runs completely locally** (no backend, no `INTERNET` permission). The demo **must show the denied-permission path and airplane mode**, where airplane mode shows that nothing changes (see [PROJECT_PLAN.md §9](PROJECT_PLAN.md)).

This board covers Weeks 5–8 from the plan, plus demo preparation. Work from Week 10 onward (QR transfer, "Delete my data", the full test suite, usability sessions) is **not** needed for M2.

**Roles** (from [PROJECT_PLAN.md §10](PROJECT_PLAN.md), still to be assigned): **A** UI/UX & Accessibility · **B** Data & Logic · **C** Platform & Network

**Priority:** **P0** is needed for the M2 demo · **P1** is planned for this period but can slip to Week 10 if needed

---

## Current state (as of this board)

- ⚠️ **There is no Android project yet.** `app.kt` is empty and the repo has no Gradle files. The Week 1 output ("Compose starter app runs") is not done, so it blocks everything else.
- ⚠️ **The roles table is still "to assign".**
- ⚠️ **The README's acceptance criteria are out of date:** US1 there says "500–50,000 steps", but the plan uses four presets. The README also links a different Canva prototype from the M1 submission.
- `.idea/` and `M1_Submission.docx` are untracked. `.idea/` should go in `.gitignore`.

---

## Phase 0 — Foundation (do first, it blocks everything)

| ID | Task | Role | Prio | Done when |
|---|---|---|---|---|
| F1 | Create the Android Studio project: Kotlin, Compose, Navigation, a version catalog with the dependencies from §5, and min SDK 29 (Android 10+, matching the physical test phone). Delete the empty `app.kt`. | C | P0 | The app builds and runs on both the emulator and the phone |
| F2 | Add a `.gitignore` for Android (`.idea/`, `build/`, `local.properties`, `*.iml`) | C | P0 | `git status` is clean after a build |
| F3 | Set up packages `ui/`, `domain/`, `data/`, `di/`, plus an `AppContainer` for manual dependency injection | B | P0 | Other tasks have a place to put their code |
| F4 | Assign roles A/B/C in PROJECT_PLAN §10. Agree on a branch and PR workflow. | All | P0 | Names are in the table |
| F5 | Create `AI_USE.md` with its first entry (a Week 5 requirement) | All | P0 | The file exists and has an entry |
| F6 | Update the README: user stories in line with the plan (presets), one Canva link, build instructions | B | P1 | The README matches PROJECT_PLAN |

---

## Phase 1 — Week 5: User journey with fake data

**Goal:** Setup → Today → Challenges works end to end with in-memory steps.

### UI (A)
| ID | Task | Prio | Done when |
|---|---|---|---|
| U1 | Theme and shared components: a minimum touch size of 48 dp (AR1), a single number format such as `7,842` | P0 | Shared components are used on every screen |
| U2 | Navigation graph: an onboarding flow, then the main app with a bottom bar. The bar **highlights the current tab** (a prototype fix). | P0 | Back stack is correct; onboarding is skipped once it has been completed |
| U3 | Onboarding screens: Welcome, Setup (nickname, main goal, activity level, presets), Choose mascot, Step permission (Allow / **Not now**), Reminder (Allow / Not now) | P0 | "Continue" stays disabled until a goal is picked; the suggested preset follows the activity level (US1) |
| U4 | Today screen: steps vs. goal, streak, hearts, weekly active days, XP and level. Uses merged semantics (AR2). | P0 | TalkBack reads "Today's steps: 7,842 of 10,000, 78 percent" |
| U5 | Challenges screen: Active (with progress bars) and Available (with "Activate") | P0 | Shows current / target, as in US3 |
| U6 | Feedback states: Goal reached, Streak lost, Empty state (Today and Progress) | P0 | All three can be triggered from fake data |
| U7 | Progress screen: level and XP, active days, a weekly chart where goal met/missed has an icon or label as well as colour (AR3) | P1 | The 3- and 6-month views can wait |
| U8 | Profile stub: mascot, goal, reminder time, permissions. Leave placeholders for transfer, backup and delete. | P1 | Changing the goal works (US1) |

### Domain logic (B), pure Kotlin with unit tests started now
| ID | Task | Prio | Done when |
|---|---|---|---|
| D1 | `StreakCalculator`: streak and hearts from `ProgressBaseline` + `DailyRecord`s (1 heart per 7-day streak, at most 2) | P0 | Tests cover a missed day with 2, 1 and 0 hearts |
| D2 | `XpCalculator`: `steps/100` + 50 bonus, plus challenge XP and the level. **Choose the level formula first** (see the open decisions). | P0 | Tested |
| D3 | `ChallengeEvaluator`: at most 3 active, XP awarded once, failing never touches the streak, plus the "Weekend Walker" and "Morning Mover" rules | P0 | Tested, including "completed on the day it was activated" and "4th activation rejected" |
| D4 | Suggested-preset mapping and challenge ordering by main goal | P1 | Tested |

### State (B + A)
| ID | Task | Prio | Done when |
|---|---|---|---|
| S1 | A `StepRepository` interface with a `FakeStepRepository` and a **debug-only "+500 steps"** button | P0 | Steps on Today update live |
| S2 | `OnboardingViewModel`, `TodayViewModel` (`StateFlow<TodayUiState>`), `ChallengesViewModel`, `ProgressViewModel` | P0 | Screens keep their state through rotation |

---

## Phase 2 — Week 6: Local storage

| ID | Task | Role | Prio | Done when |
|---|---|---|---|---|
| L1 | Room: entities `DailyRecord`, `UserChallenge` and `ProgressBaseline`, type converters for `LocalDate` and `StepSource`, and DAOs | B | P0 | DAO tests pass |
| L2 | **Built-in challenge catalogue:** a bundled Kotlin list of `ChallengeDefinition` (`id`, `title`, `type`, `target`, `xp`), using the reworded "Weekend Walker". `UserChallenge.challengeId` refers to it. | B | P0 | All 5 challenges from §5 show on Challenges |
| L3 | DataStore `SettingsRepository`: nickname, main goal, activity level, daily goal, mascot, reminder time, onboarding done | B | P0 | Values survive killing the app |
| L4 | Write today's `goal` into each `DailyRecord`, so changing the goal does not rewrite history | B | P0 | Changing the goal mid-day is covered by a test |
| L5 | Switch the ViewModels from fake data to the repositories | A+B | P0 | **Data survives a restart and rotation** |
| L6 | JSON backup and restore: `BackupRepository`, kotlinx.serialization, the file picker (SAF), validation (version, checksum, value ranges), a "This will replace…" confirmation | B | P1 | Round trip restores the same streak and XP; an invalid file leaves data unchanged |

---

## Phase 3 — Week 7: Networking (separate exercise, not in the app)

The app stays local-only, so the Week 7 networking requirement is met with a **separate small exercise**, as the course allows.

| ID | Task | Role | Prio | Done when |
|---|---|---|---|---|
| N1 | Check with the course what the separate networking exercise must contain and how it is graded | C | P0 | Requirement written down here |
| N2 | Build the exercise in its own module or repo folder, **not** part of the Steppy app | C | P0 | Runs and handles loading / error / offline |
| N3 | Confirm the Steppy manifest has **no `INTERNET` permission** | C | P0 | Checked in a review |

## Phase 4 — Week 8: Sensor & notifications (test on a physical phone)

| ID | Task | Role | Prio | Done when |
|---|---|---|---|---|
| P1 | `SensorStepRepository`: `TYPE_STEP_COUNTER` exposed as a `callbackFlow`, with a daily baseline (`today = current − baseline`) | C | P0 | Updates within 10 s of walking (US2) |
| P2 | Reboot detection (`current < baseline`) and day rollover at midnight that closes the day | C+B | P0 | Tested on a real reboot and by changing the device clock |
| P3 | `ACTIVITY_RECOGNITION` runtime permission. If it is denied or there is no sensor, use **manual step entry** and save those days as `MANUAL`. | C+A | P0 | Demoable: deny → manual entry works |
| P4 | A WorkManager snapshot worker every 15 minutes, which also stores `stepsBeforeNoon` | C | P1 | The day closes correctly even if the app was not opened |
| P5 | Daily reminder: notification channel, `POST_NOTIFICATIONS` on Android 13+, a worker that runs at the chosen time with "1,200 steps left to keep your 12-day streak", and no reminder once the goal is met | C | P0 | Denying notifications means the app works, just without reminders |
| P6 | Make the debug "+500 steps" button exist only in debug builds | C | P1 | Not present in the release build |

---

## Phase 5 — Week 9: M2 demo prep

| ID | Task | Role | Prio |
|---|---|---|---|
| M1 | Write the demo script (below) and rehearse it twice, timed | All | P0 |
| M2 | Fictional demo data, labelled as test data. Seed it with a debug action. | B | P0 |
| M3 | Physical phone with screen mirroring (e.g. `scrcpy`), plus an emulator as a backup | C | P0 |
| M4 | Accessibility pass: 48 dp targets, a TalkBack walkthrough of Today, 200% font size | A | P1 |
| M5 | Check the course brief for any other M2 deliverables (slides, video, report). **They are not documented in this repo.** | All | P0 |
| M6 | Update `AI_USE.md` and the README for M2 | All | P0 |

### Demo script draft
1. Fresh install → onboarding → **Today in under 60 s** (the measurable criterion)
2. Step permission → **"Not now"** → manual entry works
3. Add steps until "Goal reached" → streak +1, +50 XP
4. Challenges: activate 3, then show that a 4th is blocked
5. **Airplane mode** → every screen works exactly the same (the app is fully local)
6. Kill and reopen the app, and rotate the screen → the data is still there
7. Notifications denied → the app still works; then allow them → show the reminder
8. (Physical phone) Real sensor: walk and watch the count update

---

## Open decisions (settle before or during Week 5)

1. **Level formula:** thresholds (100/250/500/1000) or `floor(sqrt(xp/50))`. This blocks D2.
2. **Level rewards:** implement mascot unlocks at levels 5 and 10, or drop "Next reward unlocked at Level 5" from Today.
3. **Min SDK 29?** That assumes the test phone runs Android 10+.
4. **Who is A, B and C?**
5. **What the separate networking exercise needs** (N1).

## Dependency order

```
F1 → F3 → (U*, D*, S1) → S2 → L1/L2/L3 → L5 → P1/P3 → M1
          D* can start right after F3, in parallel with the UI
          The networking exercise (N1–N2) is independent and can run in parallel
          A sensor spike on the phone (P1) can start in Week 5 to reduce risk
```
