package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType
import org.joda.time.DateTime
import org.joda.time.Duration

import de.dkfz.tbi.flowcontrol.cluster.api.JobState;
import de.dkfz.tbi.flowcontrol.ws.api.pbs.JobInfo;
import de.dkfz.tbi.flowcontrol.ws.client.FlowControlClient

import static org.junit.Assert.*
import org.junit.*

class ClusterJobServiceTests {

    public static final String TEST_KEY_1 = "testKey_1"
    public static final String TEST_HOST_1 = "testHost_1"
    public static final int TEST_PORT_1 = 1
    public static final String TEST_KEY_2 = "testKey_2"
    public static final String TEST_HOST_2 = "testHost_2"
    public static final int TEST_PORT_2 = 2

    ClusterJobService clusterJobService

    @After
    void tearDown() {
        TestCase.removeMetaClass(ClusterJobService, clusterJobService)
    }

    @Test
    void testCreateClusterJobAndCompleteClusterJob() {
        Realm realm = DomainFactory.createRealmDataProcessingDKFZ()

        assertNotNull(realm.save([flush: true, failOnError: true]))

        String clusterJobId = "testId"
        ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep("testClass")
        SeqType seqType = new SeqType(
                name: "seqTypeName",
                libraryLayout: "library",
                dirName: "dirName"
        )

        assertNotNull(seqType.save([flush: true, failOnError: true]))

        ClusterJob job = clusterJobService.createClusterJob(realm, clusterJobId, processingStep, seqType)

        assertNotNull(job.save([flush: true, failOnError: true]))

        ClusterJobIdentifier clusterJobIdentifier = new ClusterJobIdentifierImpl(job.realm, job.clusterJobId)
        JobInfo jobInfo = new JobInfo()
        jobInfo.setState(JobState.FAILED)
        jobInfo.setExitcode(5)
        jobInfo.setStarted(new Date(2000, 1, 1, 0, 0, 0))
        jobInfo.setEnded(new Date(2000, 1, 1, 1, 0, 0))
        jobInfo.setCputimeMS(4000000L)
        jobInfo.setMemoryUsedKB(2048L)
        jobInfo.setCores(8)
        jobInfo.setMemoryRequestedKB(2048L)
        jobInfo.setWalltimeRequestedMS(4000000L)

        TestCase.removeMetaClass(ClusterJobService, clusterJobService)
        ClusterJobService.metaClass.getClusterJobInformation = { ClusterJob j ->
            assert j == job
            return jobInfo
        }

        clusterJobService.completeClusterJob(clusterJobIdentifier)

        assertEquals(job.exitStatus, ClusterJob.Status.FAILED)
        assertEquals(job.exitCode, 5)
        assertEquals(job.started, new DateTime(new Date(2000, 1, 1, 0, 0, 0)))
        assertEquals(job.ended, new DateTime(new Date(2000, 1, 1, 1, 0, 0)))
        assertEquals(job.usedCores, null)
        assertEquals(job.cpuTime, new Duration(4000000L))
        assertEquals(job.usedMemory, 2048L)
        assertEquals(job.requestedCores, 8)
        assertEquals(job.requestedMemory, 2048L)
        assertEquals(job.requestedWalltime, new Duration(4000000L))
    }

    @Test
    void testGetFlowControlClient() {
        Realm realm1 = DomainFactory.createRealmDataProcessingDKFZ()

        assertNotNull(realm1.save([flush: true, failOnError: true]))
        assertNull(clusterJobService.getFlowControlClient(realm1))

        realm1.flowControlKey = TEST_KEY_1
        realm1.flowControlHost = TEST_HOST_1
        realm1.flowControlPort = TEST_PORT_1

        assertNotNull(realm1.save([flush: true, failOnError: true]))

        clusterJobService.metaClass.createFlowControlClient = { String k, String h, int p ->
            assert k == TEST_KEY_1
            assert h == TEST_HOST_1
            assert p == TEST_PORT_1
            return new FlowControlClient.Builder().build()
        }
        FlowControlClient fcc1 = clusterJobService.getFlowControlClient(realm1)

        assertNotNull(fcc1)

        Realm realm2 = DomainFactory.createRealmDataProcessingDKFZ()
        realm2.flowControlKey = TEST_KEY_2
        realm2.flowControlHost = TEST_HOST_2
        realm2.flowControlPort = TEST_PORT_2

        assertNotNull(realm2.save([flush: true, failOnError: true]))

        TestCase.removeMetaClass(ClusterJobService, clusterJobService)
        clusterJobService.metaClass.createFlowControlClient = { String k, String h, int p ->
            assert k == TEST_KEY_2
            assert h == TEST_HOST_2
            assert p == TEST_PORT_2
            return new FlowControlClient.Builder().build()
        }
        FlowControlClient fcc2 = clusterJobService.getFlowControlClient(realm2)

        assertNotNull(fcc2)

        assert fcc1 != fcc2

        TestCase.removeMetaClass(ClusterJobService, clusterJobService)
        clusterJobService.metaClass.createFlowcontrolClient = { String k, String h, int p ->
            // method should not be called because client should be cached for this realm
            assert false
        }
        FlowControlClient fcc3 = clusterJobService.getFlowControlClient(realm1)

        assert fcc1 == fcc3
    }
}