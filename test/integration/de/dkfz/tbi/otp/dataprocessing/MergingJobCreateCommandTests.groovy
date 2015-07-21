package de.dkfz.tbi.otp.dataprocessing

import static org.junit.Assert.*

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.job.jobs.merging.MergingJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.Run.StorageRealm
import de.dkfz.tbi.otp.ngsdata.SoftwareTool.Type
import org.junit.*

class MergingJobCreateCommandTests {

    TestData testData = new TestData()
    MergingJob mergingJob
    ProcessedMergedBamFile processedMergedBamFile
    MergingSet mergingSet
    Sample sample
    ProcessedBamFile processedBamFile
    SeqPlatform seqPlatform
    SeqType seqType
    MergingPass mergingPass

    String basePath
    String basePathAlignment
    String basePathMerging
    String basePathMergingOutput

    static final String SEQ_TYPE_NAME = 'TestSeqType'
    static final String LIBRARY_LAYOUT_NAME = 'TestLibraryLayout'

    @Before
    void setUp() {
        Realm realm = DomainFactory.createRealmDataProcessingDKFZ()
        assertNotNull(realm.save([flush: true, failOnError: true]))

        basePath = "${realm.processingRootPath}/dirName/results_per_pid/pid_1"
        basePathAlignment = "${basePath}/alignment"
        basePathMerging = "${basePath}/merging"
        basePathMergingOutput = "${basePathMerging}//name_1/${SEQ_TYPE_NAME}/${LIBRARY_LAYOUT_NAME}/DEFAULT/0/pass0"

        seqPlatform = SeqPlatform.build()

        seqType = new SeqType(
                        name: SEQ_TYPE_NAME,
                        libraryLayout: LIBRARY_LAYOUT_NAME,
                        dirName: "dir"
                        )
        assertNotNull(seqType.save([flush: true, failOnError: true]))

        Project project = TestData.createProject(
                        name: "name_1",
                        dirName: "dirName",
                        realmName: realm.name
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
                        name: "name_1"
                        )
        assertNotNull(sampleType.save([flush: true, failOnError: true]))

        sample = new Sample(
                        individual: individual,
                        sampleType: sampleType
                        )
        assertNotNull(sample.save([flush: true, failOnError: true]))

        MergingWorkPackage mergingWorkPackage = testData.createMergingWorkPackage(
                        sample: sample,
                        seqType: seqType,
                        seqPlatformGroup: seqPlatform.seqPlatformGroup,
                        )
        assertNotNull(mergingWorkPackage.save([flush: true, failOnError: true]))

        mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage
                        )
        assertNotNull(mergingSet.save([flush: true, failOnError: true]))

        mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true, failOnError: true]))

        processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass)
        assertNotNull(processedMergedBamFile.save([flush: true, failOnError: true]))

        MergingSetAssignment mergingSetAssignment = createMergingSetAssignment("1")

        ProcessingOption processingOption = new ProcessingOption(
                        name: "picardJavaSetting",
                        type: null,
                        value: "JAVA_OPTIONS=-Xmx50G",
                        project: null,
                        comment: "Java options for Picard"
                        )
        assertNotNull(processingOption.save([flush: true, failOnError: true]))

        ProcessingOption processingOption1 = new ProcessingOption(
                        name: "picardMdup",
                        type: null,
                        value: "VALIDATION_STRINGENCY=SILENT REMOVE_DUPLICATES=FALSE ASSUME_SORTED=TRUE MAX_RECORDS_IN_RAM=12500000 CREATE_INDEX=TRUE",
                        project: null,
                        comment: "picard option used in duplicates removal"
                        )
        assertNotNull(processingOption1.save([flush: true, failOnError: true]))
    }

    @After
    void tearDown() {
        basePath = basePathAlignment = basePathMerging = basePathMergingOutput = null
        processedMergedBamFile = null
        mergingSet = null
        sample = null
        processedBamFile = null
        seqPlatform = null
        seqType = null
        mergingPass = null
    }

    @Test(expected = NullPointerException)
    void testCreateCommandProcessedMergedBamFileIsNull() {
        mergingJob.createCommand(null)
    }

    @Test
    void testCreateCommandOneBamFile() {
        String tempDirExp = "\${PBS_SCRATCH_DIR}/\${PBS_JOBID}"
        String createTempDirExp = "mkdir -p -m 2750 ${tempDirExp}"
        String javaOptionsExp = "JAVA_OPTIONS=-Xmx50G"
        String picardExp = "picard.sh MarkDuplicates"
        String inputFilePathExp = " I=${basePathAlignment}//run_1_laneId_1/pass1/name_1_run_1_s_laneId_1_${LIBRARY_LAYOUT_NAME}.sorted.bam"
        String outputFilePathExp = "${basePathMergingOutput}/name_1_pid_1_${SEQ_TYPE_NAME}_${LIBRARY_LAYOUT_NAME}_merged.mdup.bam"
        String metricsPathExp = "${basePathMergingOutput}/name_1_pid_1_${SEQ_TYPE_NAME}_${LIBRARY_LAYOUT_NAME}_merged.mdup_metrics.txt"
        String baiFilePath = "${basePathMergingOutput}/name_1_pid_1_${SEQ_TYPE_NAME}_${LIBRARY_LAYOUT_NAME}_merged.mdup.bai"
        String picardFilesExp = "${inputFilePathExp} OUTPUT=${outputFilePathExp} METRICS_FILE=${metricsPathExp} TMP_DIR=${tempDirExp}"
        String picardOptionsExp = "VALIDATION_STRINGENCY=SILENT REMOVE_DUPLICATES=FALSE ASSUME_SORTED=TRUE MAX_RECORDS_IN_RAM=12500000 CREATE_INDEX=TRUE"
        String chmodExp = "chmod 440 ${outputFilePathExp} ${metricsPathExp} ${baiFilePath}"
        String createCommandOutputExp = "${createTempDirExp}; ${javaOptionsExp}; ${picardExp} ${picardFilesExp} ${picardOptionsExp}; ${chmodExp}"
        String createCommandOutputAct = mergingJob.createCommand(processedMergedBamFile)
        assertEquals(createCommandOutputExp, createCommandOutputAct)
    }

    /**
     * Merging set contains two {@link ProcessedBamFile}s.
     */
    @Test
    void testCreateCommandTwoBamFiles() {
        createMergingSetAssignment("2")
        String tempDirExp = "\${PBS_SCRATCH_DIR}/\${PBS_JOBID}"
        String createTempDirExp = "mkdir -p -m 2750 ${tempDirExp}"
        String javaOptionsExp = "JAVA_OPTIONS=-Xmx50G"
        String picardExp = "picard.sh MarkDuplicates"
        String inputFilePathExp = " I=${basePathAlignment}//run_1_laneId_1/pass1/name_1_run_1_s_laneId_1_${LIBRARY_LAYOUT_NAME}.sorted.bam I=${basePathAlignment}//run_2_laneId_2/pass2/name_1_run_2_s_laneId_2_${LIBRARY_LAYOUT_NAME}.sorted.bam"
        String outputFilePathExp = "${basePathMergingOutput}/name_1_pid_1_${SEQ_TYPE_NAME}_${LIBRARY_LAYOUT_NAME}_merged.mdup.bam"
        String metricsPathExp = "${basePathMergingOutput}/name_1_pid_1_${SEQ_TYPE_NAME}_${LIBRARY_LAYOUT_NAME}_merged.mdup_metrics.txt"
        String baiFilePath = "${basePathMergingOutput}/name_1_pid_1_${SEQ_TYPE_NAME}_${LIBRARY_LAYOUT_NAME}_merged.mdup.bai"
        String picardFilesExp = "${inputFilePathExp} OUTPUT=${outputFilePathExp} METRICS_FILE=${metricsPathExp} TMP_DIR=${tempDirExp}"
        String picardOptionsExp = "VALIDATION_STRINGENCY=SILENT REMOVE_DUPLICATES=FALSE ASSUME_SORTED=TRUE MAX_RECORDS_IN_RAM=12500000 CREATE_INDEX=TRUE"
        String chmodExp = "chmod 440 ${outputFilePathExp} ${metricsPathExp} ${baiFilePath}"
        String createCommandOutputExp = "${createTempDirExp}; ${javaOptionsExp}; ${picardExp} ${picardFilesExp} ${picardOptionsExp}; ${chmodExp}"
        String createCommandOutputAct = mergingJob.createCommand(processedMergedBamFile)
        assertEquals(createCommandOutputExp, createCommandOutputAct)
    }

    /**
     * Merging set contains one {@link ProcessedBamFile} and one {@link ProcessedMergedBamFile}.
     */
    @Test
    void testCreateCommandBamFileWithProcessedMergedBamFile() {
        final ProcessedMergedBamFile pmbf = createMergedMergingSetAssignment(mergingSet, 2).bamFile  // Any dummy merging set could be used as the argument instead of mergingSet.
        String tempDirExp = "\${PBS_SCRATCH_DIR}/\${PBS_JOBID}"
        String createTempDirExp = "mkdir -p -m 2750 ${tempDirExp}"
        String javaOptionsExp = "JAVA_OPTIONS=-Xmx50G"
        String picardExp = "picard.sh MarkDuplicates"
        String inputFilePathExp = " I=${basePathAlignment}//run_1_laneId_1/pass1/name_1_run_1_s_laneId_1_${LIBRARY_LAYOUT_NAME}.sorted.bam I=${basePathMerging}//name_1/${SEQ_TYPE_NAME}/${LIBRARY_LAYOUT_NAME}/DEFAULT/0/pass${pmbf.mergingPass.identifier}/name_1_pid_1_${SEQ_TYPE_NAME}_${LIBRARY_LAYOUT_NAME}_merged.mdup.bam"
        String outputFilePathExp = "${basePathMergingOutput}/name_1_pid_1_${SEQ_TYPE_NAME}_${LIBRARY_LAYOUT_NAME}_merged.mdup.bam"
        String metricsPathExp = "${basePathMergingOutput}/name_1_pid_1_${SEQ_TYPE_NAME}_${LIBRARY_LAYOUT_NAME}_merged.mdup_metrics.txt"
        String baiFilePath = "${basePathMergingOutput}/name_1_pid_1_${SEQ_TYPE_NAME}_${LIBRARY_LAYOUT_NAME}_merged.mdup.bai"
        String picardFilesExp = "${inputFilePathExp} OUTPUT=${outputFilePathExp} METRICS_FILE=${metricsPathExp} TMP_DIR=${tempDirExp}"
        String picardOptionsExp = "VALIDATION_STRINGENCY=SILENT REMOVE_DUPLICATES=FALSE ASSUME_SORTED=TRUE MAX_RECORDS_IN_RAM=12500000 CREATE_INDEX=TRUE"
        String chmodExp = "chmod 440 ${outputFilePathExp} ${metricsPathExp} ${baiFilePath}"
        String createCommandOutputExp = "${createTempDirExp}; ${javaOptionsExp}; ${picardExp} ${picardFilesExp} ${picardOptionsExp}; ${chmodExp}"
        String createCommandOutputAct = mergingJob.createCommand(processedMergedBamFile)
        assertEquals(createCommandOutputExp, createCommandOutputAct)
    }

    /**
     * Merging set contains two {@link ProcessedBamFile}s and one {@link ProcessedMergedBamFile}.
     */
    @Test
    void testCreateCommandTwoBamFileWithProcessedMergedBamFile() {
        createMergingSetAssignment("2")
        final ProcessedMergedBamFile pmbf = createMergedMergingSetAssignment(mergingSet, 3).bamFile  // Any dummy merging set could be used as the argument instead of mergingSet.
        String tempDirExp = "\${PBS_SCRATCH_DIR}/\${PBS_JOBID}"
        String createTempDirExp = "mkdir -p -m 2750 ${tempDirExp}"
        String javaOptionsExp = "JAVA_OPTIONS=-Xmx50G"
        String picardExp = "picard.sh MarkDuplicates"
        String inputFilePathExp = " I=${basePathAlignment}//run_1_laneId_1/pass1/name_1_run_1_s_laneId_1_${LIBRARY_LAYOUT_NAME}.sorted.bam I=${basePathAlignment}//run_2_laneId_2/pass2/name_1_run_2_s_laneId_2_${LIBRARY_LAYOUT_NAME}.sorted.bam I=${basePathMerging}//name_1/${SEQ_TYPE_NAME}/${LIBRARY_LAYOUT_NAME}/DEFAULT/0/pass${pmbf.mergingPass.identifier}/name_1_pid_1_${SEQ_TYPE_NAME}_${LIBRARY_LAYOUT_NAME}_merged.mdup.bam"
        String outputFilePathExp = "${basePathMergingOutput}/name_1_pid_1_${SEQ_TYPE_NAME}_${LIBRARY_LAYOUT_NAME}_merged.mdup.bam"
        String metricsPathExp = "${basePathMergingOutput}/name_1_pid_1_${SEQ_TYPE_NAME}_${LIBRARY_LAYOUT_NAME}_merged.mdup_metrics.txt"
        String baiFilePath = "${basePathMergingOutput}/name_1_pid_1_${SEQ_TYPE_NAME}_${LIBRARY_LAYOUT_NAME}_merged.mdup.bai"
        String picardFilesExp = "${inputFilePathExp} OUTPUT=${outputFilePathExp} METRICS_FILE=${metricsPathExp} TMP_DIR=${tempDirExp}"
        String picardOptionsExp = "VALIDATION_STRINGENCY=SILENT REMOVE_DUPLICATES=FALSE ASSUME_SORTED=TRUE MAX_RECORDS_IN_RAM=12500000 CREATE_INDEX=TRUE"
        String chmodExp = "chmod 440 ${outputFilePathExp} ${metricsPathExp} ${baiFilePath}"
        String createCommandOutputExp = "${createTempDirExp}; ${javaOptionsExp}; ${picardExp} ${picardFilesExp} ${picardOptionsExp}; ${chmodExp}"
        String createCommandOutputAct = mergingJob.createCommand(processedMergedBamFile)
        assertEquals(createCommandOutputExp, createCommandOutputAct)
    }

    private MergingSetAssignment createMergingSetAssignment(String identifier) {
        SeqCenter seqCenter = new SeqCenter(
                        name: "seqCenter_name_${identifier}",
                        dirName: "seqCenter_dir_${identifier}"
                        )
        assertNotNull(seqCenter.save([flush: true, failOnError: true]))

        SoftwareTool pipelineVersion = new SoftwareTool(
                        programName: "softwareTool_name_${identifier}",
                        type: Type.ALIGNMENT
                        )
        assertNotNull(pipelineVersion.save([flush: true, failOnError: true]))

        Run run = new Run(
                        name: "run_${identifier}",
                        seqCenter: seqCenter,
                        seqPlatform: seqPlatform,
                        storageRealm: StorageRealm.DKFZ
                        )
        assertNotNull(run.save([flush: true, failOnError: true]))

        SeqTrack seqTrack = new SeqTrack(
                        laneId: "laneId_${identifier}",
                        run: run,
                        sample: sample,
                        seqType: seqType,
                        seqPlatform: seqPlatform,
                        pipelineVersion: pipelineVersion
                        )
        assertNotNull(seqTrack.save([flush: true, failOnError: true]))

        AlignmentPass alignmentPass = testData.createAlignmentPass(
                        identifier: identifier,
                        seqTrack: seqTrack
                        )
        assertNotNull(alignmentPass.save([flush: true, failOnError: true]))

        processedBamFile = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: BamType.SORTED
                        )
        assertNotNull(processedBamFile.save([flush: true, failOnError: true]))

        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                        mergingSet: mergingSet,
                        bamFile: processedBamFile
                        )
        assertNotNull(mergingSetAssignment.save([flush: true, failOnError: true]))
    }

    /**
     * Creates a new {@link MergingPass} and a new {@link ProcessedMergedBamFile} and assigns the latter to
     * {@link #mergingSet}.
     *
     * @param mergingSet The merging set that the new merging pass belongs to.
     * @param identifier The identifier of the new merging pass.
     */
    private MergingSetAssignment createMergedMergingSetAssignment(MergingSet mergingSet, int identifier) {
        MergingPass mergingPass1 = new MergingPass(
                identifier: identifier,
                mergingSet: mergingSet
        )
        assertNotNull(mergingPass1.save([flush: true, failOnError: true]))

        ProcessedMergedBamFile processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingPass1)
        assertNotNull(processedMergedBamFile.save([flush: true, failOnError: true]))

        MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                        mergingSet: this.mergingSet,
                        bamFile: processedMergedBamFile
                        )
        assertNotNull(mergingSetAssignment.save([flush: true, failOnError: true]))

        return mergingSetAssignment
    }
}
