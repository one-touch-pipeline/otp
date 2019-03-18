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
import grails.test.mixin.TestFor
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector

import de.dkfz.tbi.TestCase

@TestFor(ReferenceGenomeProjectSeqType)
@Build([ReferenceGenomeProjectSeqType])
class ReferenceGenomeProjectSeqTypeUnitTests {

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    void test_constraint_onStatSizeFileName_withCorrectName_ShouldBeValid() {
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.buildWithoutSave([statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME])
        assert referenceGenomeProjectSeqType.validate()
    }

    @Test
    void test_constraint_onStatSizeFileName_withNull_ShouldBeValid() {
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.buildWithoutSave([statSizeFileName: null])
        assert referenceGenomeProjectSeqType.validate()
    }

    @Test
    void test_constraint_onStatSizeFileName_WhenBlank_ShouldBeInvalid() {
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.buildWithoutSave([:])
        referenceGenomeProjectSeqType.statSizeFileName = '' //setting empty string does not work via map

        TestCase.assertValidateError(referenceGenomeProjectSeqType, 'statSizeFileName', 'blank', '')
    }

    @Test
    void test_constraint_onStatSizeFileName_WhenValidSpecialChar_ShouldBeValid() {
        "-_.".each {
            try {
                String name = "File${it}.tab"
                ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.buildWithoutSave([statSizeFileName: name])
                referenceGenomeProjectSeqType.validate()
                assert 0 == referenceGenomeProjectSeqType.errors.errorCount
            } catch (Throwable e) {
                collector.addError(e)
            }
        }
    }

    @Test
    void test_constraint_onStatSizeFileName_WhenInvalidSpecialChar_ShouldBeInvalid() {
        "\"',:;%\$§&<>|^§!?=äöüÄÖÜß´`".each {
            try {
                String name = "File${it}.tab"
                ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.buildWithoutSave([statSizeFileName: name])
                TestCase.assertAtLeastExpectedValidateError(referenceGenomeProjectSeqType, 'statSizeFileName', 'matches.invalid', name)
            } catch (Throwable e) {
                collector.addError(e)
            }
        }
    }
}
