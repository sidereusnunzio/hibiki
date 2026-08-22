package com.hibiki.domain.model

data class BuiltInSubject(
    val id: String,
    val displayName: String,
    val japaneseName: String,
    val prompt: String,
)

object BuiltInUmamusume {
    val CONTEXT_PROMPT = """
Audio proveniente dal videogioco Umamusume: Pretty Derby, versione giapponese.

L'audio può contenere brevi battute pronunciate durante allenamenti, gare, eventi, schermate di gioco e interazioni con i personaggi. Molte battute sono brevi e ricorrenti.

Possono comparire terminologia specifica delle corse, dell'allenamento e del gioco, nomi propri, soprannomi e termini caratteristici dell'universo di Umamusume.

Il parlato può essere molto informale, veloce, contratto o enfatico. I personaggi possono avere modi di parlare fortemente caratterizzati.
""".trimIndent()

    val CHARACTERS = listOf(
        BuiltInSubject(
            id = "vodka",
            displayName = "Vodka",
            japaneseName = "ウオッカ",
            prompt = """
Vodka parla generalmente in modo informale, energico, diretto e ruvido, con tratti linguistici tendenzialmente mascolini.

Può usare forme colloquiali e contratte, espressioni enfatiche, interiezioni e terminazioni brusche. Il suo parlato può includere trasformazioni colloquiali come ない → ねえ.

Non normalizzare il suo parlato in un giapponese più formale o standard.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "silence_suzuka",
            displayName = "Silence Suzuka",
            japaneseName = "サイレンススズカ",
            prompt = """
Silence Suzuka (サイレンススズカ) parla generalmente in modo calmo, controllato e relativamente delicato.

Il suo registro tende a essere meno ruvido e meno enfatico rispetto a molti altri personaggi. Può utilizzare costruzioni semplici e naturali, con un tono composto anche quando parla di gare o allenamento.

Non aggiungere espressioni formali o delicate sulla base del personaggio: usa queste informazioni soltanto per disambiguare ciò che è effettivamente udibile.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "special_week",
            displayName = "Special Week",
            japaneseName = "スペシャルウィーク",
            prompt = """
Special Week (スペシャルウィーク) parla generalmente in modo spontaneo, cordiale ed energico.

Il suo parlato può diventare molto emotivo o concitato e contenere interiezioni, esclamazioni, esitazioni e forme colloquiali. Il registro rimane generalmente naturale e accessibile, senza la marcata ruvidità di personaggi come Vodka.

Conserva interiezioni, ripetizioni e forme colloquiali effettivamente pronunciate.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "tokai_teio",
            displayName = "Tokai Teio",
            japaneseName = "トウカイテイオー",
            prompt = """
Tokai Teio (トウカイテイオー) parla in modo vivace, sicuro, giocoso ed espressivo.

Il parlato può essere rapido e molto informale, con esclamazioni, allungamenti, particelle finali e variazioni enfatiche della pronuncia. Alcune battute possono avere un tono volutamente infantile, scherzoso o presuntuoso.

Presta particolare attenzione alle terminazioni e alle interiezioni senza normalizzarle o eliminarle.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "mejiro_mcqueen",
            displayName = "Mejiro McQueen",
            japaneseName = "メジロマックイーン",
            prompt = """
Mejiro McQueen (メジロマックイーン) utilizza generalmente un registro educato, composto e raffinato.

Può usare forme cortesi e costruzioni associate a un modo di parlare elegante e femminile. Il contrasto tra questo registro e momenti più emotivi può produrre variazioni marcate nel parlato.

Non semplificare le forme cortesi o le terminazioni caratteristiche se sono effettivamente pronunciate.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "sakura_bakushin_o",
            displayName = "Sakura Bakushin O",
            japaneseName = "サクラバクシンオー",
            prompt = """
Sakura Bakushin O (サクラバクシンオー) parla in modo estremamente energico, enfatico e spesso ad alto volume.

Il parlato può essere rapido, scandito con forza e ricco di esclamazioni, ripetizioni e interiezioni. Utilizza frequentemente il proprio stile retorico esagerato e può pronunciare termini o slogan con enfasi insolita.

Non eliminare ripetizioni, esclamazioni o interiezioni solo perché sembrano ridondanti.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "gold_ship",
            displayName = "Gold Ship",
            japaneseName = "ゴールドシップ",
            prompt = """
Gold Ship (ゴールドシップ) ha un parlato estremamente variabile, informale, eccentrico e imprevedibile.

Può cambiare improvvisamente registro, utilizzare giochi di parole, espressioni insolite, imitazioni, esagerazioni, termini fuori contesto e costruzioni volutamente assurde. Una frase apparentemente strana può essere intenzionale.

Non correggere o normalizzare una formulazione soltanto perché sembra semanticamente insolita. Dai priorità al parlato effettivamente udibile.
""".trimIndent(),
        ),
    )
}
