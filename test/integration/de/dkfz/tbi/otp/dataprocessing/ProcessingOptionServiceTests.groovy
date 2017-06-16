package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import org.junit.*

import static org.junit.Assert.*

// it looks like domain.findWhere() is not supported by DomainClassUnitTestMixin
// => make the test as integration
class ProcessingOptionServiceTests {

    final OptionName NAME = OptionName.PIPELINE_RODDY_SNV_PLUGIN_NAME
    final String VALUE = "testValue"

    ProcessingOptionService processingOptionService

    private void createProcessingOption() {
        ProcessingOption option = new ProcessingOption(
            name: NAME,
            value: VALUE,
            comment: "test"
        )
        assertNotNull(option.save(flush: true))
    }

    @Test
    void testFindOptionAssureSuccessed() {
        createProcessingOption()
        String result = ProcessingOptionService.findOptionAssure(NAME, null, null)
        assertEquals(VALUE, result)
    }

    @Test
    void testFindOptionAssureNullName() {
        createProcessingOption()
        shouldFail(IllegalArgumentException) {
            ProcessingOption testOption = ProcessingOptionService.findOptionAssure(null, null, null)
        }
    }

    @Test
    void testGetValueOfProcessingOption_NoProcessingOptionWithThisNameExists_ShouldFail() {
        shouldFail(AssertionError) {
            processingOptionService.getValueOfProcessingOption(NAME)
        }
    }

    @Test
    void testGetValueOfProcessingOption_MoreThanOneProcessingOptionWithThisNameExists_ShouldFail() {
        createProcessingOption()
        createProcessingOption()
        shouldFail(AssertionError) {
            processingOptionService.getValueOfProcessingOption(NAME)
        }
    }

    @Test
    void testGetValueOfProcessingOption_AllFine() {
        createProcessingOption()
        VALUE == processingOptionService.getValueOfProcessingOption(NAME)
    }
}
