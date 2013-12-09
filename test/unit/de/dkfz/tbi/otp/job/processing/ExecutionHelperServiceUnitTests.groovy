package de.dkfz.tbi.otp.job.processing

import java.io.File

import de.dkfz.tbi.otp.ngsdata.Realm
import grails.test.mixin.*
import grails.test.mixin.support.*
import groovy.mock.interceptor.MockFor

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
    final static String ARBITRARY_PBS_ID = '1234'

    def createFakeExecutionService() {
        [
            executeJob: { realm, text, jobIdentifier -> 'some PBS response' },
            extractPbsIds: { pbsResponse -> [ARBITRARY_PBS_ID]},
        ] as ExecutionService
    }

    void testSendScriptWithRealmIsNull() {
        shouldFail(NullPointerException) { service.sendScript(null, ARBITRARY_JOB_IDENTFIER) }
    }

    void testSendScriptWithClosureWhenJobIdentifierIsNotGiven() {
        service.executionService = createFakeExecutionService()
        String output = service.sendScript(new Realm()) { 'I am a script given as Closure' }
        String expected = ARBITRARY_PBS_ID
        assertEquals expected, output
    }

    void testSendScriptWithClosureWhenJobIdentifierIsGiven() {
        service.executionService = createFakeExecutionService()
        String output = service.sendScript(new Realm(), ARBITRARY_JOB_IDENTFIER) { 'I am a script given as Closure' }
        String expected = ARBITRARY_PBS_ID
        assertEquals expected, output
    }

    void testSendScriptWithStringWhenJobIdentifierIsNotGiven() {
        service.executionService = createFakeExecutionService()
        String output = service.sendScript(new Realm(), 'I am a script given as String')
        String expected = ARBITRARY_PBS_ID
        assertEquals expected, output
    }

    void testSendScriptWithStringWhenJobIdentifierIsGiven() {
        service.executionService = createFakeExecutionService()
        String output = service.sendScript(new Realm(), 'I am a script given as String', ARBITRARY_JOB_IDENTFIER)
        String expected = ARBITRARY_PBS_ID
        assertEquals expected, output
    }
}
