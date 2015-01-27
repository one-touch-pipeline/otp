package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.*
import grails.test.mixin.support.*

import org.junit.After
import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.ngsdata.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestFor(AlignmentPassService)
@TestMixin(GrailsUnitTestMixin)
@Mock([
    AlignmentPass,
    AlignmentPassService,
    DataFile,
    FileType,
    Individual,
    ProcessedBamFile,
    Project,
    Realm,
    ReferenceGenome,
    ReferenceGenomeProjectSeqType,
    Run,
    RunSegment,
    Sample,
    SampleType,
    SeqCenter,
    SeqPlatform,
    SeqTrack,
    SeqType,
    SoftwareTool,
])

class AlignmentPassServiceUnitTests extends TestData {

    AlignmentPassService alignmentPassService
    AlignmentPass alignmentPass

    @Before
    void setUp() {
        alignmentPassService = new AlignmentPassService()
        alignmentPassService.qualityAssessmentPassService = new QualityAssessmentPassService()
        alignmentPassService.referenceGenomeService = new ReferenceGenomeService()
        alignmentPassService.referenceGenomeService.configService = new ConfigService()
        createObjects()
        alignmentPass = createAlignmentPass(identifier: 2)
        alignmentPass.save(flush: true)
    }

    @After
    void tearDown() {
        alignmentPass = null
        referenceGenome = null
        referenceGenomeProjectSeqType = null
        alignmentPassService = null
        directory.deleteOnExit()
        file.deleteOnExit()
    }

    @Test(expected = RuntimeException)
    void testReferenceGenomeNoReferenceGenomeForProjectAndSeqType() {
        Project project2 = new Project()
        project2.name = "test"
        project2.dirName = "/tmp/test"
        project2.realmName = "test"
        project2.save(flush: true)
        referenceGenomeProjectSeqType.project = project2
        referenceGenomeProjectSeqType.save(flush: true)
        alignmentPassService.referenceGenomePath(alignmentPass)
    }

    @Test
    void testReferenceGenomePathAllCorrect() {
        String pathExp = "${referenceGenomePath}prefixName.fa"
        String pathAct = alignmentPassService.referenceGenomePath(alignmentPass)
        assertEquals(pathExp, pathAct)
    }

    @Test(expected = IllegalArgumentException)
    void testAlignmentPassFinishedNull() {
        alignmentPass = null
        alignmentPassService.alignmentPassFinished(alignmentPass)
    }

    @Test
    void testSetReferenceGenomeAsConfigured_notConfigured() {
        referenceGenomeProjectSeqType.delete()
        alignmentPass.referenceGenome = null
        assert shouldFail(RuntimeException, { alignmentPassService.setReferenceGenomeAsConfigured(
                alignmentPass) }).startsWith("Reference genome is not configured for SeqTrack")
        assert alignmentPass.referenceGenome == null
    }

    @Test
    void testSetReferenceGenomeAsConfigured_notSetYet() {
        alignmentPass.referenceGenome = null
        alignmentPassService.setReferenceGenomeAsConfigured(alignmentPass)
        assert alignmentPass.referenceGenome == referenceGenome
    }

    @Test
    void testSetReferenceGenomeAsConfigured_alreadySetToSame() {
        alignmentPass.referenceGenome = referenceGenome
        alignmentPassService.setReferenceGenomeAsConfigured(alignmentPass)
        assert alignmentPass.referenceGenome == referenceGenome
    }

    @Test
    void testSetReferenceGenomeAsConfigured_alreadySetToOther() {
        final ReferenceGenome otherReferenceGenome = createReferenceGenome()
        alignmentPass.referenceGenome = otherReferenceGenome
        assert shouldFail(AssertionError, { alignmentPassService.setReferenceGenomeAsConfigured(alignmentPass) })
        assert alignmentPass.referenceGenome == otherReferenceGenome
    }

    @Test
    void testAlignmentPassFinished() {
        ProcessedBamFile bamFile = new ProcessedBamFile(
                        type: BamType.SORTED,
                        alignmentPass: alignmentPass,
                        withdrawn: false,
                        qualityAssessmentStatus: QaProcessingStatus.NOT_STARTED
                        )
        bamFile.save(flush: true)
        alignmentPassService.alignmentPassFinished(alignmentPass)
        assertEquals(SeqTrack.DataProcessingState.FINISHED, alignmentPass.seqTrack.alignmentState)
        assertEquals(AbstractBamFile.QaProcessingStatus.NOT_STARTED, bamFile.qualityAssessmentStatus)
    }
}
