# Lexicon — rules for Claude sessions and agents

App for learning Polish, Kotlin Multiplatform, Android first (iOS paused). Main branch
`development`. Full workflow: `docs/AI_WORKFLOW.md`. Modules and layer rules:
`docs/architecture.md`; terms: `docs/domain/universal-ddd-language.md`; feature docs in `docs/`.
The owner's decisions in chat outrank these files.

## Communication
- Chat in the owner's language. Everything in the repo (UI, content, docs, code, commits, PRs)
  is English, with Polish only as the language being learned. No Russian in the repo.
- Short status; reports ≤ 300 words, details in files. Questions one at a time, each with a default.
- Ask before deciding anything the request leaves open (limits, ranges, display, behaviour).
  Mark self-invented numbers "[Claude]" and list them for approval.

## Where to look
- Feature code sits in same-named packages across layers, e.g. `course`, `vocabularycourse`,
  `presets`, `training`, `pronunciation`, `conjugation`, `countries`, `numbers`.
- `model` rules · `interactors` use-case contracts · `application` use cases · `boundary` ports ·
  `data` Room, assets, remote clients · `android` speech, audio, Google Cloud · `presentation`
  screens, ViewModels · `app` DI, navigation, UI tests · `common` clock, dispatchers, text
  folding · `shared` iOS framework (paused) · `baselineprofile` baseline profile.
- UI text: `presentation/src/main/res/values/strings.xml`. Content pipelines: `tools/` (Python).

## Code and data
- Extend the existing component; fold duplicates into it.
- No code comments; explanation goes to commits and PRs; silent failures get logs.
- Every UI string from `strings.xml`. Words, courses and content live in assets built by
  `tools/`, never in code.
- iOS paused: do not touch Swift, `ios/` or `*/src/iosMain`, do not fix or list iOS breakage.
  iOS work starts only when the owner orders the port of the Android changes to iOS or
  declares the project finished.
- DB: destructive version bumps are fine before release; take the next free version.
- Licences: the app is non-commercial; NC licences are allowed. Check the licence before
  downloading and credit every data source in Settings › Sources.

## Git
- Branch from fresh `development`; one PR per task, never stacked; the owner merges; open PRs
  with `gh pr create --base development` (`main` is old history: never branch from or target it).
- Commit, push and PR only when the owner asked. Gate every commit and push on the branch:
  `[ "$(git branch --show-current)" = "<branch>" ] && git commit …`; before pushing to an
  existing PR run `gh pr view <n> --json state`.
- Keep `git status` clean: ignore, untrack or discard strays at once.
- A hung `git push` or `gh` is killed and retried; run `gh pr list --head <branch>` before
  creating a PR again.

## Verification
- While working: only the affected module or class,
  `./gradlew -q :<module>:testDebugUnitTest --tests '<pattern>'`.
- Once at the end: `./gradlew -q check --rerun-tasks`, exit code 0. CI runs the same check on
  every PR and push to `development` (`docs/AI_WORKFLOW.md` §5); a PR is ready when it is green.
- Devices: always `ANDROID_SERIAL=emulator-5554`, never the owner's phone.

## Workflow, models and effort
| Task | Runner | Model | Effort |
|---|---|---|---|
| Decisions, feature questions, medium design | main chat | Opus | high |
| Large design; code review; complex logic (SM-2, course run, DB migrations, sync) | agent | Opus | high |
| Tests first; ordinary implementation | agent | Opus | medium |
| Emulator QA; web research | agent | Sonnet | medium |
| Doc edits from decisions; code search; mechanical edits; browser work | agent | Sonnet | low |

- Set model and effort explicitly on every agent or session. One task at a time; parallel
  agents or sessions only when the owner asks; a writing parallel session uses its own
  `git worktree`.
- An agent brief links this file instead of copying rules (`docs/AI_WORKFLOW.md` §3).
- Review only `git diff development...HEAD`; finding = severity · `file:line` · failure
  scenario · fix; verify each bug; the reviewer does not edit code.
- ChatGPT and the owner's Windows PC: `docs/AI_WORKFLOW.md` §9–10, each with the owner's OK per task.
- Never enter passwords, keys or payment data, never create accounts; sending, publishing and
  buying need the owner's yes. Before asking for access, check SSH config, key files and memory.
