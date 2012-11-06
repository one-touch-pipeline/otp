package de.dkfz.tbi.otp.ngsqc

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import org.springframework.security.access.prepost.PreAuthorize

class FastqcResultsService {

    def seqTrackService

/*
    @PreAuthorize('''
hasPermission(#dataFile.project.id, 'de.dkfz.tbi.otp.ngsdata.DataFile', read)
or hasPermission(#dataFile.run.id, 'de.dkfz.tbi.otp.ngsdata.Run', read)
or hasRole('ROLE_OPERATOR')
''')
*/
    public boolean isFastqcAvailable(DataFile dataFile) {
        return FastqcProcessedFile.findByDataFileAndContentUploaded(dataFile, true)
    }

    // ACL: access to dataFile
    public FastqcProcessedFile fastqcProcessedFile(DataFile dataFile) {
        return FastqcProcessedFile.findByDataFile(dataFile)
    }

    // Needs ACLs ?
    public boolean isFastqcAvailable(Run run) {
        return !fastqcFilesForRun(run).empty
    }

    // ACL ?
    public List<FastqcProcessedFile> fastqcFilesForRun(Run run) {
        def c = FastqcProcessedFile.createCriteria()
        List<FastqcProcessedFile> files = c.list {
            and {
                eq("contentUploaded", true)
                dataFile {
                    eq("run", run)
                }
            }
        }
        return files
    }

    // Needs for ACLs not clear
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
     * This function generates condensed overview of the fastqc results.
     * The function is documented due too usage of Map as output
     * @param Run run
     * @return map with the id of the dataFile as key and string with condensed info
     */
    // Needs for ACLs not clear
    public Map<Long, String> fastqcSummaryMap(Run run) {
        Map<Long, String> results = [:]
        for (FastqcProcessedFile fastqc in fastqcFilesForRun(run)) {
            results.put(fastqc.dataFile.id, fastqcSummaryForDataFile(fastqc))
        }
        return results
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
df.run.id in (:runIds)
AND df.seqTrack.id in (:seqTrackIds)
'''
        return DataFile.executeQuery(query, [runIds: sequences.collect{ it.runId }, seqTrackIds: sequences.collect{ it.seqTrackId }])
    }

    private String fastqcSummaryForDataFile(FastqcProcessedFile fastqc) {
        final List statusList = [
            FastqcModuleStatus.Status.PASS,
            FastqcModuleStatus.Status.WARN,
            FastqcModuleStatus.Status.FAIL
        ]
        List<Integer> N = []
        for (FastqcModuleStatus.Status status in statusList) {
            N << FastqcModuleStatus.countByFastqcProcessedFileAndStatus(fastqc, status)
        }
        return "(${N.get(0)}/${N.get(1)}/${N.get(2)})"
    }

    // Needs ACLs: prerequisite access to dataFile
    public List<FastqcModuleStatus> moduleStatusForDataFile(FastqcProcessedFile fastqc) {
        return FastqcModuleStatus.findAllByFastqcProcessedFile(fastqc)
    }

    // Needs ACLs: prerequisite access to dataFile
    public FastqcBasicStatistics basicStatisticsForDataFile(FastqcProcessedFile fastqc) {
        return FastqcBasicStatistics.findByFastqcProcessedFile(fastqc)
    }

    // Needs ACLs: prerequisite access to dataFile
    public List<FastqcKmerContent> kmerContentForDataFile(FastqcProcessedFile fastqc) {
        return FastqcKmerContent.findAllByFastqcProcessedFile(fastqc)
    }

    // Needs ACLs: prerequisite access to dataFile
    public List<FastqcOverrepresentedSequences> overrepresentedSequencesForDataFile(FastqcProcessedFile fastqc) {
        return FastqcOverrepresentedSequences.findAllByFastqcProcessedFile(fastqc)
    }
}
