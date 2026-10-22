# Cases course

A course that drills the seven cases of Polish nouns and adjectives. The learner marks the
nouns and adjectives to practise; each training shows one word and asks for its forms, one
case at a time, as a table.

Like the conjugation course, it is built from the study set: there is no separate choice to
save. Marking a noun or adjective to learn or favourite is what puts it in the course, and
changing its status takes it out.

## Choosing the words

Plan → **Cases** opens *Choose words*, a searchable list of every word the course can
teach, each with the usual status button. A word is listed when the vocabulary carries
grammar that lets the course teach it (see *The tables*). Search matches the Polish word
and its English gloss, ignoring case and Polish letters (`foldForSearch`), and the list
loads a page at a time.

The training opens from the course card on the dashboard, which appears once something is
marked. If the screen is reached with nothing marked, or with every form mastered, it says so
instead of starting.

## The tables

A word is asked as one table per **column**, and a table has a row for each case it has a
form in (`DeclinableWord`, `DeclensionColumn`):

| Word | Columns |
| --- | --- |
| noun | singular, plural; a plural-only noun has the plural alone |
| adjective | masculine, feminine, neuter, plural for men, other plural |

The cases are nominative, genitive, dative, accusative, instrumental, locative and
vocative (`GrammaticalCase`).

Where the forms come from:

- **Nouns**: the declension in `vocabulary_pl.json`, written by `tools/vocabulary/build_grammar.py`
  from SGJP, the Grammatical Dictionary of Polish. A form is never invented in the app; a
  case the asset leaves out is not asked. A noun is taught only when its nominative is the
  word itself.
- **Adjectives**: built from the five nominative forms by `PolishAdjectives.decline`. An
  adjective is taught from its masculine form only, and only when it is regular: one word
  ending in *-y* or *-i*, with a feminine in *-a* and a neuter in *-e*. Others are not
  offered, rather than taught wrongly.

The book icon at the top of the list and of a training opens *Przypadki*, a reference page on
the cases that ships as `rules.json` (`ReferenceRule`).

## A training step

- The word is shown with a picture (or the word alone when there is none), its translation,
  its part of speech and, for a noun, its gender, and its IPA transcription. The example
  sentence appears after the check.
- Every case of the column is a field in the table. **Check** is on once every field has
  text, and checks them all together.
- Answers are checked as everywhere else (`LessonAnswerChecker`): capitals, spaces and
  punctuation do not matter, and a slip in the Polish letters only is "almost", which is recorded as wrong.
  A case may accept more than one form.
- A play button beside each row reads the form aloud. After the check each row shows right
  or wrong, with the correct form for a wrong one.
- A training has as many steps as the *Steps per session* setting (ten by default), then the
  usual results screen, which lists every case asked, marked right or wrong. The step
  counting and results are `StepSession`, shared with the conjugation, numbers and countries
  courses.

## Progress

- The unit is a **variant**: one word, one column and one case (`DeclensionVariant`).
- **Mastered**: a variant answered correctly twice in a row (`TABLE_DRILL_MASTERY_STREAK`,
  shared with the conjugation course). A wrong answer resets the streak.
- **Next table**: among the words in the study set, a column that is not fully mastered,
  of a word not yet asked this session, tried the fewest times, at random among equals
  (`NextDeclensionTableUseCase`, through `nextToPractise`). Once every word has been asked,
  asked words count again. When every table is mastered, the training ends. Verb
  Conjugation picks its next verb the same way (`NextConjugationTableUseCase`).
- The dashboard card shows variants mastered out of all variants of the words in the course
  (`DeclensionCourseProgress`).

## Where it lives

| Layer | Code |
| --- | --- |
| Model and contracts | `interactors.declension` — `DeclinableWord`, `DeclensionColumn`, `DeclensionVariant`, `PolishAdjectives`, the use cases |
| Use cases | `application.declension.DeclensionUseCasesImpl` |
| Port | `boundary.DeclensionRepository` |
| Storage | `data.local.DeclensionEntities` — table `declension_progress` (one row per word, column and case tried, keyed with the course id); the words and their forms are the vocabulary's own |
| Screens | `presentation.declension` — `DeclensionWordsScreen` (the list), `DeclensionScreen` (the training); `presentation.rules.ReferenceRuleScreen` for *Przypadki* |
| Entry points | the **Cases** tile on the Plan tab and the course card on the dashboard |

Android only for now; see [ios/README.md](../ios/README.md).
