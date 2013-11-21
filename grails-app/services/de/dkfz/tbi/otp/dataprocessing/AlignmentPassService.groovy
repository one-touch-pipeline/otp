package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * Execution of the alignment on the particular data file is called AlignmentPass
 * AlignmentPass object is a process parameter of any AlignmentWorkflow
 * and AlignmentPassService is responsible to create and update this object
 *
 *
 */
class AlignmentPassService {

    def configService
    def processingOptionService
    ReferenceGenomeService referenceGenomeService
    QualityAssessmentPassService qualityAssessmentPassService

    /**
     * Returns any SeqTrack with at least one data file which is not withdrawn. If no such SeqTrack
     * exists, <code>null</code> is returned.
     */
    private SeqTrack findNotStartedSeqTrackWithNonWithdrawnDataFile() {
        def state = SeqTrack.DataProcessingState.NOT_STARTED
        DataFile dataFile = DataFile.createCriteria().get {
            eq("fileWithdrawn", false)
            seqTrack {
                eq("alignmentState", state)
            }
        }
        return dataFile?.seqTrack
    }

    public AlignmentPass createAlignmentPass() {
        SeqTrack seqTrack = findNotStartedSeqTrackWithNonWithdrawnDataFile()
        if (!seqTrack) {
            return null
        }
        int pass = AlignmentPass.countBySeqTrack(seqTrack)
        AlignmentPass alignmentPass = new AlignmentPass(identifier: pass, seqTrack: seqTrack)
        assert(alignmentPass.save(flush: true))
        return alignmentPass
    }

    public void alignmentPassStarted(AlignmentPass alignmentPass) {
        update(alignmentPass, SeqTrack.DataProcessingState.IN_PROGRESS)
    }

    public void alignmentPassFinished(AlignmentPass alignmentPass) {
        notNull(alignmentPass, "the alignmentPass for the method alignmentPassFinished ist null")
        update(alignmentPass, SeqTrack.DataProcessingState.FINISHED)
        ProcessedBamFile bamFile = ProcessedBamFile.findByAlignmentPass(alignmentPass)
        qualityAssessmentPassService.notStarted(bamFile)
    }

    private void update(AlignmentPass alignmentPass, SeqTrack.DataProcessingState state) {
        notNull(alignmentPass, "The input alignmentPass of the method update is null")
        notNull(state, "The input state of the method update is null")
        SeqTrack seqTrack = alignmentPass.seqTrack
        seqTrack.alignmentState = state
        assert(seqTrack.save(flush: true))
    }

    public Realm realmForDataProcessing(AlignmentPass alignmentPass) {
        return configService.getRealmDataProcessing(project(alignmentPass))
    }

    public Project project(AlignmentPass alignmentPass) {
        return alignmentPass.seqTrack.sample.individual.project
    }

    public SeqType seqType(AlignmentPass alignmentPass) {
        return alignmentPass.seqTrack.seqType
    }

    public String referenceGenomePath(AlignmentPass alignmentPass) {
        Project project = project(alignmentPass)
        SeqType seqType = seqType(alignmentPass)
        ReferenceGenome referenceGenome = referenceGenomeService.referenceGenome(project, seqType)
        String path = referenceGenomeService.fastaFilePath(project, referenceGenome)
        if (!path) {
            throw new ProcessingException("Undefined path to reference genome for project ${project.name}")
        }
        return path
    }

    public int maximalIdentifier(ProcessedBamFile processedBamFile) {
        notNull(processedBamFile, "the input bam file for the method maximalIdentifier is null")
        SeqTrack seqTrackPerBamFile = processedBamFile.alignmentPass.seqTrack
        int maxIdentifier = AlignmentPass.createCriteria().get {
            eq("seqTrack", seqTrackPerBamFile)
            projections{
                max("identifier")
            }
        }
        return maxIdentifier
    }
}
