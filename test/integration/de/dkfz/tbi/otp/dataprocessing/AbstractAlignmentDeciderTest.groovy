package de.dkfz.tbi.otp.dataprocessing

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.ngsdata.RunSegment
import de.dkfz.tbi.otp.ngsdata.SeqPlatformGroup
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.TestData
import org.junit.Test

public class AbstractAlignmentDeciderTest {

    @Autowired
    ApplicationContext applicationContext

    AbstractAlignmentDecider decider

    final shouldFail = new GroovyTestCase().&shouldFail

    @Before
    void before() {
        decider = newDecider()
    }

    private AbstractAlignmentDecider newDecider(Map methods = [:]) {
        AbstractAlignmentDecider decider = ([
                canWorkflowAlign: { SeqTrack seqTrack -> return true },
                prepareForAlignment: { MergingWorkPackage workPackage, SeqTrack seqTrack, boolean forceRealign -> },
                getWorkflow: { return Workflow.findOrSaveByNameAndType(Workflow.Name.DEFAULT_OTP, Workflow.Type.ALIGNMENT) },
        ] + methods) as AbstractAlignmentDecider
        decider.applicationContext = applicationContext
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
                workflow: Workflow.findOrSaveByNameAndType(Workflow.Name.RODDY, Workflow.Type.ALIGNMENT),
        )
        workPackage.save(failOnError: true)

        shouldFail(AssertionError.class, {
            decider.decideAndPrepareForAlignment(seqTrack, true)
        })
    }


    @Test
    void testDecideAndPrepareForAlignment_whenDifferentSeqPlatformGroup_shouldReturnEmptyList() {
        SeqTrack seqTrack = buildSeqTrack()

        MergingWorkPackage workPackage = new MergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: SeqPlatformGroup.build(),
                referenceGenome: exactlyOneElement(ReferenceGenome.list()),
                workflow: Workflow.findOrSaveByNameAndType(Workflow.Name.DEFAULT_OTP, Workflow.Type.ALIGNMENT),
        )
        workPackage.save(failOnError: true)

        Collection<MergingWorkPackage> workPackages = decider.decideAndPrepareForAlignment(seqTrack, true)

        assert workPackages.empty
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
    void testMayAlign_everythingIsOkay_shouldReturnTrue() {
        SeqTrack seqTrack = buildSeqTrack()

        assert decider.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenDataFileWithdrawn_shouldReturnFalse() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save(failOnError: true)

        DataFile dataFile = testData.createDataFile(seqTrack: seqTrack)
        dataFile.fileWithdrawn = true
        dataFile.save(failOnError: true)

        assert !decider.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenNoDataFile_shouldReturnFalse() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save(failOnError: true)

        assert !decider.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenWrongFileType_shouldReturnFalse() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save(failOnError: true)

        DataFile dataFile = testData.createDataFile(seqTrack: seqTrack)
        dataFile.fileType = testData.createFileType(FileType.Type.SOURCE)
        dataFile.save(failOnError: true)

        assert !decider.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenRunSegmentMustNotAlign_shouldReturnFalse() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save(failOnError: true)

        RunSegment runSegment = testData.createRunSegment(align: false)
        runSegment.save(failOnError: true)
        DataFile dataFile = testData.createDataFile(seqTrack: seqTrack)
        dataFile.runSegment = runSegment
        dataFile.save(failOnError: true)

        assert !decider.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenExomeKitReliabilityIsUnknownVerified_shouldReturnFalse() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createExomeSeqTrack(testData.run)
        seqTrack.libraryPreparationKit = null
        seqTrack.kitInfoReliability = InformationReliability.UNKNOWN_VERIFIED
        seqTrack.save(failOnError: true)

        DataFile dataFile = testData.createDataFile(seqTrack: seqTrack)
        dataFile.save(failOnError: true)

        assert !decider.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_whenSeqPlatformGroupIsNull_shouldReturnFalse() {
        SeqTrack seqTrack = buildSeqTrack()

        seqTrack.seqPlatform.seqPlatformGroup = null
        seqTrack.seqPlatform.save(failOnError: true)

        assert !decider.mayAlign(seqTrack)
    }

    @Test
    void testMayAlign_returnsFalseIfCanWorkflowAlignDoes() {
        SeqTrack st = buildSeqTrack()
        decider = newDecider(canWorkflowAlign: { SeqTrack seqTrack -> false })

        assert !decider.mayAlign(st)
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
