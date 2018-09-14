package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.buildtestdata.mixin.*
import grails.test.mixin.*
import org.apache.commons.logging.*
import org.junit.*

import static org.junit.Assert.*

@TestFor(ReferenceGenome)
@Mock([
        BedFile,
        ExomeSeqTrack,
        LibraryPreparationKit,
        MergingCriteria,
        ProcessingOption,
        QualityAssessmentMergedPass,
        Realm,
])
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

        def clusterJobSchedulerService = [
            executeJob: { realmIn, cmd -> 'pbsID' }
            ] as ClusterJobSchedulerService

        ProcessingOptionService.metaClass.static.findOptionSafe = { OptionName a, String b, Project c ->
            'qualityAssessment.sh ${processedBamFilePath} ${processedBaiFilePath} ${qualityAssessmentFilePath} ${coverageDataFilePath} ${insertSizeDataFilePath} false ${allChromosomeName} 36 25 0 1 1000 10 false ${bedFilePath} ${refGenMetaInfoFilePath}'
        }

        def referenceGenomeService = [
            referenceGenome: { a1, a2 -> referenceGenome},
            referenceGenomeMetaInformationPath: { a1 -> new File('/referenceGenomeMetaInformationPath') }
            ] as ReferenceGenomeService

        def bedFileService = [
            filePath: { a1 -> 'bedFilePath' }
            ] as BedFileService

        job = new ExecuteMergedBamFileQaAnalysisJob(
                processedMergedBamFileQaFileService: processedMergedBamFileQaFileService,
                qualityAssessmentMergedPassService: qualityAssessmentMergedPassService,
                processedMergedBamFileService: processedMergedBamFileService,
                referenceGenomeService: referenceGenomeService,
                bedFileService: bedFileService,
                clusterJobSchedulerService: clusterJobSchedulerService,
                )

        ExecuteMergedBamFileQaAnalysisJob.metaClass.getProcessParameterValue = { -> 1 as long }
        ExecuteMergedBamFileQaAnalysisJob.metaClass.addOutputParameter = { String a1, String a2 -> }
    }

    @After
    void cleanUp() {
        GroovySystem.metaClassRegistry.removeMetaClass(ProcessingOptionService)
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

        job.clusterJobSchedulerService = [executeJob: { realm, cmd ->
                String expCommand = "qualityAssessment.sh processedBamFileFilePath baiFilePath qualityAssessmentDataFilePath coverageDataFilePath insertSizeDataFilePath false ${Chromosomes.overallChromosomesLabel()} 36 25 0 1 1000 10 false bedFilePath /referenceGenomeMetaInformationPath; chmod 440 qualityAssessmentDataFilePath coverageDataFilePath insertSizeDataFilePath"
                assertEquals(expCommand, cmd)
                return 'pbsID'
            }] as ClusterJobSchedulerService

        job.execute()
    }

    @Test
    void testExecuteCorrectExomeWithoutBedFile() {

        seqType.name = SeqTypeNames.EXOME.seqTypeName
        seqType.libraryLayout = 'PAIRED'

        job.clusterJobSchedulerService = [executeJob: { realm, cmd ->
            assert false //this method should not be executed
        }] as ClusterJobSchedulerService

        assert "Could not find a bed file for ${referenceGenome} and LibraryPreparationKit" == shouldFail(ProcessingException) {
            job.execute()
        }
    }

    @Test
    void testExecuteCorrectWholeGenome() {

        seqType.name = SeqTypeNames.WHOLE_GENOME.seqTypeName
        seqType.libraryLayout = 'PAIRED'

        job.clusterJobSchedulerService = [executeJob: { realm, cmd ->
                String expCommand = "qualityAssessment.sh processedBamFileFilePath baiFilePath qualityAssessmentDataFilePath coverageDataFilePath insertSizeDataFilePath false ${Chromosomes.overallChromosomesLabel()} 36 25 0 1 1000 10 false; chmod 440 qualityAssessmentDataFilePath coverageDataFilePath insertSizeDataFilePath"
                assertEquals(expCommand, cmd)
                return 'pbsID'
            }] as ClusterJobSchedulerService

        job.execute()
    }

    @Test(expected = IllegalArgumentException)
    void testExecuteNotSupportedSeqType() {

        seqType.name = SeqTypeNames.WHOLE_GENOME.seqTypeName
        seqType.libraryLayout = 'SINGLE'
        job.execute()
    }
}
