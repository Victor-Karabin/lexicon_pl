import Shared
import SwiftUI

private let labelWidth: CGFloat = 96

struct WordGrammarRows: View {
    let wordId: VocabularyId
    let tint: Color
    let mutedTint: Color

    @State private var grammar: WordGrammar?

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.tiny) {
            if let grammar {
                Text(heading(of: grammar))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(mutedTint)

                ForEach(rows(of: grammar), id: \.label) { row in
                    HStack(alignment: .firstTextBaseline, spacing: 0) {
                        Text(row.label)
                            .font(.caption)
                            .foregroundStyle(mutedTint)
                            .frame(width: labelWidth, alignment: .leading)
                        Text(row.value).font(.caption).foregroundStyle(tint)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .task(id: wordId.value) { grammar = try? await deps.getWordGrammar.invoke(id: wordId) }
    }

    private func heading(of grammar: WordGrammar) -> String {
        let name = partOfSpeechName(grammar.partOfSpeech)
        guard let noun = grammar as? WordGrammarNoun else { return name }
        return "\(name) · \(genderName(noun.gender))"
    }

    private func rows(of grammar: WordGrammar) -> [(label: String, value: String)] {
        if let noun = grammar as? WordGrammarNoun {
            return noun.plural.map { [(Strings.grammarPlural, $0)] } ?? []
        }
        if let adjective = (grammar as? WordGrammarAdjective)?.forms {
            return [
                (Strings.grammarMasculine, adjective.masculine),
                (Strings.grammarFeminine, adjective.feminine),
                (Strings.grammarNeuter, adjective.neuter),
                (Strings.grammarPluralPersonal, adjective.pluralPersonal),
                (Strings.grammarPluralOther, adjective.pluralOther),
            ]
        }
        guard let conjugation = (grammar as? WordGrammarVerb)?.conjugation else { return [] }
        return conjugation.persons.map { person in
            (person.label, conjugation.formsFor(person: person).joined(separator: " / "))
        }
    }

    private func partOfSpeechName(_ partOfSpeech: PartOfSpeech) -> String {
        switch partOfSpeech {
        case .noun: return Strings.grammarPosN
        case .verb: return Strings.grammarPosV
        case .adjective: return Strings.grammarPosAdj
        case .adverb: return Strings.grammarPosAdv
        case .preposition: return Strings.grammarPosPrep
        case .conjunction: return Strings.grammarPosConj
        case .pronoun: return Strings.grammarPosPrn
        case .numeral: return Strings.grammarPosNum
        case .particle: return Strings.grammarPosPart
        case .interjection: return Strings.grammarPosInterj
        default: return Strings.grammarPosExpr
        }
    }

    private func genderName(_ gender: Gender) -> String {
        switch gender {
        case .masculinePersonal: return Strings.grammarGenderMasculinePersonal
        case .masculineAnimate: return Strings.grammarGenderMasculineAnimate
        case .masculineInanimate: return Strings.grammarGenderMasculineInanimate
        case .feminine: return Strings.grammarGenderFeminine
        case .neuter: return Strings.grammarGenderNeuter
        default: return Strings.grammarGenderPluralOnly
        }
    }
}
