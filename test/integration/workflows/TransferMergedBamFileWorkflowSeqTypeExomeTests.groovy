package workflows

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*
import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.domain.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.InformationReliability
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

class TransferMergedBamFileWorkflowSeqTypeExomeTests extends GroovyScriptAwareIntegrationTest {

    /*
     * Preparation:
     *
     *  - in conf/spring/resources.groovy: Change line to
     *      if (Environment.getCurrent() == Environment.TEST && false) {
     *              task.executor(id: "taskExecutor", "pool-size": 10) -> 1
     task.scheduler(id: "taskScheduler", "pool-size": 10) -> 1
     *    to start scheduler
     *  - Mount LSDF to your local system via sshfs:
     *    sudo mkdir -p STORAGE_ROOT/dmg/otp/test/TransferWorkflow
     *    sudo chown $USER WORKFLOW_ROOT/TransferWorkflow
     *    sshfs headnode:WORKFLOW_ROOT/TransferWorkflow WORKFLOW_ROOT/TransferWorkflow
     *  - Check your PBS (Linux cluster) password in ~/.otp.properties
     *  - remove @Ignore of the test
     *  - If you test for BioQuant and DKFZ setups, it is *required* to use the *same* passwords in case
     *    the accounts differ
     */

    ProcessingOptionService processingOptionService

    TransferMergedBamFileStartJob transferMergedBamFileStartJob

    // The scheduler needs to access the created objects while the test is being executed
    boolean transactional = false

    // TODO want to get rid of this hardcoded.. idea: maybe calculating from the walltime of the cluster jobs.. -> OTP-570/OTP-672
    int SLEEPING_TIME_IN_MINUTES = 40

    LsdfFilesService lsdfFilesService
    ExecutionService executionService

    // TODO This paths should be obtained from somewhere else..  maybe from ~/.otp.properties, but I am hardcoding for now.. -> OTP-570/OTP-672
    String dkfzRootPath = 'WORKFLOW_ROOT/TransferWorkflow/root_path'
    String dkfzProcessingPath = 'WORKFLOW_ROOT/TransferWorkflow/processing_root_path'
    String dkfzLoggingPath = 'WORKFLOW_ROOT/TransferWorkflow/logging_root_path'
    String bqRootPath = '$BQ_ROOTPATH/dmg/otp/workflow-tests/TransferWorkflow/root_path'
    String bqProcessingPath = '$BQ_ROOTPATH/dmg/otp/workflow-tests/TransferWorkflow/processing_root_path'

    // Paths for testing on DKFZ
    String rootPath = dkfzRootPath
    String processingRootPath = dkfzProcessingPath
    String loggingRootPath = dkfzLoggingPath

    /*
     // Paths for testing on BioQuant
     String rootPath = bqRootPath
     String processingRootPath = dkfzProcessingPath
     */

    private static final String CHROMOSOME_X_NAME = "CHR_X"
    private static final String CHROMOSOME_Y_NAME = "CHR_Y"

    // files to be processed by the tests: 2 merged bam files, 2 bai files, 2 qa results per merged/single lane
    String mergingMiddleDir = "${processingRootPath}/project1/results_per_pid/pid_1/merging/control/EXON/PAIRED/DEFAULT"
    String alignmentMiddleDir = "${processingRootPath}/project1/results_per_pid/pid_1/alignment"
    String filePathMergedBamFile1 = "${mergingMiddleDir}/0/pass0/"
    String fileNameMergedBamFile1 = "${filePathMergedBamFile1}control_pid_1_EXON_PAIRED_merged.mdup.bam"
    String filePathMergedBamFile2 = "${mergingMiddleDir}/1/pass0/"
    String fileNameMergedBamFile2 = "${filePathMergedBamFile2}control_pid_1_EXON_PAIRED_merged.mdup.bam"
    String filePathBaiFile1 = "${mergingMiddleDir}/0/pass0/"
    String fileNameBaiFile1 = "${filePathBaiFile1}control_pid_1_EXON_PAIRED_merged.mdup.bai"
    String filePathBaiFile2 = "${mergingMiddleDir}/1/pass0/"
    String fileNameBaiFile2 = "${filePathBaiFile2}control_pid_1_EXON_PAIRED_merged.mdup.bai"
    String filePathBamFileQA1 = "${alignmentMiddleDir}/runName_laneId/pass0/QualityAssessment/pass0/"
    String fileNameBamFileQA1 = "${filePathBamFileQA1}/plot.jpg"
    String filePathBamFileQA2 = "${alignmentMiddleDir}/runName_laneId1/pass0/QualityAssessment/pass0/"
    String fileNameBamFileQA2 = "${filePathBamFileQA2}/plot.jpg"
    String filePathMergedBamFileQA1 = "${mergingMiddleDir}/0/pass0/QualityAssessment/pass0/"
    String fileNameMergedBamFileQA1 = "${filePathMergedBamFileQA1}/plot.jpg"
    String filePathMergedBamFileQA2 = "${mergingMiddleDir}/1/pass0/QualityAssessment/pass0/"
    String fileNameMergedBamFileQA2 = "${filePathMergedBamFileQA2}/plot.jpg"
    String destinationDirMergedBamFile = "${rootPath}/project1/sequencing/exome_sequencing/view-by-pid/pid_1/control/paired/merged-alignment"
    String destinationFileNameMergedBamFile = "${destinationDirMergedBamFile}/control_pid_1_EXON_PAIRED_merged.mdup.bam"
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
        String seqTypeDirName = "exome_sequencing"
        String seqTypeName = SeqTypeNames.EXOME.seqTypeName
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
        String realmBioquantUnixUser = "unixUser2"

        def paths = [
            rootPath: "${rootPath}",
            processingRootPath: "${processingRootPath}",
            programsRootPath: '/',
            loggingRootPath: "${loggingRootPath}"
        ]

        // Realms for testing on DKFZ
        realm = DomainFactory.createRealmDataManagementDKFZ(paths)
        assertNotNull(realm.save(flush: true))

        realm = DomainFactory.createRealmDataProcessingDKFZ(paths)
        assertNotNull(realm.save(flush: true))

        /*
         // Realms for testing on BioQuant
         realm = new Realm(
         name: "BioQuant",
         env: Environment.getCurrent().getName(),
         operationType: Realm.OperationType.DATA_PROCESSING,
         cluster: Realm.Cluster.DKFZ, // Data processing for BQ projects is done on DKFZ, this is correct.
         rootPath:           bqRootPath,
         processingRootPath: dkfzProcessingPath,
         programsRootPath: realmProgramsRootPath,
         webHost: realmWebHost,
         host: 'headnode',
         port: 22,
         unixUser: realmDKFZUnixUser,
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
         programsRootPath: realmProgramsRootPath,
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
         programsRootPath: realmProgramsRootPath,
         webHost: realmWebHost,
         host: 'headnode',
         port: 22,
         unixUser: realmDKFZUnixUser,
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
         programsRootPath: realmProgramsRootPath,
         webHost: realmWebHost,
         host: 'headnode',
         port: 22,
         unixUser: realmDKFZUnixUser,
         timeout: realmTimeout,
         pbsOptions: realmPbsOptionsDKFZ
         )
         assertNotNull(realm.save(flush: true))
         */

        Project project = new Project(
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

        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = new ReferenceGenomeProjectSeqType(
                        project: project,
                        seqType: seqType,
                        referenceGenome: referenceGenome,
                        )
        assertNotNull(referenceGenomeProjectSeqType.save([flush: true, failOnError: true]))

        SeqPlatform seqPlatform = new SeqPlatform(
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

        ExomeEnrichmentKit exomeEnrichmentKit  = new ExomeEnrichmentKit(
                        name: "exomeEnrichmentKit"
                        )
        assertNotNull(exomeEnrichmentKit.save([flush: true, failOnError: true]))

        BedFile bedFile = new BedFile(
                        fileName: "bedFileName",
                        targetSize: 66,
                        mergedTargetSize: 50,
                        referenceGenome: referenceGenome,
                        exomeEnrichmentKit: exomeEnrichmentKit
                        )
        assertNotNull(bedFile.save([flush: true, failOnError: true]))

        ExomeSeqTrack seqTrack = new ExomeSeqTrack(
                        laneId: "laneId",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool,
                        exomeEnrichmentKit: exomeEnrichmentKit,
                        kitInfoReliability: InformationReliability.KNOWN
                        )
        assertNotNull(seqTrack.save([flush: true, failOnError: true]))

        FileType fileType = new FileType(
                        type: Type.SEQUENCE
                        )
        assertNotNull(fileType.save([flush: true, failOnError: true]))

        DataFile dataFile = new DataFile(
                        fileName: "dataFile1",
                        seqTrack: seqTrack,
                        fileType: fileType
                        )
        assertNotNull(dataFile.save([flush: true, failOnError: true]))

        DataFile dataFile1 = new DataFile(
                        fileName: "dataFile2",
                        seqTrack: seqTrack,
                        fileType: fileType
                        )
        assertNotNull(dataFile1.save([flush: true, failOnError: true]))

        AlignmentPass alignmentPass = new AlignmentPass(
                        identifier: 0,
                        seqTrack: seqTrack,
                        description: "test"
                        )
        assertNotNull(alignmentPass.save([flush: true, failOnError: true]))

        ProcessedBamFile processedBamFile = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED,
                        status: AbstractBamFile.State.PROCESSED,
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

        ExomeSeqTrack seqTrack1 = new ExomeSeqTrack(
                        laneId: "laneId1",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: softwareTool,
                        exomeEnrichmentKit: exomeEnrichmentKit,
                        kitInfoReliability: InformationReliability.INFERRED
                        )
        assertNotNull(seqTrack1.save([flush: true, failOnError: true]))

        DataFile dataFile2 = new DataFile(
                        fileName: "dataFile3",
                        seqTrack: seqTrack1,
                        fileType: fileType
                        )
        assertNotNull(dataFile2.save([flush: true, failOnError: true]))

        DataFile dataFile3 = new DataFile(
                        fileName: "dataFile4",
                        seqTrack: seqTrack1,
                        fileType: fileType
                        )
        assertNotNull(dataFile3.save([flush: true, failOnError: true]))

        AlignmentPass alignmentPass1 = new AlignmentPass(
                        identifier: 0,
                        seqTrack: seqTrack1,
                        description: "test"
                        )
        assertNotNull(alignmentPass1.save([flush: true, failOnError: true]))

        ProcessedBamFile processedBamFile1 = new ProcessedBamFile(
                        alignmentPass: alignmentPass1,
                        type: BamType.SORTED,
                        status: AbstractBamFile.State.PROCESSED,
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

        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                        sample: sample,
                        seqType: seqType
                        )
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
            filePathBamFileQA1,
            filePathBamFileQA2,
            filePathMergedBamFileQA1,
            filePathMergedBamFileQA2
        ].collect { "mkdir -p ${it}" }.join " && "

        List<String> files = [
            fileNameMergedBamFile1,
            fileNameMergedBamFile2,
            fileNameBaiFile1,
            fileNameBaiFile2,
            fileNameBamFileQA1,
            fileNameBamFileQA2,
            fileNameMergedBamFileQA1,
            fileNameMergedBamFileQA2
        ]

        String cmdBuildFileStructure = files.collect {"echo -n \"${it}\" > ${it}"}.join " && "

        // Call "sync" to block termination of script until I/O is done
        executionService.executeCommand(realm, "${cmdCleanUp}; ${cmdBuildDirStructure} && ${cmdBuildFileStructure} && sync")
        checkFiles(files)
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
            percentReadPairsMapToDiffChrom: 0.11828206222955773,
            onTargetMappedBases: 50,
            allBasesMapped: 66
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
     * Pauses the test until the workflow is finished or the timeout is reached
     * @return true if the process is finished, false otherwise
     */
    boolean waitUntilWorkflowIsOverOrTimeout(int timeout) {
        println "Started to wait (until workflow is over or timeout)"
        int timeCount = 0
        boolean finished = false
        while (!finished && (timeCount < timeout)) {
            finished = areAllProcessFinished()
            println "waiting ... "
            timeCount++
            sleep(60000)
        }
        return finished
    }

    /**
     * return true if all processed are finished
     */
    boolean areAllProcessFinished() {
        List<Process> processes = Process.list()
        boolean finished = false
        if (processes.size() > 0) {
            processes*.refresh()
            List<Process> processesFinished = Process.createCriteria().list {
                eq("finished", true)
            }
            if (processesFinished.size() == processes.size()) {
                finished = true
            }
        }
        return finished
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
        executionService.executeCommand(realm, cleanUpTestFoldersCommand())
        realm = null
    }

    /**
     * Test execution of the workflow without any processing options defined
     */
    @Ignore
    @Test
    void testExecutionWithoutProcessingOptions() {
        // Import workflow from script file
        run("scripts/TransferMergedBamFileWorkflow.groovy")

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
        boolean workflowFinishedSucessfully = waitUntilWorkflowIsOverOrTimeout(SLEEPING_TIME_IN_MINUTES)
        assertTrue(workflowFinishedSucessfully)
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
        // has to wait, since the Transfer workflow checks only every minute if there are new files with status NEEDS_PROCESSING
        // without waiting no new process would be in the process list when it is checked
        Thread.currentThread().sleep(120000)
        workflowFinishedSucessfully = waitUntilWorkflowIsOverOrTimeout(SLEEPING_TIME_IN_MINUTES)
        assertTrue(workflowFinishedSucessfully)
        assertEquals(fileNameMergedBamFile2, mergedBamFile.getText())
        checkNumberOfStoredMd5sums(2)
        checkDestinationFileStructure()

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
            "${destinationDirMergedBamFile}/control_pid_1_EXON_PAIRED_merged.mdup.bai",
            "${destinationDirMergedBamFile}/control_pid_1_EXON_PAIRED_merged.mdup.bam.md5sum",
            "${destinationDirMergedBamFile}/control_pid_1_EXON_PAIRED_merged.mdup.bai.md5sum",
            "${destinationDirQaResults}/MD5SUMS",
            "${destinationDirQaResults}/plot.jpg",
            "${destinationDirQaResults}runName_laneId/plot.jpg",
            "${destinationDirQaResults}runName_laneId1/plot.jpg",
            fastqFilesInMergedBamFile
        ])
    }

    void checkFiles(List paths) {
        paths.each {
            File file = new File(it)
            assertTrue(file.canRead())
            assertTrue(file.size() > 0)
        }
    }
}
