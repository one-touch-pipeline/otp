/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
