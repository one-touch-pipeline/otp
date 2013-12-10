package de.dkfz.tbi.otp.job.processing

import grails.test.mixin.*
import grails.test.mixin.support.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * Unit tests for the {@link ExecutionHelperService}.
 *
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(ExecutionHelperService)
@Mock([ProcessingStep, Realm])
class ExecutionHelperServiceUnitTests extends GroovyTestCase {

    final static String ARBITRARY_JOB_IDENTFIER = '42'
    final static String FIRST_PBS_ID = '1234'
    final static String SECOND_PBS_ID = '2345'
    final static String SCRIPT_CONTENT = '# I am a script'

    private createFakeExecutionService() {
        [
            executeJob: { realm, text, jobIdentifier, processingStep -> 'some PBS response' },
            extractPbsIds: { pbsResponse -> [FIRST_PBS_ID]},
        ] as ExecutionService
    }

    private createFakeExecutionServiceSeveralPBSIDs() {
        [
            executeJob: { realm, text, jobIdentifier, processingStep -> 'some PBS response' },
            extractPbsIds: { pbsResponse -> [FIRST_PBS_ID, SECOND_PBS_ID]},
        ] as ExecutionService
    }

    private createFakeExecutionServicePBSisNull() {
        [
            executeJob: { realm, text, jobIdentifier, processingStep -> 'some PBS response' },
            extractPbsIds: { pbsResponse -> []},
        ] as ExecutionService
    }

    void testSendScriptWithRealmIsNull() {
        shouldFail(NullPointerException) { service.sendScript(null, ARBITRARY_JOB_IDENTFIER) }
    }

    // Script via closure

    void testSendScriptWithClosureWhenJobIdentifierIsNotGiven() {
        service.executionService = createFakeExecutionService()
        Realm fakeRealm = new Realm()
        String output = service.sendScript(fakeRealm) { SCRIPT_CONTENT }
        assert FIRST_PBS_ID == output
    }

    void testSendScriptWithClosureWhenJobIdentifierIsGiven() {
        service.executionService = createFakeExecutionService()
        Realm fakeRealm = new Realm()
        String output = service.sendScript(fakeRealm, ARBITRARY_JOB_IDENTFIER) { SCRIPT_CONTENT }
        assert FIRST_PBS_ID == output
    }

    void testSendScriptWithClosureWhenMoreThanOnePBSIDIsReturned() {
        service.executionService = createFakeExecutionServiceSeveralPBSIDs()
        shouldFail ProcessingException, { service.sendScript(new Realm(), ARBITRARY_JOB_IDENTFIER) { SCRIPT_CONTENT } }
    }

    void testSendScriptWithClosureWhenPBSIDisEmpty() {
        service.executionService = createFakeExecutionServicePBSisNull()
        shouldFail ProcessingException, { service.sendScript(new Realm(), ARBITRARY_JOB_IDENTFIER) { SCRIPT_CONTENT } }
    }

    // Script as String

    void testSendScriptWithStringWhenJobIdentifierIsNotGiven() {
        service.executionService = createFakeExecutionService()
        Realm fakeRealm = new Realm()
        String output = service.sendScript(fakeRealm, SCRIPT_CONTENT)
        assert FIRST_PBS_ID == output
    }

    void testSendScriptWithStringWhenJobIdentifierIsGiven() {
        service.executionService = createFakeExecutionService()
        Realm fakeRealm = new Realm()
        String output = service.sendScript(fakeRealm, SCRIPT_CONTENT, ARBITRARY_JOB_IDENTFIER)
        assert FIRST_PBS_ID == output
    }

    void testSendScriptWithStringWhenJobIdentifierAndProcessingStepAreGiven() {
        service.executionService = createFakeExecutionService()
        Realm fakeRealm = new Realm()
        ProcessingStep fakeProcessingStep = new ProcessingStep()
        String output = service.sendScript(fakeRealm, SCRIPT_CONTENT, ARBITRARY_JOB_IDENTFIER, fakeProcessingStep)
        assert FIRST_PBS_ID == output
    }

    void testSendScriptWithStringWhenMoreThanOnePBSIDIsReturned() {
        service.executionService = createFakeExecutionServiceSeveralPBSIDs()
        shouldFail ProcessingException, { service.sendScript(new Realm(), ARBITRARY_JOB_IDENTFIER) { SCRIPT_CONTENT } }
    }

    void testSendScriptWithStringWhenPBSIDisEmpty() {
        service.executionService = createFakeExecutionServicePBSisNull()
        shouldFail ProcessingException, { service.sendScript(new Realm(), ARBITRARY_JOB_IDENTFIER) { SCRIPT_CONTENT } }
    }
}
