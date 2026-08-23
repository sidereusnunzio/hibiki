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
            id = "special_week",
            displayName = "Special Week",
            japaneseName = "スペシャルウィーク",
            prompt = """
Parla in modo spontaneo, cordiale ed energico.

Il registro è generalmente naturale e informale; nei momenti emotivi può diventare concitata, con esclamazioni, esitazioni e ripetizioni.

Conserva interiezioni e forme colloquiali effettivamente pronunciate.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "silence_suzuka",
            displayName = "Silence Suzuka",
            japaneseName = "サイレンススズカ",
            prompt = """
Parla generalmente in modo calmo, controllato e delicato.

Usa un giapponese relativamente semplice e composto, con poche espressioni aggressive o particolarmente enfatiche.

Non attribuirle automaticamente forme formali o delicate se non sono udibili.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "tokai_teio",
            displayName = "Tokai Teio",
            japaneseName = "トウカイテイオー",
            prompt = """
Parla in modo vivace, sicuro, giocoso e molto espressivo.

Può usare parlato rapido e informale, esclamazioni, allungamenti vocalici e particelle finali marcate.

Conserva terminazioni, interiezioni e pronunce enfatiche.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "maruzensky",
            displayName = "Maruzensky",
            japaneseName = "マルゼンスキー",
            prompt = """
Parla in modo sicuro, allegro e femminile.

Può utilizzare espressioni volutamente antiquate o datate rispetto al giapponese contemporaneo, spesso con tono giocoso.

Non modernizzare espressioni che sembrano insolite o fuori moda.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "fuji_kiseki",
            displayName = "Fuji Kiseki",
            japaneseName = "フジキセキ",
            prompt = """
Parla in modo sicuro, elegante e teatrale, talvolta con un tono da intrattenitrice o seduttrice.

Può usare formulazioni ricercate e battute enfatiche.

Conserva il registro e non normalizzare espressioni volutamente teatrali.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "oguri_cap",
            displayName = "Oguri Cap",
            japaneseName = "オグリキャップ",
            prompt = """
Parla generalmente in modo diretto, serio e semplice.

Il registro tende a essere poco ornamentale e relativamente neutro. Può diventare molto diretta quando parla di cibo, gare o allenamento.

Non aggiungere sfumature emotive non udibili.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "gold_ship",
            displayName = "Gold Ship",
            japaneseName = "ゴールドシップ",
            prompt = """
Il parlato è estremamente variabile, informale, eccentrico e imprevedibile.

Può cambiare improvvisamente registro, usare giochi di parole, imitazioni, esagerazioni, termini fuori contesto e costruzioni volutamente assurde.

Non correggere una formulazione soltanto perché sembra semanticamente strana.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "vodka",
            displayName = "Vodka",
            japaneseName = "ウオッカ",
            prompt = """
Parla generalmente in modo informale, energico, diretto e ruvido, con tratti tendenzialmente mascolini.

Può usare contrazioni colloquiali, interiezioni e terminazioni brusche, comprese forme come ない → ねえ.

Non normalizzare il parlato in un giapponese più formale o standard.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "daiwa_scarlet",
            displayName = "Daiwa Scarlet",
            japaneseName = "ダイワスカーレット",
            prompt = """
Parla in modo sicuro, competitivo e assertivo, ma generalmente femminile.

Può diventare brusca o fortemente enfatica quando è irritata o competitiva.

Conserva particelle finali, esclamazioni e cambi di registro.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "taiki_shuttle",
            displayName = "Taiki Shuttle",
            japaneseName = "タイキシャトル",
            prompt = """
Parla in modo estremamente energico e amichevole.

Il suo giapponese può includere frequentemente parole ed espressioni inglesi, code-switching e costruzioni influenzate dalla sua caratterizzazione americana.

Non tradurre, correggere o eliminare gli elementi inglesi pronunciati.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "grass_wonder",
            displayName = "Grass Wonder",
            japaneseName = "グラスワンダー",
            prompt = """
Parla generalmente in modo calmo, educato e femminile, con formulazioni composte.

Anche quando è competitiva tende a mantenere un registro controllato.

Conserva le forme cortesi effettivamente pronunciate.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "hishi_amazon",
            displayName = "Hishi Amazon",
            japaneseName = "ヒシアマゾン",
            prompt = """
Parla in modo energico, diretto e deciso, con caratteristiche relativamente rudi e mascoline.

Può utilizzare espressioni colloquiali e terminazioni forti.

Non rendere automaticamente il parlato più femminile o formale.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "mejiro_mcqueen",
            displayName = "Mejiro McQueen",
            japaneseName = "メジロマックイーン",
            prompt = """
Utilizza generalmente un registro educato, composto, raffinato e femminile.

Può usare forme cortesi e terminazioni eleganti. Nei momenti emotivi il registro può cambiare sensibilmente.

Non semplificare le forme cortesi effettivamente pronunciate.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "el_condor_pasa",
            displayName = "El Condor Pasa",
            japaneseName = "エルコンドルパサー",
            prompt = """
Parla in modo energico, teatrale e sicuro.

Può inserire parole straniere, soprattutto spagnole, e usare esclamazioni o formulazioni volutamente enfatiche.

Conserva integralmente gli elementi non giapponesi pronunciati.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "t_m_opera_o",
            displayName = "T.M. Opera O",
            japaneseName = "テイエムオペラオー",
            prompt = """
Parla in modo estremamente teatrale, grandioso e autoreferenziale.

Può usare formulazioni solenni, ricercate o deliberatamente esagerate.

Non normalizzare il parlato solo perché appare innaturalmente pomposo.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "narita_brian",
            displayName = "Narita Brian",
            japaneseName = "ナリタブライアン",
            prompt = """
Parla in modo asciutto, serio, diretto e spesso brusco.

Tende a usare frasi concise e un registro informale poco ornamentale.

Conserva terminazioni dure e forme contratte effettivamente udibili.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "symboli_rudolf",
            displayName = "Symboli Rudolf",
            japaneseName = "シンボリルドルフ",
            prompt = """
Parla generalmente in modo autorevole, controllato e articolato.

Può utilizzare un registro formale o elevato, ma anche giochi di parole intenzionalmente banali.

Non correggere battute o formulazioni insolite se compatibili con l'audio.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "air_groove",
            displayName = "Air Groove",
            japaneseName = "エアグルーヴ",
            prompt = """
Parla in modo disciplinato, deciso e autorevole.

Il registro può essere diretto e severo, con formulazioni relativamente precise.

Conserva imperativi, terminazioni brusche e forme forti effettivamente pronunciate.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "agnes_digital",
            displayName = "Agnes Digital",
            japaneseName = "アグネスデジタル",
            prompt = """
Il parlato può diventare estremamente rapido, eccitato e irregolare, soprattutto parlando di altre Umamusume.

Può usare gergo otaku, esclamazioni, ripetizioni, abbreviazioni e vocalizzazioni insolite.

Non normalizzare formulazioni apparentemente eccentriche.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "seiun_sky",
            displayName = "Seiun Sky",
            japaneseName = "セイウンスカイ",
            prompt = """
Parla in modo rilassato, informale e spesso giocoso.

Può trascinare suoni, usare espressioni casuali e assumere un tono deliberatamente svogliato.

Conserva allungamenti e forme colloquiali quando udibili.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "tamamo_cross",
            displayName = "Tamamo Cross",
            japaneseName = "タマモクロス",
            prompt = """
Parla frequentemente in Kansai-ben, con registro energico, diretto e comico.

Possono comparire forme dialettali come や, やろ, へん e altre costruzioni Kansai.

Non convertire il dialetto in giapponese standard.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "fine_motion",
            displayName = "Fine Motion",
            japaneseName = "ファインモーション",
            prompt = """
Parla generalmente in modo educato, curioso, allegro e femminile.

Può usare forme cortesi con naturalezza, ma anche diventare più informale nelle interazioni amichevoli.

Conserva il livello di cortesia effettivamente udibile.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "biwa_hayahide",
            displayName = "Biwa Hayahide",
            japaneseName = "ビワハヤヒデ",
            prompt = """
Parla in modo razionale, preciso e relativamente formale.

Può utilizzare lessico analitico o tecnico e formulazioni articolate.

Non semplificare termini apparentemente complessi.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "mayano_top_gun",
            displayName = "Mayano Top Gun",
            japaneseName = "マヤノトップガン",
            prompt = """
Parla in modo vivace, infantile, affettuoso e molto informale.

Può usare esclamazioni, allungamenti vocalici e forme giocose.

Conserva caratteristiche infantili e interiezioni.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "manhattan_cafe",
            displayName = "Manhattan Cafe",
            japaneseName = "マンハッタンカフェ",
            prompt = """
Parla generalmente a bassa intensità, con tono calmo, esitante e introspettivo.

Può usare pause, esitazioni e riferimenti apparentemente enigmatici.

Non completare automaticamente frasi frammentarie o poco convenzionali.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "mihono_bourbon",
            displayName = "Mihono Bourbon",
            japaneseName = "ミホノブルボン",
            prompt = """
Parla in modo controllato, letterale e quasi meccanico.

Può utilizzare terminologia sistemica, formulazioni rigide e dichiarazioni simili a output operativi.

Non rendere il parlato più naturale se la formulazione robotica è effettivamente pronunciata.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "mejiro_ryan",
            displayName = "Mejiro Ryan",
            japaneseName = "メジロライアン",
            prompt = """
Parla in modo diretto, positivo e sportivo, generalmente informale.

Può diventare timida o esitante in contesti romantici o legati alla femminilità.

Conserva esitazioni e cambi di registro.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "hishi_akebono",
            displayName = "Hishi Akebono",
            japaneseName = "ヒシアケボノ",
            prompt = """
Parla in modo allegro, affettuoso e generalmente semplice.

Può utilizzare espressioni entusiaste, soprattutto riguardo al cibo e alla cucina.

Conserva allungamenti ed esclamazioni.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "rice_shower",
            displayName = "Rice Shower",
            japaneseName = "ライスシャワー",
            prompt = """
Parla generalmente in modo timido, delicato ed esitante.

Può riferirsi a sé stessa come ライス e usare pause, esitazioni e formulazioni insicure.

Non eliminare ripetizioni o esitazioni.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "ines_fujin",
            displayName = "Ines Fujin",
            japaneseName = "アイネスフウジン",
            prompt = """
Parla in modo cordiale, pratico ed energico, con registro generalmente informale.

Può usare un tono da sorella maggiore e formulazioni quotidiane.

Conserva le contrazioni colloquiali effettivamente pronunciate.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "agnes_tachyon",
            displayName = "Agnes Tachyon",
            japaneseName = "アグネスタキオン",
            prompt = """
Parla in modo eccentrico, analitico e spesso teatrale, con lessico scientifico o sperimentale.

Può chiamare l'interlocutore モルモット e utilizzare termini tecnici insoliti.

Non semplificare o normalizzare il lessico scientifico.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "admire_vega",
            displayName = "Admire Vega",
            japaneseName = "アドマイヤベガ",
            prompt = """
Parla in modo serio, contenuto e spesso distaccato.

Le frasi possono essere brevi e poco emotive, con occasionali momenti più delicati.

Non aggiungere cordialità o enfasi assenti dall'audio.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "winning_ticket",
            displayName = "Winning Ticket",
            japaneseName = "ウイニングチケット",
            prompt = """
Parla in modo molto energico, sincero ed emotivo.

Può gridare, ripetere parole e utilizzare numerose esclamazioni.

Conserva ripetizioni e interiezioni anche quando sembrano ridondanti.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "eishin_flash",
            displayName = "Eishin Flash",
            japaneseName = "エイシンフラッシュ",
            prompt = """
Parla in modo preciso, disciplinato e generalmente educato.

Può utilizzare formulazioni formali e riferimenti a orari, programmi o procedure.

Non rendere più colloquiali le costruzioni effettivamente formali.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "curren_chan",
            displayName = "Curren Chan",
            japaneseName = "カレンチャン",
            prompt = """
Parla in modo dolce, consapevolmente carino e molto espressivo.

Può usare カレン per riferirsi a sé stessa e impiegare linguaggio associato alla propria immagine “cute”.

Conserva autoreferenze e terminazioni caratteristiche.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "kawakami_princess",
            displayName = "Kawakami Princess",
            japaneseName = "カワカミプリンセス",
            prompt = """
Parla in modo estremamente energico e idealistico, con frequenti esclamazioni.

Può alternare un registro da “principessa” a espressioni molto più aggressive o fisiche.

Conserva questi contrasti senza normalizzarli.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "gold_city",
            displayName = "Gold City",
            japaneseName = "ゴールドシチー",
            prompt = """
Parla generalmente in modo moderno, diretto e relativamente informale.

Può avere un tono distaccato o irritato e utilizzare espressioni quotidiane.

Non rendere il registro artificialmente elegante sulla base del suo lavoro di modella.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "sakura_bakushin_o",
            displayName = "Sakura Bakushin O",
            japaneseName = "サクラバクシンオー",
            prompt = """
Parla in modo estremamente energico, enfatico e spesso ad alto volume.

Il parlato può essere rapido, scandito con forza e ricco di esclamazioni, ripetizioni e slogan.

Non eliminare ripetizioni o espressioni apparentemente ridondanti.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "super_creek",
            displayName = "Super Creek",
            japaneseName = "スーパークリーク",
            prompt = """
Parla in modo molto dolce, materno e rassicurante.

Può utilizzare espressioni affettuose e un registro morbido.

Conserva allungamenti e formule affettuose effettivamente pronunciate.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "smart_falcon",
            displayName = "Smart Falcon",
            japaneseName = "スマートファルコン",
            prompt = """
Parla con uno stile molto energico da idol.

Può riferirsi a sé stessa come ファル子 e usare formule da idol, esclamazioni, slogan e terminazioni molto vivaci.

Conserva soprannomi e autoreferenze.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "tosen_jordan",
            displayName = "Tosen Jordan",
            japaneseName = "トーセンジョーダン",
            prompt = """
Parla in modo molto casuale, moderno e giovanile.

Può utilizzare slang, contrazioni e formulazioni grammaticalmente molto colloquiali.

Non correggere il parlato verso una forma scolastica o formale.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "narita_taishin",
            displayName = "Narita Taishin",
            japaneseName = "ナリタタイシン",
            prompt = """
Parla in modo secco, informale e spesso irritabile.

Utilizza frequentemente frasi brevi, contrazioni e terminazioni brusche.

Non ammorbidire o formalizzare il registro.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "nishino_flower",
            displayName = "Nishino Flower",
            japaneseName = "ニシノフラワー",
            prompt = """
Parla in modo molto educato, dolce e relativamente infantile.

Può usare forme cortesi anche in situazioni quotidiane.

Conserva il livello di cortesia e le esitazioni.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "haru_urara",
            displayName = "Haru Urara",
            japaneseName = "ハルウララ",
            prompt = """
Parla in modo estremamente allegro, semplice e spontaneo.

Può usare costruzioni infantili, ripetizioni ed esclamazioni.

Non rendere il parlato più adulto o sofisticato.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "bamboo_memory",
            displayName = "Bamboo Memory",
            japaneseName = "バンブーメモリー",
            prompt = """
Parla in modo energico, disciplinato e sportivo, con forte enfasi su correttezza e spirito combattivo.

Può utilizzare esclamazioni e formulazioni vigorose.

Conserva il registro diretto.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "matikanefukukitaru",
            displayName = "Matikanefukukitaru",
            japaneseName = "マチカネフクキタル",
            prompt = """
Parla in modo eccentrico ed entusiasta, con frequenti riferimenti a fortuna, divinazione e presagi.

Può usare esclamazioni, formule rituali e terminologia insolita.

Non normalizzare espressioni apparentemente bizzarre.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "meisho_doto",
            displayName = "Meisho Doto",
            japaneseName = "メイショウドトウ",
            prompt = """
Parla spesso in modo insicuro, agitato ed esitante.

Può balbettare, ripetere sillabe, interrompersi e utilizzare numerose interiezioni.

Non ripulire balbettii o ripetizioni effettivamente udibili.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "mejiro_dober",
            displayName = "Mejiro Dober",
            japaneseName = "メジロドーベル",
            prompt = """
Parla generalmente in modo riservato, serio e talvolta brusco.

Può diventare esitante o imbarazzata nelle interazioni personali.

Conserva pause, esitazioni e cambi di registro.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "nice_nature",
            displayName = "Nice Nature",
            japaneseName = "ナイスネイチャ",
            prompt = """
Parla in modo molto colloquiale, pragmatico e autoironico.

Può utilizzare espressioni quotidiane, commenti sarcastici e un tono da persona comune.

Non rendere il parlato più formale.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "king_halo",
            displayName = "King Halo",
            japaneseName = "キングヘイロー",
            prompt = """
Parla in modo orgoglioso, enfatico e spesso altezzoso.

Può usare formulazioni molto assertive e riferimenti a sé stessa come una persona di prima classe.

Conserva enfasi e registro elevato quando presenti.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "matikanetannhauser",
            displayName = "Matikanetannhauser",
            japaneseName = "マチカネタンホイザ",
            prompt = """
Parla in modo allegro, semplice e amichevole, con occasionali espressioni buffe o vocalizzazioni caratteristiche.

Conserva interiezioni e formulazioni giocose.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "mejiro_palmer",
            displayName = "Mejiro Palmer",
            japaneseName = "メジロパーマー",
            prompt = """
Parla in modo rilassato, amichevole e relativamente informale, nonostante l'origine Mejiro.

Può utilizzare espressioni casuali e un tono leggero.

Non renderla automaticamente formale.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "satono_diamond",
            displayName = "Satono Diamond",
            japaneseName = "サトノダイヤモンド",
            prompt = """
Parla generalmente in modo educato, dolce e composto.

Può diventare molto determinata quando parla di sfidare superstizioni o aspettative.

Conserva le forme cortesi effettivamente pronunciate.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "kitasan_black",
            displayName = "Kitasan Black",
            japaneseName = "キタサンブラック",
            prompt = """
Parla in modo energico, sincero, cordiale e relativamente diretto.

Può usare esclamazioni e un registro informale molto positivo.

Conserva l'energia e le forme colloquiali.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "sakura_chiyono_o",
            displayName = "Sakura Chiyono O",
            japaneseName = "サクラチヨノオー",
            prompt = """
Parla in modo educato, sincero e diligente.

Può utilizzare proverbi, massime o formulazioni apprese, talvolta in maniera particolare.

Non correggere automaticamente espressioni insolite se sono udibili.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "mejiro_ardan",
            displayName = "Mejiro Ardan",
            japaneseName = "メジロアルダン",
            prompt = """
Parla in modo molto calmo, elegante, educato e delicato.

Utilizza generalmente un registro raffinato e controllato.

Conserva forme cortesi e terminazioni femminili.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "yaeno_muteki",
            displayName = "Yaeno Muteki",
            japaneseName = "ヤエノムテキ",
            prompt = """
Parla in modo disciplinato, serio e rispettoso, influenzato dalla sua formazione nelle arti marziali.

Può utilizzare formulazioni rigide o formali.

Non colloquializzare il registro.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "mejiro_bright",
            displayName = "Mejiro Bright",
            japaneseName = "メジロブライト",
            prompt = """
Parla in modo estremamente tranquillo, morbido e spesso lento.

Può allungare il ritmo delle frasi e utilizzare un registro elegante.

Non eliminare pause o allungamenti solo per rendere la frase più rapida.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "sweep_tosho",
            displayName = "Sweep Tosho",
            japaneseName = "スイープトウショウ",
            prompt = """
Parla in modo infantile, capriccioso, assertivo e molto emotivo.

Può usare imperativi, proteste, esclamazioni e riferimenti alla magia.

Conserva forme infantili e brusche.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "air_shakur",
            displayName = "Air Shakur",
            japaneseName = "エアシャカール",
            prompt = """
Parla in modo brusco, tecnico e fortemente informale.

Può utilizzare slang, contrazioni, lessico informatico o matematico e terminazioni ruvide.

Non normalizzare il registro o semplificare termini tecnici.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "inari_one",
            displayName = "Inari One",
            japaneseName = "イナリワン",
            prompt = """
Parla in modo energico, diretto e tradizionalmente popolare, con caratteristiche associate al parlato di Edo/Tokyo.

Può utilizzare forme rudi o antiquate.

Non standardizzare automaticamente queste forme.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "copano_rickey",
            displayName = "Copano Rickey",
            japaneseName = "コパノリッキー",
            prompt = """
Parla in modo allegro, energico e amichevole, con frequenti riferimenti al feng shui e alla fortuna.

Può utilizzare terminologia specifica relativa a direzioni, colori e buona sorte.

Non correggere termini apparentemente insoliti appartenenti a questo dominio.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "yukino_bijin",
            displayName = "Yukino Bijin",
            japaneseName = "ユキノビジン",
            prompt = """
Parla con caratteristiche regionali del Tōhoku/Iwate, soprattutto quando è rilassata o emotiva.

Possono comparire forme dialettali accanto al giapponese standard.

Non normalizzare automaticamente il dialetto.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "seeking_the_pearl",
            displayName = "Seeking the Pearl",
            japaneseName = "シーキングザパール",
            prompt = """
Parla in modo estremamente sicuro, teatrale e cosmopolita.

Può inserire parole inglesi o straniere e usare formulazioni grandiose ed esclamative.

Conserva integralmente code-switching ed espressioni straniere.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "aston_machan",
            displayName = "Aston Machan",
            japaneseName = "アストンマーチャン",
            prompt = """
Parla in modo calmo, peculiare e deliberatamente memorabile, talvolta riferendosi a sé stessa in terza persona come マーチャン.

Può utilizzare formulazioni insolite legate al ricordo e alla propria presenza.

Non normalizzare autoreferenze o costruzioni eccentriche.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "yamanin_zephyr",
            displayName = "Yamanin Zephyr",
            japaneseName = "ヤマニンゼファー",
            prompt = """
Parla in modo calmo, poetico e contemplativo.

Utilizza frequentemente metafore e vocaboli relativi al vento e alla natura, talvolta con formulazioni poco comuni nel parlato quotidiano.

Non sostituire metafore insolite con formulazioni più ordinarie.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "nakayama_festa",
            displayName = "Nakayama Festa",
            japaneseName = "ナカヤマフェスタ",
            prompt = """
Parla in modo informale, sicuro e ruvido, con atteggiamento da giocatrice d'azzardo.

Può usare lessico relativo a rischio, scommesse e probabilità.

Conserva forme colloquiali e terminazioni dure.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "wonder_acute",
            displayName = "Wonder Acute",
            japaneseName = "ワンダーアキュート",
            prompt = """
Parla con modi molto tranquilli, gentili e antiquati, associati deliberatamente a una persona anziana.

Può utilizzare espressioni non comuni nel giapponese giovanile contemporaneo.

Non modernizzare il suo parlato.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "zenno_rob_roy",
            displayName = "Zenno Rob Roy",
            japaneseName = "ゼンノロブロイ",
            prompt = """
Parla generalmente in modo timido, educato e riservato.

Può utilizzare lessico letterario e riferimenti a libri, storie ed eroi.

Conserva formulazioni letterarie anche quando sembrano insolite nel parlato.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "hokko_tarumae",
            displayName = "Hokko Tarumae",
            japaneseName = "ホッコータルマエ",
            prompt = """
Parla in modo cordiale e relativamente naturale, con frequenti riferimenti a Tomakomai e alla promozione locale.

Può utilizzare nomi geografici, specialità regionali e terminologia da ambasciatrice locale.

Tratta questi elementi come possibili nomi propri.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "daitaku_helios",
            displayName = "Daitaku Helios",
            japaneseName = "ダイタクヘリオス",
            prompt = """
Parla con forte slang giovanile e gyaru, in modo rapido, informale ed estremamente energico.

Può usare abbreviazioni, parole inglesi, neologismi e forme non standard.

Non normalizzare il suo slang.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "shinko_windy",
            displayName = "Shinko Windy",
            japaneseName = "シンコウウインディ",
            prompt = """
Parla in modo infantile, dispettoso e aggressivamente giocoso.

Può usare forme semplici, esclamazioni e riferimenti al mordere.

Conserva costruzioni infantili e vocalizzazioni.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "twin_turbo",
            displayName = "Twin Turbo",
            japaneseName = "ツインターボ",
            prompt = """
Parla in modo estremamente energico, infantile e impulsivo, spesso riferendosi a sé stessa come ターボ.

Può gridare, ripetere parole e usare frasi grammaticalmente semplici.

Conserva autoreferenze e ripetizioni.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "mr_c_b",
            displayName = "Mr. C.B.",
            japaneseName = "ミスターシービー",
            prompt = """
Parla in modo rilassato, libero e informale.

Può usare frasi poco convenzionali e un tono spontaneo, evitando formulazioni troppo rigide.

Non rendere il parlato più formale.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "daiichi_ruby",
            displayName = "Daiichi Ruby",
            japaneseName = "ダイイチルビー",
            prompt = """
Parla in modo estremamente composto, formale e controllato.

Può utilizzare un lessico raffinato e mantenere distanza sociale anche nelle conversazioni quotidiane.

Conserva rigorosamente le forme cortesi.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "symboli_kris_s",
            displayName = "Symboli Kris S",
            japaneseName = "シンボリクリスエス",
            prompt = """
Parla generalmente in modo molto conciso, serio e controllato.

Può produrre frasi brevi e dirette, talvolta con una struttura percepita come rigida.

Non espandere frasi concise per renderle più naturali.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "sakura_laurel",
            displayName = "Sakura Laurel",
            japaneseName = "サクラローレル",
            prompt = """
Parla in modo calmo, maturo, positivo e generalmente gentile.

Può mantenere un tono morbido anche quando esprime forte determinazione.

Conserva il contrasto tra registro tranquillo e contenuto deciso.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "neo_universe",
            displayName = "Neo Universe",
            japaneseName = "ネオユニヴァース",
            prompt = """
Usa un linguaggio deliberatamente anomalo, astratto e quasi alieno.

Può impiegare termini scientifici, cosmologici, inglesi o formulazioni che sembrano semanticamente strane.

Non correggere una frase perché appare innaturale: l'anomalia può essere intenzionale.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "hishi_miracle",
            displayName = "Hishi Miracle",
            japaneseName = "ヒシミラクル",
            prompt = """
Parla in modo estremamente quotidiano, rilassato e ordinario.

Può usare esitazioni, lamentele e formulazioni colloquiali poco enfatiche.

Non trasformare il parlato in un registro eroico o particolarmente elegante.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "tanino_gimlet",
            displayName = "Tanino Gimlet",
            japaneseName = "タニノギムレット",
            prompt = """
Parla in modo teatrale, ruvido e deliberatamente grandioso, con lessico drammatico e talvolta difficile.

Può utilizzare formulazioni quasi chūnibyō.

Non normalizzare termini pomposi o insoliti.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "marvelous_sunday",
            displayName = "Marvelous Sunday",
            japaneseName = "マーベラスサンデー",
            prompt = """
Parla in modo estremamente energico ed eccentrico e utilizza frequentemente マーベラス come esclamazione, concetto o risposta.

Può produrre formulazioni volutamente vaghe o assurde.

Non sostituire o eliminare ripetizioni di マーベラス.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "katsuragi_ace",
            displayName = "Katsuragi Ace",
            japaneseName = "カツラギエース",
            prompt = """
Parla in modo energico, diretto e competitivo, con registro relativamente informale e vigoroso.

Può utilizzare terminazioni forti ed espressioni da sfida.

Conserva le forme rudi effettivamente pronunciate.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "sirius_symboli",
            displayName = "Sirius Symboli",
            japaneseName = "シリウスシンボリ",
            prompt = """
Parla in modo sicuro, provocatorio e dominante, generalmente informale.

Può utilizzare forme brusche, sarcasmo e terminazioni mascoline.

Non ammorbidire il registro.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "narita_top_road",
            displayName = "Narita Top Road",
            japaneseName = "ナリタトップロード",
            prompt = """
Parla in modo sincero, educato e fortemente determinato.

Può diventare molto enfatica ed emotiva durante gare e allenamenti.

Conserva esclamazioni e cambi di intensità.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "k_s_miracle",
            displayName = "K.S. Miracle",
            japaneseName = "ケイエスミラクル",
            prompt = """
Parla generalmente in modo gentile, controllato e modesto.

Può utilizzare formulazioni cortesi e un tono relativamente morbido.

Non aggiungere esitazioni o debolezza se non sono presenti nell'audio.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "mejiro_ramonu",
            displayName = "Mejiro Ramonu",
            japaneseName = "メジロラモーヌ",
            prompt = """
Parla in modo estremamente elegante, maturo e sicuro, con lessico raffinato e una particolare enfasi sul concetto di amore.

Può usare formulazioni sofisticate o sensuali.

Non semplificare il registro.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "tap_dance_city",
            displayName = "Tap Dance City",
            japaneseName = "タップダンスシチー",
            prompt = """
Parla in modo energico, diretto e spettacolare, con possibili parole o inflessioni inglesi legate alla sua caratterizzazione americana.

Conserva code-switching e formulazioni enfatiche.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "satono_crown",
            displayName = "Satono Crown",
            japaneseName = "サトノクラウン",
            prompt = """
Parla in modo vivace e sicuro.

Può inserire elementi linguistici o riferimenti collegati a Hong Kong e utilizzare occasionalmente parole non giapponesi.

Conserva integralmente eventuale code-switching.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "cheval_grand",
            displayName = "Cheval Grand",
            japaneseName = "シュヴァルグラン",
            prompt = """
Parla in modo timido, introverso e spesso esitante.

Può utilizzare pause, frasi brevi e formulazioni insicure, diventando più decisa in contesti competitivi.

Non eliminare esitazioni effettivamente udibili.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "vivlos",
            displayName = "Vivlos",
            japaneseName = "ヴィブロス",
            prompt = """
Parla in modo molto socievole, moderno, affettuoso e femminile.

Può usare linguaggio casuale e giovanile e un tono volutamente seducente o viziato.

Conserva slang e terminazioni colloquiali.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "biko_pegasus",
            displayName = "Biko Pegasus",
            japaneseName = "ビコーペガサス",
            prompt = """
Parla in modo estremamente energico e idealista, con linguaggio ispirato agli eroi e alla giustizia.

Può gridare slogan ed espressioni enfatiche.

Non eliminare ripetizioni o teatralità.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "duramente",
            displayName = "Duramente",
            japaneseName = "ドゥラメンテ",
            prompt = """
Parla in modo serio, intenso e molto diretto.

Può utilizzare frasi concise e un registro controllato, con forte enfasi competitiva.

Non aggiungere ornamenti o cordialità assenti.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "ikuno_dictus",
            displayName = "Ikuno Dictus",
            japaneseName = "イクノディクタス",
            prompt = """
Parla in modo razionale, disciplinato e generalmente formale.

Può utilizzare formulazioni precise e analitiche.

Conserva termini tecnici e costruzioni formali.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "transcend",
            displayName = "Transcend",
            japaneseName = "トランセンド",
            prompt = """
Parla con un registro moderno, casuale e fortemente influenzato dalla cultura digitale.

Può utilizzare slang di Internet, termini informatici, abbreviazioni ed espressioni da gamer.

Non normalizzare gergo digitale apparentemente insolito.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "rhein_kraft",
            displayName = "Rhein Kraft",
            japaneseName = "ラインクラフト",
            prompt = """
Parla in modo luminoso, sincero ed energico, generalmente con un registro naturale e femminile.

Può diventare fortemente enfatica in contesti competitivi.

Conserva interiezioni e intensificazioni.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "sounds_of_earth",
            displayName = "Sounds of Earth",
            japaneseName = "サウンズオブアース",
            prompt = """
Parla in modo molto teatrale e musicale, con frequenti riferimenti italiani e alla terminologia musicale.

Possono comparire parole italiane o straniere all'interno del giapponese.

Non tradurre né correggere questi elementi durante la trascrizione.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "north_flight",
            displayName = "North Flight",
            japaneseName = "ノースフライト",
            prompt = """
Parla in modo elegante, moderno e attento alla moda.

Può utilizzare terminologia relativa a stile, abbigliamento e immagine personale.

Conserva eventuali prestiti inglesi e termini fashion.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "jungle_pocket",
            displayName = "Jungle Pocket",
            japaneseName = "ジャングルポケット",
            prompt = """
Parla in modo estremamente energico, ruvido e competitivo, con forti caratteristiche mascoline e colloquiali.

Può usare contrazioni, grida e terminazioni aggressive.

Non normalizzare il registro.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "dream_journey",
            displayName = "Dream Journey",
            japaneseName = "ドリームジャーニー",
            prompt = """
Parla generalmente in modo educato, calmo e sofisticato, ma il tono può diventare più freddo o intimidatorio senza modificare necessariamente il livello di cortesia.

Conserva le forme cortesi anche quando il contenuto è minaccioso.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "calstone_light_o",
            displayName = "Calstone Light O",
            japaneseName = "カルストンライトオ",
            prompt = """
Parla in modo estremamente diretto e ossessionato dalla linearità e dall'andare dritto.

Può produrre formulazioni eccentriche e ripetitive relative a 直線 e concetti simili.

Non correggere ripetizioni apparentemente assurde.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "gentildonna",
            displayName = "Gentildonna",
            japaneseName = "ジェンティルドンナ",
            prompt = """
Parla in modo estremamente sicuro, dominante ed elegante.

Può usare formulazioni forti e autorevoli mantenendo un registro raffinato.

Non attenuare espressioni aggressive o categoriche.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "cesario",
            displayName = "Cesario",
            japaneseName = "シーザリオ",
            prompt = """
Parla generalmente in modo composto, intelligente e cortese.

Può utilizzare formulazioni precise e un registro relativamente maturo.

Conserva il livello di formalità effettivamente pronunciato.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "durandal",
            displayName = "Durandal",
            japaneseName = "デュランダル",
            prompt = """
Parla con un registro cavalleresco, solenne e fortemente teatrale.

Può usare lessico relativo a cavalieri, lealtà e servizio, con formulazioni non comuni nel parlato moderno.

Non modernizzare questo linguaggio.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "bubble_gum_fellow",
            displayName = "Bubble Gum Fellow",
            japaneseName = "バブルガムフェロー",
            prompt = """
Parla in modo sicuro, competitivo e relativamente maturo.

Può utilizzare espressioni dirette e provocatorie mantenendo un forte autocontrollo.

Conserva terminazioni brusche e registro colloquiale quando presenti.
""".trimIndent(),
        ),
        BuiltInSubject(
            id = "air_messiah",
            displayName = "Air Messiah",
            japaneseName = "エアメサイア",
            prompt = """
Parla generalmente in modo serio, composto e intelligente.

Può usare un registro educato e formulazioni riflessive.

Non semplificare costruzioni articolate o lessico meno comune.
""".trimIndent(),
        ),
    )
}
