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

    MultiplexingService multiplexingService

    @Before
    void setUp() {
        multiplexingService = new MultiplexingService()
    }

    @After
    void tearDown() {
        multiplexingService = null
    }

    void testBarcodeValidLength6() {
        testValidBarcode("ACACAC")
    }

    void testBarcodeValidLength7() {
        testValidBarcode("ACACACA")
    }

    void testBarcodeValidLength8() {
        testValidBarcode("GATCGATC")
    }

    void testBarcodeToLong() {
        testInvalidBarcode("ACACACACA")
    }

    void testBarcodeToShort() {
        testInvalidBarcode("ACACA")
    }

    void testBarcodeInvalidUnsupportedChars() {
        testInvalidBarcode("ABCDEF")
    }

    void testBarcodeInvalidLowerCaseChars() {
        testInvalidBarcode("acacac")
    }

    private void testValidBarcode(String barcode) {
        final String file = "example_${barcode}_fileR1_1.fastq.gz"
        assert barcode == multiplexingService.barcode(file)
    }

    private void testInvalidBarcode(String barcode) {
        final String file = "example_${barcode}_fileR1_1.fastq.gz"
        assert null == multiplexingService.barcode(file)
    }

}
