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

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class MultiplexingServiceSpec extends Specification implements ServiceUnitTest<MultiplexingService> {

    void testBarcodePatternGATC_ValidLength6() {
        expect:
        testValidBarcodePatternGATC("ACACAC")
    }

    void testBarcodePatternGATC_ValidLength7() {
        expect:
        testValidBarcodePatternGATC("ACACACA")
    }

    void testBarcodePatternGATC_ValidLength8() {
        expect:
        testValidBarcodePatternGATC("GATCGATC")
    }

    void testBarcodePatternGATC_ToLong() {
        expect:
        testInvalidBarcodePatternGATC("ACACACACA")
    }

    void testBarcodePatternGATC_ToShort() {
        expect:
        testInvalidBarcodePatternGATC("ACACA")
    }

    void testBarcodePatternGATC_InvalidUnsupportedChars() {
        expect:
        testInvalidBarcodePatternGATC("ABCDEF")
    }

    void testBarcodePatternGATC_InvalidLowerCaseChars() {
        expect:
        testInvalidBarcodePatternGATC("acacac")
    }

    void testBarcodePatternThreeDigitsFastqValid() {
        expect:
        testBarcodePatternThreeDigitsFastq("000", "_0_000")
        testBarcodePatternThreeDigitsFastq("023", "_0_023")
        testBarcodePatternThreeDigitsFastq("000", "_2_000")
        testBarcodePatternThreeDigitsFastq("023", "_4_023")
    }

    void testBarcodePatternThreeDigitsFastqInvalidToShort() {
        expect:
        testBarcodePatternThreeDigitsFastq(null, "_4_03")
    }

    void testBarcodePatternThreeDigitsFastqInvalidToLong() {
        expect:
        testBarcodePatternThreeDigitsFastq(null, "_0_0345")
    }

    void testBarcodePatternThreeDigitsFastqInvalidNoDigitInBarcodePart() {
        expect:
        testBarcodePatternThreeDigitsFastq(null, "_0_0aa")
    }

    void testBarcodePatternThreeDigitsFastqInvalidNoDigitInMatchPart() {
        expect:
        testBarcodePatternThreeDigitsFastq(null, "_a_000")
    }

    void testBarcodePatternThreeDigitsFastqInvalidMissingUnderscore() {
        expect:
        testBarcodePatternThreeDigitsFastq(null, "4_030")
    }

    void testBarcodePatternThreeDigitsBamValid() {
        expect:
        testBarcodePatternThreeDigitsBam("000", "_0_000")
        testBarcodePatternThreeDigitsBam("023", "_0_023")
        testBarcodePatternThreeDigitsBam("000", "_2_000")
        testBarcodePatternThreeDigitsBam("023", "_4_023")
    }

    void testBarcodePatternThreeDigitsBamInvalidToShort() {
        expect:
        testBarcodePatternThreeDigitsBam(null, "_4_03")
    }

    void testBarcodePatternThreeDigitsBamInvalidToLong() {
        expect:
        testBarcodePatternThreeDigitsBam(null, "_0_0345")
    }

    void testBarcodePatternThreeDigitsBamInvalidNoDigitInBarcodePart() {
        expect:
        testBarcodePatternThreeDigitsBam(null, "_0_0aa")
    }

    void testBarcodePatternThreeDigitsBamInvalidNoDigitInMatchPart() {
        expect:
        testBarcodePatternThreeDigitsBam(null, "_a_000")
    }

    void testBarcodePatternThreeDigitsBamInvalidMissingUnderscore() {
        expect:
        testBarcodePatternThreeDigitsBam(null, "4_030")
    }

    void testBarcodeNoCode() {
        given:
        String someFile = "example_something.bam"

        expect:
        null == MultiplexingService.barcode(someFile)
    }

    private boolean testValidBarcodePatternGATC(String barcode) {
        return testBarcodePatternGATC(barcode, barcode)
    }

    private boolean testInvalidBarcodePatternGATC(String barcode) {
        return testBarcodePatternGATC(null, barcode)
    }

    private boolean testBarcodePatternGATC(String expected, String barcodePattern) {
        final String fileName = "example_${barcodePattern}_fileR1_1.fastq.gz"
        return expected == MultiplexingService.barcode(fileName)
    }

    private boolean testBarcodePatternThreeDigitsFastq(String expected, String barcodePattern) {
        final String fileName = "example${barcodePattern}_1_sequence.txt.gz"
        return expected == MultiplexingService.barcode(fileName)
    }

    private boolean testBarcodePatternThreeDigitsBam(String expected, String barcodePattern) {
        final String fileName = "example_140226${barcodePattern}.bam"
        return expected == MultiplexingService.barcode(fileName)
    }
}
