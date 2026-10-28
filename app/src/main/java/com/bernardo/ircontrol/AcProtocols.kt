package com.bernardo.ircontrol

/** Estado desejado do ar-condicionado. Os controles de AC enviam o estado inteiro a cada toque. */
data class AcState(
    val power: Boolean = false,
    val mode: AcMode = AcMode.COOL,
    val temp: Int = 24,
    val fan: AcFan = AcFan.AUTO,
    val swing: Boolean = false,
)

enum class AcMode(val label: String) { AUTO("Auto"), COOL("Frio"), DRY("Seco"), FAN("Ventilar"), HEAT("Quente") }
enum class AcFan(val label: String) { AUTO("Auto"), LOW("Baixa"), MED("Média"), HIGH("Alta") }

/**
 * Protocolos de AC baseados em estado (estrutura conforme a biblioteca aberta IRremoteESP8266).
 * Durações positivas = mark, negativas = space (µs).
 */
enum class AcProtocol(val title: String) {
    GREE("Gree (YB/YAA)"),
    COOLIX("Midea / Coolix"),
    LG("LG"),
    LG2("LG (modelos novos)");

    /** Sinal para aplicar [s] (ou desligar, se [s].power == false). */
    fun encode(s: AcState): IrSignal = when (this) {
        GREE -> gree(s)
        COOLIX -> coolix(if (s.power) coolixState(s) else 0xB27BE0)
        LG, LG2 -> lg(s, this == LG2)
    }

    /** Alguns protocolos (Coolix) mandam o vaivém como um comando separado de liga/desliga. */
    fun swingToggle(): IrSignal? = if (this == COOLIX) coolix(0xB26BE0) else null

    val swingInState: Boolean get() = this == GREE
    val tempRange: IntRange get() = if (this == COOLIX) 17..30 else 16..30
}

private class Pulses {
    val d = ArrayList<Int>()
    fun add(v: Int) { if (d.isNotEmpty() && (d.last() > 0) == (v > 0)) d[d.size - 1] += v else d.add(v) }
    fun bits(value: Long, n: Int, mark: Int, one: Int, zero: Int, msb: Boolean) {
        for (i in 0 until n) {
            val bit = if (msb) (value shr (n - 1 - i)) and 1L else (value shr i) and 1L
            add(mark); add(-(if (bit == 1L) one else zero))
        }
    }
    fun signal(freq: Int) = IrSignal(freq, IntArray(d.size) { kotlin.math.abs(d[it]) })
}

// ---------- Gree ----------

internal fun greeBytes(s: AcState): IntArray {
    val mode = when (s.mode) { AcMode.AUTO -> 0; AcMode.COOL -> 1; AcMode.DRY -> 2; AcMode.FAN -> 3; AcMode.HEAT -> 4 }
    val fan = when (s.fan) { AcFan.AUTO -> 0; AcFan.LOW -> 1; AcFan.MED -> 2; AcFan.HIGH -> 3 }
    val on = if (s.power) 1 else 0
    val b = IntArray(8)
    b[0] = mode or (on shl 3) or (fan shl 4) or ((if (s.swing) 1 else 0) shl 6)
    b[1] = s.temp.coerceIn(16, 30) - 16
    b[2] = 0x20 or (on shl 6) // luz do display + "Power2"
    b[3] = 0x50
    b[4] = if (s.swing) 1 else 0
    b[5] = 0x20
    b[6] = 0
    return b.also { greeChecksum(it) }
}

internal fun greeChecksum(b: IntArray) {
    var sum = 10
    for (i in 0..3) sum += b[i] and 0xF
    for (i in 4..6) sum += b[i] shr 4
    b[7] = ((sum and 0xF) shl 4) or (b[7] and 0xF)
}

private fun gree(s: AcState): IrSignal {
    val b = greeBytes(s)
    val p = Pulses()
    p.add(9000); p.add(-4500)
    for (i in 0..3) p.bits(b[i].toLong(), 8, 620, 1600, 540, false)
    p.bits(0b010, 3, 620, 1600, 540, false)
    p.add(620); p.add(-19980)
    for (i in 4..7) p.bits(b[i].toLong(), 8, 620, 1600, 540, false)
    p.add(620); p.add(-19980)
    return p.signal(38000)
}

// ---------- Coolix (Midea, Springer, Carrier...) ----------

private val COOLIX_TEMP = intArrayOf(0b0000, 0b0001, 0b0011, 0b0010, 0b0110, 0b0111, 0b0101, 0b0100, 0b1100, 0b1101, 0b1001, 0b1000, 0b1010, 0b1011)

internal fun coolixState(s: AcState): Long {
    var mode = when (s.mode) { AcMode.COOL -> 0; AcMode.DRY -> 1; AcMode.AUTO -> 2; AcMode.HEAT -> 3; AcMode.FAN -> 1 }
    var temp = COOLIX_TEMP[s.temp.coerceIn(17, 30) - 17]
    if (s.mode == AcMode.FAN) { temp = 0b1110; mode = 1 }
    val fan = when {
        s.mode == AcMode.AUTO || s.mode == AcMode.DRY -> 0b000
        s.fan == AcFan.LOW -> 0b100
        s.fan == AcFan.MED -> 0b010
        s.fan == AcFan.HIGH -> 0b001
        else -> 0b101
    }
    return (0xB2L shl 16) or (((fan shl 5) or 0x1F).toLong() shl 8) or ((temp shl 4) or (mode shl 2)).toLong()
}

private fun coolix(v: Long): IrSignal {
    val p = Pulses()
    repeat(2) {
        p.add(4692); p.add(-4416)
        for (shift in intArrayOf(16, 8, 0)) {
            val byte = (v shr shift) and 0xFF
            p.bits(byte, 8, 552, 1656, 552, true)
            p.bits(byte.inv() and 0xFF, 8, 552, 1656, 552, true)
        }
        p.add(552); p.add(-5244)
    }
    return p.signal(38000)
}

// ---------- LG ----------

internal fun lgValue(s: AcState): Long {
    if (!s.power) return 0x88C0051
    val mode = when (s.mode) { AcMode.COOL -> 0; AcMode.DRY -> 1; AcMode.FAN -> 2; AcMode.AUTO -> 3; AcMode.HEAT -> 4 }
    val fan = when (s.fan) { AcFan.LOW -> 0; AcFan.MED -> 2; AcFan.HIGH -> 4; AcFan.AUTO -> 5 }
    val n = intArrayOf(0, mode, s.temp.coerceIn(16, 30) - 15, fan)
    val sum = n.sum() and 0xF
    return (0x88L shl 20) or (n[0].toLong() shl 16) or (n[1].toLong() shl 12) or (n[2].toLong() shl 8) or (n[3].toLong() shl 4) or sum.toLong()
}

private fun lg(s: AcState, v2: Boolean): IrSignal {
    val p = Pulses()
    if (v2) { p.add(3200); p.add(-9900) } else { p.add(8500); p.add(-4250) }
    val mark = if (v2) 480 else 550
    p.bits(lgValue(s), 28, mark, 1600, 550, true)
    p.add(mark); p.add(-40000)
    return p.signal(38000)
}

/** Marcas vendidas no Brasil e os protocolos que costumam usar (o primeiro é o mais provável). */
data class AcBrand(val name: String, val protocols: List<AcProtocol>)

object AcBrands {
    private val G = AcProtocol.GREE
    private val C = AcProtocol.COOLIX
    private val L = listOf(AcProtocol.LG, AcProtocol.LG2)

    val all: List<AcBrand> = listOf(
        AcBrand("Gree", listOf(G)),
        AcBrand("Midea", listOf(C)),
        AcBrand("Springer Midea", listOf(C)),
        AcBrand("Springer", listOf(C)),
        AcBrand("Carrier", listOf(C)),
        AcBrand("LG", L),
        AcBrand("Elgin", listOf(G, C)),
        AcBrand("Agratto", listOf(G, C)),
        AcBrand("Philco", listOf(G, C)),
        AcBrand("Electrolux", listOf(C, G)),
        AcBrand("Consul", listOf(C, G)),
        AcBrand("Britânia", listOf(G, C)),
        AcBrand("Komeco", listOf(G, C)),
        AcBrand("TCL", listOf(C, G)),
        AcBrand("Hisense", listOf(C, G)),
        AcBrand("Comfee", listOf(C)),
        AcBrand("Toshiba", listOf(C)),
        AcBrand("Outra marca (testar todos)", AcProtocol.entries.toList()),
    )

    fun find(name: String?) = all.firstOrNull { it.name == name }
}
