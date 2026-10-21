# Numbers course

A course that drills writing numbers as Polish words. The learner types the numbers to
practise; each training shows one of them in digits and asks for the words — as a plain
number, as an ordinal, or as a point in a list.

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
every number, because progress belongs to the number and its form, not to the choice.

Parsing is `NumberSelection.parse`, in `interactors.numbers`.

## Forms

Every chosen number is practised in each form it has (`NumberKind`, `NumberForm.formsOf`):

| Form | Shown as | Asked for | Which numbers |
| --- | --- | --- | --- |
| cardinal | `21` | *dwadzieścia jeden* | all |
| ordinal | `21st` | *dwudziesty pierwszy*, *dwudziesta pierwsza*, *dwudzieste pierwsze* | 1 to 999 999 999, except round thousands above 1 000 and round millions |
| list point | `secondly` | *po drugie* | 1 to 10 |

**Genders** (`NumberGender`, `PolishNumbers.hasGenders`). A form whose words change with the
noun is asked three times, masculine, feminine and neuter, and the prompt names the gender:

- every ordinal: *pierwszy / pierwsza / pierwsze*, and in a longer ordinal every ordinal
  word changes (*dwudziesta pierwsza*);
- the cardinal 1: *jeden / jedna / jedno*;
- cardinals ending in 2, except those ending in 12: *dwa / dwie / dwa*, also inside longer
  numbers (*dwadzieścia dwie*, *dwieście dwie*). The neuter is asked even though it matches
  the masculine.

Other cardinals and list points have one form. Round thousands and millions above 1 000
(*dwutysięczny*, *milionowy*) are left out: they are built as one compound word, a separate
system. *Po jedenaste* and higher exist but are rare, so list points stop at ten. Choosing
`3-4` therefore gives ten forms to master: per number one cardinal, three ordinals and one
list point.

## A training step

- The number is shown large, in digits, grouped in threes from 10 000 up (`2024`,
  `12 345`); an ordinal takes its English suffix (`11th`, `21st`, `2024th`), and a list
  point is shown as its English adverb (`secondly`). The labels come from
  `strings.xml` (`NumberLabel.kt`); `PolishNumbers.digits` is only the grouped number.
- The prompt above the field names the form without giving examples that answer it, and the field has focus with the keyboard
  open as soon as the step appears. The keyboard's Done key checks the answer.
- A play button beside it is always there, before and after the check. It reads the
  number aloud with the app's voice.
- The learner types the words. Capitals, spaces and punctuation do not matter; a slip in
  the Polish letters only (`dwanascie`) is "almost", as everywhere else.
- After **Check** the screen shows the correct words when the answer was wrong, with the
  letters the learner got wrong in bold; the IPA transcription either way, in square
  brackets like every transcription in the app: `[dvaˈnaɕt͡ɕɛ]`; and **How it is built**,
  the rules that make up this number (`PolishNumbers.rules`, `NumberRule`).
- The results screen marks the wrong letters of each missed answer the same way.

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

Ordinals and list points:

| Part | Rule | Example |
| --- | --- | --- |
| 1st–19th | one word each | *pierwszy, drugi, piętnasty* |
| tens | *-dziesty* (20–40), *-dziesiąty* (50–90); both words are ordinal | *dwudziesty pierwszy*, *pięćdziesiąty* |
| round hundreds | one word | *setny, dwusetny, trzechsetny* |
| other hundreds | the hundreds stay cardinal, only the end is ordinal | *sto pierwszy*, *dwieście dwudziesty* |
| 1000th | one word | *tysięczny* |
| above 1000 | thousands and millions stay cardinal, only the end is ordinal | *dwa tysiące dwudziesty czwarty* |
| feminine | the ordinal ends in *-a* (*-gi* → *-ga*, *-ci* → *-cia*) | *pierwsza, druga, trzecia* |
| neuter | the ordinal ends in *-e* (*-i* → *-ie*) | *pierwsze, drugie, trzecie* |
| list points | *po* + the neuter ordinal | *po pierwsze, po drugie, po trzecie* |

**Transcription.** Every number is built from a small set of words, and the transcription
is those words' IPA joined with spaces. Feminine and neuter ordinals take the masculine IPA
with the last vowel changed (*ɨ* → *a* or *ɛ*); *druga, drugie, trzecia, trzecie, jedna,
jedno* and *dwie* are written out. Joining word IPA is what `tools/vocabulary/g2p.py` does
for a phrase too. The table in `PolishNumbers` was made with `g2p.py`, with six words corrected by
hand where the transcriber is wrong: *piętnaście, szesnaście, dziewiętnaście,
pięćdziesiąt, sześćdziesiąt, dziewięćdziesiąt*.

## Progress

- **Mastered**: a form whose last answer was right — one correct answer is enough
  (`NumberProgress.MASTERY_STREAK`, the shared `SINGLE_ANSWER_MASTERY_STREAK`). A wrong
  answer later takes it back until it is answered right again.
- **Next form**: one not asked yet in this session, not mastered, and tried the fewest
  times, picked at random among equals (`nextToPractise`, shared with the
  [Countries course](countries.md)). When every form is mastered, the training keeps
  going over all of them rather than stopping.
- The dashboard card shows forms mastered out of all forms of the chosen numbers, with
  chips for the count of numbers and how many forms have been tried
  (`DrillCourseProgress`, shared with the Countries course).

## Where it lives

| Layer | Code |
| --- | --- |
| Words, transcription, parsing, contracts | `interactors.numbers` — `PolishNumbers`, `NumberSelection`, `NumberQuestion`, the use cases |
| Use cases | `application.numbers.NumberUseCasesImpl` |
| Port | `boundary.NumberRepository` |
| Storage | `data.local.NumberEntities` — tables `number_course` (the typed text, one row) and `number_progress` (one row per number and form tried, keyed by value and `kind`) |
| Screens | `presentation.numbers` — `NumberCourseScreen` (the choice), `NumberTrainingScreen` |
| Entry points | the **Numbers** tile on the Plan tab and the course card on the dashboard |

Android only for now; see [ios/README.md](../ios/README.md).
