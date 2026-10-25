package com.bernardo.ircontrol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IrEncoderTest {
    /** Decodifica os 32 bits de um frame NEC (LSB primeiro) a partir do padrão. */
    private fun necBits(p: IntArray): Long {
        var v = 0L
        for (i in 0 until 32) if (p[3 + 2 * i] > 1000) v = v or (1L shl i)
        return v
    }

    @Test fun samsungPowerIsE0E040BF() {
        val s = IrEncoder.encode(IrCode("NECX2", 7, 7, 2))!!
        assertEquals(38400, s.frequency)
        assertEquals(4512, s.pattern[0])
        // Samsung POWER = 0xE0E040BF (MSB-first na notação comum)
        assertEquals(java.lang.Long.reverse(0xE0E040BFL) ushr 32, necBits(s.pattern))
    }

    @Test fun necRepeatAndExtent() {
        val s = IrEncoder.encode(IrCode("NEC1", 4, -1, 8))!!
        assertEquals(9024, s.pattern[0])
        assertEquals(4, (necBits(s.pattern) and 0xFF))
        assertEquals(0xFB, (necBits(s.pattern) shr 8) and 0xFF) // S = ~D
        assertEquals(108000, s.pattern.take(68).sum())
    }

    @Test fun everyProtocolEncodes() {
        val protos = listOf(
            "NEC1", "NEC2", "NECX1", "NECX2", "NEC1-F16", "NEC2-F16", "PIONEER", "RC5", "RC6", "MCE",
            "SONY12", "SONY15", "SONY20", "JVC", "JVC2", "PANASONIC", "FUJITSU", "DENON", "SHARP",
            "MITSUBISHI", "AIWA", "PROTON", "RECS80", "RCA", "RCA38", "PANASONIC_OLD", "AKAI",
            "EMERSON", "THOMSON", "KONKA",
        )
        for (p in protos) {
            val s = IrEncoder.encode(IrCode(p, 5, 3, 21))
            assertNotNull(p, s)
            assertTrue(p, s!!.pattern.isNotEmpty() && s.pattern.all { it > 0 })
            assertTrue("$p muito longo", s.durationMs < 1500)
        }
    }

    @Test fun nameNormalization() {
        assertEquals("VOLUMEUP", RemoteKey.normalizeName("KEY_VOLUMEUP"))
        assertEquals("VOLUMEPLUS", RemoteKey.normalizeName("Volume +"))
        assertEquals("CHANNELMINUS", RemoteKey.normalizeName("CHANNEL -"))
        assertEquals("CHMINUS", RemoteKey.normalizeName("CH-"))
        assertEquals("AB", RemoteKey.normalizeName("A-B"))
    }
}
