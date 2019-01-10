package workflows.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.dataprocessing.singleCell.*
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import groovy.json.*
import org.junit.*

import java.time.*

@Ignore
@SuppressWarnings("JUnitTestMethodWithoutAssert")
class CellRangerAlignmentWorkflowTests extends AbstractAlignmentWorkflowTest implements CellRangerFactory {

    FileSystemService fileSystemService
    FileService fileService

    Sample sample
    SeqType seqType
    CellRangerMergingWorkPackage mwp


    List<String> fastqFiles = [
            "fastqFiles/10x/normal/paired/pbmc_1k_v3_S1_L001_R1_001.fastq.gz",
            "fastqFiles/10x/normal/paired/pbmc_1k_v3_S1_L001_R2_001.fastq.gz",
            "fastqFiles/10x/normal/paired/pbmc_1k_v3_S1_L002_R1_001.fastq.gz",
            "fastqFiles/10x/normal/paired/pbmc_1k_v3_S1_L002_R2_001.fastq.gz",
    ]

    @Before
    void setup() {
        Project project = createProject(realm: realm)
        Individual individual = DomainFactory.createIndividual(project: project)
        sample = createSample(individual: individual)

        seqType = createSeqType()

        ToolName toolName = createToolName(path: "cellranger")
        ReferenceGenome referenceGenome = createReferenceGenome(path: "hg_GRCh38")
        ReferenceGenomeIndex referenceGenomeIndex = createReferenceGenomeIndex(
                toolName: toolName,
                path: "1.2.0",
                referenceGenome: referenceGenome,
                indexToolVersion: "1.2.0",
        )

        ConfigPerProjectAndSeqType conf = createConfig(
                seqType: seqType,
                project: project,
                programVersion: "cellranger/3.0.1",
                referenceGenomeIndex: referenceGenomeIndex,
        )

        mwp = createMergingWorkPackage(
                needsProcessing: true,
                sample: sample,
                config: conf,
                expectedCells: 1000, //according to 10x
                referenceGenome: referenceGenome,
        )

        setUpRefGenomeDir(mwp, new File(referenceGenomeDirectory, 'hg_GRCh38'))

        DomainFactory.createMergingCriteriaLazy(project: project, seqType: seqType)

        findOrCreatePipeline()
    }

    SeqTrack createSeqTrack(String fastq1, String fastq2) {
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles(mwp, [
                seqType: seqType,
                dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
                sample: sample,
        ], [:], [:])

        DataFile.findAllBySeqTrack(seqTrack).eachWithIndex { DataFile dataFile, int index ->
            dataFile.vbpFileName = dataFile.fileName = "fastq_${seqTrack.individual.pid}_${seqTrack.sampleType.name}_${seqTrack.laneId}_${index + 1}.fastq.gz"
            dataFile.save(flush: true)
        }

        linkFastqFiles(seqTrack, [
                new File(getInputRootDirectory(), fastq1),
                new File(getInputRootDirectory(), fastq2),
        ])
        return seqTrack
    }

    void checkResults() {
        SingleCellBamFile singleCellBamFile = CollectionUtils.exactlyOneElement(SingleCellBamFile.all)

        assert singleCellBamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED
        assert singleCellBamFile.fileSize
        assert singleCellBamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
        assert singleCellBamFile.overallQualityAssessment
        assert singleCellBamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
    }


    @Test
    void testCellRanger_withOneLane() {
        given:
        SeqTrack seqTrack = createSeqTrack(fastqFiles[0], fastqFiles[1])
        mwp.seqTracks = [seqTrack]
        mwp.save(flush: true)

        when:
        execute()

        then:
        checkResults()
    }

    @Test
    void testCellRanger_withTwoLanes() {
        given:
        SeqTrack seqTrack1 = createSeqTrack(fastqFiles[0], fastqFiles[1])
        SeqTrack seqTrack2 = createSeqTrack(fastqFiles[2], fastqFiles[3])
        mwp.seqTracks = [seqTrack1, seqTrack2]
        mwp.save(flush: true)

        when:
        execute()

        then:
        checkResults()
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/CellRangerWorkflow.groovy",
        ]
    }

    @Override
    Duration getTimeout() {
        return Duration.ofHours(3)
    }

    @Override
    String getJobSubmissionOptions() {
        JsonOutput.toJson([
                (JobSubmissionOption.WALLTIME): Duration.ofHours(3).toString(),
                (JobSubmissionOption.MEMORY)  : "60g",
                (JobSubmissionOption.CORES)  : "16",
        ])
    }
}
