package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.ngsdata.*

class FastqcResultsService {

    SeqTrackService seqTrackService

    public boolean isFastqcAvailable(DataFile dataFile) {
        return FastqcProcessedFile.findByDataFileAndContentUploaded(dataFile, true)
    }

    public Map<Long, Boolean> fastqcLinkMap(Run run) {
        Map<Long, Boolean> map = [:]
        List<SeqTrack> seqTracks= SeqTrack.findAllByRun(run) // to be protected by ACLs
        seqTracks.each { SeqTrack seqTrack ->
            List<DataFile> files = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
            files.each { DataFile file ->
                map.put(file.id, isFastqcAvailable(file))
            }
        }
        return map
    }

    /**
     * Retrieves the FastQC {@link DataFile}s for the given {@link Sequence}s.
     *
     * The returned list includes the found DataFiles but does not order them by
     * Sequences. If a mapping to the Sequence is needed it's the task of the
     * callee to perform this operation.
     * @param sequences The Sequences for which the FastQC DataFiles should be retrieved
     * @return The FastQC DataFiles found by the Sequences
     */
    public List<DataFile> fastQCFiles(List<Sequence> sequences) {
        String query = '''
SELECT df FROM FastqcProcessedFile AS fqc
INNER JOIN fqc.dataFile AS df
WHERE
df.seqTrack.id in (:seqTrackIds)
'''
        return DataFile.executeQuery(query, [seqTrackIds: sequences.collect{ it.seqTrackId }])
    }
}
