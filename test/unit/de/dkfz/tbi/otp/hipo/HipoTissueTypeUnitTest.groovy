package de.dkfz.tbi.otp.hipo;

import static org.junit.Assert.*

class HipoTissueTypeUnitTest {

    public void testFromKey() {
        assertEquals(HipoTissueType.BLOOD, HipoTissueType.fromKey("B"))
        assertNull(HipoTissueType.fromKey("-"))
    }

}
