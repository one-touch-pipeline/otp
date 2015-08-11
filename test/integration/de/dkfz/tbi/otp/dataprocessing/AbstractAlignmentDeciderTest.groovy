package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.MailHelperService
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

public class AbstractAlignmentDeciderTest {

    @Autowired
    ApplicationContext applicationContext

    AbstractAlignmentDecider decider

    final shouldFail = new GroovyTestCase().&shouldFail

    @Before
    void setUp() {
        decider = newDecider()
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(MailHelperService, decider.mailHelperService)
    }

    private AbstractAlignmentDecider newDecider(Map methods = [:]) {
        AbstractAlignmentDecider decider = ([
                prepareForAlignment: { MergingWorkPackage workPackage, SeqTrack seqTrack, boolean forceRealign -> },
                getWorkflow: { return Workflow.findOrSaveByNameAndType(Workflow.Name.DEFAULT_OTP, Workflow.Type.ALIGNMENT) },
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
    void testDecideAndPrepareForAlignment_whenCanWorkflowAlignReturnsFalse_shouldReturnEmptyList() {
        SeqTrack st = buildSeqTrack()
        st.seqType = SeqType.build(name: "Invalid")
        st.save(failOnError: true)

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(st, true)
        assert workPackages.empty
    }

    @Test
    void testDecideAndPrepareForAlignment_whenWrongReferenceGenome_shouldThrowAssertionError() {
        SeqTrack seqTrack = buildSeqTrack()

        MergingWorkPackage workPackage = new MergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                referenceGenome: ReferenceGenome.build(),
                workflow: Workflow.findOrSaveByNameAndType(Workflow.Name.DEFAULT_OTP, Workflow.Type.ALIGNMENT),
        )
        workPackage.save(failOnError: true)

        shouldFail(AssertionError.class, {
            decider.decideAndPrepareForAlignment(seqTrack, true)
        })
    }

    @Test
    void testDecideAndPrepareForAlignment_whenWrongWorkflow_shouldThrowAssertionError() {
        SeqTrack seqTrack = buildSeqTrack()

        MergingWorkPackage workPackage = new MergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                referenceGenome: exactlyOneElement(ReferenceGenome.list()),
                workflow: Workflow.findOrSaveByNameAndType(Workflow.Name.PANCAN_ALIGNMENT, Workflow.Type.ALIGNMENT),
                statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
        )
        workPackage.save(failOnError: true)

        shouldFail(AssertionError.class, {
            decider.decideAndPrepareForAlignment(seqTrack, true)
        })
    }


    @Test
    void testDecideAndPrepareForAlignment_whenDifferentSeqPlatformGroup_shouldReturnEmptyList() {
        SeqTrack seqTrack = buildSeqTrack()
        boolean emailIsSent = false

        MergingWorkPackage workPackage = new MergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: SeqPlatformGroup.build(),
                referenceGenome: exactlyOneElement(ReferenceGenome.list()),
                workflow: Workflow.findOrSaveByNameAndType(Workflow.Name.DEFAULT_OTP, Workflow.Type.ALIGNMENT),
        )
        workPackage.save(failOnError: true)

        decider.mailHelperService.metaClass.sendEmail = {String subject, String content, String recipient ->
            assert subject.contains(seqTrack.sample.toString())
            assert content.contains(seqTrack.seqPlatformGroup.name)
            assert content.contains(workPackage.seqPlatformGroup.name)
            emailIsSent = true
        }

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        assert workPackages.empty
        assert emailIsSent
    }

    @Test
    void testDecideAndPrepareForAlignment_whenDifferentLibraryPreperationKit_shouldReturnEmptyList() {
        SeqTrack seqTrack = buildSeqTrack()
        seqTrack.libraryPreparationKit = LibraryPreparationKit.build()
        seqTrack.kitInfoReliability = InformationReliability.KNOWN
        seqTrack.save(flush: true, failOnError: true)
        boolean emailIsSent = false

        MergingWorkPackage workPackage = new MergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                referenceGenome: exactlyOneElement(ReferenceGenome.list()),
                libraryPreparationKit: LibraryPreparationKit.build(),
                workflow: Workflow.findOrSaveByNameAndType(Workflow.Name.DEFAULT_OTP, Workflow.Type.ALIGNMENT),
        )
        workPackage.save(failOnError: true)

        decider.mailHelperService.metaClass.sendEmail = {String subject, String content, String recipient ->
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
                workflow: Workflow.findOrSaveByNameAndType(Workflow.Name.DEFAULT_OTP, Workflow.Type.ALIGNMENT),
        )
        workPackage.save(failOnError: true)

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        assertSeqTrackProperties(exactlyOneElement(workPackages), seqTrack)
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

        exactlyOneElement(ReferenceGenomeProjectSeqType.list()).delete()

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
    void testCanWorkflowAlign_whenEverythingIsOkay_shouldReturnTrue() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save(failOnError: true)

        assert decider.canWorkflowAlign(seqTrack)
    }

    @Test
    void testCanWorkflowAlign_whenWrongSeqType_shouldReturnFalse() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqType seqType = SeqType.build(
                name: SeqTypeNames.WHOLE_GENOME,
                libraryLayout: SeqType.LIBRARYLAYOUT_MATE_PAIRED
        )
        seqType.save(failOnError: true)

        SeqTrack seqTrack = testData.createSeqTrack(seqType: seqType)
        seqTrack.save(failOnError: true)

        assert !decider.canWorkflowAlign(seqTrack)
    }


    private SeqTrack buildSeqTrack() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save(failOnError: true)

        DataFile dataFile = testData.createDataFile(seqTrack: seqTrack)
        dataFile.save(failOnError: true)

        return seqTrack
    }

    private void assertSeqTrackProperties(MergingWorkPackage workPackage, SeqTrack seqTrack) {
        assert workPackage.satisfiesCriteria(seqTrack)
    }
}
