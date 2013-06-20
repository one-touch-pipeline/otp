package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*

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

    public AlignmentPass createAlignmentPass() {
        def state = SeqTrack.DataProcessingState.NOT_STARTED
        SeqTrack seqTrack = SeqTrack.findByAlignmentState(state)
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
        update(alignmentPass, SeqTrack.DataProcessingState.FINISHED)
    }

    private void update(AlignmentPass alignmentPass, SeqTrack.DataProcessingState state) {
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
        ReferenceGenome referenceGenome = referenceGenomeService.getReferenceGenome(project, seqType)
        String path = referenceGenomeService.filePathOnlySuffix(project, referenceGenome)
        if (!path) {
            throw new ProcessingException("Undefined path to reference genome for project ${project.name}")
        }
        return path
    }
}
