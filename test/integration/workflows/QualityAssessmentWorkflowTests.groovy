package workflows

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsqc.FastqcBasicStatistics
import org.junit.Ignore

import static org.junit.Assert.assertNotNull

@Ignore
class QualityAssessmentWorkflowTests extends QualityAssessmentAbstractWorkflowTests {

    ProcessedBamFileService processedBamFileService


    protected Map inputFilesPath() {
        ProcessedBamFile bamFile = ProcessedBamFile.list().first()
        Map result = [:]
        result.path = processedBamFileService.getDirectory(bamFile)
        result.filePath = processedBamFileService.getFilePath(bamFile)
        result.baiFilePath = processedBamFileService.baiFilePath(bamFile)
        return result
    }

    protected void createAdditionalTestData(List<SeqTrack> seqTracks) {

        // in this test case we do not need 2 processed bam files
        SeqTrack seqTrack = seqTracks.first()
        Project project = Project.list().first()
        ReferenceGenome referenceGenome = createReferenceGenome(project, seqTrack.seqType)

        seqTrack.laneId = "laneId"
        seqTrack.run = Run.list().first()
        seqTrack.sample = Sample.list().first()
        seqTrack.seqPlatform = SeqPlatform.list().first()
        seqTrack.pipelineVersion = SoftwareTool.list().first()
        seqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED
        assertNotNull(seqTrack.save([flush: true]))

        FastqcBasicStatistics fastqcBasicStats
        2.times {
            DataFile dataFile = new DataFile(seqTrack: seqTrack)
            assertNotNull(dataFile.save(flush: true))

            FastqcProcessedFile fastqcProcessedFile = new FastqcProcessedFile(
                            contentUploaded: true,
                            dataFile: dataFile)
            assertNotNull(fastqcProcessedFile.save(flush: true))

            fastqcBasicStats = new FastqcBasicStatistics(
                            fileType: "fileType",
                            encoding: "encoding",
                            sequenceLength: "101",
                            fastqcProcessedFile: fastqcProcessedFile)
            assertNotNull(fastqcBasicStats.save(flush: true))
        }

        fastqcBasicStats.totalSequences = totalSequences

        AlignmentPass alignmentPass = TestData.createAndSaveAlignmentPass(
                        referenceGenome: referenceGenome,
                        identifier: 1,
                        seqTrack: seqTrack,
                        description: "test"
                        )
        assertNotNull(alignmentPass.save([flush: true]))

        ProcessedBamFile processedBamFile = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: AbstractBamFile.BamType.SORTED,
                        status: AbstractBamFile.State.NEEDS_PROCESSING,
                        qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.NOT_STARTED
                        )
        assertNotNull(processedBamFile.save([flush: true]))
    }

    @Override
    List<String> getWorkflowScripts() {
        return ['scripts/qa/QualityAssessmentWorkflow.groovy', 'scripts/qa/InjectQualityAssessmentWorkflowOptions.groovy']
    }
}
