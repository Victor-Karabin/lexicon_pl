# Numbers course

A course that drills writing numbers as Polish words. The learner types the numbers to
practise; each training shows one of them in digits and asks for the words.

It works the way the conjugation course does: a choice made on the Plan tab becomes a
course with its own progress on the dashboard, and a training is ten steps followed by the
usual results screen.

## Choosing the numbers

Plan → **Numbers** opens one field. It takes single numbers and ranges, separated by
commas (semicolons and new lines work too):

```
1-20, 35, 100-110, 2024, 12 345
```

- A range is two numbers joined by `-`, `–` or `—`; a backwards range (`20-1`) is read
  the right way round. Repeats are counted once.
- A space inside a number is a thousands separator only when exactly three digits follow
  it: `12 345` and `1 000 000` are one number each. Anything else with a space in it —
  `5 7`, or a missing comma in `1-20 2024` — is reported as unreadable rather than
  silently joined.
- Numbers go from 0 to 999 999 999, and at most 100 000 of them at a time.

While typing, the field shows how many numbers it read, or what is wrong: nothing typed,
a part it cannot read (named), a number too large, or too many numbers. **Start** saves
the text as typed and opens the training. Changing the text later keeps the progress of
every number, because progress belongs to the number, not to the choice.

Parsing is `NumberSelection.parse`, in `interactors.numbers`.

## A training step

- The number is shown large, in digits, grouped in threes from 10 000 up (`2024`,
  `12 345`).
- A play button beside it is always there, before and after the check. It reads the
  number aloud with the app's voice.
- The learner types the words. Capitals, spaces and punctuation do not matter; a slip in
  the Polish letters only (`dwanascie`) is "almost", as everywhere else.
- After **Check** the screen shows the correct words when the answer was wrong, and the
  IPA transcription of the number either way, in square brackets like every transcription
  in the app: `[dvaˈnaɕt͡ɕɛ]`.

**Accepted answers.** The standard form, and the same with a leading *jeden* said aloud
before *tysiąc* or *milion*: both *tysiąc sto* and *jeden tysiąc sto* are right for 1 100.

## The words

`PolishNumbers` spells any number from 0 to 999 999 999 in the nominative masculine form:

| Part | Rule | Example |
| --- | --- | --- |
| 0–19 | one word each | *dwanaście* |
| tens | *-dzieścia* (20), *-dzieści* (30, 40), *-dziesiąt* (50–90) | *czterdzieści*, *pięćdziesiąt* |
| hundreds | *sto, dwieście, trzysta, czterysta, pięćset* … *dziewięćset* | *dwieście dwanaście* |
| thousands | 1 → *tysiąc*; ending in 2–4 (not 12–14) → *tysiące*; otherwise *tysięcy* | *dwa tysiące*, *dwanaście tysięcy*, *dwadzieścia jeden tysięcy* |
| millions | the same rule with *milion, miliony, milionów* | *trzy miliony*, *czternaście milionów* |

**Transcription.** Every number is built from 43 words, and the transcription is those
words' IPA joined with spaces — the same thing `tools/vocabulary/g2p.py` does for a
phrase. The table in `PolishNumbers` was made with `g2p.py`, with six words corrected by
hand where the transcriber is wrong: *piętnaście, szesnaście, dziewiętnaście,
pięćdziesiąt, sześćdziesiąt, dziewięćdziesiąt*.

## Progress

- **Mastered**: a number whose last answer was right — one correct answer is enough
  (`NumberProgress.MASTERY_STREAK`). A wrong answer later takes it back until it is
  answered right again.
- **Next number**: one not asked yet in this session, not mastered, and tried the fewest
  times, picked at random among equals. When every chosen number is mastered, the
  training keeps going over all of them rather than stopping.
- The dashboard card shows numbers mastered out of the numbers chosen, with chips for the
  count and how many have been tried.

## Where it lives

| Layer | Code |
| --- | --- |
| Words, transcription, parsing, contracts | `interactors.numbers` — `PolishNumbers`, `NumberSelection`, `NumberQuestion`, the use cases |
| Use cases | `application.numbers.NumberUseCasesImpl` |
| Port | `boundary.NumberRepository` |
| Storage | `data.local.NumberEntities` — tables `number_course` (the typed text, one row) and `number_progress` (one row per number tried) |
| Screens | `presentation.numbers` — `NumberCourseScreen` (the choice), `NumberTrainingScreen` |
| Entry points | the **Numbers** tile on the Plan tab and the course card on the dashboard |

Android only for now; see [ios/README.md](../ios/README.md).
