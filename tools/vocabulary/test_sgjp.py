#!/usr/bin/env python3
"""Regression tests for the SGJP reader:  python3 tools/vocabulary/test_sgjp.py

The dictionary itself is 40 MB and not committed, so each case writes the few SGJP lines
it needs into a temporary file in the same format, and checks one decision the build
relies on: which homonym is taken, which gender is read, which forms come out.
"""

import gzip
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import sgjp  # noqa: E402

LINES = """\
oko	oko:S1	subst:sg:nom.acc.voc:n:ncol	nazwa_pospolita
oka	oko:S1	subst:sg:gen:n:ncol	nazwa_pospolita
oczy	oko:S1	subst:pl:nom.acc.voc:n:ncol	nazwa_pospolita
oczu	oko:S1	subst:pl:gen:n:ncol	nazwa_pospolita
oko	oko:S2	subst:sg:nom.acc.voc:n:ncol	nazwa_pospolita
oka	oko:S2	subst:sg:gen:n:ncol	nazwa_pospolita
oka	oko:S2	subst:pl:nom.acc.voc:n:ncol	nazwa_pospolita
ok	oko:S2	subst:pl:gen:n:ncol	nazwa_pospolita
drzwi	drzwi	subst:pl:nom.acc.voc:n:pt	nazwa_pospolita
mąż	mąż	subst:sg:nom:m1	nazwa_pospolita
mężowie	mąż	subst:pl:nom.voc:m1	nazwa_pospolita
dobry	dobry:A	adj:sg:nom.voc:m1.m2.m3:pos
dobra	dobry:A	adj:sg:nom.voc:f:pos
dobre	dobry:A	adj:sg:nom.voc:n:pos
dobrzy	dobry:A	adj:pl:nom.voc:m1:pos
dobre	dobry:A	adj:pl:nom.voc:m2.m3.f.n:pos
lepszy	dobry:A	adj:sg:nom.voc:m1.m2.m3:com
kupić	kupić:Vp	inf:perf
kupię	kupić:Vp	fin:sg:pri:perf
kupią	kupić:Vp	fin:pl:ter:perf
kupić	kupić:Vi	inf:imperf		przest.
kupię	kupić:Vi	fin:sg:pri:imperf		przest.
drepczę	dreptać	fin:sg:pri:imperf
drepcę	dreptać	fin:sg:pri:imperf
dreptam	dreptać	fin:sg:pri:imperf		rzad.
dreptać	dreptać	inf:imperf
"""


def lexicon() -> dict:
    directory = Path(tempfile.mkdtemp())
    with gzip.open(directory / "sgjp-20990101.tab.gz", "wt", encoding="utf-8") as out:
        out.write("#<COPYRIGHT>\nnot data\n#</COPYRIGHT>\n" + LINES)
    sgjp.CACHE = directory
    return sgjp.load({"oko", "drzwi", "mąż", "dobry", "kupić", "dreptać"})


def check(name: str, actual, expected, failures: list[str]) -> None:
    if actual != expected:
        failures.append(f"{name}: expected {expected!r}, got {actual!r}")


def main() -> int:
    words = lexicon()
    failures: list[str] = []

    eye = sgjp.closest(words["oko"], "n", lambda lexeme: sgjp.noun_mismatches(lexeme, {"nominative": ("oko", "oczy")}))
    check("the homonym whose forms agree is taken", sgjp.declension(eye)["nominative"], (["oko"], ["oczy"]), failures)
    mesh = sgjp.closest(words["oko"], "n", lambda lexeme: sgjp.noun_mismatches(lexeme, {"genitive": ("oka", "ok")}))
    check("the other sense is taken when its forms agree", sgjp.declension(mesh)["genitive"], (["oka"], ["ok"]), failures)

    check("a plural-only noun is read as one", sgjp.best(words["drzwi"], "n").gender, "plural only", failures)
    check("a masculine personal noun keeps its gender", sgjp.best(words["mąż"], "n").gender, "masculine personal", failures)

    forms = sgjp.adjective_forms(sgjp.best(words["dobry"], "adj"))
    check(
        "an adjective gives its five nominatives, not its comparative",
        [forms[name] for name in ["masculine", "feminine", "neuter", "pluralPersonal", "pluralOther"]],
        [["dobry"], ["dobra"], ["dobre"], ["dobrzy"], ["dobre"]],
        failures,
    )

    buy = sgjp.best(words["kupić"], "v")
    check("the unmarked homonym wins over an obsolete one", buy.aspect, "perfective", failures)
    check("a perfective verb conjugates in the simple future", sgjp.present_tense(buy)["oni/one"], ["kupią"], failures)

    walk = sgjp.present_tense(sgjp.best(words["dreptać"], "v"))
    check("rare variants give way to the standard ones", walk["ja"], ["drepczę", "drepcę"], failures)

    check("się is set aside to look a verb up", sgjp.verb_lemma("bać się"), ("bać", "", " się"), failures)
    check("the verb is found inside a phrase", sgjp.verb_lemma("nie docenić"), ("docenić", "nie ", ""), failures)
    check("N/A and separators are not forms", sgjp.variants("baję; bajam / N/A"), ["baję", "bajam"], failures)

    for failure in failures:
        print(f"FAIL {failure}")
    print(f"{13 - len(failures)}/13 passed")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
