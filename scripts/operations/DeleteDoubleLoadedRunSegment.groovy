import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

/**
 * Script to delete a runsegment if it was loaded twice.
 */

long runSegmentId = 0




RunSegment.withTransaction {
    RunSegment runSegment = RunSegment.get(runSegmentId)
    assert runSegment
    DataFile.findAllByRunSegment(runSegment).each { DataFile dataFile ->
        MetaDataEntry.findAllByDataFile(dataFile).each { MetaDataEntry entry ->
            entry.delete(flush: true)
        }
        assert MetaDataEntry.countByDataFile(dataFile) == 0
        CollectionUtils.exactlyOneElement(FastqcProcessedFile.findAllByDataFile(dataFile)).delete()
        SeqTrack seqTrack = dataFile.seqTrack
        dataFile.delete(flush: true)
        if (!seqTrack.dataFiles) {
            RoddyBamFile roddyBamFile = CollectionUtils.atMostOneElement(RoddyBamFile.createCriteria().list {
                seqTracks {
                    eq('id', seqTrack.id)
                }
            })
            if (roddyBamFile) {
                assert roddyBamFile.seqTracks.remove(seqTrack)
                roddyBamFile.numberOfMergedLanes--
                assert roddyBamFile.save(flush: true)
            }
            MergingAssignment.findBySeqTrack(seqTrack)*.delete()
            seqTrack.delete(flush: true)
        }
    }
    assert DataFile.countByRunSegment(runSegment) == 0
    MetaDataFile.findAllByRunSegment(runSegment).each {
        it.delete(flush: true)
    }
    assert MetaDataFile.countByRunSegment(runSegment) == 0
    assert RunSegment.get(runSegmentId) == runSegment
    runSegment.delete()
    assert RunSegment.get(runSegmentId) == null
    assert false
}
''
