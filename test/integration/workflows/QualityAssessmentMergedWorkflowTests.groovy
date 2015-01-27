package workflows

import static org.junit.Assert.*

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged.QualityAssessmentMergedStartJob

class QualityAssessmentMergedWorkflowTests extends QualityAssessmentAbstractWorkflowTests {

    QualityAssessmentMergedStartJob qualityAssessmentMergedStartJob
    ProcessedMergedBamFileService processedMergedBamFileService

    protected AbstractStartJobImpl getJob() {
        return qualityAssessmentMergedStartJob
    }

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

        MergingWorkPackage mergingWorkPackage = new MergingWorkPackage(
                        referenceGenome: referenceGenome,
                        sample: Sample.list().first(),
                        seqType: seqType
                        )
        assertNotNull(mergingWorkPackage.save([flush: true]))

        MergingSet mergingSet = new MergingSet(
                        identifier: 0,
                        mergingWorkPackage: mergingWorkPackage,
                        status: MergingSet.State.PROCESSED
                        )
        assertNotNull(mergingSet.save([flush: true]))

        MergingPass mergingPass = new MergingPass(
                        identifier: 0,
                        mergingSet: mergingSet
                        )
        assertNotNull(mergingPass.save([flush: true]))

        seqTracks.eachWithIndex { SeqTrack seqTrack, int i ->
            seqTrack.laneId = "laneId$i"
            seqTrack.run = Run.list().first()
            seqTrack.sample = Sample.list().first()
            seqTrack.seqPlatform = SeqPlatform.list().first()
            seqTrack.pipelineVersion = SoftwareTool.list().first()
            assertNotNull(seqTrack.save([flush: true]))

            AlignmentPass alignmentPass = new AlignmentPass(
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
                        qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.NOT_STARTED
                        )
        assertNotNull(processedMergedBamFile.save([flush: true]))
    }

    protected void createWorkflow() {
        run('scripts/qa/QualityAssessmentMergedWorfklow.groovy')
        SpringSecurityUtils.doWithAuth("admin") {
            run('scripts/qa/InjectQualityAssessmentMergedWorkflowOptions.groovy')
        }
    }
}
