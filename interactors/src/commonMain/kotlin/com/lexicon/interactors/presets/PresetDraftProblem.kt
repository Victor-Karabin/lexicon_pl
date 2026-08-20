package com.lexicon.interactors.presets

enum class PresetDraftProblem { MISSING_TITLE }

class PresetDraftException(val problem: PresetDraftProblem) : Exception(problem.name)
