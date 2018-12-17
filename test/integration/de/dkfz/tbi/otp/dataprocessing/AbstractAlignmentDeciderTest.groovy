package de.dkfz.tbi.otp.dataprocessing

import org.junit.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.TrackingService
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.MailHelperService

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class AbstractAlignmentDeciderTest {

    @Autowired
    ApplicationContext applicationContext

    AbstractAlignmentDecider decider

    final shouldFail = new GroovyTestCase().&shouldFail

    @Before
    void setUp() {
        decider = newDecider()
        decider.trackingService = new TrackingService()
        decider.processingOptionService = new ProcessingOptionService()
        DomainFactory.createProcessingOptionForNotificationRecipient()
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(MailHelperService, decider.mailHelperService)
    }

    private AbstractAlignmentDecider newDecider(Map methods = [:]) {
        AbstractAlignmentDecider decider = ([
                prepareForAlignment: { MergingWorkPackage workPackage, SeqTrack seqTrack, boolean forceRealign -> },
                getPipeline        : {
                    return Pipeline.findOrSaveByNameAndType(Pipeline.Name.PANCAN_ALIGNMENT, Pipeline.Type.ALIGNMENT)
                },
        ] + methods) as AbstractAlignmentDecider
        decider.applicationContext = applicationContext
        decider.mailHelperService = applicationContext.mailHelperService
        return decider
    }

    @Test
    void testDecideAndPrepareForAlignment_whenEverythingIsOkay_shouldReturnWorkPackages() {
        SeqTrack seqTrack = buildSeqTrack()

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        assertSeqTrackProperties(exactlyOneElement(workPackages), seqTrack)
        assert exactlyOneElement(workPackages).seqTracks == [seqTrack] as Set<SeqTrack>
    }

    @Test
    void testFindOrSaveWorkPackagesTwice_whenEverythingIsOkay_workPackageShouldContainBothSeqTracks() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        SeqTrack seqTrack2 = DomainFactory.createSeqTrack(sample: seqTrack.sample, seqType: seqTrack.seqType, run: DomainFactory.createRun(seqPlatform: seqTrack.seqPlatform))
        DomainFactory.createMergingCriteriaLazy(project: seqTrack2.project, seqType: seqTrack2.seqType)
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
        DomainFactory.createReferenceGenomeProjectSeqType(project: seqTrack.project, referenceGenome: referenceGenome, seqType: seqTrack.seqType, statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME)

        Collection<MergingWorkPackage> workPackages = decider.findOrSaveWorkPackages(seqTrack, seqTrack.configuredReferenceGenomeProjectSeqType, decider.getPipeline(seqTrack))
        decider.findOrSaveWorkPackages(seqTrack2, seqTrack2.configuredReferenceGenomeProjectSeqType, decider.getPipeline(seqTrack2))

        assert exactlyOneElement(workPackages).seqTracks == [seqTrack, seqTrack2] as Set<SeqTrack>
    }

    @Test
    void testDecideAndPrepareForAlignment_noDataFile_shouldReturnEmptyList() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save(failOnError: true)

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)
        assert workPackages.empty
    }

    @Test
    void testDecideAndPrepareForAlignment_whenCanPipelineAlignReturnsFalse_shouldReturnEmptyList() {
        SeqTrack st = buildSeqTrack()
        st.seqType = SeqType.build(name: "Invalid")
        st.save(failOnError: true)

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(st, true)
        assert workPackages.empty
    }

    @Test
    void testDecideAndPrepareForAlignment_whenWrongReferenceGenome_shouldThrowAssertionError() {
        SeqTrack seqTrack = buildSeqTrack()

        DomainFactory.createMergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                statSizeFileName: seqTrack.configuredReferenceGenomeProjectSeqType.statSizeFileName,
        )

        shouldFail(AssertionError.class, {
            decider.decideAndPrepareForAlignment(seqTrack, true)
        })
    }

    @Test
    void testDecideAndPrepareForAlignment_whenWrongPipeline_shouldThrowAssertionError() {
        SeqTrack seqTrack = buildSeqTrack()

        DomainFactory.createMergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                referenceGenome: exactlyOneElement(ReferenceGenome.list()),
                pipeline: Pipeline.findOrSaveByNameAndType(Pipeline.Name.RODDY_RNA_ALIGNMENT, Pipeline.Type.ALIGNMENT),
        )

        shouldFail(AssertionError.class, {
            decider.decideAndPrepareForAlignment(seqTrack, true)
        })
    }


    @Test
    void testDecideAndPrepareForAlignment_whenDifferentSeqPlatformGroup_shouldReturnEmptyListAndSendMail() {
        String prefix = "PRFX"
        DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)

        OtrsTicket ticket = DomainFactory.createOtrsTicket()
        RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: ticket)
        SeqTrack seqTrack = buildSeqTrack()
        seqTrack.dataFiles*.runSegment = runSegment
        seqTrack.dataFiles*.save(flush: true)

        boolean emailIsSent = false

        DomainFactory.createMergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                referenceGenome: exactlyOneElement(ReferenceGenome.list()),
                statSizeFileName: seqTrack.configuredReferenceGenomeProjectSeqType.statSizeFileName,
        )

        decider.mailHelperService.metaClass.sendEmail = { String subject, String content, String recipient ->
            assert content.contains(prefix)
            assert content.contains(ticket.ticketNumber)
            assert subject.contains(seqTrack.sample.toString())
            emailIsSent = true
        }

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        assert workPackages.empty
        assert emailIsSent
    }

    private List<Entity> prepareDifferentLibraryPreparationKit() {
        SeqTrack seqTrack = buildSeqTrack()
        seqTrack.libraryPreparationKit = LibraryPreparationKit.build()
        seqTrack.kitInfoReliability = InformationReliability.KNOWN
        seqTrack.save(flush: true, failOnError: true)

        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                referenceGenome: exactlyOneElement(ReferenceGenome.list()),
                statSizeFileName: seqTrack.configuredReferenceGenomeProjectSeqType.statSizeFileName,
        )
        workPackage.save(failOnError: true)

        return [seqTrack, workPackage]
    }

    @Test
    void testDecideAndPrepareForAlignment_whenDifferentLibraryPreparationKit_shouldReturnEmptyListAndSendMailWithTicket() {
        SeqTrack seqTrack; MergingWorkPackage workPackage
        (seqTrack, workPackage) = prepareDifferentLibraryPreparationKit()

        String prefix = "PRFX"
        DomainFactory.createProcessingOptionForOtrsTicketPrefix(prefix)
        OtrsTicket ticket = DomainFactory.createOtrsTicket()
        RunSegment runSegment = DomainFactory.createRunSegment(otrsTicket: ticket)
        seqTrack.dataFiles*.runSegment = runSegment
        seqTrack.dataFiles*.save(flush: true)

        boolean emailIsSent = false

        decider.mailHelperService.metaClass.sendEmail = { String subject, String content, String recipient ->
            assert content.contains(prefix)
            assert content.contains(ticket.ticketNumber)
            assert subject.contains(seqTrack.sample.toString())
            assert content.contains(seqTrack.libraryPreparationKit.name)
            assert content.contains(workPackage.libraryPreparationKit.name)
            emailIsSent = true
        }

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        assert workPackages.empty
        assert emailIsSent
    }

    @Test
    void testDecideAndPrepareForAlignment_whenDifferentLibraryPreparationKit_shouldReturnEmptyListAndSendMailWithoutTicket() {
        SeqTrack seqTrack; MergingWorkPackage workPackage
        (seqTrack, workPackage) = prepareDifferentLibraryPreparationKit()

        boolean emailIsSent = false

        decider.mailHelperService.metaClass.sendEmail = { String subject, String content, String recipient ->
            assert subject.contains(seqTrack.sample.toString())
            assert content.contains(seqTrack.libraryPreparationKit.name)
            assert content.contains(workPackage.libraryPreparationKit.name)
            emailIsSent = true
        }

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        assert workPackages.empty
        assert emailIsSent
    }

    @Test
    void testDecideAndPrepareForAlignment_whenMergingWorkPackageExists_shouldReturnIt() {
        SeqTrack seqTrack = buildSeqTrack()

        MergingWorkPackage workPackage = new MergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                referenceGenome: exactlyOneElement(ReferenceGenome.list()),
                pipeline: Pipeline.findOrSaveByNameAndType(Pipeline.Name.PANCAN_ALIGNMENT, Pipeline.Type.ALIGNMENT),
                statSizeFileName: seqTrack.configuredReferenceGenomeProjectSeqType.statSizeFileName,
        )
        workPackage.save(failOnError: true)

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        assertSeqTrackProperties(exactlyOneElement(workPackages), seqTrack)
        assert exactlyOneElement(workPackages).seqTracks.contains(seqTrack)
        assert exactlyOneElement(workPackages).seqTracks.size() == 1
    }

    @Test
    void testDecideAndPrepareForAlignment_callsEnsureConfigurationIsComplete() {
        SeqTrack st = buildSeqTrack()
        int callCount = 0
        decider = newDecider(
                ensureConfigurationIsComplete: { SeqTrack seqTrack ->
                    assert seqTrack == st
                    callCount++
                }
        )

        decider.decideAndPrepareForAlignment(st, true)

        assert callCount == 1
    }

    @Test
    void testDecideAndPrepareForAlignment_callsPrepareForAlignment() {
        SeqTrack st = buildSeqTrack()
        Collection<MergingWorkPackage> calledForMergingWorkPackages = []
        decider = newDecider(
                prepareForAlignment: { MergingWorkPackage workPackage, SeqTrack seqTrack, boolean forceRealign ->
                    assert seqTrack == st
                    assert forceRealign == true
                    calledForMergingWorkPackages.add(workPackage)
                }
        )

        assert TestCase.containSame(calledForMergingWorkPackages, decider.decideAndPrepareForAlignment(st, true))
        assert calledForMergingWorkPackages.size() > 0
    }

    @Test
    void testEnsureConfigurationIsComplete_whenReferenceGenomeNull_shouldThrowRuntimeException() {
        SeqTrack seqTrack = buildSeqTrack()

        exactlyOneElement(ReferenceGenomeProjectSeqType.list()).delete(flush: true)

        shouldFail(RuntimeException.class, {
            decider.ensureConfigurationIsComplete(seqTrack)
        })
    }

    @Test
    void testEnsureConfigurationIsComplete_whenLibraryPreparationKitIsMissing_shouldThrowRuntimeException() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createExomeSeqTrack(testData.run)
        seqTrack.libraryPreparationKit = null
        seqTrack.kitInfoReliability = InformationReliability.UNKNOWN_UNVERIFIED
        seqTrack.save(failOnError: true)

        DataFile dataFile = testData.createDataFile(seqTrack: seqTrack)
        dataFile.save(failOnError: true)

        shouldFail(RuntimeException.class, {
            decider.ensureConfigurationIsComplete(seqTrack)
        })
    }


    @Test
    void testCanPipelineAlign_whenEverythingIsOkay_shouldReturnTrue() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save(failOnError: true)

        assert decider.canPipelineAlign(seqTrack)
    }

    @Test
    void testCanPipelineAlign_whenWrongSeqType_shouldReturnFalse() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqType seqType = DomainFactory.createWholeGenomeSeqType(LibraryLayout.MATE_PAIR)

        SeqTrack seqTrack = testData.createSeqTrack(seqType: seqType)
        seqTrack.save(failOnError: true)

        assert !decider.canPipelineAlign(seqTrack)
    }


    private SeqTrack buildSeqTrack() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save(failOnError: true)
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        DataFile dataFile = testData.createDataFile(seqTrack: seqTrack)
        dataFile.save(failOnError: true)

        return seqTrack
    }

    private void assertSeqTrackProperties(MergingWorkPackage workPackage, SeqTrack seqTrack) {
        assert workPackage.satisfiesCriteria(seqTrack)
    }
}
