package workflows


import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import static org.junit.Assert.*

import org.joda.time.Duration

import grails.test.mixin.*
import grails.test.mixin.domain.*
import grails.test.mixin.support.*
import grails.util.Environment
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.filehandling.FileNames
import de.dkfz.tbi.otp.job.jobs.transferMergedBamFile.TransferMergedBamFileStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.FileType.Type
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import de.dkfz.tbi.otp.testing.GroovyScriptAwareIntegrationTest

/**
 * Preparation for execution: see src/docs/guide/devel/testing/workflowTesting.gdoc
 */
class TransferMergedBamFileWorkflowTests extends AbstractWorkflowTest {

    ProcessingOptionService processingOptionService

    TransferMergedBamFileStartJob transferMergedBamFileStartJob

    // The scheduler needs to access the created objects while the test is being executed
    boolean transactional = false

    // TODO want to get rid of this hardcoded.. idea: maybe calculating from the walltime of the cluster jobs.. -> OTP-570/OTP-672
    final Duration TIMEOUT = Duration.standardMinutes(40)

    LsdfFilesService lsdfFilesService
    ExecutionService executionService

    // TODO This paths should be obtained from somewhere else..  maybe from ~/.otp.properties, but I am hardcoding for now.. -> OTP-570/OTP-672
    String dkfzBasePath = 'WORKFLOW_ROOT/TransferWorkflow'
    String dkfzRootPath = "${dkfzBasePath}/root_path"
    String dkfzProcessingPath = "${dkfzBasePath}/processing_root_path"
    String dkfzLoggingPath = "${dkfzBasePath}/logging_root_path"
    String bqBasePath = '$BQ_ROOTPATH/dmg/otp/workflow-tests/TransferWorkflow'
    String bqRootPath = "${bqBasePath}/root_path"
    String bqProcessingPath = "${bqBasePath}/processing_root_path"
    String bqLoggingPath = "${bqBasePath}/logging_root_path"

    // Paths for testing on DKFZ
    //*
    String rootPath = dkfzRootPath
    String processingRootPath = dkfzProcessingPath
    String loggingRootPath = dkfzLoggingPath
    //*/

    /*
    // Paths for testing on BioQuant
    String rootPath = bqRootPath
    String processingRootPath = dkfzProcessingPath
    String loggingRootPath = bqLoggingPath
    //*/

    private static final String CHROMOSOME_X_NAME = "CHR_X"
    private static final String CHROMOSOME_Y_NAME = "CHR_Y"

    // files to be processed by the tests: 2 merged bam files, 2 bai files, 2 qa results per merged/single lane
    String mergingMiddleDir = "${processingRootPath}/project1/results_per_pid/pid_1/merging/control/WHOLE_GENOME/PAIRED/DEFAULT"
    String filePathMergedBamFile1 = "${mergingMiddleDir}/0/pass0/"
    String fileNameMergedBamFile1 = "${filePathMergedBamFile1}control_pid_1_WHOLE_GENOME_PAIRED_merged.mdup.bam"
    String filePathMergedBamFile2 = "${mergingMiddleDir}/1/pass0/"
    String fileNameMergedBamFile2 = "${filePathMergedBamFile2}control_pid_1_WHOLE_GENOME_PAIRED_merged.mdup.bam"
    String filePathBaiFile1 = "${mergingMiddleDir}/0/pass0/"
    String fileNameBaiFile1 = "${filePathBaiFile1}control_pid_1_WHOLE_GENOME_PAIRED_merged.mdup.bai"
    String filePathBaiFile2 = "${mergingMiddleDir}/1/pass0/"
    String fileNameBaiFile2 = "${filePathBaiFile2}control_pid_1_WHOLE_GENOME_PAIRED_merged.mdup.bai"
    String filePathMergedBamFileQA1 = "${mergingMiddleDir}/0/pass0/QualityAssessment/pass0/"
    String fileNameMergedBamFileQA1 = "${filePathMergedBamFileQA1}/plot.jpg"
    String filePathMergedBamFileQA2 = "${mergingMiddleDir}/1/pass0/QualityAssessment/pass0/"
    String fileNameMergedBamFileQA2 = "${filePathMergedBamFileQA2}/plot.jpg"
    String destinationDirMergedBamFile = "${rootPath}/project1/sequencing/whole_genome_sequencing/view-by-pid/pid_1/control/paired/merged-alignment"
    String destinationFileNameMergedBamFile = "${destinationDirMergedBamFile}/control_pid_1_WHOLE_GENOME_PAIRED_merged.mdup.bam"
    String destinationDirQaResults = "${destinationDirMergedBamFile}/QualityAssessment/"
    String qaResultOverviewFile = "${destinationDirQaResults}/${FileNames.QA_RESULT_OVERVIEW}"
    String qaResultOverviewExtendedFile = "${destinationDirQaResults}/${FileNames.QA_RESULT_OVERVIEW_EXTENDED}"
    String fastqFilesInMergedBamFile = "${destinationDirMergedBamFile}/${FileNames.FASTQ_FILES_IN_MERGEDBAMFILE}"

    /**
     * Realm necessary to cleanup folder structure
     */
    Realm realm

    void setUp() {
        /*
         * Initialize database
         */

        // Project
        String projectName = "project"
        String projectDirName = "project1"
        // SeqType
        String seqTypeDirName = "whole_genome_sequencing"
        String seqTypeName = SeqTypeNames.WHOLE_GENOME.seqTypeName
        String seqTypeLibrary = "PAIRED"
        // SeqCenter
        String seqCenterName = "DKFZ"
        String seqCenterDirName = "core"
        // Run
        String runName = "runName"
        String runDirName = "run${runName}"
        // Realm for DKFZ
        String realmName = "DKFZ"
        // Realm for BioQuant (change if testing there)
        //String realmName = "BioQuant"

        def paths = [
            rootPath: "${rootPath}",
            processingRootPath: "${processingRootPath}",
            programsRootPath: '/',
            loggingRootPath: loggingRootPath,
        ]

        // Realms for testing on DKFZ
        realm = DomainFactory.createRealmDataManagementDKFZ(paths)
        assertNotNull(realm.save(flush: true))

        realm = DomainFactory.createRealmDataProcessingDKFZ(paths)
        assertNotNull(realm.save(flush: true))


        /*
        //Variables needed for bioquant test
        String realmBioquantUnixUser = "unixUser2"
        String realmDkfzUnixUser = "unixUser"
        String realmProgramsRootPath = "/"
        String realmHost = "headnode"
        int realmPort = 22
        String realmWebHost = "https://otp.local/ngsdata/"
        String realmPbsOptionsDKFZ = '{"-l": {nodes: "1:lsdf", walltime: "5:00"}}'
        String realmPbsOptionsBQ = '{"-l": {nodes: "1:xeon", walltime: "5:00"}, "-W": {x: "NACCESSPOLICY:SINGLEJOB"}}'
        int realmTimeout = 0
        realmName = "BioQuant"

         // Realms for testing on BioQuant
         realm = new Realm(
         name: "BioQuant",
         env: Environment.getCurrent().getName(),
         operationType: Realm.OperationType.DATA_PROCESSING,
         cluster: Realm.Cluster.DKFZ, // Data processing for BQ projects is done on DKFZ, this is correct.
         rootPath:           bqRootPath,
         processingRootPath: dkfzProcessingPath,
         programsRootPath: '/',
         loggingRootPath: dkfzLoggingPath,
         webHost: realmWebHost,
         host: 'headnode',
         port: 22,
         unixUser: realmDkfzUnixUser,
         timeout: realmTimeout,
         pbsOptions: realmPbsOptionsDKFZ
         )
         assertNotNull(realm.save(flush: true))
         realm = new Realm(
         name: "BioQuant",
         env: Environment.getCurrent().getName(),
         operationType: Realm.OperationType.DATA_MANAGEMENT,
         cluster: Realm.Cluster.BIOQUANT,
         rootPath:           bqRootPath,
         processingRootPath: bqProcessingPath,
         programsRootPath: '/',
         loggingRootPath: bqLoggingPath,
         webHost: realmWebHost,
         host: "otphost-other.example.org",
         port: 22,
         unixUser: realmBioquantUnixUser,
         timeout: realmTimeout,
         pbsOptions: realmPbsOptionsBQ
         )
         assertNotNull(realm.save(flush: true))
         realm = new Realm(
         name: "DKFZ",
         env: Environment.getCurrent().getName(),
         operationType: Realm.OperationType.DATA_MANAGEMENT,
         cluster: Realm.Cluster.DKFZ,
         rootPath:           dkfzRootPath,
         processingRootPath: dkfzProcessingPath,
         programsRootPath: '/',
         loggingRootPath: dkfzLoggingPath,
         webHost: realmWebHost,
         host: 'headnode',
         port: 22,
         unixUser: realmDkfzUnixUser,
         timeout: realmTimeout,
         pbsOptions: realmPbsOptionsDKFZ
         )
         assertNotNull(realm.save(flush: true))
         // this will be used to create the directories, so this needs to be last
         realm = new Realm(
         name: "DKFZ",
         env: Environment.getCurrent().getName(),
         operationType: Realm.OperationType.DATA_PROCESSING,
         cluster: Realm.Cluster.DKFZ,
         rootPath:           dkfzRootPath,
         processingRootPath: dkfzProcessingPath,
         programsRootPath: '/',
         loggingRootPath: dkfzLoggingPath,
         webHost: realmWebHost,
         host: 'headnode',
         port: 22,
         unixUser: realmDkfzUnixUser,
         timeout: realmTimeout,
         pbsOptions: realmPbsOptionsDKFZ
         )
         assertNotNull(realm.save(flush: true))
         //*/

        Project project = TestData.createProject(
                        name: projectName,
                        dirName: projectDirName,
                        realmName: realmName
                        )
        assertNotNull(project.save([flush: true, failOnError: true]))

        Individual individual = new Individual(
                        pid: "pid_1",
                        mockPid: "mockPid_1",
                        mockFullName: "mockFullName_1",
                        type: Individual.Type.UNDEFINED,
                        project: project
                        )
        assertNotNull(individual.save([flush: true, failOnError: true]))

        SampleType sampleType = new SampleType(
                        name: "control"
                        )
        assertNotNull(sampleType.save([flush: true, failOnError: true]))

        Sample sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true, failOnError: true]))

        SeqType seqType = new SeqType(
                        name: seqTypeName,
                        libraryLayout: seqTypeLibrary,
                        dirName: seqTypeDirName
                        )
        assertNotNull(seqType.save([flush: true, failOnError: true]))

        SeqPlatform seqPlatform = new SeqPlatform(
                        seqPlatformGroup: SeqPlatformGroup.build(),
                        name: "Illumina",
                        model: "model"
                        )
        assertNotNull(seqPlatform.save([flush: true, failOnError: true]))

        SeqCenter seqCenter = new SeqCenter(
                        name: seqCenterName,
                        dirName: seqCenterDirName
                        )
        assertNotNull(seqCenter.save([flush: true, failOnError: true]))

        Run run = new Run(
                        name: runName,
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform
                        )
        assertNotNull(run.save([flush: true, failOnError: true]))

        SoftwareTool softwareTool = new SoftwareTool(
                        programName: "softwareToolName",
                        programVersion: "version",
                        qualityCode: "quality",
                        type: SoftwareTool.Type.ALIGNMENT
                        )
        assertNotNull(softwareTool.save([flush: true, failOnError: true]))

        SeqTrack seqTrack = new SeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack.save([flush: true, failOnError: true]))

        FileType fileType = new FileType(
                        type: Type.SEQUENCE
                        )
        assertNotNull(fileType.save([flush: true, failOnError: true]))

        DataFile dataFile1 = new DataFile(
                        fileName: "dataFile1",
                        seqTrack: seqTrack,
                        fileType: fileType
                        )
        assertNotNull(dataFile1.save([flush: true, failOnError: true]))

        DataFile dataFile2 = new DataFile(
                        fileName: "dataFile2",
                        seqTrack: seqTrack,
                        fileType: fileType
                        )
        assertNotNull(dataFile2.save([flush: true, failOnError: true]))

        ReferenceGenome referenceGenome = new ReferenceGenome(
                        name: "hs37d5",
                        path: "bwa06_1KGRef",
                        fileNamePrefix: "hs37d5",
                        length: 3137454505,
                        lengthWithoutN: 2900434419,
                        lengthRefChromosomes: 3095677412,
                        lengthRefChromosomesWithoutN: 2858658097
                        )
        assertNotNull(referenceGenome.save([flush: true, failOnError: true]))

        AlignmentPass alignmentPass = TestData.createAndSaveAlignmentPass(
                        referenceGenome: referenceGenome,
                        identifier: 0,
                        seqTrack: seqTrack,
                        description: "test"
                        )
        assertNotNull(alignmentPass.save([flush: true, failOnError: true]))

        ProcessedBamFile processedBamFile = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        status: AbstractBamFile.State.NEEDS_PROCESSING,
                        qualityAssessmentStatus: QaProcessingStatus.FINISHED
                        )
        assertNotNull(processedBamFile.save([flush: true, failOnError: true]))

        QualityAssessmentPass qualityAssessmentPass = new QualityAssessmentPass(
                        identifier: 0,
                        processedBamFile: processedBamFile
                        )
        assertNotNull(qualityAssessmentPass.save([flush: true, failOnError: true]))

        ChromosomeQualityAssessment chromosomeQualityAssessmentChrX = new ChromosomeQualityAssessment(
                        chromosomeName: CHROMOSOME_X_NAME,
                        qualityAssessmentPass: qualityAssessmentPass
                        )
        setProperties(chromosomeQualityAssessmentChrX)
        assertNotNull(chromosomeQualityAssessmentChrX.save([flush: true]))

        ChromosomeQualityAssessment chromosomeQualityAssessmentChrY = new ChromosomeQualityAssessment(
                        chromosomeName: CHROMOSOME_Y_NAME,
                        qualityAssessmentPass: qualityAssessmentPass
                        )
        setProperties(chromosomeQualityAssessmentChrY)
        assertNotNull(chromosomeQualityAssessmentChrY.save([flush: true]))

        OverallQualityAssessment overallQualityAssessment = new OverallQualityAssessment(
                        qualityAssessmentPass: qualityAssessmentPass
                        )
        setProperties(overallQualityAssessment)
        assertNotNull(overallQualityAssessment.save([flush: true]))

        SeqTrack seqTrack1 = new SeqTrack(
                        laneId: "laneId1",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool
                        )
        assertNotNull(seqTrack1.save([flush: true, failOnError: true]))

        DataFile dataFile3 = new DataFile(
                        fileName: "dataFile3",
                        seqTrack: seqTrack1,
                        fileType: fileType
                        )
        assertNotNull(dataFile3.save([flush: true, failOnError: true]))

        DataFile dataFile4 = new DataFile(
                        fileName: "dataFile4",
                        seqTrack: seqTrack1,
                        fileType: fileType
                        )
        assertNotNull(dataFile4.save([flush: true, failOnError: true]))

        AlignmentPass alignmentPass1 = TestData.createAndSaveAlignmentPass(
                        referenceGenome: referenceGenome,
                        identifier: 0,
                        seqTrack: seqTrack1,
                        description: "test"
                        )
        assertNotNull(alignmentPass1.save([flush: true, failOnError: true]))

        ProcessedBamFile processedBamFile1 = new ProcessedBamFile(
                        alignmentPass: alignmentPass1,
                        type: BamType.SORTED,
                        status: AbstractBamFile.State.NEEDS_PROCESSING,
                        qualityAssessmentStatus: QaProcessingStatus.FINISHED
                        )
        assertNotNull(processedBamFile1.save([flush: true, failOnError: true]))

        QualityAssessmentPass qualityAssessmentPass1 = new QualityAssessmentPass(
                        identifier: 0,
                        processedBamFile: processedBamFile1
                        )
        assertNotNull(qualityAssessmentPass1.save([flush: true, failOnError: true]))

        ChromosomeQualityAssessment chromosomeQualityAssessmentChrX1 = new ChromosomeQualityAssessment(
                        chromosomeName: CHROMOSOME_X_NAME,
                        qualityAssessmentPass: qualityAssessmentPass1
                        )
        setProperties(chromosomeQualityAssessmentChrX1)
        assertNotNull(chromosomeQualityAssessmentChrX1.save([flush: true]))

        ChromosomeQualityAssessment chromosomeQualityAssessmentChrY1 = new ChromosomeQualityAssessment(
                        chromosomeName: CHROMOSOME_Y_NAME,
                        qualityAssessmentPass: qualityAssessmentPass1
                        )
        setProperties(chromosomeQualityAssessmentChrY1)
        assertNotNull(chromosomeQualityAssessmentChrY1.save([flush: true]))

        OverallQualityAssessment overallQualityAssessment1 = new OverallQualityAssessment(
                        qualityAssessmentPass: qualityAssessmentPass1
                        )
        setProperties(overallQualityAssessment1)
        assertNotNull(overallQualityAssessment1.save([flush: true]))

        MergingWorkPackage mergingWorkPackage = TestData.findOrSaveMergingWorkPackage(seqTrack, referenceGenome)
        assertNotNull(mergingWorkPackage.save([flush: true, failOnError: true]))

        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage,
                        status: MergingSet.State.PROCESSED
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))

        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile
                        )
        assertNotNull(mergingSetAssignment.save([flush: true, failOnError: true]))

        MergingPass mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true, failOnError: true]))

        ProcessedMergedBamFile processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass,
                        fileExists: true,
                        type: BamType.MDUP,
                        qualityAssessmentStatus: QaProcessingStatus.FINISHED,
                        md5sum: null,
                        status: AbstractBamFile.State.PROCESSED
                        )
        assertNotNull(processedMergedBamFile.save([flush: true, failOnError: true]))

        QualityAssessmentMergedPass qualityAssessmentMergedPass = new QualityAssessmentMergedPass(
                        identifier: 0,
                        processedMergedBamFile: processedMergedBamFile
                        )
        assertNotNull(qualityAssessmentMergedPass.save([flush: true, failOnError: true]))

        OverallQualityAssessmentMerged overallQualityAssessmentMerged = new OverallQualityAssessmentMerged(
                        qualityAssessmentMergedPass: qualityAssessmentMergedPass
                        )
        setProperties(overallQualityAssessmentMerged)
        assertNotNull(overallQualityAssessmentMerged.save([flush: true, failOnError: true]))

        MergingSet mergingSet1 = new MergingSet(
                        identifier: 1,
                        mergingWorkPackage: mergingWorkPackage,
                        status: MergingSet.State.PROCESSED
                        )
        assertNotNull(mergingSet1.save([flush: true, failOnError: true]))

        MergingSetAssignment mergingSetAssignment1 = new MergingSetAssignment(
                        mergingSet: mergingSet1,
                        bamFile: processedBamFile1
                        )
        assertNotNull(mergingSetAssignment1.save([flush: true, failOnError: true]))

        MergingSetAssignment mergingSetAssignment2 = new MergingSetAssignment(
                        mergingSet: mergingSet1,
                        bamFile: processedMergedBamFile
                        )
        assertNotNull(mergingSetAssignment2.save([flush: true, failOnError: true]))

        MergingPass mergingPass1 = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet1
                        )
        assertNotNull(mergingPass1.save([flush: true, failOnError: true]))

        ProcessedMergedBamFile processedMergedBamFile1 = new ProcessedMergedBamFile(
                        mergingPass: mergingPass1,
                        fileExists: true,
                        type: BamType.MDUP,
                        qualityAssessmentStatus: QaProcessingStatus.FINISHED,
                        md5sum: null,
                        status: AbstractBamFile.State.PROCESSED
                        )
        assertNotNull(processedMergedBamFile1.save([flush: true, failOnError: true]))

        QualityAssessmentMergedPass qualityAssessmentMergedPass1 = new QualityAssessmentMergedPass(
                        identifier: 0,
                        processedMergedBamFile: processedMergedBamFile1
                        )
        assertNotNull(qualityAssessmentMergedPass1.save([flush: true, failOnError: true]))

        OverallQualityAssessmentMerged overallQualityAssessmentMerged1 = new OverallQualityAssessmentMerged(
                        qualityAssessmentMergedPass: qualityAssessmentMergedPass1
                        )
        setProperties(overallQualityAssessmentMerged1)
        assertNotNull(overallQualityAssessmentMerged1.save([flush: true, failOnError: true]))

        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = new ReferenceGenomeProjectSeqType(
                        project: project,
                        seqType: seqType,
                        referenceGenome: referenceGenome,
                        )
        assertNotNull(referenceGenomeProjectSeqType.save([flush: true, failOnError: true]))

        ReferenceGenomeEntry referenceGenomeEntryChrX = new ReferenceGenomeEntry(
                        alias: Chromosomes.CHR_X.alias,
                        classification: Classification.CHROMOSOME,
                        length: 249250621,
                        lengthWithoutN: 225280621,
                        name: CHROMOSOME_X_NAME,
                        referenceGenome: referenceGenome
                        )
        assertNotNull(referenceGenomeEntryChrX.save([flush: true, failOnError: true]))

        ChromosomeQualityAssessmentMerged chromosomeXQualityAssessmentMerged = new ChromosomeQualityAssessmentMerged(
                        chromosomeName: CHROMOSOME_X_NAME,
                        qualityAssessmentMergedPass: qualityAssessmentMergedPass
                        )
        setProperties(chromosomeXQualityAssessmentMerged)
        assertNotNull(chromosomeXQualityAssessmentMerged.save([flush: true, failOnError: true]))

        ChromosomeQualityAssessmentMerged chromosomeXQualityAssessmentMerged1 = new ChromosomeQualityAssessmentMerged(
                        chromosomeName: CHROMOSOME_X_NAME,
                        qualityAssessmentMergedPass: qualityAssessmentMergedPass1
                        )
        setProperties(chromosomeXQualityAssessmentMerged1)
        assertNotNull(chromosomeXQualityAssessmentMerged1.save([flush: true, failOnError: true]))

        ReferenceGenomeEntry referenceGenomeEntryChrY = new ReferenceGenomeEntry(
                        alias: Chromosomes.CHR_Y.alias,
                        classification: Classification.CHROMOSOME,
                        length: 249250621,
                        lengthWithoutN: 225280621,
                        name: CHROMOSOME_Y_NAME,
                        referenceGenome: referenceGenome
                        )
        assertNotNull(referenceGenomeEntryChrY.save([flush: true, failOnError: true]))

        ChromosomeQualityAssessmentMerged chromosomeYQualityAssessmentMerged = new ChromosomeQualityAssessmentMerged(
                        chromosomeName: CHROMOSOME_Y_NAME,
                        qualityAssessmentMergedPass: qualityAssessmentMergedPass
                        )
        setProperties(chromosomeYQualityAssessmentMerged)
        assertNotNull(chromosomeYQualityAssessmentMerged.save([flush: true, failOnError: true]))

        ChromosomeQualityAssessmentMerged chromosomeYQualityAssessmentMerged1 = new ChromosomeQualityAssessmentMerged(
                        chromosomeName: CHROMOSOME_Y_NAME,
                        qualityAssessmentMergedPass: qualityAssessmentMergedPass1
                        )
        setProperties(chromosomeYQualityAssessmentMerged1)
        assertNotNull(chromosomeYQualityAssessmentMerged1.save([flush: true, failOnError: true]))

        /*
         * Setup directories and files for corresponding database objects
         */
        // Just to be sure the rootPath and the processingRootPath are clean for new test
        String cmdCleanUp = cleanUpTestFoldersCommand()
        String cmdBuildDirStructure = [
            filePathMergedBamFile1,
            filePathMergedBamFile2,
            filePathBaiFile1,
            filePathBaiFile2,
            filePathMergedBamFileQA1,
            filePathMergedBamFileQA2,
            loggingRootPath + "/log/status/",
        ].collect { "mkdir -p ${it}" }.join " && "

        List<String> files = [
            fileNameMergedBamFile1,
            fileNameMergedBamFile2,
            fileNameBaiFile1,
            fileNameBaiFile2,
            fileNameMergedBamFileQA1,
            fileNameMergedBamFileQA2
        ]
        String cmdBuildFileStructure = files.collect {"echo -n \"${it}\" > ${it}"}.join " && "
        // Call "sync" to block termination of script until I/O is done
        executionService.executeCommand(realm, "${cmdCleanUp}; ${cmdBuildDirStructure} && ${cmdBuildFileStructure} && ${createMd5SumFile(fileNameMergedBamFile1)} && ${createMd5SumFile(fileNameMergedBamFile2)} && sync")
        checkFiles(files)
    }

    // only the hash code of the md5sum output is needed since picard also only provides the hash code
    private String createMd5SumFile(String fileName) {
        return "md5sum ${fileName} | awk '{ print \$1 }' > ${fileName}.md5"
    }

    private void setProperties(AbstractQualityAssessment abstractQualityAssessment) {
        [
            referenceLength: 63025520,
            duplicateR1: 0,
            duplicateR2: 0,
            properPairStrandConflict: 0,
            referenceAgreement: 8758188,
            referenceAgreementStrandConflict: 338,
            mappedQualityLongR1: 4163201,
            mappedQualityLongR2: 4244677,
            qcBasesMapped: 848198844,
            mappedLowQualityR1: 178743,
            mappedLowQualityR2: 110287,
            mappedShortR1: 4575,
            mappedShortR2: 1529,
            notMappedR1: 61809,
            notMappedR2: 51675,
            endReadAberration: 10400,
            totalReadCounter: 8816496,
            qcFailedReads: 0,
            duplicates: 0,
            totalMappedReadCounter: 8792542,
            pairedInSequencing: 8816496,
            pairedRead2: 4408328,
            pairedRead1: 4408168,
            properlyPaired: 8720830,
            withItselfAndMateMapped: 8768588,
            withMateMappedToDifferentChr: 10400,
            withMateMappedToDifferentChrMaq: 7159,
            singletons: 23954,
            insertSizeMean: 223.50400111,
            insertSizeSD: 23.87320658027467,
            insertSizeMedian: 225.0,
            insertSizeRMS: 224.77537343891422,
            percentIncorrectPEorientation: 0.0038592457709288723,
            percentReadPairsMapToDiffChrom: 0.11828206222955773
        ].each { key, value ->
            abstractQualityAssessment."${key}" = value
        }
    }


    /**
     * Returns a comand to clean up the rootPath and processingRootPath
     * @return Command to clean up used folders
     */
    String cleanUpTestFoldersCommand() {
        return "rm -rf ${rootPath}/* ${processingRootPath}/* ${loggingRootPath}/*"
        /* When testing on BioQuant, there is no write access. You have to replace the
         * above line by something like 'return "true"' */
    }

    /**
     * Helper to see logs at console ( besides seeing at the reports in the end)
     * @msg Message to be shown
     */
    void println(String msg) {
        log.debug(msg)
        System.out.println(msg)
    }

    void tearDown() {
        //executionService.executeCommand(realm, cleanUpTestFoldersCommand())
        realm = null
    }

    /**
     * Test execution of the workflow without any processing options defined
     */
    @Ignore
    @Test
    void testExecutionWithoutProcessingOptions() {
        // Import workflow from script file
        run("scripts/workflows/TransferMergedBamFileWorkflow.groovy")

        // there will be only one at the database
        JobExecutionPlan jobExecutionPlan = JobExecutionPlan.list()?.first()

        assertNotNull(jobExecutionPlan)
        // setup start condition (the fastqc file as ready to be processed)
        /* is triggered automatically when
         * the qa is finished,
         * the qa for the included single lane bam files are finished
         * the merged bam file is not used in a merging process
         */
        // TODO hack to be able to start the workflow -> OTP-570/OTP-672
        transferMergedBamFileStartJob.setJobExecutionPlan(jobExecutionPlan)
        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.createCriteria().list {
            eq("fileOperationStatus", FileOperationStatus.DECLARED)
            order("id", "asc")
        }?.first()
        processedMergedBamFile.fileOperationStatus = FileOperationStatus.NEEDS_PROCESSING
        assertNotNull(processedMergedBamFile.save([flush: true, failOnError: true]))
        waitUntilWorkflowFinishesWithoutFailure(TIMEOUT)
        File mergedBamFile = new File(destinationFileNameMergedBamFile)
        assertEquals(fileNameMergedBamFile1, mergedBamFile.getText())
        checkNumberOfStoredMd5sums(1)
        // process to copy the second merged bam file
        // since the first file does not have the status DECLARED anymore, the newer file is the first
        processedMergedBamFile = ProcessedMergedBamFile.createCriteria().list {
            eq("fileOperationStatus", FileOperationStatus.DECLARED)
            order("id", "asc")
        }?.first()
        processedMergedBamFile.fileOperationStatus = FileOperationStatus.NEEDS_PROCESSING
        assertNotNull(processedMergedBamFile.save([flush: true, failOnError: true]))
        waitUntilWorkflowFinishesWithoutFailure(TIMEOUT, 2)
        assertEquals(fileNameMergedBamFile2, mergedBamFile.getText())
        checkNumberOfStoredMd5sums(2)
        checkDestinationFileStructure()

        /* TODO: Test for work-around for OTP-1018. Unfortunately, there is no way to test for
           readability for "other" since Java 6 does not have PosixFilePermission of the java.nio
           library. The test can only be implemented after the upgrade to versions >= 7. */
    }

    //check if the md5sum was stored properly in the database
    void checkNumberOfStoredMd5sums(int expected) {
        int mergedBamFilesWithStoredMd5sum = ProcessedMergedBamFile.createCriteria().list { isNotNull("md5sum") }.size()
        assertEquals(expected, mergedBamFilesWithStoredMd5sum)
        int mergedBamFilesWithProcessedOperationStatus = ProcessedMergedBamFile.createCriteria().list { eq("fileOperationStatus", FileOperationStatus.PROCESSED) }.size()
        assertEquals(expected, mergedBamFilesWithProcessedOperationStatus)
    }

    //check that all files are copied correctly
    void checkDestinationFileStructure() {
        checkFiles([
            qaResultOverviewFile,
            qaResultOverviewExtendedFile,
            destinationFileNameMergedBamFile,
            "${destinationDirMergedBamFile}/control_pid_1_WHOLE_GENOME_PAIRED_merged.mdup.bai",
            "${destinationDirMergedBamFile}/control_pid_1_WHOLE_GENOME_PAIRED_merged.mdup.bam.md5sum",
            "${destinationDirMergedBamFile}/control_pid_1_WHOLE_GENOME_PAIRED_merged.mdup.bai.md5sum",
            "${destinationDirQaResults}/MD5SUMS",
            "${destinationDirQaResults}/plot.jpg",
            fastqFilesInMergedBamFile
        ])
    }

    void checkFiles(List paths) {
        paths.each {
            File file = new File(it)
            assert file.canRead()
            assert file.size() > 0
        }
    }
}
