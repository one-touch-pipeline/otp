package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import org.junit.Before
import org.junit.Test

class AbstractMaybeSubmitWaitValidateJobTests extends TestCase {

    ClusterJobService clusterJobService
    AbstractMaybeSubmitWaitValidateJob abstractMaybeSubmitWaitValidateJob

    @Before
    void setUp() {
        abstractMaybeSubmitWaitValidateJob = [:] as AbstractMaybeSubmitWaitValidateJob
    }

    @Test
    void testCreateExceptionString() {
        Realm realm = DomainFactory.createRealmDataProcessingDKFZ()
        assert realm.save([flush: true, failOnError: true])

        ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep()
        assert processingStep

        ClusterJob clusterJob1 = clusterJobService.createClusterJob(realm, "1111", realm.unixUser, processingStep)
        clusterJob1.jobLog = "/test-job1.log"
        clusterJob1.save(flush: true, failOnError: true)
        ClusterJobIdentifier identifier1 = new ClusterJobIdentifier(clusterJob1)

        ClusterJob clusterJob2 = clusterJobService.createClusterJob(realm, "2222", realm.unixUser, processingStep)
        clusterJob1.jobLog = "/test-job1.log"
        clusterJob1.save(flush: true, failOnError: true)
        ClusterJobIdentifier identifier2 = new ClusterJobIdentifier(clusterJob2)

        Map failedClusterJobs = [(identifier2): "Failed2.", (identifier1): "Failed1."]
        List finishedClusterJobs = [identifier2, identifier1]

        String expected = """\

            2 of 2 cluster jobs failed:

            ${identifier1}: Failed1.
            Log file: ${clusterJob1.jobLog}

            ${identifier2}: Failed2.
            Log file: ${clusterJob2.jobLog}
            """.stripIndent()

        String actual = abstractMaybeSubmitWaitValidateJob.createExceptionString(failedClusterJobs, finishedClusterJobs)

        assert expected == actual
    }
}
