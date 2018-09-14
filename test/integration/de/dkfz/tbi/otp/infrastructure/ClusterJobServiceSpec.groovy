package de.dkfz.tbi.otp.infrastructure

import de.dkfz.roddy.config.*
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.tools.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.joda.time.*
import spock.lang.*

import java.time.*

import static java.util.concurrent.TimeUnit.*

class ClusterJobServiceSpec extends Specification {

    public static final org.joda.time.LocalDate SDATE_LOCALDATE = new org.joda.time.LocalDate()
    public static final org.joda.time.LocalDate EDATE_LOCALDATE = SDATE_LOCALDATE.plusDays(1)
    public static final org.joda.time.DateTime SDATE_DATETIME = SDATE_LOCALDATE.toDateTimeAtStartOfDay()
    public static final org.joda.time.DateTime EDATE_DATETIME = EDATE_LOCALDATE.toDateTimeAtStartOfDay()

    public static final Long MINUTES_TO_MILLIS = MINUTES.toMillis(1)
    public static final Long HOURS_TO_MILLIS = HOURS.toMillis(1)

    public static final Long GiB_TO_KiB = 1024 * 1024

    ClusterJobService clusterJobService
    ConfigService configService

    SeqType seqType


    void setup() {
        seqType = DomainFactory.createSeqType()
    }

    void "test amendClusterJob with values"() {
        given:
        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.TIME_ZONE, type: null, value: "Europe/Berlin")

        def(job, run) = createClusterJobWithRun(null, [seqType: seqType])
        job = job as ClusterJob

        ClusterJob c2 = createClusterJob(seqType: seqType)

        GenericJobInfo jobInfo = new GenericJobInfo(null, null, null, null, [c2.clusterJobId])

        jobInfo.askedResources = new ResourceSet(new BufferValue(7), 8, 9, java.time.Duration.ofSeconds(10), new BufferValue(11), "fasttrack", null)
        jobInfo.logFile = new File("/file.log")
        jobInfo.account = "257"

        when:
        clusterJobService.amendClusterJob(job, jobInfo)

        then:
        job.requestedCores == 8
        job.requestedWalltime == org.joda.time.Duration.standardSeconds(10)
        job.requestedMemory == 7*1024*1024

        job.accountName == "257"
        job.jobLog == new File("/file.log").absolutePath
        job.dependencies == [c2] as Set
    }

    void "test amendClusterJob empty"() {
        given:
        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.TIME_ZONE, type: null, value: "Europe/Berlin")

        def(job, run) = createClusterJobWithRun(null, [seqType: seqType])
        job = job as ClusterJob

        GenericJobInfo jobInfo = new GenericJobInfo(null, null, null, null, null)

        when:
        clusterJobService.amendClusterJob(job, jobInfo)

        then:
        job.requestedCores == null
        job.requestedWalltime == null
        job.requestedMemory == null

        job.accountName == null
        job.jobLog == null
        job.dependencies == [] as Set
    }

    void "test completeClusterJob with values"() {
        given:
        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.TIME_ZONE, type: null, value: "Europe/Berlin")

        def(job, run) = createClusterJobWithRun(null, [seqType: seqType])
        job = job as ClusterJob

        ClusterJob c2 = createClusterJob(seqType: seqType)

        ClusterJobIdentifier clusterJobIdentifier = new ClusterJobIdentifier(job.realm, job.clusterJobId, job.userName)
        GenericJobInfo jobInfo = new GenericJobInfo(null, null, null, null, [c2.id as String])

        jobInfo.submitTime = java.time.ZonedDateTime.of(2017, 8, 8, 1, 0, 0, 0, configService.timeZoneId)
        jobInfo.eligibleTime = java.time.ZonedDateTime.of(2017, 8, 9, 2, 0, 0, 0, configService.timeZoneId)
        jobInfo.startTime = java.time.ZonedDateTime.of(2017, 8, 10, 3, 0, 0, 0, configService.timeZoneId)
        jobInfo.endTime = java.time.ZonedDateTime.of(2017, 8, 11, 4, 0, 0, 0, configService.timeZoneId)
        jobInfo.timeSystemSuspState = java.time.Duration.ofSeconds(123)
        jobInfo.timeUserSuspState = java.time.Duration.ofSeconds(456)

        jobInfo.cpuTime = java.time.Duration.ofSeconds(789)
        jobInfo.usedResources = new ResourceSet(new BufferValue(2), 3, 4, java.time.Duration.ofSeconds(5), new BufferValue(6), "fasttrack", null)
        jobInfo.askedResources = new ResourceSet(new BufferValue(7), 8, 9, java.time.Duration.ofSeconds(10), new BufferValue(11), "fasttrack", null)
        jobInfo.usedResources.swap = new BufferValue(12)

        jobInfo.executionHosts = ["host"]
        jobInfo.account = "257"
        jobInfo.startCount = 361

        when:
        clusterJobService.completeClusterJob(clusterJobIdentifier, ClusterJob.Status.COMPLETED, jobInfo)

        then:
        job.exitStatus == ClusterJob.Status.COMPLETED
        job.exitCode == jobInfo.exitCode

        job.queued == clusterJobService.convertFromJava8ZonedDateTimeToJodaDateTime(java.time.ZonedDateTime.of(2017, 8, 8, 1, 0, 0, 0, configService.timeZoneId))
        job.eligible ==clusterJobService.convertFromJava8ZonedDateTimeToJodaDateTime(java.time.ZonedDateTime.of(2017, 8, 9, 2, 0, 0, 0, configService.timeZoneId))
        job.started ==clusterJobService.convertFromJava8ZonedDateTimeToJodaDateTime(java.time.ZonedDateTime.of(2017, 8, 10, 3, 0, 0, 0, configService.timeZoneId))
        job.ended ==clusterJobService.convertFromJava8ZonedDateTimeToJodaDateTime(java.time.ZonedDateTime.of(2017, 8, 11, 4, 0, 0, 0, configService.timeZoneId))

        job.systemSuspendStateDuration == org.joda.time.Duration.standardSeconds(123)
        job.userSuspendStateDuration == org.joda.time.Duration.standardSeconds(456)

        job.cpuTime == org.joda.time.Duration.standardSeconds(789)
        job.usedCores == 3
        job.usedMemory == 2*1024*1024
        job.usedSwap == 12*1024*1024

        job.node == "host"
        job.startCount == 361
    }

    void "test completeClusterJob empty"() {
        given:
        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.TIME_ZONE, type: null, value: "Canada/Saskatchewan")

        def(job, run) = createClusterJobWithRun(null, [seqType: seqType])
        job = job as ClusterJob
        org.joda.time.DateTime queued = job.queued

        ClusterJobIdentifier clusterJobIdentifier = new ClusterJobIdentifier(job.realm, job.clusterJobId, job.userName)
        GenericJobInfo jobInfo = new GenericJobInfo(null, null, null, null, null)

        when:
        clusterJobService.completeClusterJob(clusterJobIdentifier, ClusterJob.Status.FAILED, jobInfo)

        then:
        job.exitStatus == ClusterJob.Status.FAILED
        job.exitCode == null

        job.queued == queued
        job.eligible == null
        job.started == null
        job.ended == null
        job.systemSuspendStateDuration == null
        job.userSuspendStateDuration == null

        job.cpuTime == null
        job.usedCores == null
        job.usedMemory == null
        job.requestedCores == null
        job.requestedWalltime == null
        job.requestedMemory == null
        job.usedSwap == null

        job.node == null
        job.accountName == null
        job.startCount == null
        job.dependencies == null
    }

    void "test convertFromJava8DurationToJodaDuration"() {
        expect:
        jo == clusterJobService.convertFromJava8DurationToJodaDuration(j8)

        where:
        j8                               || jo
        null                             || null
        java.time.Duration.ofSeconds(20) || org.joda.time.Duration.standardSeconds(20)
        java.time.Duration.ofMinutes(30) || org.joda.time.Duration.standardMinutes(30)
        java.time.Duration.ofHours(40)   || org.joda.time.Duration.standardHours(40)
        java.time.Duration.ofDays(50)    || org.joda.time.Duration.standardDays(50)
    }

    void "test convertFromJava8ZonedDateTimeToJodaDateTime with null"() {
        expect:
        null == clusterJobService.convertFromJava8ZonedDateTimeToJodaDateTime(null)
    }

    void "test convertFromJava8ZonedDateTimeToJodaDateTime with value"() {
        given:
        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.TIME_ZONE, type: null, value: "Australia/West")

        expect:
        new org.joda.time.DateTime(2017, 8, 8, 1, 0, DateTimeZone.default) ==
                clusterJobService.convertFromJava8ZonedDateTimeToJodaDateTime(java.time.ZonedDateTime.of(2017, 8, 8, 1, 0, 0, 0, ZoneId.systemDefault()))
    }

    void testFindWorkflowObjectByClusterJob() {
        given:
        def(job, run) = createClusterJobWithRun()

        expect:
        run == clusterJobService.findProcessParameterObjectByClusterJob(job)
    }

    void testFindAllClusterJobsToOtpJob_WhenDifferentProcessingSteps_ShouldReturnClusterJobsOfSameProcessingStepAndJobClass() {
        given:
        ClusterJob job1 = createClusterJob()

        ClusterJob job2 = createClusterJob()
        job2.processingStep = job1.processingStep
        job2.save(flush: true)

        createClusterJob()

        expect:
        CollectionUtils.containSame([job1, job2], ClusterJobService.findAllClusterJobsToOtpJob(job1))
    }

    void testFindAllClusterJobsToOtpJob_WhenDifferentJobClasses_ShouldReturnClusterJobsOfSameProcessingStepAndJobClass() {
        given:
        String jobClass1 = "testClass1"
        String jobClass2 = "testClass2"

        ClusterJob job1 = createClusterJob([jobClass: jobClass1])

        ClusterJob job2 = createClusterJob([jobClass: jobClass1])
        job2.processingStep = job1.processingStep
        job2.save(flush: true)

        ClusterJob job3 = createClusterJob([jobClass: jobClass2])
        job3.processingStep = job1.processingStep
        job3.save(flush: true)

        expect:
        CollectionUtils.containSame([job1, job2], ClusterJobService.findAllClusterJobsToOtpJob(job1))
    }

    void testGetBasesSum_WhenContainedSeqTracksContainBasesAndSeveralJobsBelongToOtpJob_ShouldReturnNormalizedSumOfBases() {
        given:
        def (job, run) = setupClusterJobsOfSameProcessingStepAndRun()

        DomainFactory.createSeqTrack([run: run, nBasePairs: 150L])
        DomainFactory.createSeqTrack([run: run, nBasePairs: 150L])

        expect:
        100L == ClusterJobService.getBasesSum(job)
    }

    void testGetBasesSum_WhenNoContainedSeqTracks_ShouldReturnNull() {
        given:
        def(job, run) = setupClusterJobsOfSameProcessingStepAndRun()

        expect:
        null == ClusterJobService.getBasesSum(job)
    }

    void testGetBasesSum_WhenContainedSeqTracksContainNoBases_ShouldReturnNull() {
        given:
        def(job, run) = setupClusterJobsOfSameProcessingStepAndRun()

        DomainFactory.createSeqTrack([run: run])

        expect:
        null == ClusterJobService.getBasesSum(job)
    }

    void testGetFileSizesSum_WhenContainedDataFilesContainFileSizesAndSeveralJobsBelongToOtpJob_ShouldReturnNormalizedSumOfFileSizes() {
        given:
        def(job, run) = setupClusterJobsOfSameProcessingStepAndRun()

        DomainFactory.createSeqTrackWithOneDataFile([run: run], [fileSize: 150L])
        DomainFactory.createSeqTrackWithOneDataFile([run: run], [fileSize: 150L])

        expect:
        100L == ClusterJobService.getFileSizesSum(job)
    }

    void testGetFileSizesSum_WhenNoContainedDataFiles_ShouldReturnNull() {
        given:
        def(job, run) = setupClusterJobsOfSameProcessingStepAndRun()

        expect:
        null == ClusterJobService.getFileSizesSum(job)
    }

    void testGetReadsSum_WhenContainedSeqTracksContainBasesAndSeveralJobsBelongToOtpJob_ShouldReturnNormalizedSumOfReads() {
        given:
        def(job, run) = setupClusterJobsOfSameProcessingStepAndRun()

        DomainFactory.createSeqTrackWithOneDataFile([run: run], [nReads: 150L])
        DomainFactory.createSeqTrackWithOneDataFile([run: run], [nReads: 150L])

        expect:
        100L == ClusterJobService.getReadsSum(job)
    }

    void testGetReadsSum_WhenNoContainedSeqTracks_ShouldReturnNull() {
        given:
        def(job, run) = setupClusterJobsOfSameProcessingStepAndRun()

        expect:
        null == ClusterJobService.getReadsSum(job)
    }

    void testGetReadsSum_WhenContainedSeqTracksContainNoBases_ShouldReturnNull() {
        given:
        def(job, run) = setupClusterJobsOfSameProcessingStepAndRun()

        DomainFactory.createSeqTrack([run: run])

        expect:
        null == ClusterJobService.getReadsSum(job)
    }

    void testIsXten_WhenSeqTrackProcessedWithXten_ShouldReturnTrue() {
        given:
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build(name: "HiSeq X Ten")
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup([seqPlatformModelLabel: seqPlatformModelLabel])
        def(job, run) = createClusterJobWithRun(DomainFactory.createRun(seqPlatform: seqPlatform))
        DomainFactory.createSeqTrack(run: run)

        expect:
        ClusterJobService.isXten(job)
    }

    void testIsXten_WhenSeqTrackNotProcessedWithXten_ShouldReturnFalse() {
        given:
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build(name: "HiSeq2500")
        SeqPlatform seqPlatform = DomainFactory.createSeqPlatformWithSeqPlatformGroup([seqPlatformModelLabel: seqPlatformModelLabel])
        def(job, run) = createClusterJobWithRun(DomainFactory.createRun(seqPlatform: seqPlatform))
        DomainFactory.createSeqTrack(run: run)

        expect:
        !(ClusterJobService.isXten(job))
    }

    void test_handleObviouslyFailedClusterJob_WhenElapsedWalltimeUnderDurationJobObviuoslyFailed_ShouldChangeExitStatusToFailed() {
        given:
        ClusterJob job = createClusterJob([queued: SDATE_DATETIME,
                                           started: SDATE_DATETIME,
                                           ended: SDATE_DATETIME.plusMillis(ClusterJobService.DURATION_JOB_OBVIOUSLY_FAILED.millis as int),
                                           exitStatus: ClusterJob.Status.COMPLETED])

        clusterJobService.handleObviouslyFailedClusterJob(job)

        expect:
        job.exitStatus == ClusterJob.Status.FAILED
    }

    void test_handleObviouslyFailedClusterJob_WhenElapsedWalltimeOverDurationJobObviuoslyFailed_ShouldKeepExitStatusCompleted() {
        ClusterJob job = createClusterJob([queued: SDATE_DATETIME,
                                           started: SDATE_DATETIME,
                                           ended: SDATE_DATETIME.plusMillis(ClusterJobService.DURATION_JOB_OBVIOUSLY_FAILED.millis + 1 as int),
                                           exitStatus: ClusterJob.Status.COMPLETED])

        clusterJobService.handleObviouslyFailedClusterJob(job)

        expect:
        job.exitStatus == ClusterJob.Status.COMPLETED
    }

    void test_findAllClusterJobsByDateBetween_WhenNoJobsFound_ShouldReturnEmptyList() {
        expect:
        [] == clusterJobService.findAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, "", 0, 10, 'clusterJobId', 'asc')
    }

    void test_findAllClusterJobsByDateBetween_WhenJobIsOutOfTimeSpanToEarly_ShouldReturnEmptyList() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1),
                          started: SDATE_DATETIME.minusDays(1),
                          ended: SDATE_DATETIME.minusDays(1)])

        expect:
        [] == clusterJobService.findAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, "", 0, 10, 'clusterJobId', 'asc')
    }

    void test_findAllClusterJobsByDateBetween_WhenJobIsOutOfTimeSpanToLate_ShouldReturnEmptyList() {
        given:
        createClusterJob([queued: SDATE_DATETIME.plusDays(2),
                          started: SDATE_DATETIME.plusDays(2),
                          ended: SDATE_DATETIME.plusDays(2)])

        expect:
        [] == clusterJobService.findAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, "", 0, 10, 'clusterJobId', 'asc')
    }

    void test_findAllClusterJobsByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithJobs() {
        given:
        ClusterJob job1 = createClusterJob([queued: SDATE_DATETIME,
                                            started: SDATE_DATETIME,
                                            ended: EDATE_DATETIME])

        ClusterJob job2 = createClusterJob([queued: SDATE_DATETIME,
                                            started: SDATE_DATETIME,
                                            ended: EDATE_DATETIME])

        expect:
        [job1, job2] == clusterJobService.findAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, "", 0, 10, 'clusterJobId', 'asc')
    }

    void test_findAllClusterJobsByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithJobs_PassingFilter() {
        given:
        String filter = 'filter'
        ClusterJob job1 = createClusterJob([queued: SDATE_DATETIME,
                                            started: SDATE_DATETIME,
                                            ended: EDATE_DATETIME,
                                            clusterJobName: "Value with ${filter} something _testClass"])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          clusterJobName: "some other value _testClass"])

        ClusterJob job3 = createClusterJob([queued: SDATE_DATETIME,
                                            started: SDATE_DATETIME,
                                            ended: EDATE_DATETIME,
                                            clusterJobName: "Value with ${filter.toUpperCase()} something _testClass"])

        expect:
        [job1, job3] == clusterJobService.findAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, filter, 0, 10, 'clusterJobId', 'asc')
    }

    void test_findAllClusterJobsByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithJobs_UsingPage() {
        given:
        List<ClusterJob> jobs = (1..10).collect {
            createClusterJob([queued: SDATE_DATETIME,
                              started: SDATE_DATETIME,
                              ended: EDATE_DATETIME])
        }

        expect:
        jobs.subList(3,7) == clusterJobService.findAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, '', 3, 4, 'clusterJobId', 'asc')
    }

    void test_findAllClusterJobsByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithJobs_SortedDesc() {
        given:
        ClusterJob job1 = createClusterJob([queued: SDATE_DATETIME,
                                            started: SDATE_DATETIME,
                                            ended: EDATE_DATETIME])

        ClusterJob job2 = createClusterJob([queued: SDATE_DATETIME,
                                            started: SDATE_DATETIME,
                                            ended: EDATE_DATETIME])

        expect:
        [job2, job1] == clusterJobService.findAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, "", 0, 10, 'clusterJobId', 'desc')
    }

    void test_findAllClusterJobsByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithJobs_SortedByName() {
        given:
        ClusterJob job1 = createClusterJob([queued: SDATE_DATETIME,
                                            started: SDATE_DATETIME,
                                            ended: EDATE_DATETIME,
                                            clusterJobName: "name3 _testClass"])

        ClusterJob job2 = createClusterJob([queued: SDATE_DATETIME,
                                            started: SDATE_DATETIME,
                                            ended: EDATE_DATETIME,
                                            clusterJobName: "name1 _testClass"])

        ClusterJob job3 = createClusterJob([queued: SDATE_DATETIME,
                                            started: SDATE_DATETIME,
                                            ended: EDATE_DATETIME,
                                            clusterJobName: "name2 _testClass"])

        expect:
        [job2, job3, job1] == clusterJobService.findAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, '', 0, 10, 'clusterJobName', 'asc')
    }

    void test_countAllClusterJobsByDateBetween_WhenNoJobsFound_ShouldReturnZero() {
        expect:
        0 == clusterJobService.countAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, "")
    }

    void test_countAllClusterJobsByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnZero() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1),
                          started: SDATE_DATETIME.minusDays(1),
                          ended: SDATE_DATETIME.minusDays(1)])

        expect:
        0 == clusterJobService.countAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, "")
    }

    void test_countAllClusterJobsByDateBetween_WhenSeveralJobsAreFound_ShouldReturnTwo() {
        given:
        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME])

        expect:
        2 == clusterJobService.countAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, "")
    }

    void test_countAllClusterJobsByDateBetween_WhenSeveralJobsPassFilter_ShouldReturnTwo() {
        given:
        String filter = 'filter'
        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          clusterJobName: "Value with ${filter} something _testClass"])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          clusterJobName: "some other value _testClass"])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          clusterJobName: "Value with ${filter.toUpperCase()} something _testClass"])

        expect:
        2 == clusterJobService.countAllClusterJobsByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE, filter)
    }

    void test_findAllJobClassesByDateBetween_WhenNoJobsFound_ShouldReturnEmptyList() {
        expect:
        [] == clusterJobService.findAllJobClassesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findAllJobClassesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnEmptyList() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1),
                          started: SDATE_DATETIME.minusDays(1),
                          ended: SDATE_DATETIME.minusDays(1),
                          jobClass: 'jobClass1'])

        expect:
        [] == clusterJobService.findAllJobClassesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findAllJobClassesByDateBetween_WhenSeveralJobClassesAreFound_ShouldReturnUniqueListWithJobClasses() {
        given:
        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          jobClass: 'jobClass1'])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          jobClass: 'jobClass1'])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          jobClass: 'jobClass2'])

        expect:
        ['jobClass1', 'jobClass2'] == clusterJobService.findAllJobClassesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findAllExitCodesByDateBetween_WhenNoJobsFound_ShouldReturnEmptyList() {
        expect:
        [] == clusterJobService.findAllExitCodesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findAllExitCodesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnEmptyList() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1),
                          started: SDATE_DATETIME.minusDays(1),
                          ended: SDATE_DATETIME.minusDays(1),
                          exitCode: 0])

        expect:
        [] == clusterJobService.findAllExitCodesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findAllExitCodesByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithOccurancesPerExitCodes() {
        given:
        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          exitCode: 0])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          exitCode: 0])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          exitCode: 1])

        expect:
        [[0, 2], [1, 1]] == clusterJobService.findAllExitCodesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findAllExitStatusesByDateBetween_WhenNoJobsFound_ShouldReturnEmptyList() {
        expect:
        [] == clusterJobService.findAllExitStatusesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findAllExitStatusesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnEmptyList() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1),
                          started: SDATE_DATETIME.minusDays(1),
                          ended: EDATE_DATETIME.minusDays(1),
                          exitStatus: ClusterJob.Status.COMPLETED])

        expect:
        [] == clusterJobService.findAllExitStatusesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findAllExitStatusesByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithOccurancesPerExitStatuses() {
        given:
        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          exitStatus: ClusterJob.Status.COMPLETED])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          exitStatus: ClusterJob.Status.COMPLETED])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          exitStatus: ClusterJob.Status.FAILED])

        expect:
        [[ClusterJob.Status.COMPLETED, 2], [ClusterJob.Status.FAILED, 1]] == clusterJobService.findAllExitStatusesByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findAllFailedByDateBetween_WhenNoJobsFound_ShouldReturnMapWithListInInitialState() {
        expect:
        [0] * 25 == clusterJobService.findAllFailedByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    void test_findAllFailedByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnMapWithListInInitialState() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1),
                          started: SDATE_DATETIME.minusDays(1),
                          ended: SDATE_DATETIME.minusDays(1),
                          exitStatus: ClusterJob.Status.FAILED])

        expect:
        [0] * 25 == clusterJobService.findAllFailedByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    void test_findAllFailedByDateBetween_WhenSeveralJobsAreFound_ShouldReturnMapWithListContainingOccurencesOfFailedJobsPerHour() {
        given:
        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: SDATE_DATETIME.plusMinutes(30),
                          exitStatus: ClusterJob.Status.FAILED])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: SDATE_DATETIME.plusMinutes(30),
                          exitStatus: ClusterJob.Status.FAILED])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: SDATE_DATETIME.plusHours(1),
                          exitStatus: ClusterJob.Status.FAILED])

        expect:
        [2, 1] + [0] * 23 == clusterJobService.findAllFailedByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    void test_findAllStatesByDateBetween_WhenNoJobsFound_ShouldReturnMapWithListInInitialState() {
        given:
        Map statesMap = clusterJobService.findAllStatesByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE)

        expect:
        [0] * 25 == statesMap.data.queued
        [0] * 25 == statesMap.data.started
        [0] * 25 == statesMap.data.ended
    }

    void test_findAllStatesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnMapWithListInInitialState() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1),
                          started: SDATE_DATETIME.minusDays(1),
                          ended: SDATE_DATETIME.minusDays(1)])

        Map statesMap = clusterJobService.findAllStatesByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE)

        expect:
        [0] * 25 == statesMap.data.queued
        [0] * 25 == statesMap.data.started
        [0] * 25 == statesMap.data.ended
    }

    void test_findAllStatesByDateBetween_WhenSeveralJobsAreFound_ShouldReturnMapWithListsContainingOccurencesOfStatesPerHour() {
        given:
        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: SDATE_DATETIME])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME.plusHours(1),
                          ended: SDATE_DATETIME.plusHours(2)])

        Map statesMap = clusterJobService.findAllStatesByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE)

        expect:
        [2] + [0] * 24 ==  statesMap.data.queued
        [1, 1] + [0] * 23 == statesMap.data.started
        [1, 0, 1] + [0] * 22 == statesMap.data.ended
    }

    void test_findAllAvgCoreUsageByDateBetween_WhenNoJobsFound_ShouldReturnMapWithListInInitialState() {
        expect:
        [0] * 25 == clusterJobService.findAllAvgCoreUsageByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    void test_findAllAvgCoreUsageByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnMapWithListInInitialState() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1),
                          started: SDATE_DATETIME.minusDays(1),
                          ended: SDATE_DATETIME.minusDays(1),
                          cpuTime: new org.joda.time.Duration(30 * MINUTES_TO_MILLIS),
                          exitStatus: ClusterJob.Status.COMPLETED])

        expect:
        [0] * 25 == clusterJobService.findAllAvgCoreUsageByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    void test_findAllAvgCoreUsageByDateBetween_WhenSeveralJobsAreFound_ShouldReturnMapWithListContainingCoreUsagePerHour() {
        given:
        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: SDATE_DATETIME.plusMinutes(30),
                          cpuTime: new org.joda.time.Duration(30 * MINUTES_TO_MILLIS),
                          exitStatus: ClusterJob.Status.COMPLETED])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: SDATE_DATETIME.plusMinutes(30),
                          cpuTime: new org.joda.time.Duration(30 * MINUTES_TO_MILLIS),
                          exitStatus: ClusterJob.Status.COMPLETED])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME.plusHours(1),
                          ended: SDATE_DATETIME.plusHours(1).plusMinutes(30),
                          cpuTime: new org.joda.time.Duration(30 * MINUTES_TO_MILLIS),
                          exitStatus: ClusterJob.Status.COMPLETED])

        expect:
        [2, 1] + [0] * 23 == clusterJobService.findAllAvgCoreUsageByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    void test_findAllMemoryUsageByDateBetween_WhenNoJobsFound_ShouldReturnMapWithDataListInInitialState() {
        expect:
        [0] * 25 == clusterJobService.findAllAvgCoreUsageByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    void test_findAllMemoryUsageByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnMapWithDataListInInitialState() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1),
                          started: SDATE_DATETIME.minusDays(1),
                          ended: SDATE_DATETIME.minusDays(1),
                          usedMemory: GiB_TO_KiB,
                          exitStatus: ClusterJob.Status.COMPLETED])

        expect:
        [0] * 25 == clusterJobService.findAllAvgCoreUsageByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    void test_findAllMemoryUsageByDateBetween_WhenSeveralJobsAreFound_ShouldReturnMapWithDataListContainingMemoryUsagePerHours() {
        given:
        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: SDATE_DATETIME.plusMinutes(30),
                          usedMemory: GiB_TO_KiB,
                          exitStatus: ClusterJob.Status.COMPLETED])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: SDATE_DATETIME.plusMinutes(30),
                          usedMemory: GiB_TO_KiB,
                          exitStatus: ClusterJob.Status.COMPLETED])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME.plusHours(1),
                          ended: SDATE_DATETIME.plusHours(1).plusMinutes(30),
                          usedMemory: GiB_TO_KiB,
                          exitStatus: ClusterJob.Status.COMPLETED])

        expect:
        [2, 1] + [0] * 23 == clusterJobService.findAllMemoryUsageByDateBetween(SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    void test_findAllStatesTimeDistributionByDateBetween_WhenNoJobsFound_ShouldReturnMapInInitialState() {
        expect:
        [queue: [0, '0'], process: [0, '0']] == clusterJobService.findAllStatesTimeDistributionByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findAllStatesTimeDistributionByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnMapInInitialState() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(3),
                          started: EDATE_DATETIME.minusDays(2),
                          ended: EDATE_DATETIME.minusDays(1),
                          exitStatus: ClusterJob.Status.COMPLETED])

        expect:
        [queue: [0, '0'], process: [0, '0']] == clusterJobService.findAllStatesTimeDistributionByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }


    void test_findAllStatesTimeDistributionByDateBetween_WhenSeveralJobsAreFound_ShouldReturnMapContainingPercentagesAndAbsoluteValuesOfStatesTimeDistribution() {
        given:
        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME.plusHours(12),
                          ended: EDATE_DATETIME,
                          exitStatus: ClusterJob.Status.COMPLETED])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME.plusHours(6),
                          ended: EDATE_DATETIME,
                          exitStatus: ClusterJob.Status.COMPLETED])

        expect:
        [queue: [37, '18'], process: [63, '30']] == clusterJobService.findAllStatesTimeDistributionByDateBetween(SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassSpecificSeqTypesByDateBetween_WhenNoJobsFound_ShouldReturnEmptyList() {
        expect:
        [] == clusterJobService.findJobClassSpecificSeqTypesByDateBetween('jobClass1', SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassSpecificSeqTypesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnEmptyList() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1),
                          started:  SDATE_DATETIME.minusDays(1),
                          ended: EDATE_DATETIME.minusDays(1),
                          seqType: seqType,
                          jobClass: 'jobClass1'])

        expect:
        [] == clusterJobService.findJobClassSpecificSeqTypesByDateBetween('jobClass1', SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassSpecificSeqTypesByDateBetween_WhenSeveralSeqTypesAreFound_ShouldReturnUniqueListWithSeqTypesByJobClass() {
        given:
        SeqType seqType2 = DomainFactory.createSeqType(
                dirName: 'testDir',
        )

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          seqType: seqType,
                          jobClass: 'jobClass1'])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          seqType: seqType,
                          jobClass: 'jobClass1'])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          seqType: seqType2,
                          jobClass: 'jobClass1'])

        expect:
        [seqType, seqType2] == clusterJobService.findJobClassSpecificSeqTypesByDateBetween('jobClass1', SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassAndSeqTypeSpecificExitCodesByDateBetween_WhenNoJobsFound_ShouldReturnEmptyList() {
        expect:
        [] == clusterJobService.findJobClassAndSeqTypeSpecificExitCodesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassAndSeqTypeSpecificExitCodesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnEmptyList() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1),
                          started: SDATE_DATETIME.minusDays(1),
                          ended: SDATE_DATETIME.minusDays(1),
                          jobClass: 'jobClass1',
                          seqType: seqType, exitCode: 0])

        expect:
        [] == clusterJobService.findJobClassAndSeqTypeSpecificExitCodesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassAndSeqTypeSpecificExitCodesByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithOccurencesOfExitCodesByJobClassAndSeqType() {
        given:
        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          exitCode: 0])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          exitCode: 0])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          exitCode: 10])

        expect:
        [[0, 2], [10, 1]] == clusterJobService.findJobClassAndSeqTypeSpecificExitCodesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassAndSeqTypeSpecificExitStatusesByDateBetween_WhenNoJobsFound_ShouldReturnEmptyList() {
        expect:
        [] == clusterJobService.findJobClassAndSeqTypeSpecificExitStatusesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassAndSeqTypeSpecificExitStatusesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnEmptyList() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1),
                          started: SDATE_DATETIME.minusDays(1),
                          ended: SDATE_DATETIME.minusDays(1),
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          exitStatus: ClusterJob.Status.COMPLETED])

        expect:
        [] == clusterJobService.findJobClassAndSeqTypeSpecificExitStatusesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassAndSeqTypeSpecificExitStatusesByDateBetween_WhenSeveralJobsAreFound_ShouldReturnListWithOccurancesOfExitStatussesByJobClassAndSeqType() {
        given:
        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          exitStatus: ClusterJob.Status.COMPLETED])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          exitStatus: ClusterJob.Status.COMPLETED])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: EDATE_DATETIME,
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          exitStatus: ClusterJob.Status.FAILED])

        expect:
        [[ClusterJob.Status.COMPLETED, 2], [ClusterJob.Status.FAILED, 1]] == clusterJobService.findJobClassAndSeqTypeSpecificExitStatusesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassAndSeqTypeSpecificStatesByDateBetween_WhenNoJobsFound_ShouldReturnMapWithDataListsInInitialState() {
        expect:
        ['queued': [0] * 25, 'started': [0] * 25, 'ended': [0] * 25] == clusterJobService.findJobClassAndSeqTypeSpecificStatesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    void test_findJobClassAndSeqTypeSpecificStatesByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnMapWithDataListsInInitialState() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1),
                          started: SDATE_DATETIME.minusDays(1),
                          ended: SDATE_DATETIME.minusDays(1)])

        expect:
        ['queued': [0] * 25, 'started': [0] * 25, 'ended': [0] * 25] == clusterJobService.findJobClassAndSeqTypeSpecificStatesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, SDATE_LOCALDATE).data
    }

    void test_findJobClassAndSeqTypeSpecificStatesByDateBetween_ShouldReturnMapWithDataListsContainingOccurencesOfStatesPerHour() {
        given:
        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME,
                          ended: SDATE_DATETIME,
                          jobClass: 'jobClass1',
                          seqType: seqType])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME.plusHours(1),
                          ended: SDATE_DATETIME.plusHours(2),
                          jobClass: 'jobClass1',
                          seqType: seqType])

        Map statesMap = clusterJobService.findJobClassAndSeqTypeSpecificStatesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, SDATE_LOCALDATE)

        expect:
        [2] + [0] * 24 ==  statesMap.data.queued
        [1, 1] + [0] * 23 == statesMap.data.started
        [1, 0, 1] + [0] * 22 == statesMap.data.ended
    }

    void test_findJobClassAndSeqTypeSpecificWalltimesByDateBetween_WhenNoJobsFound_ShouldReturnMapInInitialState() {
        given:
        Map walltimeMap = clusterJobService.findJobClassAndSeqTypeSpecificWalltimesByDateBetween('jobClass', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)

        expect:
        [] == walltimeMap.data
        0 == walltimeMap.xMax
    }

    void test_findJobClassAndSeqTypeSpecificWalltimesByDateBetween_WhenJobIstOutOfTimeSpan_ShouldReturnMapInInitialState() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1),
                          started: SDATE_DATETIME.minusDays(1).plusHours(12),
                          ended: SDATE_DATETIME.minusDays(1),
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          requestedWalltime: new org.joda.time.Duration(12 * HOURS_TO_MILLIS),
                          exitStatus: ClusterJob.Status.COMPLETED])

        Map walltimeMap = clusterJobService.findJobClassAndSeqTypeSpecificWalltimesByDateBetween('jobClass', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)

        expect:
        [] == walltimeMap.data
        0 == walltimeMap.xMax
    }

    void test_findJobClassAndSeqTypeSpecificWalltimesByDateBetween_WhenSeveralJobsAreFound_ShouldReturnMapContainingMaximumWalltimeAndWalltimesPerHour() {
        given:
        ClusterJob job1 = createClusterJob([queued: SDATE_DATETIME,
                                            started: SDATE_DATETIME.plusHours(12),
                                            ended: EDATE_DATETIME,
                                            jobClass: 'jobClass1',
                                            seqType: seqType,
                                            exitStatus: ClusterJob.Status.COMPLETED,
                                            xten: false,
                                            nReads: 100 * 1000 * 1000])

        ClusterJob job2 = createClusterJob([queued: SDATE_DATETIME,
                                            started: SDATE_DATETIME.plusHours(18),
                                            ended: EDATE_DATETIME,
                                            jobClass: 'jobClass1',
                                            seqType: seqType,
                                            exitStatus: ClusterJob.Status.COMPLETED,
                                            xten: true,
                                            nReads: 100 * 1000 * 1000])

        Map walltimeMap = clusterJobService.findJobClassAndSeqTypeSpecificWalltimesByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)

        expect:
        [[100, 12 * HOURS_TO_MILLIS /1000 /60, 'black', job1.id], [100, 6 * HOURS_TO_MILLIS /1000 /60, 'blue', job2.id]] == walltimeMap.data
        100 == walltimeMap.xMax
    }

    void test_findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween_WhenNoJobsFound_ShouldReturnNullValue() {
        expect:
        0 == clusterJobService.findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnNullValue() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1),
                          started: SDATE_DATETIME.minusDays(1),
                          ended: SDATE_DATETIME.minusDays(1),
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          cpuTime: new org.joda.time.Duration(24 * HOURS_TO_MILLIS),
                          exitStatus: ClusterJob.Status.COMPLETED])

        expect:
        0 == clusterJobService.findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween_WhenSeveralJobsAreFound_ShouldReturnAverageCoreUsage() {
        given:
        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME.plusDays(1),
                          ended: EDATE_DATETIME.plusDays(1),
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          cpuTime: new org.joda.time.Duration(24 * HOURS_TO_MILLIS),
                          exitStatus: ClusterJob.Status.COMPLETED])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME.plusDays(1),
                          ended: EDATE_DATETIME.plusDays(1),
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          cpuTime: new org.joda.time.Duration(12 * HOURS_TO_MILLIS),
                          exitStatus: ClusterJob.Status.COMPLETED])

        expect:
        0.75 == clusterJobService.findJobClassAndSeqTypeSpecificAvgCoreUsageByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE.plusDays(1))
    }

    void test_findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween_WhenNoJobsFound_ShouldReturnNullValue() {
        expect:
        0 == clusterJobService.findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween_WhenNoJobIsOutOfTimeSpan_ShouldReturnNullValue() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1),
                          started: SDATE_DATETIME.minusDays(1),
                          ended: SDATE_DATETIME.minusDays(1),
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          usedMemory: 900L,
                          exitStatus: ClusterJob.Status.COMPLETED])

        expect:
        0 == clusterJobService.findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween_WhenSeveralJobsAreFound_ShouldReturnAverageMemoryUsage() {
        given:
        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME.plusDays(1),
                          ended: EDATE_DATETIME.plusDays(1),
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          usedMemory: 900L,
                          exitStatus: ClusterJob.Status.COMPLETED])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME.plusDays(1),
                          ended: EDATE_DATETIME.plusDays(1),
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          usedMemory: 100L,
                          exitStatus: ClusterJob.Status.COMPLETED])

        expect:
        500 == clusterJobService.findJobClassAndSeqTypeSpecificAvgMemoryByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE.plusDays(1))
    }

    void test_findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween_WhenNoJobsFound_ShouldReturnNullValues() {
        expect:
        ['avgQueue': 0, 'avgProcess': 0] == clusterJobService.findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween_WhenJobIsOutOfTimeSpan_ShouldReturnNullValues() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), started: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1), jobClass: 'jobClass1', seqType: seqType, exitStatus: ClusterJob.Status.COMPLETED])

        expect:
        ['avgQueue': 0, 'avgProcess': 0] == clusterJobService.findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween_WhenSeveralJobsAreFound_ShouldReturnAverageStatesTimeDistribution() {
        given:
        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME.plusHours(12),
                          ended: EDATE_DATETIME,
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          exitStatus: ClusterJob.Status.COMPLETED,
                          nBases: 1])

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME.plusHours(18),
                          ended: EDATE_DATETIME,
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          exitStatus: ClusterJob.Status.COMPLETED,
                          nBases: 1])

        expect:
        ['avgQueue': 15 * HOURS_TO_MILLIS, 'avgProcess': 9 * HOURS_TO_MILLIS] == clusterJobService.findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE)
    }

    void test_findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween_WhenBasesToBeNormalized_ShouldReturnAverageStatesTimeDistributionNormalizedToBases() {
        given:
        Long bases = 10
        Long basesToBeNormalized = 5

        createClusterJob([queued: SDATE_DATETIME,
                          started: SDATE_DATETIME.plusHours(12),
                          ended: EDATE_DATETIME,
                          jobClass: 'jobClass1',
                          seqType: seqType,
                          exitStatus: ClusterJob.Status.COMPLETED,
                          nBases: bases])

        expect:
        ['avgQueue': 12 * HOURS_TO_MILLIS, 'avgProcess': 12 * HOURS_TO_MILLIS / bases * basesToBeNormalized] == clusterJobService.findJobClassAndSeqTypeSpecificAvgStatesTimeDistributionByDateBetween('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE, basesToBeNormalized)
    }

    void test_findJobClassAndSeqTypeSpecificCoverages_WhenNoJobsFound_ShouldReturnNullValues() {
        expect:
        ["minCov": null, "maxCov": null, "avgCov": null, "medianCov": null] == clusterJobService.findJobClassAndSeqTypeSpecificCoverages('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE, 1)
    }

    void test_findJobClassAndSeqTypeSpecificCoverages_WhenJobIsOutOfTimeSpan_ShouldReturnNullValues() {
        given:
        createClusterJob([queued: SDATE_DATETIME.minusDays(1), started: SDATE_DATETIME.minusDays(1), ended: SDATE_DATETIME.minusDays(1), jobClass: 'jobClass1', seqType: seqType, exitStatus: ClusterJob.Status.COMPLETED])

        expect:
        ["minCov": null, "maxCov": null, "avgCov": null, "medianCov": null] == clusterJobService.findJobClassAndSeqTypeSpecificCoverages('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE, 1)
    }


    void test_findJobClassAndSeqTypeSpecificCoverages_WhenOddNumberOfJobsAreFound_ShouldReturnCoverageStatistics() {
        given:
        createClusterJobsWithBasesInList([1, 9, 10, 80, 100], seqType)

        expect:
        ["minCov": 1.00, "maxCov": 100.00, "avgCov": 40.00, "medianCov": 10.00] == clusterJobService.findJobClassAndSeqTypeSpecificCoverages('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE, 1)
    }

    void test_findJobClassAndSeqTypeSpecificCoverages_WhenEvenNumberOfJobsAreFound_ShouldReturnCoverageStatistics() {
        given:
        createClusterJobsWithBasesInList([1, 5, 9, 25], seqType)

        expect:
        ["minCov": 1.00, "maxCov": 25.00, "avgCov": 10.00, "medianCov": 7.00] == clusterJobService.findJobClassAndSeqTypeSpecificCoverages('jobClass1', seqType, SDATE_LOCALDATE, EDATE_LOCALDATE, 1)
    }

    void test_findJobSpecificStatesTimeDistributionByJobId_WhenNoJobIsFound_ShouldReturnNull() {
        expect:
        null == clusterJobService.findJobSpecificStatesTimeDistributionByJobId(0)
    }

    void test_findJobSpecificStatesTimeDistributionByJobId_WhenJobIsFound_ReturnStatesTimeDistribution() {
        given:
        ClusterJob job1 = createClusterJob([queued: SDATE_DATETIME,
                                            started: SDATE_DATETIME.plusHours(18),
                                            ended: EDATE_DATETIME])

        expect:
        ['queue': [75, 18 * HOURS_TO_MILLIS], 'process': [25, 6 * HOURS_TO_MILLIS]] == clusterJobService.findJobSpecificStatesTimeDistributionByJobId(job1.id)
    }

    void test_getLatestJobDate_WhenNoJobsFound_ShouldReturnNull() {
        expect:
        null == clusterJobService.getLatestJobDate()
    }

    void test_getLatestJobDate_WhenSeveralJobsAreFound_ShouldReturnLatestJobDate() {
        given:
        createClusterJob([queued: SDATE_DATETIME])
        createClusterJob([queued: SDATE_DATETIME.plusDays(1)])
        createClusterJob([queued: SDATE_DATETIME.plusDays(3)])

        expect:
        SDATE_DATETIME.plusDays(3).toLocalDate() == clusterJobService.getLatestJobDate()
    }


    private static ClusterJob createClusterJob(Map myProps = [:]) {
        Map props = [
                jobClass: 'testClass',
                xten: false,
        ] + myProps

        Realm realm = DomainFactory.createRealm()
        assert realm.save([flush: true, failOnError: true])

        ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep(props.jobClass)
        assert processingStep

        ClusterJob job = DomainFactory.createClusterJob(realm: realm, userName: "unixUser", processingStep: processingStep, seqType: props.seqType)

        job.clusterJobName = "test_" + processingStep.getNonQualifiedJobClass()

        props.remove(['jobClass', 'seqType'])

        props.each { key, value ->
            job."${key}" = value
        }

        assert job.save([flush: true, failOnError: true])

        return job
    }

    private static List createClusterJobWithRun(Run run = null, Map clusterJobProps = [:]) {
        ClusterJob job = createClusterJob(clusterJobProps)

        if (!run) {
            run = DomainFactory.createRun().save([flush: true, failOnError: true])
        }

        DomainFactory.createProcessParameter(job.processingStep.process, 'de.dkfz.tbi.otp.ngsdata.Run', run.id.toString())

        return [job, run]
    }

    private static List createClusterJobWithProcessingStepAndRun( ProcessingStep step = null, Run run = null, Map myProps = [:]) {
        def (j, r) = createClusterJobWithRun(run, myProps)
        j.processingStep = step
        assert j.save(flush:true)
        return [j, r]
    }

    private static List setupClusterJobsOfSameProcessingStepAndRun() {
        def (job, run) = createClusterJobWithRun()
        createClusterJobWithProcessingStepAndRun(job.processingStep, run)
        createClusterJobWithProcessingStepAndRun(job.processingStep, run)
        return [job, run]
    }

    private static List<ClusterJob> createClusterJobsWithBasesInList(List<Long> basesInList, SeqType seqType) {
        return basesInList.collect {
            createClusterJob([queued: SDATE_DATETIME,
                              started: SDATE_DATETIME.plusHours(12),
                              ended: EDATE_DATETIME,
                              jobClass: 'jobClass1',
                              seqType: seqType,
                              exitStatus: ClusterJob.Status.COMPLETED,
                              nBases: it])
        }
    }
}
