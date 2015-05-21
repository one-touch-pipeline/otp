package workflows

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

import static org.junit.Assert.assertNotNull

class QualityAssessmentMergedWorkflowTests extends QualityAssessmentAbstractWorkflowTests {

    ProcessedMergedBamFileService processedMergedBamFileService
    ProcessingOptionService processingOptionService


    protected Map inputFilesPath() {
        ProcessedMergedBamFile bamFile = ProcessedMergedBamFile.list().first()
        Map result = [:]
        result.path = processedMergedBamFileService.directory(bamFile)
        result.filePath = processedMergedBamFileService.filePath(bamFile)
        result.baiFilePath = processedMergedBamFileService.filePathForBai(bamFile)
        return result
    }

    protected void createAdditionalTestData(List<SeqTrack> seqTracks) {

        SeqType seqType = seqTracks.first().seqType
        ReferenceGenome referenceGenome = createReferenceGenome(Project.list().first(), seqType)

        MergingSet mergingSet
        MergingPass mergingPass

        seqTracks.eachWithIndex { SeqTrack seqTrack, int i ->
            seqTrack.laneId = "laneId$i"
            seqTrack.run = Run.list().first()
            seqTrack.sample = Sample.list().first()
            seqTrack.seqPlatform = SeqPlatform.list().first()
            seqTrack.pipelineVersion = SoftwareTool.list().first()
            assertNotNull(seqTrack.save([flush: true]))

            if (i == 0) {
                MergingWorkPackage mergingWorkPackage = TestData.findOrSaveMergingWorkPackage(seqTrack, referenceGenome)
                assertNotNull(mergingWorkPackage.save([flush: true]))

                mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage,
                        status: MergingSet.State.PROCESSED
                )
                assertNotNull(mergingSet.save([flush: true]))

                mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                )
                assertNotNull(mergingPass.save([flush: true]))
            }

            AlignmentPass alignmentPass = TestData.createAndSaveAlignmentPass(
                            referenceGenome: referenceGenome,
                            identifier: i,
                            seqTrack: seqTrack,
                            description: "test"
                            )
            assertNotNull(alignmentPass.save([flush: true]))

            ProcessedBamFile processedBamFile = new ProcessedBamFile(
                            alignmentPass: alignmentPass,
                            type: AbstractBamFile.BamType.SORTED,
                            status: AbstractBamFile.State.NEEDS_PROCESSING,
                            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.FINISHED
                            )
            assertNotNull(processedBamFile.save([flush: true]))

            MergingSetAssignment mergingSetAssignment = new MergingSetAssignment(
                            mergingSet: mergingSet,
                            bamFile: processedBamFile
                            )
            assertNotNull(mergingSetAssignment.save([flush: true]))
        }

        ProcessedMergedBamFile processedMergedBamFile = new ProcessedMergedBamFile(
                        mergingPass: mergingPass,
                        fileExists: true,
                        type: AbstractBamFile.BamType.MDUP,
                        status: AbstractBamFile.State.PROCESSED,
                        qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.NOT_STARTED,
                        numberOfMergedLanes: 1,
                        )
        assertNotNull(processedMergedBamFile.save([flush: true]))
    }

    @Override
    List<String> getWorkflowScripts() {
        return ['scripts/qa/QualityAssessmentMergedWorfklow.groovy', 'scripts/qa/InjectQualityAssessmentMergedWorkflowOptions.groovy']
    }
}
