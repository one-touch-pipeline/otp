package de.dkfz.tbi.otp.hipo

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull

class HipoTissueTypeUnitTest {

    @Test
    public void testFromKey() {
        assertEquals(HipoTissueType.BLOOD, HipoTissueType.fromKey("B"))
        assertNull(HipoTissueType.fromKey("-"))
    }
}
