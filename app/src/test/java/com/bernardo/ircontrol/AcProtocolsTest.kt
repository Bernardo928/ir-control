package com.bernardo.ircontrol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcProtocolsTest {
    @Test fun greeChecksumMatchesKnownState() {
        // estado padrão de um controle Gree, com checksum 0x5 no nibble alto do último byte
        val b = intArrayOf(0x00, 0x09, 0x20, 0x50, 0x00, 0x20, 0x00, 0x00)
        greeChecksum(b)
        assertEquals(0x50, b[7])
    }

    @Test fun greeCool24() {
        val b = greeBytes(AcState(power = true, mode = AcMode.COOL, temp = 24, fan = AcFan.AUTO))
        assertEquals(0x09, b[0]) // cool + power
        assertEquals(8, b[1])
    }

    @Test fun coolixKnownCodes() {
        assertEquals(0xB2BF40L, coolixState(AcState(power = true, mode = AcMode.COOL, temp = 24, fan = AcFan.AUTO)))
        assertEquals(0xB2BFE4L, coolixState(AcState(power = true, mode = AcMode.FAN, fan = AcFan.AUTO)))
    }

    @Test fun lgOff() {
        assertEquals(0x88C0051L, lgValue(AcState(power = false)))
        assertEquals(0x8800909L, lgValue(AcState(power = true, mode = AcMode.COOL, temp = 24, fan = AcFan.LOW)))
    }

    @Test fun allProtocolsEncode() {
        for (p in AcProtocol.entries) for (on in listOf(true, false)) {
            val s = p.encode(AcState(power = on))
            assertTrue(p.name, s.pattern.isNotEmpty() && s.pattern.all { it > 0 } && s.pattern.size % 2 == 0)
            assertTrue(p.name, s.durationMs < 1500)
        }
        assertTrue(AcProtocol.COOLIX.swingToggle() != null)
    }
}
