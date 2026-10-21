# Countries course

A course that drills the Polish names of countries, the words for a man and a woman from
each, and their capitals. The learner ticks the countries to practise; each training shows
one of them as a flag and its English name and asks for the Polish.

It works the way the Numbers course does: a choice made on the Plan tab becomes a course
with its own progress on the dashboard, and a training is ten steps followed by the usual
results screen.

## Choosing the countries

Plan → **Countries** opens a checklist of all 197 states, in alphabetical order of their
English names. A row is a flag and the English name (`🇩🇪 Germany`); tapping it ticks or
unticks it.

- **Search** matches the English name and every Polish name, ignoring case, Polish letters
  and other accents (`foldAccentsForSearch`, built on the vocabulary search's fold):
  `bialorus` finds Belarus, `nauru` and `naoero` both find Nauru, `cote` finds Côte
  d'Ivoire and `sao tome` finds São Tomé and Príncipe. Capitals are not searched. Typing
  or clearing a search scrolls the list back to the top.
- **Select all** ticks the rows shown and **Clear** unticks the rows shown. With a search
  typed in, ticks outside the search stay as they were; with no search they act on every
  country.
- A line above the list counts what is ticked. **Start** is off while nothing is ticked.
  The first time, nothing is ticked.

**Start** saves the choice and opens the training. Changing the choice later keeps the
progress of every country, because progress belongs to the country and its field, not to
the choice. The choice is stored in list order; a code that is not in the list is dropped,
and an empty choice means no course.

## A training step

- The flag is shown large with the English name of the country beside it.
- The learner fills one field per row: **Country** (the Polish name), **Man** and **Woman**
  (the nationality), and **Capital** (the Polish name of the capital). The English name of
  the capital is the grey placeholder of the Capital field, and is gone once the learner
  types. A country whose Polish spelling is the same as the English one is still asked.
- The keyboard opens on the first field at each step, and its Done key checks the answers.
  **Check** is on only when every field of the country has text.
- After **Check** each row shows right or wrong; for a wrong one the correct answer appears
  with the letters the learner got wrong in bold. A play button beside each row reads the
  answer aloud with the app's voice.
- A step is one country with all its fields; a training is ten steps, and a country is not
  asked twice in one training while unasked ones remain.
- The results screen lists every field of every country asked, marked right or wrong, with
  the country's English name and the field.

## Accepted answers

Answers are checked as everywhere else (`LessonAnswerChecker`, whole answer):

- Capitals, spaces and punctuation do not matter, so `Saint John's` matches `Saint John’s`
  and `Papua Nowa Gwinea` matches `Papua-Nowa Gwinea`.
- A slip in the Polish letters only (`Bialorus`) is "almost", shown in the warning colour
  and recorded as wrong.
- Other accents are required: `Brasilia` for *Brasília*, or *Reykjavik*, *San Jose*,
  *São Tomé* and *Lomé* without their accents, is wrong.
- **Naoero** is also accepted as **Nauru**: the country field takes either.
- An empty field is wrong.

## The states without a nationality

The official list gives no words for a man or a woman from 15 states: Antigua and Barbuda,
Barbados, Bosnia and Herzegovina, Cabo Verde, Cook Islands, Côte d'Ivoire, Kosovo, Marshall
Islands, Niue, Saint Kitts and Nevis, Saint Lucia, Saint Vincent and the Grenadines,
Solomon Islands, São Tomé and Príncipe, and Trinidad and Tobago. They are asked two fields,
the country and the capital: the screen shows two rows, **Check** needs two, and only two
attempts are recorded. So the 197 countries give 182 × 4 + 15 × 2 = 758 fields to master.

## Progress

- **Mastered**: a field whose last answer was right — one correct answer is enough, the
  same shared constant as in the Numbers course (`SINGLE_ANSWER_MASTERY_STREAK`). A wrong
  answer later takes it back until it is answered right again.
- **Next country**: one not asked yet in this training, not fully mastered (some field
  still not mastered), and with the fewest steps so far, picked at
  random among equals (`nextToPractise`, shared with the Numbers course). When every field
  of every chosen country is mastered, the training keeps going over all of them rather
  than stopping.
- A country's steps are counted from its Country field, which every country has, so a
  country with two fields gets as many turns as one with four.
- Every field of the country is checked and recorded at each step, whether right or wrong.
- The dashboard card shows fields mastered out of all fields of the chosen countries, with
  chips for the count of countries and how many fields have been tried
  (`DrillCourseProgress`, shared with the Numbers course).

## Data

`data/src/androidMain/assets/countries.json` has one entry per state: ISO code, English
and Polish names (a list, because Nauru has two), the nationality for a man and for a
woman (`null` where the list gives none), and the capital in Polish and in English. It is
read through `AssetReader` on first use and kept in memory; it is not stored in the
database. The flag is built from the code.

The source is the official list of country names, *Urzędowy wykaz nazw państw i terytoriów
niesamodzielnych*, 8th edition (2025), by the Komisja Standaryzacji Nazw Geograficznych
poza Granicami Rzeczypospolitej Polskiej (KSNG), free to download from
<https://www.gov.pl/web/ksng/Urzedowy-wykaz-nazw-panstw-i-terytoriow-niesamodzielnych>.
`tools/countries/extract_countries.py` builds the asset from the PDF. The two updates
printed at the end of the publication replace their entries: Equatorial Guinea's capital
(Ciudad de la Paz) and Nauru's new name (Naoero). The flags' codes and the English names
are added by the script, since the list is Polish only.

The country, nationality and capital words are not in the vocabulary or the presets;
lessons show them as plain rows without a status button.

## Where it lives

| Layer | Code |
| --- | --- |
| Model and contracts | `interactors.countries` — `Country`, `CountryField`, `CountryFieldProgress`, `CountryCourse`, the use cases; `interactors.training.DrillCourseProgress` |
| Use cases | `application.countries.CountryUseCasesImpl`; the picking rule is `application.training.nextToPractise` |
| Port | `boundary.CountryRepository` |
| Storage | `data.local.CountryEntities` — tables `country_choice` (one row per ticked code) and `country_progress` (one row per country and field tried, keyed by code and field); the asset is parsed by `data.local.CountryAsset`. Added in database version 45 |
| Screens | `presentation.countries` — `CountryCourseScreen` (the checklist), `CountryTrainingScreen` |
| Entry points | the **Countries** tile on the Plan tab and the course card on the dashboard |

Android only for now; see [ios/README.md](../ios/README.md).
