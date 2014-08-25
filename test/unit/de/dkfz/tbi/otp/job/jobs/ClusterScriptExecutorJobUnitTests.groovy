package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.job.processing.AbstractMultiJob
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.ngsdata.Realm
import grails.test.mixin.Mock

import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.REALM
import static de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys.SCRIPT

@Mock([ExecutionHelperService, Realm])
class ClusterScriptExecutorJobUnitTests extends GroovyTestCase {

    final static String ARBITRARY_REALM_ID = '1'
    final static String ARBITRARY_JOB_ID = '1234'
    final static String ARBITRARY_DUMMY_SCRIPT = '#some script'

    void test_maybeSubmit_WhenScriptIsNull_ShouldFail() {
        ClusterScriptExecutorJob clusterScriptExecutorJob = createClusterScriptExecutorJobWithRealmIdAndScript(ARBITRARY_REALM_ID, null)

        shouldFail AssertionError, { clusterScriptExecutorJob.maybeSubmit() }
    }

    void test_maybeSubmit_WhenOnlyScriptIsEmpty_ShouldFail() {
        ClusterScriptExecutorJob clusterScriptExecutorJob = createClusterScriptExecutorJobWithRealmIdAndScript(ARBITRARY_REALM_ID, '')

        shouldFail RuntimeException, { clusterScriptExecutorJob.maybeSubmit() }
    }

    void test_maybeSubmit_WhenScriptAndRealmAreEmpty_ShouldSucceed() {
        ClusterScriptExecutorJob clusterScriptExecutorJob = createClusterScriptExecutorJobWithRealmIdAndScript('', '')

        assert clusterScriptExecutorJob.maybeSubmit() == AbstractMultiJob.NextAction.SUCCEED
    }

    void test_maybeSubmit_WhenOnlyRealmIsEmpty_ShouldFail() {
        ClusterScriptExecutorJob clusterScriptExecutorJob = createClusterScriptExecutorJobWithRealmIdAndScript('', ARBITRARY_DUMMY_SCRIPT)

        shouldFail RuntimeException, { clusterScriptExecutorJob.maybeSubmit() }
    }

    void test_maybeSubmit_WhenRealmIsNull_ShouldFail() {
        ClusterScriptExecutorJob clusterScriptExecutorJob = createClusterScriptExecutorJobWithRealmIdAndScript(null, ARBITRARY_DUMMY_SCRIPT)

        shouldFail AssertionError, { clusterScriptExecutorJob.maybeSubmit() }
    }

    void test_maybeSubmit_WhenAllOK_ShouldReturnWaitForClusterJobs() {
        ClusterScriptExecutorJob clusterScriptExecutorJob = createClusterScriptExecutorJobWithRealmIdAndScript(ARBITRARY_REALM_ID, ARBITRARY_DUMMY_SCRIPT)
        Realm.metaClass.static.findById = { new Realm() }

        assert clusterScriptExecutorJob.maybeSubmit() == AbstractMultiJob.NextAction.WAIT_FOR_CLUSTER_JOBS
    }

    // Helper methods

    private static ClusterScriptExecutorJob createClusterScriptExecutorJobWithRealmIdAndScript(String realmID, String script) {
        ClusterScriptExecutorJob clusterScriptExecutorJob = new ClusterScriptExecutorJob(
                executionHelperService: [
                        sendScript: { Realm r, String s -> ARBITRARY_JOB_ID }
                ] as ExecutionHelperService
        )
        clusterScriptExecutorJob.metaClass.getParameterValueOrClass = { String param ->
            switch (param) {
                case "${REALM}": realmID; break;
                case "${SCRIPT}": script; break;
                default: throw new IllegalArgumentException('This should not happen. Forgot to add a key?')
            }
        }
        clusterScriptExecutorJob
    }
}
