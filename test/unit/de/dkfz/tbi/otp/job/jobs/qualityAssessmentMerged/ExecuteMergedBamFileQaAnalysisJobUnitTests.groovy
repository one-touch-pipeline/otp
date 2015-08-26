package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import grails.test.mixin.support.*

import org.apache.commons.logging.Log

import static org.junit.Assert.*

import org.junit.*

@TestFor(ReferenceGenome)
@Mock([QualityAssessmentMergedPass, Realm,
    ExomeSeqTrack,
    LibraryPreparationKit, BedFile])
@Build([
    AlignmentPass,
    MergingPass,
    MergingSet,
    ProcessedBamFile,
])
class ExecuteMergedBamFileQaAnalysisJobUnitTests {

    ExecuteMergedBamFileQaAnalysisJob job
    SeqType seqType

    LibraryPreparationKit libraryPreparationKit

    ReferenceGenome referenceGenome

    @Before
    void setUp() {

        Realm realm = new Realm()
        ProcessedMergedBamFile bamFile = DomainFactory.createProcessedMergedBamFile()
        seqType = bamFile.seqType
        referenceGenome = bamFile.referenceGenome

        libraryPreparationKit = new LibraryPreparationKit(name: "LibraryPreparationKit")
        assertNotNull(libraryPreparationKit.save([flush: true, validate: false]))

        QualityAssessmentMergedPass pass = new QualityAssessmentMergedPass(abstractMergedBamFile: bamFile)
        assertNotNull(pass.save([flush: true, validate: false]))
        seqType = new SeqType()

        def processedMergedBamFileQaFileService = [
            qualityAssessmentDataFilePath: { 'qualityAssessmentDataFilePath' },
            coverageDataFilePath: { 'coverageDataFilePath' },
            insertSizeDataFilePath: { 'insertSizeDataFilePath' }
            ] as ProcessedMergedBamFileQaFileService

        def qualityAssessmentMergedPassService = [
            realmForDataProcessing: { realm }
            ] as QualityAssessmentMergedPassService

        def processedMergedBamFileService = [
            filePath: { 'processedBamFileFilePath' },
            filePathForBai: { 'baiFilePath' },
            project: {},
            seqType: { seqType },
            libraryPreparationKit: {libraryPreparationKit}
            ] as ProcessedMergedBamFileService

        def executionHelperService = [
            sendScript: { realmIn, cmd -> 'pbsID' }
            ] as ExecutionHelperService

        def processingOptionService = [
            findOptionAssure: { a1, a2, a3 -> 'qualityAssessment.sh ${processedBamFilePath} ${processedBaiFilePath} ${qualityAssessmentFilePath} ${coverageDataFilePath} ${insertSizeDataFilePath} false ${allChromosomeName} 36 25 0 1 1000 10 false ${bedFilePath} ${refGenMetaInfoFilePath}'}
            ] as ProcessingOptionService

        def referenceGenomeService = [
            referenceGenome: { a1, a2 -> referenceGenome},
            referenceGenomeMetaInformationPath: { a1, a2 -> 'referenceGenomeMetaInformationPath' }
            ] as ReferenceGenomeService

        def bedFileService = [
            filePath: { a1, a2 -> 'bedFilePath' }
            ] as BedFileService

        Log log = { println it } as Log

        job = new ExecuteMergedBamFileQaAnalysisJob(
                processedMergedBamFileQaFileService: processedMergedBamFileQaFileService,
                qualityAssessmentMergedPassService: qualityAssessmentMergedPassService,
                processedMergedBamFileService: processedMergedBamFileService,
                processingOptionService: processingOptionService,
                referenceGenomeService: referenceGenomeService,
                bedFileService: bedFileService,
                executionHelperService: executionHelperService,
                log: log
                )

        ExecuteMergedBamFileQaAnalysisJob.metaClass.getProcessParameterValue = { -> 1 as long }
        ExecuteMergedBamFileQaAnalysisJob.metaClass.addOutputParameter = { String a1, String a2 -> }
    }

    @Test
    void testExecuteCorrectExomeWithBedFile() {

        seqType.name = SeqTypeNames.EXOME.seqTypeName
        seqType.libraryLayout = 'PAIRED'

        BedFile bedfile = new BedFile(
            referenceGenome: referenceGenome,
            libraryPreparationKit: libraryPreparationKit
            )
        assertNotNull(bedfile.save([flush: true, validate: false]))

        job.executionHelperService = [sendScript: { realm, cmd, jobId ->
                assert jobId
                String expCommand = "qualityAssessment.sh processedBamFileFilePath baiFilePath qualityAssessmentDataFilePath coverageDataFilePath insertSizeDataFilePath false ${Chromosomes.overallChromosomesLabel()} 36 25 0 1 1000 10 false bedFilePath referenceGenomeMetaInformationPath; chmod 440 qualityAssessmentDataFilePath coverageDataFilePath insertSizeDataFilePath"
                assertEquals(expCommand, cmd)
                return 'pbsID'
            }] as ExecutionHelperService

        job.execute()
    }

    @Test
    void testExecuteCorrectExomeWithoutBedFile() {

        seqType.name = SeqTypeNames.EXOME.seqTypeName
        seqType.libraryLayout = 'PAIRED'

        job.executionHelperService = [sendScript: { realm, cmd, jobId ->
            assert jobId
            assert false //this method should not be executed
        }] as ExecutionHelperService

        assert "Could not find a bed file for ${referenceGenome} and LibraryPreparationKit" == shouldFail(ProcessingException) {
            job.execute()
        }
    }

    @Test
    void testExecuteCorrectWholeGenome() {

        seqType.name = SeqTypeNames.WHOLE_GENOME.seqTypeName
        seqType.libraryLayout = 'PAIRED'

        job.executionHelperService = [sendScript: { realm, cmd, jobId ->
                assert jobId
                String expCommand = "qualityAssessment.sh processedBamFileFilePath baiFilePath qualityAssessmentDataFilePath coverageDataFilePath insertSizeDataFilePath false ${Chromosomes.overallChromosomesLabel()} 36 25 0 1 1000 10 false; chmod 440 qualityAssessmentDataFilePath coverageDataFilePath insertSizeDataFilePath"
                assertEquals(expCommand, cmd)
                return 'pbsID'
            }] as ExecutionHelperService

        job.execute()
    }

    @Test(expected = IllegalArgumentException)
    void testExecuteNotSupportedSeqType() {

        seqType.name = SeqTypeNames.WHOLE_GENOME.seqTypeName
        seqType.libraryLayout = 'SINGLE'
        job.execute()
    }
}
