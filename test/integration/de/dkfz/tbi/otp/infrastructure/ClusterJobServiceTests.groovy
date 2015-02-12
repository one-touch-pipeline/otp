package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.flowcontrol.cluster.api.JobState
import de.dkfz.tbi.flowcontrol.ws.api.pbs.JobInfo
import de.dkfz.tbi.flowcontrol.ws.api.response.JobInfos
import de.dkfz.tbi.flowcontrol.ws.client.FlowControlClient
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test

import javax.xml.namespace.QName
import javax.xml.soap.SOAPConstants
import javax.xml.soap.SOAPFactory
import javax.xml.soap.SOAPFault
import javax.xml.ws.soap.SOAPFaultException

import static java.util.concurrent.TimeUnit.HOURS
import static java.util.concurrent.TimeUnit.MINUTES
import static org.junit.Assert.*

class ClusterJobServiceTests extends AbstractIntegrationTest {

    public static final String TEST_KEY_1 = "testKey_1"
    public static final String TEST_HOST_1 = "testHost_1"
    public static final int TEST_PORT_1 = 1
    public static final String TEST_KEY_2 = "testKey_2"
    public static final String TEST_HOST_2 = "testHost_2"
    public static final int TEST_PORT_2 = 2

    public static final LocalDate SDATE_LOCALDATE = new LocalDate()
    public static final LocalDate EDATE_LOCALDATE = SDATE_LOCALDATE.plusDays(1)
    public static final DateTime SDATE_DATETIME = SDATE_LOCALDATE.toDateTimeAtStartOfDay()
    public static final DateTime EDATE_DATETIME = EDATE_LOCALDATE.toDateTimeAtStartOfDay()

    public static final Long MINUTES_TO_MILLIS = MINUTES.toMillis(1)
    public static final Long HOURS_TO_MILLIS = HOURS.toMillis(1)

    public static final Long GiB_TO_KiB = 1024 * 1024

    int uniqueIdCounter = 0

    ClusterJobService clusterJobService

    SeqType seqType

    TestData testData = new TestData()

    @Before
    void setUp() {
        seqType = TestData.createSeqType()
        assert seqType.save([flush: true])
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(ClusterJobService, clusterJobService)
        clusterJobService.clientCache.clear()
    }

    @Test
    void testCreateClusterJobAndCompleteClusterJob() {
        ClusterJob job = createClusterJob()

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

    @Test
    void testClientSessionExpiredException() {
        ClusterJob job = createClusterJob()

        clusterJobService.metaClass.getFlowControlClient = { Realm r ->
            FlowControlClient client =  new FlowControlClient.Builder().build()
            client.metaClass.requestJobInfos = { String i ->
                SOAPFault soapFault = createSoapFault()
                throw new SOAPFaultException(soapFault)
            }
            return client
        }

        shouldFail (SOAPFaultException) {
            clusterJobService.getClusterJobInformation(job)
        }
    }

    @Test
    void testClientSessionExpiredReconstruct() {
        ClusterJob job = createClusterJob()

        int callCount = 0
        clusterJobService.metaClass.getFlowControlClient = { Realm r ->
            FlowControlClient client =  new
                    FlowControlClient.Builder().build()
            if (callCount == 0) {
                client.metaClass.requestJobInfos = { String i ->
                    SOAPFault soapFault = createSoapFault()
                    throw new SOAPFaultException(soapFault)
                }
            } else if (callCount == 1) {
                client.metaClass.requestJobInfos = { String i ->
                    return new JobInfos([(job.clusterJobId): new JobInfo()])
                }
            } else {
                assert false
            }
            callCount++
            return client
        }

        assertNotNull(clusterJobService.getClusterJobInformation(job))
        assert callCount == 2
    }

    @Test
    void testFindIndividualByClusterJob() {
          ClusterJob job = createClusterJob()

          SeqCenter seqCenter = new SeqCenter()
          seqCenter.name = "DKFZ"
          seqCenter.dirName = "core"
          seqCenter.save([flush: true, failOnError: true])

          SeqPlatform seqPlatform = new SeqPlatform()
          seqPlatform.name = "solid"
          seqPlatform.model = "4"
          seqPlatform.save([flush: true, failOnError: true])

          Run run = testData.createRun([seqCenter: seqCenter, seqPlatform: seqPlatform])
          run.save([flush: true, failOnError: true])

          DomainFactory.createProcessParameter(job.processingStep.process, 'de.dkfz.tbi.otp.ngsdata.Run', run.id.toString())

          assert run.getIndividual() == clusterJobService.findIndividualByClusterJob(job)
    }

    @Test
    void test_findAllClusterJobsByDateBetween_WhenNoJobsFound_ShouldReturnEmptyList() {
        assert [] == clusterJobService.findAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, "", 0, 10, 'clusterJobId', 'asc')
    }

    @Test
    void test_findAllClusterJobsByDateBetween_WhenJobIsOutOfTimeSpanToEarly_ShouldReturnEmptyList() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1)])

        assert [] == clusterJobService.findAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, "", 0, 10, 'clusterJobId', 'asc')
    }

    @Test
    void test_findAllClusterJobsByDateBetween_WhenJobIsOutOfTimeSpanToLate_ShouldReturnEmptyList() {
        createClusterJob([queued: SDATE_DATETIME.plusDays(2), ended: SDATE_DATETIME.plusDays(2)])

        assert [] == clusterJobService.findAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, "", 0, 10, 'clusterJobId', 'asc')
    }

    @Test
    void test_findAllClusterJobsByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithJobs() {
        ClusterJob job1 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME])
        ClusterJob job2 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME])

        assert [job1, job2] == clusterJobService.findAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, "", 0, 10, 'clusterJobId', 'asc')
    }

    @Test
    void test_findAllClusterJobsByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithJobs_PassingFilter() {
        String filter = 'filter'
        ClusterJob job1 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, clusterJobName: "Value with ${filter} something _testClass"])
        ClusterJob job2 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, clusterJobName: "some other value _testClass"])
        ClusterJob job3 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, clusterJobName: "Value with ${filter.toUpperCase()} something _testClass"])

        assert [job1, job3] == clusterJobService.findAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, filter, 0, 10, 'clusterJobId', 'asc')
    }

    @Test
    void test_findAllClusterJobsByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithJobs_UsingPage() {
        List<ClusterJob> jobs = (1..10).collect {
            createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME])
        }

        assert jobs.subList(3,7) == clusterJobService.findAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, '', 3, 4, 'clusterJobId', 'asc')
    }

    @Test
    void test_findAllClusterJobsByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithJobs_SortedDesc() {
        ClusterJob job1 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME])
        ClusterJob job2 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME])

        assert [job2, job1] == clusterJobService.findAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, "", 0, 10, 'clusterJobId', 'desc')
    }

    @Test
    void test_findAllClusterJobsByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithJobs_SortedByName() {
        ClusterJob job1 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, clusterJobName: "name3 _testClass"])
        ClusterJob job2 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, clusterJobName: "name1 _testClass"])
        ClusterJob job3 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, clusterJobName: "name2 _testClass"])

        assert [job2, job3, job1] == clusterJobService.findAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, '', 0, 10, 'clusterJobName', 'asc')
    }



    @Test
    void test_countAllClusterJobsByDateBetween_WhenNoJobsFound_ShouldReturnZero() {
        assert 0 == clusterJobService.countAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, "")
    }

    @Test
    void test_countAllClusterJobsByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnZero() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1)])

        assert 0 == clusterJobService.countAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, "")
    }

    @Test
    void test_countAllClusterJobsByDateBetween_WhenSeveralJobsAreFound_ShouldReturnTwo() {
        ClusterJob job1 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME])
        ClusterJob job2 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME])

        assert 2 == clusterJobService.countAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, "")
    }

    @Test
    void test_countAllClusterJobsByDateBetween_WhenSeveralJobsPassFilter_ShouldReturnTwo() {
        String filter = 'filter'
        ClusterJob job1 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, clusterJobName: "Value with ${filter} something _testClass"])
        ClusterJob job2 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, clusterJobName: "some other value _testClass"])
        ClusterJob job3 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, clusterJobName: "Value with ${filter.toUpperCase()} something _testClass"])

        assert 2 == clusterJobService.countAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, filter)
    }



    @Test
    void test_findAllJobClassesByDateBetween_WhenNoJobsFound_ShouldReturnEmptyList() {
        assert [] == clusterJobService.findAllJobClassesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findAllJobClassesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnEmptyList() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1), jobClass: 'jobClass1'])

        assert [] == clusterJobService.findAllJobClassesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findAllJobClassesByDateBetween_WhenSeveralJobClassesAreFound_ShouldReturnUniqueListWithJobClasses() {
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, jobClass: 'jobClass1'])
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, jobClass: 'jobClass1'])
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, jobClass: 'jobClass2'])

        assert ['jobClass1', 'jobClass2'] == clusterJobService.findAllJobClassesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findAllExitCodesByDateBetween_WhenNoJobsFound_ShouldReturnEmptyList() {
        assert [] == clusterJobService.findAllExitCodesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findAllExitCodesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnEmptyList() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1), exitCode: 0])

        assert [] == clusterJobService.findAllExitCodesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findAllExitCodesByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithOccurancesPerExitCodes() {
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, exitCode: 0])
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, exitCode: 0])
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, exitCode: 1])

        assert [[0, 2], [1, 1]] == clusterJobService.findAllExitCodesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findAllExitStatusesByDateBetween_WhenNoJobsFound_ShouldReturnEmptyList() {
        assert [] == clusterJobService.findAllExitStatusesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findAllExitStatusesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnEmptyList() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), ended: EDATE_DATETIME.minusDays(1), exitStatus: ClusterJob.Status.COMPLETED])

        assert [] == clusterJobService.findAllExitStatusesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findAllExitStatusesByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithOccurancesPerExitStatuses() {
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, exitStatus: ClusterJob.Status.COMPLETED])
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, exitStatus: ClusterJob.Status.COMPLETED])
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, exitStatus: ClusterJob.Status.FAILED])

        assert [[ClusterJob.Status.COMPLETED, 2], [ClusterJob.Status.FAILED, 1]] == clusterJobService.findAllExitStatusesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findAllFailedByDateBetween_WhenNoJobsFound_ShouldReturnMapWithListInInitialState() {
        assert [0] * 25 == clusterJobService.findAllFailedByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    @Test
    void test_findAllFailedByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnMapWithListInInitialState() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1), exitStatus: ClusterJob.Status.FAILED])

        assert [0] * 25 == clusterJobService.findAllFailedByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    @Test
    void test_findAllFailedByDateBetween_WhenSeveralJobsAreFound_ShouldReturnMapWithListContainingOccurencesOfFailedJobsPerHour() {
        createClusterJob([queued: SDATE_DATETIME, ended: SDATE_DATETIME.plusMinutes(30), exitStatus: ClusterJob.Status.FAILED])
        createClusterJob([queued: SDATE_DATETIME, ended: SDATE_DATETIME.plusMinutes(30), exitStatus: ClusterJob.Status.FAILED])
        createClusterJob([queued: SDATE_DATETIME, ended: SDATE_DATETIME.plusHours(1), exitStatus: ClusterJob.Status.FAILED])

        assert [2, 1] + [0] * 23 == clusterJobService.findAllFailedByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    @Test
    void test_findAllStatesByDateBetween_WhenNoJobsFound_ShouldReturnMapWithListInInitialState() {
        Map statesMap = clusterJobService.findAllStatesByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE)

        assert [0] * 25 == statesMap.data.queued
        assert [0] * 25 == statesMap.data.started
        assert [0] * 25 == statesMap.data.ended
    }

    @Test
    void test_findAllStatesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnMapWithListInInitialState() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), started: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1)])

        Map statesMap = clusterJobService.findAllStatesByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE)

        assert [0] * 25 == statesMap.data.queued
        assert [0] * 25 == statesMap.data.started
        assert [0] * 25 == statesMap.data.ended
    }

    @Test
    void test_findAllStatesByDateBetween_WhenSeveralJobsAreFound_ShouldReturnMapWithListsContainingOccurencesOfStatesPerHour() {
        createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME, ended: SDATE_DATETIME])
        createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME.plusHours(1), ended: SDATE_DATETIME.plusHours(2)])

        Map statesMap = clusterJobService.findAllStatesByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE)

        assert [2] + [0] * 24 ==  statesMap.data.queued
        assert [1, 1] + [0] * 23 == statesMap.data.started
        assert [1, 0, 1] + [0] * 22 == statesMap.data.ended
    }

    @Test
    void test_findAllAvgCoreUsageByDateBetween_WhenNoJobsFound_ShouldReturnMapWithListInInitialState() {
        assert [0] * 25 == clusterJobService.findAllAvgCoreUsageByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    @Test
    void test_findAllAvgCoreUsageByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnMapWithListInInitialState() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), started: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1), cpuTime: new Duration(30 * MINUTES_TO_MILLIS)])

        assert [0] * 25 == clusterJobService.findAllAvgCoreUsageByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    @Test
    void test_findAllAvgCoreUsageByDateBetween_WhenSeveralJobsAreFound_ShouldReturnMapWithListContainingCoreUsagePerHour() {
        createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME, ended: SDATE_DATETIME.plusMinutes(30), cpuTime: new Duration(30 * MINUTES_TO_MILLIS)])
        createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME, ended: SDATE_DATETIME.plusMinutes(30), cpuTime: new Duration(30 * MINUTES_TO_MILLIS)])
        createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME.plusHours(1), ended: SDATE_DATETIME.plusHours(1).plusMinutes(30), cpuTime: new Duration(30 * MINUTES_TO_MILLIS)])

        assert [2, 1] + [0] * 23 == clusterJobService.findAllAvgCoreUsageByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    @Test
    void test_findAllMemoryUsageByDateBetween_WhenNoJobsFound_ShouldReturnMapWithDataListInInitialState() {
        assert [0] * 25 == clusterJobService.findAllAvgCoreUsageByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    @Test
    void test_findAllMemoryUsageByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnMapWithDataListInInitialState() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1), usedMemory: GiB_TO_KiB])

        assert [0] * 25 == clusterJobService.findAllAvgCoreUsageByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    @Test
    void test_findAllMemoryUsageByDateBetween_WhenSeveralJobsAreFound_ShouldReturnMapWithDataListContainingMemoryUsagePerHours() {
        createClusterJob([queued: SDATE_DATETIME, ended: SDATE_DATETIME, usedMemory: GiB_TO_KiB])
        createClusterJob([queued: SDATE_DATETIME, ended: SDATE_DATETIME, usedMemory: GiB_TO_KiB])
        createClusterJob([queued: SDATE_DATETIME, ended: SDATE_DATETIME.plusHours(1), usedMemory: GiB_TO_KiB])

        assert [2, 1] + [0] * 23 == clusterJobService.findAllMemoryUsageByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    @Test
    void test_findAllStatesTimeDistributionByDateBetween_WhenNoJobsFound_ShouldReturnMapInInitialState() {
        assert [queue: [0, '0'], process: [0, '0']] == clusterJobService.findAllStatesTimeDistributionByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findAllStatesTimeDistributionByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnMapInInitialState() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(3), started: EDATE_DATETIME.minusDays(2), ended: EDATE_DATETIME.minusDays(1)])

        assert [queue: [0, '0'], process: [0, '0']] == clusterJobService.findAllStatesTimeDistributionByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findAllStatesTimeDistricutionByDateBetween_WhenSeveralJobsAreFound_ShouldReturnMapContainingPercentagesAndAbsoluteValuesOfStatesTimeDistribution() {
        createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME.plusHours(12), ended: EDATE_DATETIME])
        createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME.plusHours(6), ended: EDATE_DATETIME])

        assert [queue: [37, '18'], process: [63, '30']] == clusterJobService.findAllStatesTimeDistributionByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobClassSpecificSeqTypesByDateBetween_WhenNoJobsFound_ShouldReturnEmptyList() {
        assert [] == clusterJobService.findJobClassSpecificSeqTypesByDateBetween('jobClass1', SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobClassSpecificSeqTypesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnEmptyList() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), ended: EDATE_DATETIME.minusDays(1), seqType: seqType, jobClass: 'jobClass1'])

        assert [] == clusterJobService.findJobClassSpecificSeqTypesByDateBetween('jobClass1', SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobClassSpecificSeqTypesByDateBetween_WhenSeveralSeqTypesAreFound_ShouldReturnUniqueListWithSeqTypesByJobClass() {
        SeqType seqType2 = TestData.createSeqType([dirName: 'testDir'])
        seqType2.save([flush: true, failOnError: true])

        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, seqType: seqType, jobClass: 'jobClass1'])
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, seqType: seqType, jobClass: 'jobClass1'])
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, seqType: seqType2, jobClass: 'jobClass1'])

        assert [seqType, seqType2] == clusterJobService.findJobClassSpecificSeqTypesByDateBetween('jobClass1', SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificExitCodesByDateBetween_WhenNoJobsFound_ShouldReturnEmptyList() {
        assert [] == clusterJobService.findJobClassAndSeqTypeSpecificExitCodesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificExitCodesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnEmptyList() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1), jobClass: 'jobClass1', seqType: seqType, exitCode: 0])

        assert [] == clusterJobService.findJobClassAndSeqTypeSpecificExitCodesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificExitCodesByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithOccurencesOfExitCodesByJobClassAndSeqType() {
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, jobClass: 'jobClass1', seqType: seqType, exitCode: 0])
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, jobClass: 'jobClass1', seqType: seqType, exitCode: 0])
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, jobClass: 'jobClass1', seqType: seqType, exitCode: 10])

        assert [[0, 2], [10, 1]] == clusterJobService.findJobClassAndSeqTypeSpecificExitCodesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificExitStatusesByDateBetween_WhenNoJobsFound_ShouldReturnEmptyList() {
        assert [] == clusterJobService.findJobClassAndSeqTypeSpecificExitStatusesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificExitStatusesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnEmptyList() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1), jobClass: 'jobClass1', seqType: seqType, exitStatus: ClusterJob.Status.COMPLETED])

        assert [] == clusterJobService.findJobClassAndSeqTypeSpecificExitStatusesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificExitStatusesByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithOccurancesOfExitStatussesByJobClassAndSeqType() {
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, jobClass: 'jobClass1', seqType: seqType, exitStatus: ClusterJob.Status.COMPLETED])
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, jobClass: 'jobClass1', seqType: seqType, exitStatus: ClusterJob.Status.COMPLETED])
        createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, jobClass: 'jobClass1', seqType: seqType, exitStatus: ClusterJob.Status.FAILED])

        assert [[ClusterJob.Status.COMPLETED, 2], [ClusterJob.Status.FAILED, 1]] == clusterJobService.findJobClassAndSeqTypeSpecificExitStatusesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificStatesByDateBetween_WhenNoJobsFound_ShouldReturnMapWithDataListsInInitialState() {
        assert ['queued': [0] * 25, 'started': [0] * 25, 'ended': [0] * 25] == clusterJobService.findJobClassAndSeqTypeSpecificStatesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificStatesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnMapWithDataListsInInitialState() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), started: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1)])

        assert ['queued': [0] * 25, 'started': [0] * 25, 'ended': [0] * 25] == clusterJobService.findJobClassAndSeqTypeSpecificStatesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificStatesByDateBetween_ShouldReturnMapWithDataListsContainingOccurencesOfStatesPerHour() {
        createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME, ended: SDATE_DATETIME, jobClass: 'jobClass1', seqType: seqType])
        createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME.plusHours(1), ended: SDATE_DATETIME.plusHours(2), jobClass: 'jobClass1', seqType: seqType])

        Map statesMap = clusterJobService.findJobClassAndSeqTypeSpecificStatesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, SDATE_LOCALDATE)

        assert [2] + [0] * 24 ==  statesMap.data.queued
        assert [1, 1] + [0] * 23 == statesMap.data.started
        assert [1, 0, 1] + [0] * 22 == statesMap.data.ended
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificWalltimesByDateBetween_WhenNoJobsFound_ShouldReturnMapInInitialState() {
        Map walltimeMap = clusterJobService.findJobClassAndSeqTypeSpecificWalltimesByDateBetween('jobClass', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)

        assert [] == walltimeMap.data
        assert 0 == walltimeMap.xMax
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificWalltimesByDateBetween_WhenJobIstOutOfTimeSpan_ShouldReturnMapInInitialState() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), started: SDATE_DATETIME.minusDays(1).plusHours(12), ended: SDATE_DATETIME.minusDays(1), jobClass: 'jobClass1', seqType: seqType, requestedWalltime: new Duration(12 * HOURS_TO_MILLIS)])

        Map walltimeMap = clusterJobService.findJobClassAndSeqTypeSpecificWalltimesByDateBetween('jobClass', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)

        assert [] == walltimeMap.data
        assert 0 == walltimeMap.xMax
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificWalltimesByDateBetween_WhenSeveralJobsAreFound_ShouldReturnMapContainingMaximumWalltimeAndWalltimesPerHour() {
        ClusterJob job1 = createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME.plusHours(12), ended: EDATE_DATETIME, jobClass: 'jobClass1', seqType: seqType, requestedWalltime: new Duration(12 * HOURS_TO_MILLIS)])
        ClusterJob job2 = createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME.plusHours(18), ended: EDATE_DATETIME, jobClass: 'jobClass1', seqType: seqType, requestedWalltime: new Duration(12 * HOURS_TO_MILLIS)])

        Map walltimeMap = clusterJobService.findJobClassAndSeqTypeSpecificWalltimesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)

        assert [[12 * HOURS_TO_MILLIS, 12 * HOURS_TO_MILLIS, job1.id], [6 * HOURS_TO_MILLIS, 12 * HOURS_TO_MILLIS, job2.id]] == walltimeMap.data
        assert 12 * HOURS_TO_MILLIS == walltimeMap.xMax
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificMemoriesByDateBetween_WhenNoJobsFound_ShouldReturnMapInInitialState() {
        Map memoryMap = clusterJobService.findJobClassAndSeqTypeSpecificMemoriesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)

        assert [] == memoryMap.data
        assert 0 == memoryMap.xMax
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificMemoriesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnNullValue() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1), jobClass: 'jobClass1', seqType: seqType, usedMemory: GiB_TO_KiB, requestedMemory: GiB_TO_KiB])

        Map memoryMap = clusterJobService.findJobClassAndSeqTypeSpecificMemoriesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)

        assert [] == memoryMap.data
        assert 0 == memoryMap.xMax
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificMemoriesByDateBetween_WhenSeveralJobsAreFound_ShouldReturnMaximumMemoryUsageAndListWithMemoryUsagesPerJob() {
        ClusterJob job1 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, jobClass: 'jobClass1', seqType: seqType, usedMemory: GiB_TO_KiB, requestedMemory: GiB_TO_KiB])
        ClusterJob job2 = createClusterJob([queued: SDATE_DATETIME, ended: EDATE_DATETIME, jobClass: 'jobClass1', seqType: seqType, usedMemory: GiB_TO_KiB, requestedMemory: 2 * GiB_TO_KiB])

        Map memoryMap = clusterJobService.findJobClassAndSeqTypeSpecificMemoriesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)

        assert [[GiB_TO_KiB, GiB_TO_KiB, job1.id], [GiB_TO_KiB, 2 * GiB_TO_KiB, job2.id]] == memoryMap.data
        assert GiB_TO_KiB == memoryMap.xMax
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween_WhenNoJobsFound_ShouldReturnNullValue() {
        assert 0 == clusterJobService.findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnNullValue() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), started: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1), jobClass: 'jobClass1', seqType: seqType, cpuTime: new Duration(24 * HOURS_TO_MILLIS)])

        assert 0 == clusterJobService.findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween_WhenSeveralJobsAreFound_ShouldReturnAverageCoreUsage() {
        createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME.plusDays(1), ended: EDATE_DATETIME.plusDays(1), jobClass: 'jobClass1', seqType: seqType, cpuTime: new Duration(24 * HOURS_TO_MILLIS)])
        createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME.plusDays(1), ended: EDATE_DATETIME.plusDays(1), jobClass: 'jobClass1', seqType: seqType, cpuTime: new Duration(12 * HOURS_TO_MILLIS)])

        assert 0.75 == clusterJobService.findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE.plusDays(1))
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween_WhenNoJobsFound_ShouldReturnNullValue() {
        assert 0 == clusterJobService.findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween_WhenNoJobIsOutOfTimeSpan_ShouldReturnNullValue() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), started: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1), jobClass: 'jobClass1', seqType: seqType, usedMemory: 900L])

        assert 0 == clusterJobService.findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween_WhenSeveralJobsAreFound_ShouldReturnAverageMemoryUsage() {
        createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME.plusDays(1), ended: EDATE_DATETIME.plusDays(1), jobClass: 'jobClass1', seqType: seqType, usedMemory: 900L])
        createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME.plusDays(1), ended: EDATE_DATETIME.plusDays(1), jobClass: 'jobClass1', seqType: seqType, usedMemory: 100L])

        assert 500 == clusterJobService.findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE.plusDays(1))
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween_WhenNoJobsFound_ShouldReturnNullValues() {
        assert ['avgQueue': 0, 'avgProcess': 0] == clusterJobService.findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnNullValues() {
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), started: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1), jobClass: 'jobClass1', seqType: seqType])

        assert ['avgQueue': 0, 'avgProcess': 0] == clusterJobService.findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween_WhenSeveralJobsAreFound_ShouldReturnAverageStatesTimeDistribution() {
        createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME.plusHours(12), ended: EDATE_DATETIME, jobClass: 'jobClass1', seqType: seqType])
        createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME.plusHours(18), ended: EDATE_DATETIME, jobClass: 'jobClass1', seqType: seqType])

        assert ['avgQueue': 15 * HOURS_TO_MILLIS, 'avgProcess': 9 * HOURS_TO_MILLIS] == clusterJobService.findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    @Test
    void test_findJobSpecificStatesTimeDistributionByJobId_WhenNoJobIsFound_ShouldReturnNull() {
        assert null == clusterJobService.findJobSpecificStatesTimeDistributionByJobId(0)
    }

    @Test
    void test_findJobSpecificStatesTimeDistributionByJobId_WhenJobIsFound_ReturnStatesTimeDistribution() {
        createClusterJob([queued: SDATE_DATETIME, started: SDATE_DATETIME.plusHours(18), ended: EDATE_DATETIME])

        assert ['queue': [75, 18 * HOURS_TO_MILLIS], 'process': [25, 6 * HOURS_TO_MILLIS]]
    }

    @Test
    void test_getLatestJobDate_WhenNoJobsFound_ShouldReturnNull() {
        assert null == clusterJobService.getLatestJobDate()
    }

    @Test
    void test_getLatestJobDate_WhenSeveralJobsAreFound_ShouldReturnLatestJobDate() {
        createClusterJob([queued: SDATE_DATETIME])
        createClusterJob([queued: SDATE_DATETIME.plusDays(1)])
        createClusterJob([queued: SDATE_DATETIME.plusDays(3)])

        assert SDATE_DATETIME.plusDays(3).toLocalDate()
    }

    private ClusterJob createClusterJob(Map myProps = [:]) {
        Map props = [
                jobClass: 'testClass',
                clusterJobId: "testId_${uniqueIdCounter++}",
                seqType: seqType,
        ] + myProps

        Realm realm = DomainFactory.createRealmDataProcessingDKFZ()
        assert realm.save([flush: true, failOnError: true])

        ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep(props.jobClass)
        assert processingStep

        ClusterJob job = clusterJobService.createClusterJob(realm, props.clusterJobId, processingStep, props.seqType)
        job.clusterJobName = "test_" + processingStep.getNonQualifiedJobClass()

        props.remove(['jobClass', 'clusterJobId', 'seqType'])

        props.each { key, value ->
            job."${key}" = value
        }

        assert job.save([flush: true, failOnError: true])

        return job
    }

    private SOAPFault createSoapFault() {
        SOAPFault soapFault = SOAPFactory.newInstance().createFault()
        soapFault.setFaultString("fault message")
        soapFault.setFaultCode(new QName(SOAPConstants.URI_NS_SOAP_ENVELOPE, "Sender"))
        soapFault.setFaultActor("START AP")
        return soapFault
    }
}