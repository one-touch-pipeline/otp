package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*

@TestFor(AntibodyTarget)
class AntibodyTargetTests {

    static final String VALID_NAME = "a1Az2Z"

    static final String INVALID_NAME = "a1Az2Z/"

    @Test
    void testValidName() {
        AntibodyTarget antibodyTarget = new AntibodyTarget(
            name: VALID_NAME)
        assertTrue antibodyTarget.validate()
    }

    @Test
    void testInvalidName() {
        AntibodyTarget antibodyTarget = new AntibodyTarget(
            name: INVALID_NAME)
        assertFalse antibodyTarget.validate()
    }

    @Test
    void testUniqueName() {
        AntibodyTarget antibodyTarget = new AntibodyTarget(
            name: VALID_NAME)
        antibodyTarget.save(flush: true)

        antibodyTarget = new AntibodyTarget(
            name: VALID_NAME)
        assertFalse antibodyTarget.validate()
    }

    @Test
    void testBlankName() {
        AntibodyTarget antibodyTarget = new AntibodyTarget(
            name: "")
        assertFalse antibodyTarget.validate()
    }

    @Test
    void testNullName() {
        AntibodyTarget antibodyTarget = new AntibodyTarget()
        assertFalse antibodyTarget.validate()
    }
}
