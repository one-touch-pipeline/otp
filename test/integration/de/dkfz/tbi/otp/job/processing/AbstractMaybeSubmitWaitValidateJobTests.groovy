package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifierImpl
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
        abstractMaybeSubmitWaitValidateJob = [
                getLogFilePaths: { clusterJob -> AbstractOtpJob.getLogFileNames(clusterJob) }
        ] as AbstractMaybeSubmitWaitValidateJob
    }

    @Test
    void testCreateExceptionString_AllFine() {
        Realm realm = DomainFactory.createRealmDataProcessingDKFZ()
        assert realm.save([flush: true, failOnError: true])

        ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep()
        assert processingStep

        ClusterJob clusterJob = clusterJobService.createClusterJob(realm, "0000", processingStep)
        assert clusterJob

        ClusterJobIdentifier identifier = new ClusterJobIdentifierImpl(clusterJob)

        Map failedClusterJobs = [(identifier): "Failed."]
        List finishedClusterJobs = [identifier]

        assert """
1 of 1 cluster jobs failed:
${identifier}: Failed.
${AbstractOtpJob.getLogFileNames(clusterJob)}

""" == abstractMaybeSubmitWaitValidateJob.createExceptionString(failedClusterJobs, finishedClusterJobs)
    }
}
