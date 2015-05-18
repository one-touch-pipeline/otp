package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import org.junit.*
import de.dkfz.tbi.otp.job.processing.ProcessingException


// it looks like domain.findWhere() is not supported by DomainClassUnitTestMixin
// => make the test as integration
class ProcessingOptionServiceTests extends GroovyTestCase {

    final String NAME = "test"
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

    void testFindOptionAssureSuccessed() {
        createProcessingOption()
        String result = processingOptionService.findOptionAssure(NAME, null, null)
        assertEquals(VALUE, result)
    }

    void testFindOptionAssureNullName() {
        createProcessingOption()
        shouldFail(IllegalArgumentException) {
            ProcessingOption testOption = processingOptionService.findOptionAssure(null, null, null)
        }
    }

    void testFindOptionAssureNotFound() {
        createProcessingOption()
        shouldFail(ProcessingException) {
            ProcessingOption testOption = processingOptionService.findOptionAssure("isNotThere", null, null)
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
