package com.bernardo.ircontrol

import kotlin.math.roundToInt

/** Sinal pronto para o ConsumerIrManager: frequência (Hz) e durações alternadas liga/desliga (µs). */
class IrSignal(val frequency: Int, val pattern: IntArray) {
    val key: String = frequency.toString() + ":" + pattern.joinToString(",")
    val durationMs: Long get() = pattern.sum() / 1000L
}

/** Uma entrada do IR-DB: protocolo + device/subdevice/function (-1 = ausente). */
data class IrCode(val protocol: String, val device: Int, val subdevice: Int, val function: Int)

/**
 * Converte códigos do IR-DB (notação IRP) em padrões de pulsos.
 * Durações positivas = portadora ligada (mark), negativas = desligada (space).
 */
object IrEncoder {

    private class Builder(val unit: Double) {
        val d = ArrayList<Double>()
        private var frameStart = 0.0
        private var total = 0.0

        /** Durações em múltiplos de [unit]. */
        fun u(vararg units: Double) = units.forEach { us(it * unit) }

        /** Duração em microssegundos (sinal define mark/space). */
        fun us(v: Double) {
            if (v == 0.0) return
            if (d.isNotEmpty() && (d.last() > 0) == (v > 0)) d[d.size - 1] = d.last() + v else d.add(v)
            total += kotlin.math.abs(v)
        }

        fun bits(value: Long, n: Int, zero: DoubleArray, one: DoubleArray, msb: Boolean = false) {
            for (i in 0 until n) {
                val bit = if (msb) (value shr (n - 1 - i)) and 1L else (value shr i) and 1L
                u(*(if (bit == 1L) one else zero))
            }
        }

        fun startFrame() { frameStart = total }

        /** Completa o frame com silêncio até atingir [extentUs] desde o início do frame. */
        fun extent(extentUs: Double) {
            val rest = extentUs - (total - frameStart)
            us(-(if (rest > 0) rest else unit))
        }
    }

    private fun p(vararg v: Double) = v

    fun encode(c: IrCode): IrSignal? {
        val D = c.device.toLong()
        val S = c.subdevice.toLong()
        val F = c.function.toLong()
        val hasS = c.subdevice >= 0
        val pulse = p(1.0, -1.0) to p(1.0, -3.0)

        return when (c.protocol) {
            "NEC1", "NEC2", "NECX1", "NECX2", "NEC1-F16", "NEC2-F16", "PIONEER" -> {
                val x = c.protocol.startsWith("NECX")
                val pioneer = c.protocol == "PIONEER"
                val f16 = c.protocol.endsWith("F16")
                val sub = if (hasS) S else if (x) D else D.inv() and 0xFF
                val freq = if (pioneer) 40000 else 38400
                val b = Builder(564.0)
                val frame = {
                    b.startFrame()
                    b.u(if (x) 8.0 else 16.0, -8.0)
                    b.bits(D, 8, pulse.first, pulse.second)
                    b.bits(sub, 8, pulse.first, pulse.second)
                    if (f16) b.bits(F, 16, pulse.first, pulse.second)
                    else {
                        b.bits(F, 8, pulse.first, pulse.second)
                        b.bits(F.inv() and 0xFF, 8, pulse.first, pulse.second)
                    }
                    b.u(1.0)
                    b.extent(108000.0)
                }
                frame()
                if (c.protocol == "NEC1" || c.protocol == "NEC1-F16") {
                    b.startFrame(); b.u(16.0, -4.0, 1.0); b.extent(108000.0)
                } else frame()
                b.out(freq)
            }

            "RC5" -> {
                val b = Builder(889.0)
                repeat(2) {
                    b.startFrame()
                    val zero = p(1.0, -1.0); val one = p(-1.0, 1.0)
                    b.bits(1, 1, zero, one, true)
                    b.bits(((F shr 6) and 1).inv() and 1, 1, zero, one, true)
                    b.bits(0, 1, zero, one, true) // toggle
                    b.bits(D, 5, zero, one, true)
                    b.bits(F and 0x3F, 6, zero, one, true)
                    b.extent(114000.0)
                }
                b.out(36000)
            }

            "RC6", "MCE" -> {
                val b = Builder(444.0)
                val zero = p(-1.0, 1.0); val one = p(1.0, -1.0)
                repeat(2) {
                    b.startFrame()
                    b.u(6.0, -2.0)
                    b.bits(1, 1, zero, one, true)
                    if (c.protocol == "RC6") {
                        b.bits(0, 3, zero, one, true)
                        b.u(-2.0, 2.0) // trailer (toggle 0)
                        b.bits(D, 8, zero, one, true)
                        b.bits(F, 8, zero, one, true)
                    } else {
                        b.bits(6, 3, zero, one, true)
                        b.u(-2.0, 2.0)
                        b.bits(128, 8, zero, one, true)
                        b.bits(if (hasS) S else 15, 8, zero, one, true)
                        b.bits(0, 1, zero, one, true) // toggle
                        b.bits(D, 7, zero, one, true)
                        b.bits(F, 8, zero, one, true)
                    }
                    b.extent(107000.0)
                }
                b.out(36000)
            }

            "SONY12", "SONY15", "SONY20" -> {
                val b = Builder(600.0)
                repeat(3) {
                    b.startFrame()
                    b.u(4.0, -1.0)
                    val z = p(1.0, -1.0); val o = p(2.0, -1.0)
                    b.bits(F, 7, z, o)
                    when (c.protocol) {
                        "SONY12" -> b.bits(D, 5, z, o)
                        "SONY15" -> b.bits(D, 8, z, o)
                        else -> { b.bits(D, 5, z, o); b.bits(if (hasS) S else 0, 8, z, o) }
                    }
                    b.extent(45000.0)
                }
                b.out(40000)
            }

            "JVC", "JVC2" -> {
                val b = Builder(525.0)
                if (c.protocol == "JVC") b.u(16.0, -8.0)
                repeat(2) {
                    b.bits(D, 8, pulse.first, pulse.second)
                    b.bits(F, 8, pulse.first, pulse.second)
                    b.u(1.0, -45.0)
                }
                b.out(38000)
            }

            "PANASONIC" -> {
                val b = Builder(432.0)
                val sub = if (hasS) S else 0
                repeat(2) {
                    b.u(8.0, -4.0)
                    for (v in longArrayOf(2, 32, D, sub, F, (D xor sub xor F) and 0xFF))
                        b.bits(v, 8, pulse.first, pulse.second)
                    b.u(1.0, -173.0)
                }
                b.out(37000)
            }

            "FUJITSU" -> {
                val b = Builder(432.0)
                val sub = if (hasS) S else 0
                repeat(2) {
                    b.u(8.0, -4.0)
                    b.bits(20, 8, pulse.first, pulse.second)
                    b.bits(99, 8, pulse.first, pulse.second)
                    b.bits(0, 8, pulse.first, pulse.second)
                    b.bits(D, 8, pulse.first, pulse.second)
                    b.bits(sub, 8, pulse.first, pulse.second)
                    b.bits(F, 8, pulse.first, pulse.second)
                    b.u(1.0, -110.0)
                }
                b.out(37000)
            }

            "DENON", "SHARP" -> {
                val b = Builder(264.0)
                val z = p(1.0, -3.0); val o = p(1.0, -7.0)
                val sharp = c.protocol == "SHARP"
                repeat(2) {
                    b.bits(D, 5, z, o); b.bits(F, 8, z, o)
                    b.bits(if (sharp) 1 else 0, 2, z, o); b.u(1.0, -165.0)
                    b.bits(D, 5, z, o); b.bits(F.inv() and 0xFF, 8, z, o)
                    b.bits(if (sharp) 2 else 3, 2, z, o); b.u(1.0, -165.0)
                }
                b.out(38000)
            }

            "MITSUBISHI" -> {
                val b = Builder(300.0)
                repeat(2) {
                    b.bits(D, 8, p(1.0, -3.0), p(1.0, -7.0))
                    b.bits(F, 8, p(1.0, -3.0), p(1.0, -7.0))
                    b.u(1.0, -80.0)
                }
                b.out(32600)
            }

            "AIWA" -> {
                val b = Builder(550.0)
                val sub = if (hasS) S else 0
                b.u(16.0, -8.0)
                b.bits(D, 8, pulse.first, pulse.second)
                b.bits(sub, 5, pulse.first, pulse.second)
                b.bits(D.inv() and 0xFF, 8, pulse.first, pulse.second)
                b.bits(sub.inv() and 0x1F, 5, pulse.first, pulse.second)
                b.bits(F, 8, pulse.first, pulse.second)
                b.bits(F.inv() and 0xFF, 8, pulse.first, pulse.second)
                b.u(1.0, -42.0, 16.0, -8.0, 1.0, -165.0)
                b.out(38000)
            }

            "PROTON" -> {
                val b = Builder(500.0)
                repeat(2) {
                    b.startFrame()
                    b.u(16.0, -8.0)
                    b.bits(D, 8, pulse.first, pulse.second)
                    b.u(1.0, -8.0)
                    b.bits(F, 8, pulse.first, pulse.second)
                    b.u(1.0)
                    b.extent(63000.0)
                }
                b.out(38000)
            }

            "RECS80" -> {
                val b = Builder(158.0)
                val z = p(1.0, -31.0); val o = p(1.0, -47.0)
                repeat(2) {
                    b.bits(1, 1, z, o, true); b.bits(0, 1, z, o, true)
                    b.bits(D, 3, z, o, true); b.bits(F, 6, z, o, true)
                    b.u(1.0); b.us(-45000.0)
                }
                b.out(38000)
            }

            "RCA", "RCA38" -> {
                val b = Builder(460.0)
                val z = p(1.0, -2.0); val o = p(1.0, -4.0)
                repeat(2) {
                    b.u(8.0, -8.0)
                    b.bits(D, 4, z, o, true); b.bits(F, 8, z, o, true)
                    b.bits(D.inv() and 0xF, 4, z, o, true); b.bits(F.inv() and 0xFF, 8, z, o, true)
                    b.u(1.0, -16.0)
                }
                b.out(if (c.protocol == "RCA") 58000 else 38700)
            }

            "PANASONIC_OLD" -> {
                val b = Builder(833.0)
                repeat(2) {
                    b.u(4.0, -4.0)
                    b.bits(D, 5, pulse.first, pulse.second); b.bits(F, 6, pulse.first, pulse.second)
                    b.bits(D.inv() and 0x1F, 5, pulse.first, pulse.second)
                    b.bits(F.inv() and 0x3F, 6, pulse.first, pulse.second)
                    b.u(1.0); b.us(-44000.0)
                }
                b.out(57600)
            }

            "AKAI" -> {
                val b = Builder(289.0)
                repeat(2) {
                    b.startFrame()
                    b.bits(D, 3, p(1.0, -2.6), p(1.0, -6.3)); b.bits(F, 7, p(1.0, -2.6), p(1.0, -6.3))
                    b.u(1.0); b.extent(25300.0)
                }
                b.out(38000)
            }

            "EMERSON" -> {
                val b = Builder(872.0)
                repeat(2) {
                    b.u(4.0, -4.0)
                    b.bits(D, 6, pulse.first, pulse.second); b.bits(F, 6, pulse.first, pulse.second)
                    b.bits(D.inv() and 0x3F, 6, pulse.first, pulse.second)
                    b.bits(F.inv() and 0x3F, 6, pulse.first, pulse.second)
                    b.u(1.0, -39.0)
                }
                b.out(36700)
            }

            "THOMSON" -> {
                val b = Builder(500.0)
                repeat(2) {
                    b.startFrame()
                    b.bits(D, 4, p(1.0, -4.0), p(1.0, -9.0)); b.bits(0, 1, p(1.0, -4.0), p(1.0, -9.0))
                    b.bits(F, 7, p(1.0, -4.0), p(1.0, -9.0))
                    b.u(1.0); b.extent(80000.0)
                }
                b.out(33000)
            }

            "KONKA" -> {
                val b = Builder(500.0)
                repeat(2) {
                    b.u(6.0, -6.0)
                    b.bits(D, 8, p(1.0, -3.0), p(1.0, -5.0), true); b.bits(F, 8, p(1.0, -3.0), p(1.0, -5.0), true)
                    b.u(1.0, -8.0, 1.0, -46.0)
                }
                b.out(38000)
            }

            else -> null
        }
    }

    private fun Builder.out(freq: Int): IrSignal {
        while (d.isNotEmpty() && d.first() < 0) d.removeAt(0)
        return IrSignal(freq, IntArray(d.size) { kotlin.math.abs(d[it]).roundToInt() })
    }
}
