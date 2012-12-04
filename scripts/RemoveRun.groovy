
import de.dkfz.tbi.otp.ngsdata.*

Run run = Run.findByName("PROJECT_NAME_Exome_Data_batch14")

List<DataFile> files = DataFile.findAllByRun(run)
files.each { DataFile file ->
    List<MetaDataEntry> entries = MetaDataEntry.findAllByDataFile(file)
    entries.each { MetaDataEntry entry ->
        entry.delete(flush: true)
    }
    file.delete(flush: true)
}

List<SeqTrack> tracks = SeqTrack.findAllByRun(run)
tracks.each { SeqTrack track ->
    MergingAssignment ma = MergingAssignment.findBySeqTrack(track)
    SeqScan scan = ma.seqScan
    ma.delete(flush: true)

    List<MergingAssignment> mas = MergingAssignment.findAllBySeqScan(scan)
    mas.each {
        it.delete(flush: true)
    }
    scan.delete(flush: true)
    track.delete(flush: true)
}

List<RunSegment> segments = RunSegment.findAllByRun(run)
segments.each { RunSegment segment ->
    List<MetaDataFile> mdFiles = MetaDataFile.findAllByRunSegment(segment)
    mdFiles.each {
        it.delete(flush: true)
    }
    segment.delete(flush: true)
}

run.delete(flush: true)


