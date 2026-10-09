# Project Plan — Step Streak App

> A "Duolingo-style" motivation system for walking: set a daily step goal, keep your streak alive, complete achievable challenges, level up, and share your progress with friends.

Course: *Introduction to Mobile Systems — Mobile for Social Good*

---

## 1. Problem definition

**Social problem:** According to the World Health Organization (WHO), obesity is a chronic disease that has become a major global public health challenge. One important contributing factor is **physical inactivity**, which is increasingly common due to modern lifestyles: prolonged screen time, desk-based jobs, and the widespread use of cars and other forms of transportation.

Our app addresses the physical-inactivity side of this problem by motivating people to **walk every day**.

**Target user group (specific):**
> People who spend most of the day sitting (working, studying) and want a low-effort nudge to walk more.

This includes office workers and students. Students are the easiest part of this group to reach for the Week 12 usability sessions (classmates are allowed); ideally at least one session is with someone who works at a desk.

### Evidence vs. assumptions

| Type | Statement | Source / how to verify |
|---|---|---|
| Evidence | Obesity is a chronic disease and a major global public health challenge | WHO Obesity and overweight fact sheet (cite properly) |
| Evidence | Physical inactivity is a contributing factor to obesity | WHO Guidelines on Physical Activity and Sedentary Behaviour (cite properly) |
| Evidence | Physical activity recommendations for adults | WHO Guidelines on Physical Activity and Sedentary Behaviour (cite properly) |
| Evidence | Relation between daily step counts and health outcomes | Published studies — cite real papers, do not invent numbers |
| Assumption | Streaks motivate users to keep a daily habit | To be observed in Week 12 usability sessions |
| Assumption | Small, achievable challenges are more motivating than a single fixed goal | To be observed in Week 12 usability sessions |
| Assumption | Social comparison with friends increases engagement | To be observed / discussed as a limitation |

### Existing solutions

| App | What it does | Our difference |
|---|---|---|
| Google Fit | Tracks steps and physical activity; only lets users set goals and monitor progress | Ours focuses on **motivating** users to walk every day through a streak system and achievable challenges |
| Samsung Health | Full health dashboard | Ours is lightweight, no hardware ecosystem needed |
| Pacer / StepsApp | Step counting, challenges | Ours uses a Duolingo-style streak + level loop |
| Sweatcoin | Rewards steps with currency | Ours rewards with streaks/levels, no monetisation |

### Ethical considerations

- **Streak anxiety:** losing a long streak can be discouraging → add a *streak freeze*.
- **Inclusivity:** step counting excludes wheelchair users → documented as a known limitation.
- **No shaming:** messages are encouraging, never negative.
- **No weight stigma:** although the motivation is obesity, the app never asks for weight or BMI and never mentions body size — it only talks about walking.

---

## 2. MVP scope

### Main user journey

> **Set a daily goal → walk → see progress update and today's challenge → reach goal / complete challenge → streak +1 and XP/level up → share it.**

### Screens (4–5)

1. **Onboarding / Goal setup** — choose a daily goal (4k / 6k / 8k / custom) and request the activity permission.
2. **Today (home)** — progress ring, step count, current streak 🔥, today's challenge card, level and XP bar, Share button.
3. **History** — calendar/list of past days (goal met / missed), best streak.
4. **Leaderboard / Friends** — friend codes and weekly ranking *(networking feature)*.
5. **Settings** — change goal, reminder time, delete all data.

### Out of scope

- Email/password accounts
- Chat or messaging
- GPS route tracking / maps
- Calorie estimation
- Wearable integration
- iOS version
- Achievements/badges beyond levels and challenges
- User-created or friend-vs-friend challenges
- AI features inside the app

### User stories and acceptance criteria

**US1 — Set a goal**
*As someone who sits most of the day, I want to set a daily step goal so that I have a clear, achievable target.*
- Goal must be between 500 and 50,000 steps.
- Invalid input shows a clear error message.
- The goal is preserved after closing and reopening the app.

**US2 — Track progress and streak**
*As someone who sits most of the day, I want to see today's steps and my streak so that I know how close I am to my goal.*
- The step count updates while the app is open.
- The streak increases only on days where steps ≥ goal.
- Missing a day resets the streak, unless a streak freeze is available.

**US3 — Complete achievable challenges**
*As someone who sits most of the day, I want small daily challenges so that walking feels rewarding even on days when the full goal seems hard.*
- One challenge is shown on the Today screen each day (e.g. "Walk 1,000 steps before 12:00", "Beat yesterday by 500 steps").
- Challenge targets are scaled to the user's own goal and recent history, so they are always achievable.
- Completing a challenge awards bonus XP; failing one never breaks the streak.

**US4 — Share progress**
*As someone who sits most of the day, I want to share my streak with friends so that we can motivate each other.*
- The Share button opens the Android share sheet with a text summary.
- Sharing works without an account or network connection.

---

## 3. Course requirements mapping

| Requirement | Feature in our app |
|---|---|
| ViewModel / app state (W5) | `TodayViewModel` exposing `StateFlow<TodayUiState>` |
| Local persistence (W6) | **Room** for daily records, **DataStore** for goal and settings |
| Networking (W7) | Friend leaderboard (Firebase) or mock leaderboard API |
| Device capability (W8) | **Step counter sensor** (`TYPE_STEP_COUNTER`) + `ACTIVITY_RECOGNITION` permission |
| Permission fallback (W8) | Permission denied / no sensor → **manual step entry** (days marked as "manual") |
| Notifications (optional) | Evening reminder: "1,200 steps left to keep your 12-day streak" |
| Automated tests (W11) | Streak calculator, XP/level calculator, challenge generator, day-rollover logic |

---

## 4. Social / ranking feature — decision

A real leaderboard requires a backend, which is the biggest scope risk. Two tiers:

- **MVP (guaranteed):** share streak via the **Android system share sheet**. No backend needed.
- **Networking (W7):** **Firebase Firestore + Anonymous Auth + friend codes.** Each user gets a 6-character code; adding a friend's code shows a weekly leaderboard. No emails/passwords. Must handle loading, offline, error, and stale data ("last updated 2h ago").
- **Fallback if Firebase is too much:** fetch a fictional leaderboard from a mock JSON endpoint, clearly labelled as test data (the course allows a separate networking exercise).

**Decision deadline:** Week 4 (M1), presented as a project risk.

---

## 5. Technical design

### Stack

- Kotlin + Jetpack Compose
- Navigation Compose
- Room (daily records)
- DataStore (goal, settings)
- WorkManager (periodic step snapshots)
- Manual dependency injection (no Hilt, to keep it simple)
- Firebase Firestore + Anonymous Auth (leaderboard)

### Architecture

```
UI (Compose screens)
   ↓ observes StateFlow / sends events
ViewModels (TodayViewModel, HistoryViewModel, ...)
   ↓
Repositories (StepRepository, SettingsRepository, LeaderboardRepository)
   ↓
Data sources (Room DAO, DataStore, SensorManager, Firestore)
```

Domain logic (streak, XP, level, challenges) lives in **pure Kotlin functions** so it can be unit-tested without Android.

### Data model

```kotlin
@Entity
data class DailyRecord(
    @PrimaryKey val date: LocalDate,   // stored as ISO string
    val steps: Int,
    val goal: Int,                     // goal *on that day* — changing the goal must not rewrite history
    val source: StepSource             // SENSOR or MANUAL
)
```

The challenge for a given day is **generated deterministically** from the date, the goal, and recent records, so it does not need its own table.
Streak, XP, level, and challenge completion are **derived** from the records rather than stored, to keep them consistent and testable.

### Step sensor notes

- `TYPE_STEP_COUNTER` returns the total steps **since the last device reboot**, not today's steps.
  → Store a baseline at the start of each day: `todaySteps = current - baseline`.
- After a reboot, the counter resets to 0 → detect `current < baseline` and handle it.
- Background: a WorkManager job every 15 minutes saves a snapshot so the day closes correctly even if the app was not opened.
- **The emulator has no real step sensor** → add a debug-only "simulate +500 steps" button; test the real sensor on a physical phone (Android 10+).
- Alternative: **Health Connect** (reads steps from all apps), but it requires more setup. We use the sensor unless there is a strong reason to switch.

### Gamification rules

- **XP per day** = `steps / 100`, plus a **+50 bonus** if the goal is met, plus a **+25 bonus** if the daily challenge is completed.
- **Level** thresholds: 100, 250, 500, 1000, ... (or `level = floor(sqrt(totalXP / 50))`).
- **Streak freeze:** earn 1 for every 7-day streak, holding at most 2.
- **Challenges:** one per day, picked from a small set of templates (time-of-day target, beat-yesterday, reach X% of goal by a given hour). Targets never exceed the user's daily goal, so they stay achievable.

### Edge cases to test

- Goal changed in the middle of the day
- Missed day with and without a streak freeze
- Device reboot in the middle of the day
- Crossing midnight
- Timezone change
- No records (new user) — challenges fall back to fixed easy targets
- Challenge generated before a goal change in the middle of the day

---

## 6. Accessibility requirements

1. The app remains usable with **200% font size** (no clipped or overlapping text).
2. The progress ring and icons have **meaningful TalkBack descriptions**
   (e.g. "4,200 of 6,000 steps, 70 percent").

---

## 7. Privacy

- Data collected: daily step counts, goal, settings, and a random anonymous ID (if the leaderboard is used).
- No names, emails, or location.
- Data stored locally (Room/DataStore); only weekly step totals and a display nickname are sent to Firestore.
- A "Delete my data" option in Settings removes local and remote data.
- `google-services.json` and any credentials are kept out of the public repository where appropriate.

---

## 8. Week-by-week plan

| Week | Focus | Concrete output |
|---|---|---|
| 1 | Setup | Compose starter app runs; GitHub repo + README with team and idea |
| 2 | Brief | User group, competitor comparison, 3 user stories, MVP + out-of-scope lists |
| 3 | Prototype | Paper/Figma screens 1–5; two accessibility requirements; peer test with another team |
| 4 | **M1** | Presentation; decide Firebase vs. mock leaderboard |
| 5 | User journey | Goal → Today (with challenge card) → History with fake in-memory steps + ViewModels; first `AI_USE.md` entry |
| 6 | Local storage | Room + DataStore; data survives restart and rotation |
| 7 | Networking | Friend codes + leaderboard (or mock); loading/offline/error/retry states |
| 8 | Sensor | Real step counter + permission + manual-entry fallback; tested on a physical phone |
| 9 | **M2** | Demo including denied permission and airplane mode |
| 10 | Privacy & security | Data checklist; "Delete my data"; repository and permissions review |
| 11 | Testing | Unit tests (streak, XP, challenges, rollover); manual checklist; one documented bug fix |
| 12 | Usability | 2 anonymised sessions — task: "set a 5,000 goal and share your streak" |
| 13 | Refinement | Fixes from usability testing; WorkManager/battery check; feature freeze |
| 14 | Release | Teammate builds from README; source ZIP; complete `AI_USE.md` and test summary |
| 15 | **M3** | Demo day and reflection |

---

## 9. Team responsibilities

| Role | Responsibilities |
|---|---|
| **A — UI/UX & Accessibility** | Compose screens, navigation, prototype, usability sessions |
| **B — Data & Logic** | Room, DataStore, streak/XP/challenge logic, unit tests |
| **C — Platform & Network** | Step sensor, permissions, WorkManager, notifications, Firebase |

*(For a team of 2, merge roles B and C.)*
Every member should contribute to every layer at least once, because the individual code defence covers the whole project.

---

## 10. Main risks

| Risk | Mitigation |
|---|---|
| Backend scope creep | Share sheet in MVP; leaderboard is an add-on with a mock fallback |
| Emulator cannot count steps | Debug "simulate steps" button + one physical Android 10+ phone |
| Day-boundary / reboot bugs | Pure-function logic with unit tests from Week 6 |
| Overclaiming social impact | Claim "motivates users to build a daily walking habit", not "reduces obesity" or "reduces sedentary behaviour" |
