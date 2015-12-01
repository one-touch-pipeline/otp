package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.HelperUtils
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestFor(AlignmentPassService)
@TestMixin(GrailsUnitTestMixin)
@Build([
    AlignmentPass,
    DataFile,
    FileType,
    ProcessedBamFile,
    Realm,
    ReferenceGenomeProjectSeqType,
    Run,
    RunSegment,
])
class AlignmentPassServiceUnitTests extends TestData {

    AlignmentPassService alignmentPassService
    AlignmentPass alignmentPass

    File baseDir
    File referenceGenomeDir
    File referenceGenomeFile

    @Before
    void setUp() {
        baseDir = TestCase.createEmptyTestDirectory()
        referenceGenomeDir = new File(baseDir, "reference_genomes/referenceGenome")
        referenceGenomeDir.mkdirs()
        referenceGenomeFile = new File("${referenceGenomeDir}/prefixName.fa")
        referenceGenomeFile.createNewFile()

        alignmentPassService = new AlignmentPassService()
        alignmentPassService.qualityAssessmentPassService = new QualityAssessmentPassService()
        alignmentPassService.referenceGenomeService = new ReferenceGenomeService()
        alignmentPassService.referenceGenomeService.configService = new ConfigService()
        createObjects()
        alignmentPass = createAlignmentPass(identifier: 2)
        alignmentPass.save(flush: true)

        realm.processingRootPath = baseDir.path
        assert realm.save(flush: true)
    }

    @After
    void tearDown() {
        alignmentPass = null
        referenceGenome = null
        referenceGenomeProjectSeqType = null
        alignmentPassService = null
        assert baseDir.deleteDir()
    }

    @Test
    void testReferenceGenomePath_referenceGenomeNotSet() {
        alignmentPass.workPackage.referenceGenome = null
        Project project2 = TestData.createProject()
        project2.name = "test"
        project2.dirName = HelperUtils.uniqueString
        project2.realmName = "test"
        project2.save(flush: true)
        referenceGenomeProjectSeqType.project = project2
        referenceGenomeProjectSeqType.save(flush: true)
        shouldFail(PowerAssertionError) {
            alignmentPassService.referenceGenomePath(alignmentPass)
        }
    }

    @Test
    void testReferenceGenomePathAllCorrect() {
        String pathExp = referenceGenomeFile.path
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
