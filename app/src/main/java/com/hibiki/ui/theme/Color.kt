package com.hibiki.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette cyberpunk Hibiki — unica fonte per i colori dell'app (stessi token di Arashi).
 * Usare solo i token base qui sotto; per varianti con alpha usare [withAlpha].
 * I sorgenti UI non devono definire altri colori.
 */
object Cyberpunk {

    // ── Superfici ──────────────────────────────────────────────────────────────

    /** rgb(5, 7, 15) */
    val Void = Color(0xFF05070F)

    /** rgb(10, 14, 26) */
    val Deep = Color(0xFF0A0E1A)

    /** rgb(18, 24, 42) */
    val Panel = Color(0xFF12182A)

    /** rgb(26, 34, 56) */
    val PanelElevated = Color(0xFF1A2238)

    /** [Panel] @ 92% — bottom bar, card, dialog */
    val PanelTranslucent: Color get() = withAlpha(Panel, 0.92f)

    /** [PanelElevated] @ 50% — browse list / catalog clickable rows */
    val PanelElevatedTranslucent: Color get() = withAlpha(PanelElevated, 0.50f)

    // ── Accento neon ───────────────────────────────────────────────────────────

    /** rgb(31, 224, 205) — primario */
    val NeonCyan = Color(0xFF1FE0CD)

    /** rgb(255, 43, 214) */
    val NeonMagenta = Color(0xFFFF2BD6)

    /** rgb(184, 255, 60) */
    val NeonLime = Color(0xFFB8FF3C)

    /** rgb(149, 43, 255) — API / arricchimento AI */
    val NeonViolet = Color(0xFF952BFF)

    // ── Testo ──────────────────────────────────────────────────────────────────

    /** rgb(232, 247, 255) */
    val TextPrimary = Color(0xFFE8F7FF)

    /** rgb(143, 163, 184) */
    val TextMuted = Color(0xFF8FA3B8)

    /** rgb(130, 170, 187) — accento cyan attenuato */
    val MutedCyan = Color(0xFF82AABB)

    /** rgb(156, 149, 188) — accento magenta attenuato */
    val MutedMagenta = Color(0xFF9C95BC)

    /** rgb(148, 174, 169) — accento lime attenuato */
    val MutedLime = Color(0xFF94AEA9)

    /** rgb(144, 149, 193) — accento violet attenuato; audio/testi inglese */
    val MutedViolet = Color(0xFF9095C1)

    // ── Utilità ────────────────────────────────────────────────────────────────

    /** trasparente */
    val Transparent = Color(0x00000000)

    /** [NeonCyan] @ ~13% — griglia / separatori */
    val GridLine: Color get() = withAlpha(NeonCyan, 0.13f)

    // ── Gradienti (liste di colori della palette) ──────────────────────────────

    /** Ciclo spettro: kanji home, glow menu, glow caricamento. */
    val Spectrum = listOf(
        NeonCyan,
        NeonMagenta,
        NeonLime,
        NeonViolet,
        NeonCyan,
    )

    /** Indicatore nav, barra titolo pagina (verticale). */
    val MagentaLime = listOf(
        NeonMagenta,
        NeonLime,
    )

    /** Gradiente orizzontale sotto il titolo pagina — trasparente dal 80%. */
    val TitleUnderlineGradient = arrayOf(
        0f to withAlpha(NeonMagenta, 0.85f),
        0.28f to withAlpha(NeonLime, 0.7f),
        0.55f to withAlpha(NeonCyan, 0.18f),
        0.8f to Transparent,
        1f to Transparent,
    )

    /** Applica alpha a un colore della palette (animazioni, glow, scrim). */
    fun withAlpha(base: Color, alpha: Float): Color =
        base.copy(alpha = alpha.coerceIn(0f, 1f))
}

data class CyberpunkColorSwatch(
    val token: String,
    val color: Color,
)

/** Elenco consultabile dei colori base della palette di sistema. */
val CyberpunkColorCatalog: List<CyberpunkColorSwatch> = listOf(
    CyberpunkColorSwatch("Void", Cyberpunk.Void),
    CyberpunkColorSwatch("Deep", Cyberpunk.Deep),
    CyberpunkColorSwatch("Panel", Cyberpunk.Panel),
    CyberpunkColorSwatch("PanelElevated", Cyberpunk.PanelElevated),
    CyberpunkColorSwatch("NeonCyan", Cyberpunk.NeonCyan),
    CyberpunkColorSwatch("MutedCyan", Cyberpunk.MutedCyan),
    CyberpunkColorSwatch("NeonMagenta", Cyberpunk.NeonMagenta),
    CyberpunkColorSwatch("MutedMagenta", Cyberpunk.MutedMagenta),
    CyberpunkColorSwatch("NeonLime", Cyberpunk.NeonLime),
    CyberpunkColorSwatch("MutedLime", Cyberpunk.MutedLime),
    CyberpunkColorSwatch("NeonViolet", Cyberpunk.NeonViolet),
    CyberpunkColorSwatch("MutedViolet", Cyberpunk.MutedViolet),
    CyberpunkColorSwatch("TextPrimary", Cyberpunk.TextPrimary),
    CyberpunkColorSwatch("TextMuted", Cyberpunk.TextMuted),
)

fun Color.toDisplayHex(): String {
    val alphaChannel = (alpha * 255f).toInt().coerceIn(0, 255)
    val redChannel = (red * 255f).toInt().coerceIn(0, 255)
    val greenChannel = (green * 255f).toInt().coerceIn(0, 255)
    val blueChannel = (blue * 255f).toInt().coerceIn(0, 255)
    return if (alphaChannel == 255) {
        String.format("#%02X%02X%02X", redChannel, greenChannel, blueChannel)
    } else {
        String.format("#%02X%02X%02X%02X", alphaChannel, redChannel, greenChannel, blueChannel)
    }
}
