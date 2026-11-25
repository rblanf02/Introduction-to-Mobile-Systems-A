# M2 Task Board — Steppy

M2 (Week 9) is a **working demo**: the main journey running on a real phone, data kept after restart, and the real step sensor. **Steppy runs completely locally** (no backend, no `INTERNET` permission). The demo **must show the denied-permission path and airplane mode**, where airplane mode shows that nothing changes (see [PROJECT_PLAN.md §9](PROJECT_PLAN.md)).

This board covers Weeks 5–8 from the plan, plus demo preparation. Work from Week 10 onward (QR transfer, the full test suite, usability sessions) is **not** needed for M2.

**Roles** (from [PROJECT_PLAN.md §10](PROJECT_PLAN.md), still to be assigned): **A** UI/UX & Accessibility · **B** Data & Logic · **C** Platform & Network

**Priority:** **P0** is needed for the M2 demo · **P1** is planned for this period but can slip to Week 10 if needed

**Status:** ✅ done · 🟡 partly done · ⬜ not started

---

## Status check (25 Nov 2026, `main` at `71c87b1`)

**What has been done** (Aitor and Rubén, 17–19 Nov):
- The Android project builds (Compose, Navigation, min SDK 26), and its `.gitignore` covers `.idea/`.
- All screens exist: onboarding, Today, Progress, Challenges, Profile. They share a single `SteppyViewModel`, and the data is saved in SharedPreferences.
- Step sensor (`StepSensorDataSource`, `SensorStepRepository`), the `ACTIVITY_RECOGNITION` permission with a manual-logging fallback, and a simulated counter for the emulator.
- `DailyStepCalculator` with 4 unit tests. `testDebugUnitTest` passes.

**Biggest gaps for M2:**
1. **Game rules are not separate or tested (D1–D3).** Streak, XP and challenge logic are inside `SteppyUiState` and `SteppyViewModel`. Hearts are fixed at 1. `baseXP` never grows, so XP from previous days and from challenges is lost.
2. **No daily history (L1, L4).** Only `today_steps` and `streak` are stored. The weekly chart, active days and "streak lost" have no data behind them.
3. **Day rollover bug (P2).** At midnight the sensor resets to 0, but `onSensorStepsUpdated` ignores decreases, so Today keeps showing yesterday's steps.
4. **No reminder notification (P5)** and no Reminder onboarding screen.
5. **The rest of the plan uses Room and DataStore, but the code uses SharedPreferences.** Decide whether to migrate or update the plan (see the open decisions).

---

## Phase 0 — Foundation

| ID | Task | Role | Prio | Status | Notes |
|---|---|---|---|---|---|
| F1 | Android Studio project (Kotlin, Compose, Navigation, version catalog) | C | P0 | ✅ | min SDK 26, target SDK 37 |
| F2 | Android `.gitignore` | C | P0 | ✅ | |
| F3 | Package structure and manual dependency injection | B | P0 | 🟡 | Has `ui/`, `data/` and `platform/`. There's no `domain/` package for the pure logic, and dependencies are wired in `MainActivity` |
| F4 | Assign roles A/B/C in PROJECT_PLAN §10. Agree on a branch and PR workflow. | All | P0 | ⬜ | |
| F5 | Create `AI_USE.md` with its first entry | All | P0 | ⬜ | The file doesn't exist yet |
| F6 | Update the README: user stories in line with the plan, one Canva link, build instructions | B | P1 | ⬜ | |

---

## Phase 1 — Week 5: User journey

### UI (A)
| ID | Task | Prio | Status | Notes |
|---|---|---|---|---|
| U1 | 48 dp touch targets (AR1), one number format | P0 | 🟡 | 48 dp is only on Profile; Today uses a number formatter |
| U2 | Navigation: onboarding, then a bottom bar that highlights the current tab | P0 | ✅ | |
| U3 | Onboarding: Welcome, Setup (presets), Mascot, Step permission (Allow / Not now), Reminder | P0 | 🟡 | **The Reminder screen is missing.** The presets are 3,000 / 6,000 / 10,000 / 15,000. Check that "Continue" is disabled until a goal is picked and that the preset follows the activity level |
| U4 | Today: steps vs. goal, streak, hearts, active days, XP and level, with merged semantics (AR2) | P0 | 🟡 | The date is hard-coded ("Tuesday, May 21"). Hearts and weekly active days never change. Today has no merged semantics. The "Share streak" button is out of scope, so remove it. "Next reward…" is still shown |
| U5 | Challenges: Active with progress bars, and Available | P0 | 🟡 | Works. "Weekend Walker" still says "3 walks", so reword it to 15,000 steps |
| U6 | Feedback states: Goal reached, Streak lost, Empty state | P0 | 🟡 | "Goal reached" is only a snackbar. There are no Streak lost or Empty state screens |
| U7 | Progress screen with an AR3-friendly chart | P1 | 🟡 | The screen and its semantics exist. The chart uses placeholder bars, with Spanish day letters ("L M X J…") |
| U8 | Profile: mascot, goal, reminders, permissions, delete my data | P1 | 🟡 | Most of it is done. "Log out" is the wrong wording, because there are no accounts. The reminder time is missing |

### Domain logic (B), pure Kotlin with unit tests
| ID | Task | Prio | Status | Notes |
|---|---|---|---|---|
| D1 | `StreakCalculator`: streak and hearts (1 heart per 7-day streak, at most 2) | P0 | ⬜ | The streak only goes up when today's goal is met. Missed days and hearts aren't handled |
| D2 | `XpCalculator`: `steps/100` + 50 bonus, challenge XP, and the level | P0 | 🟡 | The formula is in `SteppyUiState`: level = `floor(sqrt(xp/50)) + 1`. Move it out and test it. XP from past days and from challenges is never added to `baseXP` |
| D3 | `ChallengeEvaluator`: at most 3 active, XP awarded once, failing never touches the streak | P0 | 🟡 | The 3-active limit works in the ViewModel. No XP is awarded, and there are no tests |
| D4 | Suggested preset and challenge ordering by main goal | P1 | ⬜ | |

### State
| ID | Task | Prio | Status | Notes |
|---|---|---|---|---|
| S1 | `StepRepository` plus a simulated "+500 steps" | P0 | ✅ | `SimulatedStepCounter` replaced `FakeStepRepository` |
| S2 | ViewModel(s) with `StateFlow` | P0 | ✅ | One shared `SteppyViewModel` rather than one per screen. That's fine for M2 |

---

## Phase 2 — Week 6: Local storage

| ID | Task | Role | Prio | Status | Notes |
|---|---|---|---|---|---|
| L1 | Room: `DailyRecord`, `UserChallenge`, `ProgressBaseline` and DAOs | B | P0 | ⬜ | **Needed for history, streak, hearts and the chart.** Room isn't a dependency yet |
| L2 | Built-in challenge catalogue as a separate list | B | P0 | 🟡 | The challenges are hard-coded in `defaultChallenges()`. That works, but challenge progress is mixed into the definitions |
| L3 | Settings storage (DataStore, or keep SharedPreferences) | B | P0 | 🟡 | `LocalSteppyRepository` saves most fields, but **not the main goal or activity level**. Those are lost after a restart |
| L4 | Save each day's goal with that day's record | B | P0 | ⬜ | Depends on L1 |
| L5 | ViewModel reads and writes through the repositories | A+B | P0 | ✅ | Data survives a restart |
| L6 | JSON backup and restore | B | P1 | ⬜ | |

---

## Phase 3 — Week 7: Networking (separate exercise, not in the app)

| ID | Task | Role | Prio | Status | Notes |
|---|---|---|---|---|---|
| N1 | Find out what the separate networking exercise must contain | C | P0 | ⬜ | |
| N2 | Build the exercise outside the Steppy app | C | P0 | ⬜ | |
| N3 | Steppy has no `INTERNET` permission | C | P0 | ✅ | Checked in the manifest |

## Phase 4 — Week 8: Sensor & notifications (test on a physical phone)

| ID | Task | Role | Prio | Status | Notes |
|---|---|---|---|---|---|
| P1 | Sensor repository with a daily baseline | C | P0 | ✅ | Still need to check the 10 s refresh on a real phone |
| P2 | Reboot detection and day rollover | C+B | P0 | 🟡 | `DailyStepCalculator` handles both and has tests. **Bug:** at midnight Today keeps yesterday's steps, and a missed day doesn't reset the streak |
| P3 | Permission denied or no sensor → manual entry | C+A | P0 | 🟡 | Works. Days aren't saved as `MANUAL`, because there's no history yet (L1) |
| P4 | WorkManager snapshot every 15 min, plus `stepsBeforeNoon` | C | P1 | ⬜ | "Morning Mover" currently uses all of today's steps |
| P5 | Daily reminder with `POST_NOTIFICATIONS` and a denial path | C | P0 | ⬜ | Only a toggle exists |
| P6 | Simulated steps only in debug builds | C | P1 | 🟡 | `DebugFlags` is always `true`. Tie it to `BuildConfig.DEBUG` |

---

## Phase 5 — Week 9: M2 demo prep

| ID | Task | Role | Prio | Status |
|---|---|---|---|---|
| M1 | Write the demo script (below) and rehearse it twice, timed | All | P0 | ⬜ |
| M2 | Fictional demo data, labelled as test data | B | P0 | ⬜ |
| M3 | Physical phone with screen mirroring (e.g. `scrcpy`), plus an emulator as a backup | C | P0 | ⬜ |
| M4 | Accessibility pass: 48 dp, TalkBack on Today, 200% font size | A | P1 | ⬜ |
| M5 | Check the course brief for any other M2 deliverables | All | P0 | ⬜ |
| M6 | Update `AI_USE.md` and the README for M2 | All | P0 | ⬜ |

### Demo script draft
1. Fresh install → onboarding → **Today in under 60 s** (the measurable criterion)
2. Step permission → **"Use manual step logging instead"** → manual entry works
3. Add steps until "Goal reached" → streak +1, +50 XP
4. Challenges: activate 3, then show that a 4th is blocked
5. **Airplane mode** → every screen works exactly the same (the app is fully local)
6. Kill and reopen the app, and rotate the screen → the data is still there
7. Notifications denied → the app still works; then allow them → show the reminder
8. (Physical phone) Real sensor: walk and watch the count update

---

## Open decisions

1. ~~Level formula~~ **Decided in the code:** `floor(sqrt(xp/50)) + 1`. Update PROJECT_PLAN §5 to match.
2. **Level rewards:** implement them, or remove "Next reward unlocked…" from Today.
3. ~~Min SDK~~ **Decided:** 26.
4. **Room and DataStore (plan) or SharedPreferences (code)?** Room is the simplest way to store daily history (L1). Settings can stay in SharedPreferences if the plan is updated.
5. **Who is A, B and C?** From the commits so far, Aitor is working on C (the `role-C-Platform-And-Network` branch) and Rubén on the UI screens.
6. **What the separate networking exercise needs** (N1).

## Suggested order for the remaining work

```
D1–D3 (pure logic + tests)  ─┐
L1 + L4 (Room daily history) ─┼→ fix P2 rollover → U4/U6/U7 real data → M1 rehearsal
P5 (reminder) + U3 Reminder screen ─ independent, can run in parallel
F4, F5 (roles, AI_USE.md) ─ quick, do now
```
