package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.common.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.processing.Parameter

import org.springframework.beans.factory.annotation.Autowired

class FastqcJob extends AbstractJobImpl {

    @Autowired
    FastqcDataFilesService fastqcDataFilesService

    @Autowired
    ExecutionService executionService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    SeqTrackService seqTrackService

    @Override
    public void execute() throws Exception {
        SeqTrack seqTrack = SeqTrack.get(Long.parseLong(getProcessParameterValue()))
        String outDir = fastqcDataFilesService.fastqcOutputDirectory(seqTrack)
        Realm realm = fastqcDataFilesService.fastqcRealm(seqTrack)
        List<DataFile> seqFiles = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)

        List<String> pbsIDs = []
        seqFiles.each { seqFile ->
            String rawSeq = lsdfFilesService.getFileFinalPath(seqFile)
            String cmd = "fastqc ${rawSeq} --noextract --nogroup -o ${outDir};chmod -R 440 ${outDir}/*.zip"
            pbsIDs.add(sendScript(realm, cmd))
            fastqcDataFilesService.createFastqcProcessedFile(seqFile)
        }
        addOutputParameter("__pbsIds", pbsIDs.join(","))
        addOutputParameter("__pbsRealm", realm.id.toString())
    }

    private String sendScript(Realm realm, String text) {
        String pbsResponse = executionService.executeJob(realm, text)
        List<String> extractedPbsIds = executionService.extractPbsIds(pbsResponse)
        if (extractedPbsIds.size() != 1) {
            log.debug "Number of PBS jobs is = ${extractedPbsIds.size()}"
        }
        return extractedPbsIds.get(0)
    }
}
