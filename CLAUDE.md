# Working in this repository

## Comments

Do not write code comments.

Names, types and structure carry the explanation. When a piece of code seems to
need a comment to be understood, that is a signal to rename or restructure it,
not to annotate it. Explanation that genuinely does not belong in code — why an
approach was chosen, what went wrong before — belongs in the commit message or
the pull request, where it is read once and does not rot alongside the code.

This applies to KDoc and to inline comments alike, in Kotlin and Swift.

## Logging

Log the things that would otherwise fail silently.

A caught exception, a non-success HTTP response, a fallback being taken, a
request returning nothing usable — each of these ends up presented to the user as
some unrelated symptom, and none of them leaves a trace by default. `runCatching
{ … }.getOrNull()` in particular discards the one piece of information that would
have explained the failure.

Use `android.util.Log` with a tag naming the class. Log at `w` for something
recovered from and `e` for something that defeats the operation. Never log an API
key, a request URL carrying one, or anything a learner typed or said.
