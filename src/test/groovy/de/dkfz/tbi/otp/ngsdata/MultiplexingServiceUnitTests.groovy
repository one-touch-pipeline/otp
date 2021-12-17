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
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(MultiplexingService)
class MultiplexingServiceUnitTests {

    @Test
    void testBarcodePatternGATC_ValidLength6() {
        testValidBarcodePatternGATC("ACACAC")
    }

    @Test
    void testBarcodePatternGATC_ValidLength7() {
        testValidBarcodePatternGATC("ACACACA")
    }

    @Test
    void testBarcodePatternGATC_ValidLength8() {
        testValidBarcodePatternGATC("GATCGATC")
    }

    @Test
    void testBarcodePatternGATC_ToLong() {
        testInvalidBarcodePatternGATC("ACACACACA")
    }

    @Test
    void testBarcodePatternGATC_ToShort() {
        testInvalidBarcodePatternGATC("ACACA")
    }

    @Test
    void testBarcodePatternGATC_InvalidUnsupportedChars() {
        testInvalidBarcodePatternGATC("ABCDEF")
    }

    @Test
    void testBarcodePatternGATC_InvalidLowerCaseChars() {
        testInvalidBarcodePatternGATC("acacac")
    }

    @Test
    void testBarcodePatternThreeDigitsFastqValid() {
        testBarcodePatternThreeDigitsFastq("000", "_0_000")
        testBarcodePatternThreeDigitsFastq("023", "_0_023")
        testBarcodePatternThreeDigitsFastq("000", "_2_000")
        testBarcodePatternThreeDigitsFastq("023", "_4_023")
    }

    @Test
    void testBarcodePatternThreeDigitsFastqInvalidToShort() {
        testBarcodePatternThreeDigitsFastq(null, "_4_03")
    }

    @Test
    void testBarcodePatternThreeDigitsFastqInvalidToLong() {
        testBarcodePatternThreeDigitsFastq(null, "_0_0345")
    }

    @Test
    void testBarcodePatternThreeDigitsFastqInvalidNoDigitInBarcodePart() {
        testBarcodePatternThreeDigitsFastq(null, "_0_0aa")
    }

    @Test
    void testBarcodePatternThreeDigitsFastqInvalidNoDigitInMatchPart() {
        testBarcodePatternThreeDigitsFastq(null, "_a_000")
    }

    @Test
    void testBarcodePatternThreeDigitsFastqInvalidMissingUnderscore() {
        testBarcodePatternThreeDigitsFastq(null, "4_030")
    }

    @Test
    void testBarcodePatternThreeDigitsBamValid() {
        testBarcodePatternThreeDigitsBam("000", "_0_000")
        testBarcodePatternThreeDigitsBam("023", "_0_023")
        testBarcodePatternThreeDigitsBam("000", "_2_000")
        testBarcodePatternThreeDigitsBam("023", "_4_023")
    }

    @Test
    void testBarcodePatternThreeDigitsBamInvalidToShort() {
        testBarcodePatternThreeDigitsBam(null, "_4_03")
    }

    @Test
    void testBarcodePatternThreeDigitsBamInvalidToLong() {
        testBarcodePatternThreeDigitsBam(null, "_0_0345")
    }

    @Test
    void testBarcodePatternThreeDigitsBamInvalidNoDigitInBarcodePart() {
        testBarcodePatternThreeDigitsBam(null, "_0_0aa")
    }

    @Test
    void testBarcodePatternThreeDigitsBamInvalidNoDigitInMatchPart() {
        testBarcodePatternThreeDigitsBam(null, "_a_000")
    }

    @Test
    void testBarcodePatternThreeDigitsBamInvalidMissingUnderscore() {
        testBarcodePatternThreeDigitsBam(null, "4_030")
    }

    @Test
    void testBarcodeNoCode() {
        String someFile = "example_something.bam"
        assert null == MultiplexingService.barcode(someFile)
    }

    private void testValidBarcodePatternGATC(String barcode) {
        testBarcodePatternGATC(barcode, barcode)
    }

    private void testInvalidBarcodePatternGATC(String barcode) {
        testBarcodePatternGATC(null, barcode)
    }

    private void testBarcodePatternGATC(String expected, String barcodePattern) {
        final String fileName = "example_${barcodePattern}_fileR1_1.fastq.gz"
        assert expected == MultiplexingService.barcode(fileName)
    }

    private void testBarcodePatternThreeDigitsFastq(String expected, String barcodePattern) {
        final String fileName = "example${barcodePattern}_1_sequence.txt.gz"
        assert expected == MultiplexingService.barcode(fileName)
    }

    private void testBarcodePatternThreeDigitsBam(String expected, String barcodePattern) {
        final String fileName = "example_140226${barcodePattern}.bam"
        assert expected == MultiplexingService.barcode(fileName)
    }
}
