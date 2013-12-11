package de.dkfz.tbi.otp.dataprocessing

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
    FileType,
    Individual,
    ProcessedBamFile,
    Project,
    Realm,
    ReferenceGenome,
    ReferenceGenomeProjectSeqType,
    Run,
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
        alignmentPass = new AlignmentPass()
        alignmentPass.identifier = 2
        alignmentPass.seqTrack = seqTrack
        alignmentPass.description = "test"
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
