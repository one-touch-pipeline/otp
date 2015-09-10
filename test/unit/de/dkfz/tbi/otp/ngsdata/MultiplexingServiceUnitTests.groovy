package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.*

import org.junit.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(MultiplexingService)
class MultiplexingServiceUnitTests {

    void testBarcodePatternGATC_ValidLength6() {
        testValidBarcodePatternGATC("ACACAC")
    }

    void testBarcodePatternGATC_ValidLength7() {
        testValidBarcodePatternGATC("ACACACA")
    }

    void testBarcodePatternGATC_ValidLength8() {
        testValidBarcodePatternGATC("GATCGATC")
    }

    void testBarcodePatternGATC_ToLong() {
        testInvalidBarcodePatternGATC("ACACACACA")
    }

    void testBarcodePatternGATC_ToShort() {
        testInvalidBarcodePatternGATC("ACACA")
    }

    void testBarcodePatternGATC_InvalidUnsupportedChars() {
        testInvalidBarcodePatternGATC("ABCDEF")
    }

    void testBarcodePatternGATC_InvalidLowerCaseChars() {
        testInvalidBarcodePatternGATC("acacac")
    }



    void testBarcodePatternThreeDigitsFastqValid() {
        testBarcodePatternThreeDigitsFastq("000", "_0_000")
        testBarcodePatternThreeDigitsFastq("023", "_0_023")
        testBarcodePatternThreeDigitsFastq("000", "_2_000")
        testBarcodePatternThreeDigitsFastq("023", "_4_023")
    }

    void testBarcodePatternThreeDigitsFastqInvalidToShort() {
        testBarcodePatternThreeDigitsFastq(null, "_4_03")
    }

    void testBarcodePatternThreeDigitsFastqInvalidToLong() {
        testBarcodePatternThreeDigitsFastq(null, "_0_0345")
    }

    void testBarcodePatternThreeDigitsFastqInvalidNoDigitInBarcodePart() {
        testBarcodePatternThreeDigitsFastq(null, "_0_0aa")
    }

    void testBarcodePatternThreeDigitsFastqInvalidNoDigitInMatchPart() {
        testBarcodePatternThreeDigitsFastq(null, "_a_000")
    }

    void testBarcodePatternThreeDigitsFastqInvalidMissingUnderscore() {
        testBarcodePatternThreeDigitsFastq(null, "4_030")
    }



    void testBarcodePatternThreeDigitsBamValid() {
        testBarcodePatternThreeDigitsBam("000", "_0_000")
        testBarcodePatternThreeDigitsBam("023", "_0_023")
        testBarcodePatternThreeDigitsBam("000", "_2_000")
        testBarcodePatternThreeDigitsBam("023", "_4_023")
    }

    void testBarcodePatternThreeDigitsBamInvalidToShort() {
        testBarcodePatternThreeDigitsBam(null, "_4_03")
    }

    void testBarcodePatternThreeDigitsBamInvalidToLong() {
        testBarcodePatternThreeDigitsBam(null, "_0_0345")
    }

    void testBarcodePatternThreeDigitsBamInvalidNoDigitInBarcodePart() {
        testBarcodePatternThreeDigitsBam(null, "_0_0aa")
    }

    void testBarcodePatternThreeDigitsBamInvalidNoDigitInMatchPart() {
        testBarcodePatternThreeDigitsBam(null, "_a_000")
    }

    void testBarcodePatternThreeDigitsBamInvalidMissingUnderscore() {
        testBarcodePatternThreeDigitsBam(null, "4_030")
    }



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
