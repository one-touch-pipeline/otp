import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

SeqTrack.withTransaction {
    final List<SeqTrack> seqTracks = SeqTrack.withCriteria {
        'in'("id", [
                // the ids
        ] as long[])
    }
    seqTracks.each {SeqTrack seqTrack ->
        assert seqTrack
        assert seqTrack.fastqcState == SeqTrack.DataProcessingState.IN_PROGRESS
        println "Seqtrack: ${seqTrack}"
        DataFile.findAllBySeqTrack(seqTrack).each {
            File file = ctx.fastqcDataFilesService.fastqcOutputFile(it) as File
            println "fastqc file: ${file}  ${file.exists()}"

            final List<FastqcProcessedFile> fastqcProcessedFiles = FastqcProcessedFile.findAllByDataFile(it)
            fastqcProcessedFiles*.delete(flush: true)
        }
        seqTrack.fastqcState = SeqTrack.DataProcessingState.NOT_STARTED
        seqTrack.save(flush: true)
    }
    assert false
}
println ''
