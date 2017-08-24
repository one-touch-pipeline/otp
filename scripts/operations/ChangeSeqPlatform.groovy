/*
 * Change the SeqPlatform of a Run and its SeqTracks.
 */


import de.dkfz.tbi.otp.dataprocessing.MergingCriteria
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

def runName =         ''
def currentPlatform = [platform: 'Illumina', model: 'HiSeq ', kit: 'V']
def newPlatform =     [platform: 'Illumina', model: 'HiSeq ', kit: 'V']

Run.withTransaction {
    SeqPlatform currentSeqPlatform = ctx.seqPlatformService.findSeqPlatform(
            currentPlatform.platform, currentPlatform.model, currentPlatform.kit)
    assert currentSeqPlatform

    SeqPlatform newSeqPlatform = ctx.seqPlatformService.findSeqPlatform(
            newPlatform.platform, newPlatform.model, newPlatform.kit)
    assert newSeqPlatform

    Run run = exactlyOneElement(Run.findAllByName(runName))
    assert run.seqPlatform == currentSeqPlatform

    SeqTrack.findAllByRun(run).each { SeqTrack st ->
        assert newSeqPlatform.getSeqPlatformGroup(st.project, st.seqType) == currentSeqPlatform.getSeqPlatformGroup(st.project, st.seqType)
    }

    run.seqPlatform = newSeqPlatform
    assert run.save(flush: true)
}
