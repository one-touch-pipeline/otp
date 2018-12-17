package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import static org.junit.Assert.assertEquals

@TestMixin(GrailsUnitTestMixin)
class SAMPlatformLabelTest {

    @Test
    void testExectMatch() {
        String label = "Illumina"
        SAMPlatformLabel expectedLabel = SAMPlatformLabel.ILLUMINA
        assertEquals(expectedLabel, SAMPlatformLabel.map(label))
    }

    @Test
    void testNotExectMatch() {
        String label = "ABI_SOLiD"
        SAMPlatformLabel expectedLabel = SAMPlatformLabel.SOLID
        assertEquals(expectedLabel, SAMPlatformLabel.map(label))
    }

    @Test(expected = IllegalArgumentException)
    void testNoMatch() {
        String label = "does-not-match-platform-label"
        SAMPlatformLabel.map(label)
    }

    @Test(expected = IllegalArgumentException)
    void testMultipleMatch() {
        String label = "capillary_solid"
        SAMPlatformLabel.map(label)
    }
}
