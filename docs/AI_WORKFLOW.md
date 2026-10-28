# Claude workflow: code, review, tests, services

Approved by the owner on 2026-10-28. `CLAUDE.md` is the short version every session reads;
this file holds the details. The owner's decisions in chat outrank both. Numbers marked
"[Claude]" were proposed by Claude and approved by the owner with this file.

## 1. Who runs a task

| Runner | When | Not when |
|---|---|---|
| **Main chat** | a task of a few calls; decisions, discussion; small edits | long mechanical work that bloats the context |
| **Agent** | a self-contained piece with no questions to the owner, whose result the main chat needs to continue | the owner's decisions are needed midway |
| **Session** | the owner's decisions are needed midway, or the task has its own outcome (branch, PR, series of edits) | a piece the main chat cannot continue without |

- One task at a time. Parallel agents or sessions only when the owner asks.
- A session that writes to the repo while another works uses its own `git worktree` and its
  own files; it never switches the branch of the shared checkout.
- Feature size decides the stages:
  - small fix: main chat, module tests, quick self-review;
  - medium: design inline → one agent writes failing tests, then code → one review agent;
  - large: feature doc → owner's approval → design agent → tests agent → implementation agent
    → review + emulator QA agents → fixes → docs → PR.

## 2. Models and effort

| Task | Runner | Model | Effort |
|---|---|---|---|
| Decisions, owner questions | main chat | Opus | high |
| Feature doc; medium technical design | main chat | Opus | high |
| Large technical design | agent | Opus | high |
| Tests before code | agent | Opus | medium |
| Implementation: ordinary | agent | Opus | medium |
| Implementation: complex logic (SM-2 schedule, course run, DB migrations, sync, races) | agent | Opus | high |
| Code review | agent | Opus | high |
| QA on the emulator | agent | Sonnet | medium |
| Web research (licences, prices, models) | agent | Sonnet | medium |
| Small code and doc edits | main chat | — | as the chat |
| Doc edits from accepted decisions | agent | Sonnet | low |
| Code and doc search | agent | Sonnet | low |
| File moves and renames, mechanical edits | agent | Sonnet | low |
| ChatGPT in the browser: send, wait, download | session | Sonnet | low |
| Judging generated content or images | main chat | Opus | high |
| Windows PC setup and batch runs (TTS, images, local LLM) | session | Sonnet | medium |

Model and effort are set explicitly on every agent or session start, by this table. The
main session itself stays on Opus, high effort.

## 3. Brief for an agent or session

Required fields:
1. **Goal** — one sentence.
2. **Scope** — files and folders that may change; what must not be touched.
3. **Inputs** — the feature doc (path and section), the design, the owner's decisions.
   Link `CLAUDE.md` and this file instead of copying their rules.
4. **Done when** — checkable: tests, files, exit codes.
5. **Report** — the format in §11.
6. **Forbidden** — commit, push, PR, branch switch, deleting files, numbers not in the doc,
   anything iOS.

## 4. Writing code

1. Read the feature doc (its decisions section first), `docs/architecture.md` and the
   neighbouring code.
2. Branch from fresh `development`; a `git worktree` when another session uses the checkout.
3. Failing tests first, then code until they pass (§5).
4. Kotlin Multiplatform:
   - shared logic in `commonMain`; business rules in `model`, orchestration in `application`;
   - `expect`/`actual` only when a platform API is unavoidable; prefer designs with no new
     `expect` member, because `iosMain` stays untouched while iOS is paused.
5. Data:
   - words, courses, examples and pictures live in assets built by `tools/`; no content in code;
   - one value in one place; existing numbers change only when the owner asks;
   - new numbers are marked "[Claude]" and listed for the owner.
6. UI text only in `presentation/src/main/res/values/strings.xml`.
7. Database: destructive version bumps are fine before release; take the next free version.
8. Licences: NC licences are allowed (the app is non-commercial); check the licence before
   downloading, record it in the PR, credit data sources in Settings › Sources.
9. Commits: small, finished, English, with the trailer
   `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`; never `git add -A` over
   others' files; never commit `gradle.properties`.

## 5. Tests

| When | Command |
|---|---|
| While working: affected module or class | `./gradlew -q :<module>:testDebugUnitTest --tests '<pattern>'` |
| Python tools | `python3 tools/<dir>/test_<name>.py` |
| One instrumented class | `./gradlew :<module>:assembleDebugAndroidTest`, `adb install -r -t` the test APK, `adb shell am instrument -w -e class <fqcn> <test package>/androidx.test.runner.AndroidJUnitRunner`, then uninstall it (fallback while `connectedDebugAndroidTest` skips the API 37 emulator) |
| End, once | `./gradlew -q check --rerun-tasks`, exit code 0 (tests that read asset files are skipped without `--rerun-tasks`) |
| Before a PR | the full run above, exit code checked |

- A test fails before the code and passes after it. A test that passes at once proves
  nothing and is rewritten.
- Hardware-key and timing behaviour is proven by a Compose instrumented test, not by adb
  key events.
- Expected values in existing tests change only where the feature requires it; each change
  is listed in the report.
- iOS tests and the simulator are not run while iOS is paused.

## 6. Code review

- **Scope:** only the branch's changes, `git diff development...HEAD`.
- **Checklist:**
  1. correctness: empty lists, null, day boundaries, time zones, double taps, ordering;
  2. matches the feature doc and the owner's decisions;
  3. SM-2 rules: only a word's first answer of the day moves the schedule;
  4. DB version and migration;
  5. content in assets, no magic numbers, one value in one place;
  6. UI text in `strings.xml`; no Russian;
  7. reuse instead of duplicates; no comments;
  8. tests cover the acceptance criteria;
  9. licences of new files and libraries.
- **Finding format:** severity (bug / risk / cleanup) · `file:line` · failure scenario
  (input → wrong result) · fix.
- Each bug is verified before the report: read the code around it, reproduce it with a test
  where possible. Unverified findings are marked "possible".
- **The reviewer does not edit code.** Fixes follow the main chat's decision.
- A large feature gets two reviews: the main chat, then an independent agent without the
  implementation context.

## 7. QA on the emulator

- The scenario comes from the acceptance criteria: start, steps, expected result per item.
- Always `ANDROID_SERIAL=emulator-5554`; never install on the owner's phone.
- Read screens as text first (`uiautomator dump`, re-dumped after every action, since dumps
  go stale); screenshots only for layout questions, scaled to about 900 px, kept in the
  scratchpad; only the needed ones go to the owner.
- Always check: no raw keys or placeholder text, numbers without a stray ".0", no errors in
  logcat.
- Report each item: passed / failed / not checked, with the reason.

## 8. Docs

- English; owner decisions recorded with a date; Claude numbers marked "[Claude]".
- Every edit cleans up at once: decisions written down, old text fixed, contradictions
  resolved, stale parts removed.
- Edits from one discussion go into one branch and one PR at the end.
- When referring to a section or a question, show its content in the chat.

## 9. ChatGPT (browser, Claude in Chrome)

1. **Choice of path.** Bulk content (hundreds of items) goes through the OpenAI API scripts
   in `tools/` (Batch API where waiting is fine) or a local model on the Windows PC: a
   script costs Claude almost no tokens. The browser is for small interactive jobs: grammar
   explanations, choosing a picture style, a few dozen items, store texts.
2. **Access** only with the owner's permission for the task. Work only in the ChatGPT
   project "lexicon", in **one** tab: parallel tabs spoil drafts.
3. **Prompts** are stored in the repo (`tools/prompts/<topic>.md`); one batch of prompts per
   chat; answers are saved to files, not left only in the chat.
4. **Waiting:** read page state as text (`get_page_text`, `find`), not screenshots; readiness
   checks in loops shorter than 45 s.
5. **Judging:** screenshots or image reads only to judge a result; technical checks (size,
   format, the word present in a sentence) by script.
6. **Checking content:** generated Polish text gets a second opinion from another model
   before it ships; nothing overwrites hand-written content; changes go in as a reviewed diff.
7. **Usage log:** every 5 operations write down the weekly Claude usage percent
   (`get_usage`) outside the repo (scratchpad or the session's notes folder) and report the
   totals, to compare with the OpenAI API price per request.
8. **At the end:** archive the task's chats and give the owner a list of chats to delete.
9. Rights: the user owns ChatGPT output under OpenAI's terms, without copyright protection
   guarantees; no real people, brands or other artists' characters in images.

## 10. The owner's Windows PC and the cloud

**Windows PC (`ssh trader-pc`):**
- Hardware (checked 2026-10-28): RTX 4070 Ti SUPER 16 GB VRAM, i5-13600K (20 threads),
  32 GB RAM, Windows 10 Home; D: has about 1.3 TB free.
- Access: `ssh trader-pc` from the Mac (key `~/.ssh/trader_pc_ed25519`, local network only).
  Never print or copy the key. The shell is `cmd`; send PowerShell scripts through
  `powershell -NoProfile -Command -` on stdin.
- Work only in `D:\lexicon\` (`tools`, `sdk`, `models`, `work`, `out`) [Claude]. Do not touch
  `D:\AI` or other Trader folders; ComfyUI for this project is its own copy in `D:\lexicon\`.
- What goes there:
  - full Gradle runs and Android builds when the Mac is busy (portable JDK 17 and Android
    SDK; code moves as a `git bundle` over `scp`, so the PC needs no GitHub access);
  - TTS batches for words and sentences (Piper, Chatterbox, XTTS-v2), checked by a
    speech-recognition round trip (Whisper large-v3 / faster-whisper);
  - image batches (FLUX.1 schnell, SDXL in ComfyUI) after the style is chosen;
  - a local LLM (Bielik 11B, Qwen3 14B, quantised to fit 16 GB) for bulk second opinions on
    content.
- Not on the PC: a self-hosted CI runner (the repo is public; code from others' PRs would run
  on the owner's machine) and, for now, the Android emulator (it needs a system change).
- The PC is the owner's personal computer:
  - no system settings, services, PATH changes or autostart; portable installs only;
  - installing programs or models needs the owner's consent per task;
  - before a GPU job check `nvidia-smi`; if Trader is using the GPU, do not start and tell
    the owner; warn the owner before GPU jobs longer than 30 minutes [Claude];
  - results go back to the Mac with `scp`; nothing is committed or pushed from the PC;
  - stop every started process when done and check with `Get-Process`.

**Cloud GPUs:** only for a job that does not fit in 16 GB VRAM, with the owner's approved
budget; show the plan and the cost before a run and stop the machine after it.

**Hugging Face, GitHub, websites:**
- Check a model's or asset's licence on its card or LICENSE file before downloading.
- Downloads go into a new empty folder; Python reading foreign files runs with `-I`.
- Never enter passwords, keys or payment data, never create accounts: the owner does that.
- Sending, publishing and buying only after the owner's yes.

## 11. Reports and answers

- An agent's or session's report is at most 300 words: done; not done or not verified;
  Claude numbers for approval; files; commit. Details go to a file.
- Answers to the owner: only what is new; questions one at a time, each with a default;
  show the content you refer to.
- One conversation per feature. When it ends or the context grows long, update the
  `open-work` memory and give the owner a ready-to-paste prompt for the next chat.

## 12. Code index

No code index for now: the code map in `CLAUDE.md` and `docs/architecture.md` plus search
are enough. Build an index once the code passes 150,000 lines or 1,500 files, or after three
tasks in a row where finding code took more than four search rounds [Claude].
