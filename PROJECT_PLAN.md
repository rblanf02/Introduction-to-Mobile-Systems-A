# Project Plan — Steppy

> A "Duolingo-style" motivation system for walking: set a daily step goal, keep your streak alive, complete achievable challenges, and level up.

Course: *Introduction to Mobile Systems — Mobile for Social Good*

**Team:** Rubén Blanco · Aitor Ruiz · Nicholas Antunes

---

## 1. Problem definition

**Social problem:** According to the World Health Organization (WHO), obesity is a chronic disease that has become a major global public health challenge. One important contributing factor is **physical inactivity**, which is increasingly common due to modern lifestyles: prolonged screen time, desk-based jobs, and the widespread use of cars and other forms of transportation. The WHO reports that **31% of adults and 80% of adolescents do not meet the recommended levels of physical activity** (WHO, *Global status report on physical activity 2022*).

Our app addresses the physical-inactivity side of this problem by motivating people to **walk every day**.

**Target user group (specific):**
> Desk-bound students and professionals whose main task is incorporating short, regular walks into their sedentary daily routine.

Students are the easiest part of this group to reach for the Week 12 usability sessions (classmates are allowed); ideally at least one session is with someone who works at a desk.

### Key barriers

- **Mental fatigue and high friction:** after long study or work sessions, mental exhaustion reduces willpower, and traditional fitness routines feel overwhelming.
- **Passive engagement:** existing apps (e.g. Google Fit) act as passive mirrors — they show data but do not nudge or motivate the user to act.

### Evidence vs. assumptions

| Type | Statement | Source / how to verify |
|---|---|---|
| Evidence | 31% of adults and 80% of adolescents do not meet recommended physical activity levels | WHO. *Global status report on physical activity 2022*. Geneva: WHO; 2022. https://www.who.int/publications/i/item/9789240059153 |
| Evidence | Obesity is a chronic disease and a major global public health challenge | WHO Obesity and overweight fact sheet (cite properly) |
| Evidence | Physical inactivity is a contributing factor to obesity | WHO Guidelines on Physical Activity and Sedentary Behaviour (cite properly) |
| Evidence | Relation between daily step counts and health outcomes | Published studies — cite real papers, do not invent numbers |
| Assumption | Desk-bound users prefer short (~5-minute) walking breaks over structured exercise, which lowers the effort needed to start moving | To be observed in Week 12 usability sessions |
| Assumption | Instant XP and bite-sized daily challenges generate more engagement than passive fitness data | To be observed in Week 12 usability sessions |
| Assumption | Streaks motivate users to keep a daily habit | To be observed in Week 12 usability sessions |

### Existing solutions

| App | What it does | Our difference |
|---|---|---|
| Google Fit | Tracks steps and physical activity; only lets users set goals and monitor progress | Ours focuses on **motivating** users to walk every day through a streak system and achievable challenges |
| Samsung Health | Full health dashboard | Ours is lightweight, no hardware ecosystem needed |
| Pacer / StepsApp | Step counting, challenges | Ours uses a Duolingo-style streak + level loop |
| Sweatcoin | Rewards steps with currency | Ours rewards with streaks/levels, no monetisation |

### Competitive edge (from the M1 presentation)

| Standard apps | Steppy | Implemented by |
|---|---|---|
| Passive data mirrors | **Proactive nudging** before users lose momentum | Evening reminder notification (MVP) |
| Raw clinical metrics (distance, calories) | **Habit building** through streaks and gamification | Streak, hearts, XP, levels |
| Delayed health insights | **Immediate gratification** for micro-walks | XP per day, challenge XP, "Goal reached" screen |
| Built for self-motivated athletes | **Built for the sedentary** | Low presets (3,000 steps), small challenges |

### Ethical considerations

- **Streak anxiety:** losing a long streak can be discouraging → hearts act as *streak freezes*.
- **Inclusivity:** step counting excludes wheelchair users → documented as a known limitation.
- **No shaming:** messages are encouraging, never negative.
- **No weight stigma:** the MVP never asks for weight or BMI and never mentions body size — it only talks about walking. If weight/BMI are added later, they will be optional and never presented in a judgmental way.

---

## 2. MVP scope

### Main user journey

> **Set goal (Setup) → see steps & streak (Today) → reach goal → streak +1, XP (Goal reached) → pick challenges (Challenges)**

### Measurable task criterion

> **A first-time user can complete setup and find today's steps and current streak in under 60 seconds, without help.**

This criterion is reused as the task in the Week 12 usability sessions.

### Screens

**Onboarding flow**
1. **Welcome** — app name, "Get started", and "I already use Steppy" (→ scan a transfer QR code).
2. **Setup** — username (nickname), main goal (Move more / Build a streak / Stay healthy), activity level, daily step goal (presets: 3,000 / 6,000 / 10,000 / 15,000).
3. **Choose mascot** — Sloth, Puppy, Ostrich, Iguana.
4. **Step permission** — explains why steps are needed; "Allow access" or "Not now" (→ manual step entry).
5. **Reminder** — "Want a daily nudge?" with a reminder time; "Allow notifications" or "Not now".

**Main screens (bottom navigation)**
6. **Today** — today's steps vs. goal, current streak, hearts (streak freezes), weekly active days, XP and level.
7. **Progress** — level/XP, active days, streak, weekly / 3-month / 6-month step chart.
8. **Challenges** — active challenges with progress, available challenges to activate.
9. **Profile** — change mascot, daily step goal, reminder time, permissions, transfer to a new phone (QR code), backup / restore from file (JSON), help & support, delete my data.

**Feedback states**
10. **Goal reached** — "Goal reached! 🔥 streak +1, +50 XP".
11. **Streak lost** — shown when a day is missed and no hearts are left.
12. **Empty state** — new user with 0 steps and no history on Today and Progress.

### Use of "Main goal"

The main goal chosen in Setup orders the suggested challenges (e.g. "Build a streak" → streak challenges first; "Move more" → step challenges first). It does not change any rules.

### Scope summary

| ✅ In the MVP | ⏳ Later | ❌ Out of scope |
|---|---|---|
| Daily goal (presets) | Weight | Accounts / login |
| Step tracking + streak | BMI | Chat |
| Hearts = streak freezes | | GPS / maps |
| XP & levels | | Calories |
| Challenges | | Sharing with friends |
| Daily reminder notification | | Friend leaderboard |
| QR code transfer to a new phone (single code, last 6 months) | | CSV export |
| JSON backup / restore (file, full history) | | Wearables, iOS, AI features |

### User stories and acceptance criteria

**US1 — Set a goal**
*As someone who sits most of the day, I want to set a daily step goal so that I have a clear, achievable target.*
- The user selects one of four presets (3,000 / 6,000 / 10,000 / 15,000); "Continue" is disabled until a goal is selected.
- The suggested preset matches the chosen activity level (e.g. "Somewhat active" → 6,000).
- The goal is still shown after closing and reopening the app, and can be changed in Profile.

**US2 — Track progress and streak**
*As someone who sits most of the day, I want to see today's steps and my streak so that I know how close I am to my goal.*
- While the app is open, the step count refreshes within 10 seconds of walking.
- The streak increases by 1 only on days where steps ≥ goal.
- Missing a day uses 1 heart (streak freeze); with 0 hearts the streak resets to 0. One heart is earned per 7-day streak, maximum 2.

**US3 — Complete achievable challenges**
*As someone who sits most of the day, I want to pick challenges so that walking feels rewarding even when the full goal feels hard.*
- Tapping "Activate" moves a challenge to Active; at most 3 challenges can be active at once.
- Each active challenge shows a progress bar with current / target (e.g. 7,842 / 10,000 steps).
- On completion, the challenge's XP is added exactly once; failing a challenge never affects the streak.

---

## 3. Course requirements mapping

| Requirement | Feature in our app |
|---|---|
| ViewModel / app state (W5) | `TodayViewModel` exposing `StateFlow<TodayUiState>` |
| Local persistence (W6) | **Room** for daily records and challenges, **DataStore** for goal, nickname, main goal, mascot and settings |
| Networking (W7) | **Not used by the app** — Steppy runs completely locally; the Week 7 requirement is covered by a separate small networking exercise (see section 4) |
| Device capability (W8) | **Step counter sensor** (`TYPE_STEP_COUNTER`) + `ACTIVITY_RECOGNITION` permission |
| Permission fallback (W8) | Permission denied / no sensor → **manual step entry** (days marked as "manual") |
| Notifications (MVP) | Evening reminder: "1,200 steps left to keep your 12-day streak" — `POST_NOTIFICATIONS` on Android 13+; if denied, the app works normally without reminders |
| Automated tests (W5–W11) | Written alongside each feature from Week 5, completed in Week 11: streak + hearts calculator, XP/level calculator, challenge completion, day-rollover logic, backup encode/decode |

---

## 4. Networking — decision

**Decision: Steppy runs completely locally.** Sharing and the friend leaderboard are out of scope, so there is no backend, no remote data and no `INTERNET` permission.

- The challenge catalogue is **built into the app** (a bundled list), so it always works offline.
- The course still asks for a network interaction in Week 7 → we complete a **separate small networking exercise**, as the course allows, outside the Steppy app.
- Benefit for privacy and the demo: the app behaves the same with or without a connection (airplane mode changes nothing).

---

## 5. Technical design

### Stack

- Kotlin + Jetpack Compose
- Navigation Compose
- Room (daily records, challenges)
- DataStore (goal, nickname, main goal, mascot, settings)
- WorkManager (periodic step snapshots, daily reminder)
- kotlinx.serialization (backup file, QR payload)
- ZXing `core` (QR generation) + Google Code Scanner (QR scanning)
- Manual dependency injection (no Hilt, to keep it simple)

### Architecture

```
UI (Compose screens)
   ↓ observes StateFlow / sends events
ViewModels (TodayViewModel, ProgressViewModel, ChallengesViewModel, ...)
   ↓
Repositories (StepRepository, SettingsRepository, ChallengeRepository, BackupRepository)
   ↓
Data sources (Room DAO, DataStore, SensorManager, built-in challenge list)
```

Domain logic (streak, hearts, XP, level, challenge completion, backup encoding) lives in **pure Kotlin functions** so it can be unit-tested without Android.

### Data model

```kotlin
@Entity
data class DailyRecord(
    @PrimaryKey val date: LocalDate,   // stored as ISO string
    val steps: Int,
    val stepsBeforeNoon: Int?,         // saved by the snapshot worker; used by "Morning Mover"
    val goal: Int,                     // goal *on that day* — changing the goal must not rewrite history
    val source: StepSource             // SENSOR or MANUAL
)

@Entity
data class UserChallenge(
    @PrimaryKey val challengeId: String,
    val activatedOn: LocalDate,
    val completedOn: LocalDate?        // null while active; XP is awarded once when set
)

@Entity
data class ProgressBaseline(
    @PrimaryKey val id: Int = 0,       // single row
    val asOf: LocalDate,               // first day covered by DailyRecord rows
    val xpBefore: Int,                 // XP earned before asOf
    val streakBefore: Int,             // streak length on the day before asOf
    val heartsBefore: Int,             // hearts available on the day before asOf
    val activeDaysBefore: Int          // goal-met days before asOf ("Active days" counter)
)
```

Streak, hearts, XP, and level are **derived** rather than stored, to keep them consistent and testable:

> **value = `ProgressBaseline` + calculation over `DailyRecord` rows from `asOf` onwards**

A new user has a baseline of all zeros (`asOf` = install date), so the logic is the same for new and transferred users. The baseline only becomes non-zero after a QR transfer, which sends a limited history window (see *Data backup and transfer*).

### Step sensor notes

- `TYPE_STEP_COUNTER` returns the total steps **since the last device reboot**, not today's steps.
  → Store a baseline at the start of each day: `todaySteps = current - baseline`.
- After a reboot, the counter resets to 0 → detect `current < baseline` and handle it.
- The sensor keeps counting in hardware while the app is closed, so **we do not ask for unrestricted battery access during onboarding**. A WorkManager job every 15 minutes saves a snapshot so the day closes correctly even if the app was not opened.
- If testing on a specific phone shows lost steps (some manufacturers kill background work aggressively), the "Keep Your Streak Alive" explanation is available under **Profile → Permissions** as an optional step, not as a required onboarding screen.
- **The emulator has no real step sensor** → add a debug-only "simulate +500 steps" button; test the real sensor on a physical phone (Android 10+).
- Alternative: **Health Connect** (reads steps from all apps), but it requires more setup. We use the sensor unless there is a strong reason to switch.

### Gamification rules

- **XP per day** = `steps / 100`, plus a **+50 bonus** if the goal is met.
- **Challenge XP:** fixed reward per challenge (e.g. +100, +150, +250), awarded once on completion.
- **Level** thresholds: 100, 250, 500, 1000, ... (or `level = floor(sqrt(totalXP / 50))`).
- **Level rewards** (the presentation mentions "Next reward unlocked at Level 5"): *to confirm* — proposal: extra mascots or mascot outfits unlocked at levels 5 and 10. If not implemented, remove the "rewards/achievements" wording from the slides and the Today screen.
- **Hearts (streak freezes):** earn 1 for every 7-day streak, holding at most 2. A missed day uses 1 heart instead of resetting the streak.
- **Challenges:** step-based only (the app cannot detect separate "walks"); at most 3 active at once. Examples:
  - *10K Steps Challenge* — reach 10,000 steps in one day (+100 XP)
  - *7-Day Streak* — keep the streak for 7 days (+150 XP)
  - *Weekend Walker* — walk 15,000 steps over Saturday and Sunday (+100 XP) *(reworded from "Complete 3 walks", which cannot be detected)*
  - *50K This Week* — reach 50,000 steps in one week (+250 XP)
  - *Morning Mover* — take 2,000 steps before noon (+120 XP), based on `stepsBeforeNoon` (accuracy ±15 minutes)

### Data backup and transfer

Since there is no server, data must be moved to a new phone manually. There are two options:

| | QR code transfer | JSON file backup |
|---|---|---|
| Use case | Old and new phone side by side | Old phone lost/broken, reinstall, long-term backup |
| History | Last **6 months** of daily steps | Full history |
| Streak, XP, level, hearts, active challenges | ✅ Kept exactly | ✅ Kept exactly |
| Needs | Both phones working | A saved file (e.g. in Google Drive) |

**1. QR code transfer (MVP)**
- Old phone: *Profile → Transfer to a new phone* shows **one** QR code.
- New phone: *Welcome → I already use Steppy*, or *Profile → Scan transfer code*, opens the scanner.
- **Payload** (compact JSON → Deflate → Base64):
  - format `version` and a checksum
  - a `ProgressBaseline` calculated on the old phone for the first day of the window (XP, streak, hearts and active days *before* that day)
  - the last ~183 days of `{ steps, goal }` (goal stored as change points, not per day), **including today's partial steps**
  - active challenges (0–3) with their activation dates
  - IDs of **completed** challenges, so one-time challenges are not offered again
  - settings: username, main goal, mascot, daily goal, reminder time
- **Size target:** ≤ ~1 KB, so it fits in a single, easy-to-scan QR code (the maximum is ~2.9 KB, but very dense codes are hard to scan from a screen). *To be verified with 6 months of test data in Week 10*; if it does not fit, reduce the window to 3 months.
- **After a transfer**, the Progress chart shows "History before \<date\> was not transferred" instead of empty bars that would look like zero steps.
- **Scanning:** Google Code Scanner (Play services) opens its own camera UI, so the app **does not need the `CAMERA` permission**. If Play services are unavailable, the user is directed to the JSON file option.
- **Generating:** ZXing `core` renders the QR bitmap locally. No network is involved at any point.

**2. JSON file backup / restore (MVP)**
- *Profile → Backup to file* saves a JSON file with the **full history** using the system file picker (Storage Access Framework — no storage permission needed).
- *Profile → Restore from file* restores it.
- Works even when the old phone is lost or broken, as long as the file was saved.

**Both imports** validate the data first (version, checksum, value ranges); invalid, corrupted or empty data shows an error and leaves existing data unchanged. They always ask for confirmation ("This will replace your current progress") before overwriting.

### Edge cases to test

- Goal changed in the middle of the day
- Missed day with 2, 1 and 0 hearts
- Device reboot in the middle of the day
- Crossing midnight
- Timezone change
- No records (new user) — empty states on Today and Progress
- Challenge completed on the same day it was activated
- Trying to activate a 4th challenge
- Importing an invalid, corrupted or empty JSON file (existing data must stay unchanged)
- Export → import round trip restores the same streak, XP and history
- QR transfer round trip: streak, XP, level, hearts, active days and active challenges match exactly on the new phone
- QR transfer with a streak longer than 6 months (must come from the baseline, not the records)
- QR transfer on a day with partial steps (today's steps are kept)
- QR import with wrong version or checksum, or a non-Steppy QR code
- 6 months of realistic test data fits in one QR code (size check)
- Completed one-time challenge is not offered again after transfer
- Notification permission denied → app works without reminders

---

## 6. Accessibility requirements

From the M1 presentation:

1. **AR1 — Minimum touch target of 48 × 48 dp** for all interactive elements.
   *For:* users with larger fingers, older adults with lower fine motor precision, people using the app while walking, people with motor diseases (e.g. Parkinson's).
2. **AR2 — Text and icon of key components are combined** into one accessibility node (Compose `Modifier.semantics(mergeDescendants = true)`), so TalkBack does not read the screen in fragments
   (e.g. "Today's steps: 7,842 of 10,000, 78 percent", "2 streak freezes left").
   *For:* blind users and people with visual impairments who use TalkBack.
3. **AR3 — Colour-independent statuses:** goal met vs. missed (Progress chart), level-up badges and hearts also use a shape, icon or text label, never colour alone.
   *For:* colour-blind users, people viewing the screen in direct sunlight, people with cognitive disabilities.

Additional check (mentioned on the "Mobile for Social Good" slide):

4. **Readable enlarged text:** the app remains usable at **200% font size** (no clipped or overlapping text, including next to the mascot images).

---

## 7. Privacy

- Data collected: daily step counts (total and before noon), goal, username, main goal, activity level, mascot choice, reminder time, challenge progress.
- No full names, emails, location, weight, or other health data in the MVP.
- All data stored locally (Room/DataStore); the app has no network access at all (no `INTERNET` permission).
- "Backup to file" saves the user's data to a file they choose; nothing is uploaded by the app.
- "Restore from file" and QR import validate the data first and ask for confirmation before replacing existing data.
- The QR transfer code contains the user's last 6 months of steps and their progress: it is only shown after the user taps "Transfer", with a note to scan it only with their own phone.
- A "Delete my data" option in Profile removes all local data.
- Demonstration data uses fictional usernames and is labelled as test data.
- If weight/BMI are added later, they count as sensitive health data and must be optional, stored only locally, and included in the deletion option.

---

## 8. Prototype (Canva) — fixes needed

Found when comparing the "Mobile System" mockup with this plan:

| Screen | Issue | Fix |
|---|---|---|
| Step permission | Only "Allow access" — no denial path | Add "Not now" (→ manual step entry) |
| Battery ("Keep Your Streak Alive") | Required onboarding step | Move to Profile → Permissions as optional |
| Setup | "Intenso (10,000 pasos)" | "Intense (10,000 steps)" |
| Progress | Friday's bar is marked by orange colour only (breaks AR3) | Add an icon or label for goal met / missed |
| All main screens | Bottom navigation always highlights "Today" | Highlight the current tab |
| Today / Progress | Mixed number formats ("7.842" vs "10,000") | Use one format consistently |
| Challenges | "Weekend Walker — Complete 3 walks" | "Walk 15,000 steps this weekend" |
| Profile | "Export to CSV" | Replace with "Transfer to a new phone (QR)", "Backup / restore from file", reminder time, "Delete my data" |
| Missing | Reminder, Streak lost and Empty state screens | Add them to the prototype |
| Today | "Next reward unlocked at Level 5" | Keep only if level rewards are confirmed |

---

## 9. Week-by-week plan

| Week | Focus | Concrete output |
|---|---|---|
| 1 | Setup | Compose starter app runs; GitHub repo + README with team and idea |
| 2 | Brief | User group, competitor comparison, 3 user stories, MVP + out-of-scope lists |
| 3 | Prototype | Canva screens incl. feedback states; accessibility requirements; peer test with another team |
| 4 | **M1** | Presentation; confirm networking approach |
| 5 | User journey | Setup → Today → Challenges with fake in-memory steps + ViewModels; streak/hearts, XP/level and challenge logic **with unit tests**; first `AI_USE.md` entry |
| 6 | Local storage | Room + DataStore; data survives restart and rotation; JSON backup / restore; DAO, goal-change and backup round-trip tests |
| 7 | Networking | Separate small networking exercise (outside the app); built-in challenge catalogue in the app |
| 8 | Sensor & notifications | Real step counter + permission + manual-entry fallback; daily reminder + denial path; tested on a physical phone; reboot and day-rollover tests |
| 9 | **M2** | Demo including denied permission and airplane mode (app works fully offline) |
| 10 | Privacy & security | Data checklist; "Delete my data"; repository and permissions review; QR transfer + payload size check |
| 11 | Testing | Fill gaps in the unit tests written since Week 5 (edge cases from §5, QR encode/decode); manual checklist; test summary; one documented bug fix |
| 12 | Usability | 2 anonymised sessions — task: "complete setup and find today's steps and streak" (60 s criterion) |
| 13 | Refinement | Fixes from usability testing; WorkManager/battery check; feature freeze |
| 14 | Release | Teammate builds from README; source ZIP; complete `AI_USE.md` and test summary |
| 15 | **M3** | Demo day and reflection |

---

## 10. Team responsibilities

| Role | Responsibilities | Member |
|---|---|---|
| **A — UI/UX & Accessibility** | Compose screens, navigation, prototype, accessibility requirements, usability sessions | *to assign* |
| **B — Data & Logic** | Room, DataStore, streak/hearts/XP/challenge logic, backup format, unit tests | *to assign* |
| **C — Platform & Network** | Step sensor, permissions, WorkManager, notifications, networking exercise, QR transfer | *to assign* |

Every member should contribute to every layer at least once, because the individual code defence covers the whole project.

---

## 11. Main risks

| # | Risk | Mitigation |
|---|---|---|
| RS1 | Losing a long streak after an off day demotivates users | Hearts / streak freezes (1 per 7-day streak, max 2) |
| RS2 | Data lost when switching phones, uninstalling or clearing app data (no server) | QR code transfer between phones (progress + last 6 months); JSON file backup / restore (full history) |
| RS3 | The sensor returns steps since the last reboot, not daily steps | Daily baseline: `todaySteps = current - baseline`; reboot detection |
| RS4 | The OS kills background work or the device reboots, so steps for the day may be lost | Hardware sensor keeps counting; 15-minute snapshots; optional battery explanation in Profile → Permissions if testing shows it is needed |
| RS5 | QR payload too large or hard to scan | Only 6 months of history + a progress baseline, compressed; reduce to 3 months if needed; JSON file as fallback |
| RS6 | Transferred streak/XP wrong because old records are missing | `ProgressBaseline` carries XP, streak, hearts and active days from before the window; round-trip unit tests |
| — | Scope creep (social features, extra screens) | Sharing and leaderboard explicitly out of scope |
| — | Emulator cannot count steps | Debug "simulate steps" button + one physical Android 10+ phone |
| — | Day-boundary / reboot bugs | Pure-function logic with unit tests from Week 5, written alongside each feature |
| — | Week 7 networking requirement not met by a local-only app | Separate small networking exercise, as the course allows |
| — | Overclaiming social impact | Claim "motivates users to build a daily walking habit", not "reduces obesity" or "reduces sedentary behaviour" |

---

## 12. Bibliography

- World Health Organization. *Global status report on physical activity 2022*. Geneva: World Health Organization; 2022. https://www.who.int/publications/i/item/9789240059153
