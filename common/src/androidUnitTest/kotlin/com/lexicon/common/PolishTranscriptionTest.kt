package com.lexicon.common

import org.junit.Assert.assertEquals
import org.junit.Test

class PolishTranscriptionTest {
    private fun assertIpa(
        word: String,
        expected: String,
    ) = assertEquals(word, expected, polishTranscription(word))

    @Test
    fun `stress sits at the front of the second-to-last syllable`() {
        assertIpa("który", "ˈkturɨ")
        assertIpa("praca", "ˈprat͡sa")
        assertIpa("kobieta", "kɔˈbjɛta")

        assertIpa("kot", "kɔt")
        assertIpa("jak", "jak")
    }

    @Test
    fun `an i between a consonant and a vowel is a glide`() {
        assertIpa("mieć", "mjɛt͡ɕ")
        assertIpa("kiedy", "ˈkjɛdɨ")
        assertIpa("sobie", "ˈsɔbjɛ")
    }

    @Test
    fun `ci si zi ni soften the consonant and keep the i only when no vowel follows`() {
        assertIpa("ciasto", "ˈt͡ɕastɔ")
        assertIpa("nie", "ɲɛ")
        assertIpa("się", "ɕɛ̃")
    }

    @Test
    fun `a word ends devoiced`() {
        assertIpa("bez", "bɛs")
        assertIpa("móc", "mut͡s")
        assertIpa("teraz", "ˈtɛras")
    }

    @Test
    fun `a cluster takes the voicing of its last obstruent`() {
        assertIpa("wszystko", "ˈfʂɨstkɔ")
        assertIpa("wódka", "ˈvutka")
    }

    @Test
    fun `v and z devoice after a voiceless obstruent rather than voicing it`() {
        assertIpa("przez", "pʂɛs")
        assertIpa("krzesło", "ˈkʂɛswɔ")
        assertIpa("trzy", "tʂɨ")
        assertIpa("twój", "tfuj")
        assertIpa("kwiat", "kfjat")
    }

    @Test
    fun `a phrase is transcribed word by word, each with its own stress`() {
        assertIpa("dzień dobry", "d͡ʑɛɲ ˈdɔbrɨ")
    }

    @Test
    fun `input with no letters has no transcription`() {
        assertEquals("", polishTranscription(""))
        assertEquals("", polishTranscription("   "))
        assertEquals("", polishTranscription("!?"))
    }

    @Test
    fun `matches the shipped corpus`() {
        corpusCases.forEach { (word, expected) -> assertIpa(word, expected) }
    }

    private val corpusCases = listOf(
        "badanie" to "baˈdaɲɛ",
        "bez" to "bɛs",
        "biżuteria" to "biʐuˈtɛrja",
        "bukiet" to "ˈbukjɛt",
        "buty" to "ˈbutɨ",
        "cicho" to "ˈt͡ɕixɔ",
        "ciuch" to "t͡ɕux",
        "co to znaczy" to "t͡sɔ tɔ ˈznat͡ʂɨ",
        "coś" to "t͡sɔɕ",
        "cukier" to "ˈt͡sukjɛr",
        "czy możesz mi pomóc" to "t͡ʂɨ ˈmɔʐɛʂ mi ˈpɔmut͡s",
        "do usłyszenia" to "dɔ uswɨˈʂɛɲa",
        "dochód" to "ˈdɔxut",
        "dowodzić tezy" to "dɔˈvɔd͡ʑit͡ɕ ˈtɛzɨ",
        "drugorzędny" to "drugɔˈʐɛ̃dnɨ",
        "drzewo" to "ˈdʐɛvɔ",
        "dziadek" to "ˈd͡ʑadɛk",
        "dział" to "d͡ʑaw",
        "dzwonek" to "ˈd͡zvɔnɛk",
        "emerytura" to "ɛmɛrɨˈtura",
        "fabuła" to "faˈbuwa",
        "gorąco" to "gɔˈrɔ̃t͡sɔ",
        "grupa" to "ˈgrupa",
        "głosować" to "gwɔˈsɔvat͡ɕ",
        "jakość" to "ˈjakɔɕt͡ɕ",
        "jasny" to "ˈjasnɨ",
        "jezioro" to "jɛˈʑɔrɔ",
        "jeść" to "jɛɕt͡ɕ",
        "kalendarz" to "kaˈlɛndaʂ",
        "kantor" to "ˈkantɔr",
        "karp" to "karp",
        "kaszel" to "ˈkaʂɛl",
        "kieliszek" to "kjɛˈliʂɛk",
        "klon" to "klɔn",
        "konsolidacja" to "kɔnsɔliˈdat͡sja",
        "kurczak" to "ˈkurt͡ʂak",
        "leczyć" to "ˈlɛt͡ʂɨt͡ɕ",
        "lemoniada" to "lɛmɔˈɲada",
        "liczyć" to "ˈlit͡ʂɨt͡ɕ",
        "luty" to "ˈlutɨ",
        "maj" to "maj",
        "marzenie" to "maˈʐɛɲɛ",
        "media społecznościowe" to "ˈmɛdja spɔwɛt͡ʂnɔˈɕt͡ɕɔvɛ",
        "mieszkać" to "ˈmjɛʂkat͡ɕ",
        "mimo to" to "ˈmimɔ tɔ",
        "mięsień" to "ˈmjɛ̃ɕɛɲ",
        "miło mi" to "ˈmiwɔ mi",
        "miły" to "ˈmiwɨ",
        "muszę już iść" to "ˈmuʂɛ̃ juʂ iɕt͡ɕ",
        "myszka" to "ˈmɨʂka",
        "należeć" to "naˈlɛʐɛt͡ɕ",
        "napomknąć" to "naˈpɔmknɔ̃t͡ɕ",
        "naszyjnik" to "naˈʂɨjɲik",
        "niewypłacalność" to "ɲɛvɨpwaˈt͡salnɔɕt͡ɕ",
        "oddychać" to "ɔˈddɨxat͡ɕ",
        "odpoczynek" to "ɔtpɔˈt͡ʂɨnɛk",
        "odpoczywać" to "ɔtpɔˈt͡ʂɨvat͡ɕ",
        "opóźnienie" to "ɔpuˈʑɲɛɲɛ",
        "ostatnio" to "ɔˈstatɲɔ",
        "pan" to "pan",
        "partia" to "ˈpartja",
        "pewnie" to "ˈpɛvɲɛ",
        "pełny" to "ˈpɛwnɨ",
        "piętnować" to "pjɛ̃ˈtnɔvat͡ɕ",
        "plecy" to "ˈplɛt͡sɨ",
        "podekscytowany" to "pɔdɛkst͡sɨtɔˈvanɨ",
        "pogoda" to "pɔˈgɔda",
        "pomoc" to "ˈpɔmɔt͡s",
        "powieść" to "ˈpɔvjɛɕt͡ɕ",
        "programista" to "prɔgraˈmista",
        "proszę powtórzyć" to "ˈprɔʂɛ̃ pɔˈftuʐɨt͡ɕ",
        "puchar" to "ˈpuxar",
        "pytanie" to "pɨˈtaɲɛ",
        "reklamacja" to "rɛklaˈmat͡sja",
        "rozczarowany" to "rɔst͡ʂarɔˈvanɨ",
        "rozgrzewka" to "rɔˈzgʐɛfka",
        "rzecz" to "ʐɛt͡ʂ",
        "rząd" to "ʐɔ̃t",
        "sadzić" to "ˈsad͡ʑit͡ɕ",
        "samorząd" to "saˈmɔʐɔ̃t",
        "skrupuły" to "skruˈpuwɨ",
        "strach" to "strax",
        "strych" to "strɨx",
        "sumienność" to "suˈmjɛnnɔɕt͡ɕ",
        "suwerenność" to "suvɛˈrɛnnɔɕt͡ɕ",
        "szalik" to "ˈʂalik",
        "szeroki" to "ʂɛˈrɔki",
        "tak czy inaczej" to "tak t͡ʂɨ iˈnat͡ʂɛj",
        "temperatura" to "tɛmpɛraˈtura",
        "trener" to "ˈtrɛnɛr",
        "trudno powiedzieć" to "ˈtrudnɔ pɔˈvjɛd͡ʑɛt͡ɕ",
        "trójkąt" to "ˈtrujkɔ̃t",
        "typowy" to "tɨˈpɔvɨ",
        "tłumaczyć" to "twuˈmat͡ʂɨt͡ɕ",
        "uszczypliwość" to "uʂt͡ʂɨˈplivɔɕt͡ɕ",
        "wiadomość" to "vjaˈdɔmɔɕt͡ɕ",
        "wieloryb" to "vjɛˈlɔrɨp",
        "woda mineralna" to "ˈvɔda minɛˈralna",
        "wszędzie" to "ˈfʂɛ̃d͡ʑɛ",
        "wybory" to "vɨˈbɔrɨ",
        "wybór" to "ˈvɨbur",
        "wydatki" to "vɨˈdatki",
        "wynik" to "ˈvɨɲik",
        "wątpliwy" to "vɔ̃ˈtplivɨ",
        "z poważaniem" to "s pɔvaˈʐaɲɛm",
        "zakres" to "ˈzakrɛs",
        "zapach" to "ˈzapax",
        "zapisać" to "zaˈpisat͡ɕ",
        "zdarzać się" to "ˈzdaʐat͡ɕ ɕɛ̃",
        "zdrowy" to "ˈzdrɔvɨ",
        "zgodnie z planem" to "ˈzgɔdɲɛ s ˈplanɛm",
        "zgoła" to "ˈzgɔwa",
        "zgłosić" to "ˈzgwɔɕit͡ɕ",
        "zjawisko" to "zjaˈviskɔ",
        "zmiana" to "ˈzmjana",
        "zmęczony" to "zmɛ̃ˈt͡ʂɔnɨ",
        "zobrazować" to "zɔbraˈzɔvat͡ɕ",
        "zwycięzca" to "zvɨˈt͡ɕɛ̃st͡sa",
        "ławka" to "ˈwafka",
        "łóżko" to "ˈwuʂkɔ",
    )
}
