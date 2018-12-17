package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.TestFor
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

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
