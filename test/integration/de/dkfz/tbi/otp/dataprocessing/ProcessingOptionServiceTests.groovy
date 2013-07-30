package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.job.processing.ProcessingException


// it looks like domain.findWhere() is not supported by DomainClassUnitTestMixin
// => make the test as integration
class ProcessingOptionServiceTests extends GroovyTestCase {

    ProcessingOptionService processingOptionService

    void createProcessingOption() {
        ProcessingOption option = new ProcessingOption(
            name: "test",
            value: "testValue",
            comment: "test"
        )
        assertNotNull(option.save(flush: true))
    }

    void testFindOptionAssureSuccessed() {
        createProcessingOption()
        String result = processingOptionService.findOptionAssure("test", null, null)
        assertEquals("testValue", result)
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
}
