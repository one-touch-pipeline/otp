package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import org.junit.*

@TestFor(AntibodyTargetService)
@Build([
        AntibodyTarget
])
class AntibodyTargetServiceUnitTests {

    AntibodyTargetService antibodyTargetService

    final static String ANTIBODY_TARGET ="AntibodyTarget"

    @Before
    public void setUp() {
        antibodyTargetService = new AntibodyTargetService()
    }


    @After
    public void tearDown() {
        antibodyTargetService = null
    }

    @Test
    void testCreateAntibodyTargetUsingAntibodyTarget() {
        assertEquals(
                antibodyTargetService.createAntibodyTarget(ANTIBODY_TARGET),
                AntibodyTarget.findByNameIlike(ANTIBODY_TARGET)
        )
    }

    @Test
    void testCreateAntibodyTargetUsingAntibodyTargetTwice() {
        antibodyTargetService.createAntibodyTarget(ANTIBODY_TARGET)
        shouldFail(AssertionError) {
            antibodyTargetService.createAntibodyTarget(ANTIBODY_TARGET)
        }
    }

    @Test
    void testCreateAntibodyTargetUsingNull() {
        shouldFail(AssertionError) {
            antibodyTargetService.createAntibodyTarget(null)
        }
    }
}

