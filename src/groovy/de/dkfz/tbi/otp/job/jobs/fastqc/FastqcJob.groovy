package de.dkfz.tbi.otp.job.jobs.fastqc

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.common.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class FastqcJob extends AbstractJobImpl {

    @Autowired
    FastqcDataFilesService fastqcDataFilesService

    @Autowired
    ExecutionHelperService executionHelperService

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
        FastqcProcessedFile.withTransaction {
            seqFiles.each { seqFile ->
                assert seqFile.fileExists && seqFile.fileSize > 0L
                String rawSeq = lsdfFilesService.getFileFinalPath(seqFile)
                String cmd = "fastqc ${rawSeq} --noextract --nogroup -o ${outDir};chmod -R 440 ${outDir}/*.zip"
                pbsIDs.add(executionHelperService.sendScript(realm, cmd))
                fastqcDataFilesService.createFastqcProcessedFile(seqFile)
            }
        }
        addOutputParameter("__pbsIds", pbsIDs.join(","))
        addOutputParameter("__pbsRealm", realm.id.toString())
    }
}
