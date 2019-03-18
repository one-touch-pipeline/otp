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

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption

@TestMixin(ControllerUnitTestMixin) // Workaround for Grails bug GRAILS-11136
@Build([ProcessingOption, SampleIdentifier])
class SampleIdentifierUnitTest {

    static private String CORRECT_NAME = 'name'

    private SampleIdentifier sampleIdentifier



    @Before
    void setUp() {
        sampleIdentifier = new SampleIdentifier(
            name: CORRECT_NAME,
            sample: Sample.build())
        assert sampleIdentifier.validate()
    }



    @Test
    void test_validate_fail_nameIsNull() {
        sampleIdentifier.name = null

        TestCase.assertValidateError(sampleIdentifier, 'name', 'nullable', null)
    }


    @Test
    void test_validate_fail_sampleIsNull() {
        sampleIdentifier.sample = null

        TestCase.assertValidateError(sampleIdentifier, 'sample', 'nullable', null)
    }


    @Test
    void test_validate_fail_nameIsEmpty() {
        sampleIdentifier.name = ''

        TestCase.assertValidateError(sampleIdentifier, 'name', 'blank', '')
    }


    @Test
    void test_validate_fail_nameIsTooShort() {
        final String name = '12'
        sampleIdentifier.name = name

        TestCase.assertValidateError(sampleIdentifier, 'name', 'minSize.notmet', name)
    }


    @Test
    void test_validate_fail_nameIsNotUnique() {
        assert sampleIdentifier.save(flush: true)
        SampleIdentifier sampleIdentifier2 = new SampleIdentifier(
            name: CORRECT_NAME,
            sample: Sample.build())

        TestCase.assertValidateError(sampleIdentifier2, 'name', 'unique', CORRECT_NAME)
    }

    @Test
    void test_validate_fail_nameStartsWithSpace() {
        final String name = ' aname'
        sampleIdentifier.name = name

        TestCase.assertValidateError(sampleIdentifier, 'name', 'validator.invalid', name)
    }

    @Test
    void test_validate_fail_nameEndsWithSpace() {
        final String name = 'aname '
        sampleIdentifier.name = name

        TestCase.assertValidateError(sampleIdentifier, 'name', 'validator.invalid', name)
    }
}
