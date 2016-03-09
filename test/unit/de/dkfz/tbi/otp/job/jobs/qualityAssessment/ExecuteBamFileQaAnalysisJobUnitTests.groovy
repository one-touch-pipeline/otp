package de.dkfz.tbi.otp.job.jobs.qualityAssessment


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
@Mock([QualityAssessmentPass, ProcessedBamFile, Realm, Project,
    SeqType, ReferenceGenome, ExomeSeqTrack, SeqTrack, AlignmentPass,
    LibraryPreparationKit, BedFile])
@Build([
    ProcessedBamFile,
    ReferenceGenome,
    ])
class ExecuteBamFileQaAnalysisJobUnitTests {

    ExecuteBamFileQaAnalysisJob job
    SeqType seqType

    LibraryPreparationKit libraryPreparationKit

    ReferenceGenome referenceGenome

    @Before
    void setUp() {

        Realm realm = new Realm()
        ProcessedBamFile processedBamFile = ProcessedBamFile.build()
        seqType = processedBamFile.seqType
        referenceGenome = processedBamFile.referenceGenome

        libraryPreparationKit = new LibraryPreparationKit(name: "LibraryPreparationKit")
        assertNotNull(libraryPreparationKit.save([flush: true, validate: false]))

        QualityAssessmentPass pass = new QualityAssessmentPass(processedBamFile: processedBamFile)
        assertNotNull(pass.save([flush: true, validate: false]))

        def processedBamFileQaFileService = [
            qualityAssessmentDataFilePath: { 'qualityAssessmentDataFilePath' },
            coverageDataFilePath: { 'coverageDataFilePath' },
            insertSizeDataFilePath: { 'insertSizeDataFilePath' }
            ] as ProcessedBamFileQaFileService

        def qualityAssessmentPassService = [
            realmForDataProcessing: { realm }
            ] as QualityAssessmentPassService

        def processedBamFileService = [
            getFilePath: { 'processedBamFileFilePath' },
            baiFilePath: { 'baiFilePath' },
            project: {},
            seqType: { seqType },
            libraryPreparationKit: {libraryPreparationKit}
            ] as ProcessedBamFileService

        def pbsService = [
            executeJob: { realmIn, cmd -> 'pbsID' }
            ] as PbsService

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

        job = new ExecuteBamFileQaAnalysisJob(
                processedBamFileQaFileService: processedBamFileQaFileService,
                qualityAssessmentPassService: qualityAssessmentPassService,
                processedBamFileService: processedBamFileService,
                processingOptionService: processingOptionService,
                referenceGenomeService: referenceGenomeService,
                bedFileService: bedFileService,
                pbsService: pbsService,
                log: log
                )

        ExecuteBamFileQaAnalysisJob.metaClass.getProcessParameterValue = { -> 1 as long }
        ExecuteBamFileQaAnalysisJob.metaClass.addOutputParameter = { String a1, String a2 -> }
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

        job.pbsService = [executeJob: { realm, cmd ->
                String expCommand = "qualityAssessment.sh processedBamFileFilePath baiFilePath qualityAssessmentDataFilePath coverageDataFilePath insertSizeDataFilePath false ${Chromosomes.overallChromosomesLabel()} 36 25 0 1 1000 10 false bedFilePath referenceGenomeMetaInformationPath; chmod 440 qualityAssessmentDataFilePath coverageDataFilePath insertSizeDataFilePath"
                assertEquals(expCommand, cmd)
                return 'pbsID'
            }] as PbsService

        job.execute()
    }

    @Test
    void testExecuteCorrectExomeWithoutBedFile() {

        seqType.name = SeqTypeNames.EXOME.seqTypeName
        seqType.libraryLayout = 'PAIRED'

        job.pbsService = [executeJob: { realm, cmd ->
            assert false //this method should not be executed
        }] as PbsService

        assert "Could not find a bed file for ${referenceGenome} and LibraryPreparationKit" == shouldFail(ProcessingException) {
            job.execute()
        }
    }

    @Test
    void testExecuteCorrectWholeGenome() {

        seqType.name = SeqTypeNames.WHOLE_GENOME.seqTypeName
        seqType.libraryLayout = 'PAIRED'

        job.pbsService = [executeJob: { realm, cmd ->
                String expCommand = "qualityAssessment.sh processedBamFileFilePath baiFilePath qualityAssessmentDataFilePath coverageDataFilePath insertSizeDataFilePath false ${Chromosomes.overallChromosomesLabel()} 36 25 0 1 1000 10 false; chmod 440 qualityAssessmentDataFilePath coverageDataFilePath insertSizeDataFilePath"
                assertEquals(expCommand, cmd)
                return 'pbsID'
            }] as PbsService

        job.execute()
    }

    @Test(expected = IllegalArgumentException)
    void testExecuteNotSupportedSeqType() {

        seqType.name = SeqTypeNames.WHOLE_GENOME.seqTypeName
        seqType.libraryLayout = 'SINGLE'
        job.execute()
    }
}
