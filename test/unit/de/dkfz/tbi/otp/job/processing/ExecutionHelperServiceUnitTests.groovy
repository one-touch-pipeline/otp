package de.dkfz.tbi.otp.job.processing

import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * Unit tests for the {@link ExecutionHelperService}.
 *
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(ExecutionHelperService)
@Mock([Realm])
class ExecutionHelperServiceUnitTests extends GroovyTestCase {

    final static String ARBITRARY_JOB_IDENTFIER = '42'
    final static String FIRST_PBS_ID = '1234'
    final static String SECOND_PBS_ID = '2345'

    def createFakeExecutionService() {
        [
            executeJob: { realm, text, jobIdentifier -> 'some PBS response' },
            extractPbsIds: { pbsResponse -> [FIRST_PBS_ID]},
        ] as ExecutionService
    }

    def createFakeExecutionServiceSeveralPBSIDs() {
        [
            executeJob: { realm, text, jobIdentifier -> 'some PBS response' },
            extractPbsIds: { pbsResponse -> [FIRST_PBS_ID, SECOND_PBS_ID]},
        ] as ExecutionService
    }

    def createFakeExecutionServicePBSisNull() {
        [
            executeJob: { realm, text, jobIdentifier -> 'some PBS response' },
            extractPbsIds: { pbsResponse -> []},
        ] as ExecutionService
    }

    void testSendScriptWithRealmIsNull() {
        shouldFail(NullPointerException) { service.sendScript(null, ARBITRARY_JOB_IDENTFIER) }
    }

    void testSendScriptWithClosureWhenJobIdentifierIsNotGiven() {
        service.executionService = createFakeExecutionService()
        String output = service.sendScript(new Realm()) { 'I am a script given as Closure' }
        String expected = FIRST_PBS_ID
        assertEquals expected, output
    }

    void testSendScriptWithClosureWhenJobIdentifierIsGiven() {
        service.executionService = createFakeExecutionService()
        String output = service.sendScript(new Realm(), ARBITRARY_JOB_IDENTFIER) { 'I am a script given as Closure' }
        String expected = FIRST_PBS_ID
        assertEquals expected, output
    }

    void testSendScriptWithStringWhenJobIdentifierIsNotGiven() {
        service.executionService = createFakeExecutionService()
        String output = service.sendScript(new Realm(), 'I am a script given as String')
        String expected = FIRST_PBS_ID
        assertEquals expected, output
    }

    void testSendScriptWithStringWhenJobIdentifierIsGiven() {
        service.executionService = createFakeExecutionService()
        String output = service.sendScript(new Realm(), 'I am a script given as String', ARBITRARY_JOB_IDENTFIER)
        String expected = FIRST_PBS_ID
        assertEquals expected, output
    }

    @Test(expected = ProcessingException)
    void testSendScriptWithMoreThanOnePBSID() {
        service.executionService = createFakeExecutionServiceSeveralPBSIDs()
        service.sendScript(new Realm(), ARBITRARY_JOB_IDENTFIER) { 'I am a script given as Closure' }
    }

    @Test(expected = ProcessingException)
    void testSendScriptWithPBSIDisEmpty() {
        service.executionService = createFakeExecutionServicePBSisNull()
        service.sendScript(new Realm(), ARBITRARY_JOB_IDENTFIER) { 'I am a script given as Closure' }
    }

}
