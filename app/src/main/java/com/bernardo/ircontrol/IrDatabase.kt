package com.bernardo.ircontrol

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

data class Brand(val name: String, val models: Int)

/** Um arquivo CSV do IR-DB (um controle). [functions] usa nomes normalizados. */
class IrModel(val brand: String, val category: String, val file: String, val functions: Map<String, IrCode>) {
    val id = "$category/$file"
    val title: String get() = category.removePrefix("Unknown_").replace('_', ' ')
    val subtitle: String get() = "Código $file · ${functions.size} teclas"
    val looksLikeTv: Boolean
        get() = !NOT_TV.containsMatchIn(category) &&
            RemoteKey.pick(RemoteKey.POWER, functions) != null &&
            (RemoteKey.pick(RemoteKey.VOL_UP, functions) != null || RemoteKey.pick(RemoteKey.CH_UP, functions) != null)
    val isTvCategory: Boolean get() = TV.containsMatchIn(category)

    fun code(key: RemoteKey): IrCode? = RemoteKey.pick(key, functions)

    companion object {
        private val TV = Regex("(?i)\\b(tv|hdtv|lcd|led|plasma|television|monitor)\\b|tv")
        private val NOT_TV = Regex(
            "(?i)air|cond|\\bac\\b|dvd|blu|\\bcd|vcr|vhs|receiver|amp|sound|audio|hifi|hi-fi|speaker|radio|tuner|" +
                "satellite|\\bsat|cable|camera|camcorder|fan|light|lamp|heater|projector|dss|dvr|recorder|phono|" +
                "karaoke|laser|stereo|deck|tape|car|toy|robot|ipod|media|player|set top|stb|converter"
        )
    }
}

object IrDatabase {
    private var brandsCache: List<Brand>? = null

    fun brands(ctx: Context): List<Brand> = brandsCache ?: ctx.assets.open("brands.tsv").bufferedReader().useLines { lines ->
        lines.mapNotNull { l -> l.split('\t').takeIf { it.size == 2 }?.let { Brand(it[0], it[1].toInt()) } }.toList()
    }.also { brandsCache = it }

    /** Lê todos os controles da marca (TVs primeiro). */
    fun models(ctx: Context, brand: String): List<IrModel> {
        val byFile = LinkedHashMap<String, Pair<String, LinkedHashMap<String, IrCode>>>()
        val prefix = "$brand\t"
        BufferedReader(InputStreamReader(GZIPInputStream(ctx.assets.open("codes.tsv.gz")), Charsets.UTF_8)).use { r ->
            var started = false
            while (true) {
                val line = r.readLine() ?: break
                if (!line.startsWith(prefix)) { if (started) break else continue }
                started = true
                val p = line.split('\t')
                if (p.size != 8) continue
                val fns = byFile.getOrPut(p[1] + "/" + p[2]) { p[1] to LinkedHashMap() }.second
                val name = RemoteKey.normalizeName(p[3])
                if (name.isNotEmpty() && name !in fns)
                    fns[name] = IrCode(p[4], p[5].toInt(), p[6].toInt(), p[7].toInt())
            }
        }
        return byFile.map { (k, v) -> IrModel(brand, v.first, k.substringAfter('/'), v.second) }
            .sortedWith(compareByDescending<IrModel> { it.isTvCategory }.thenByDescending { it.looksLikeTv }.thenBy { it.title.lowercase() })
    }
}
