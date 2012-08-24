package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.common.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.processing.Parameter

import org.springframework.beans.factory.annotation.Autowired

class FastqcJob extends AbstractJobImpl {

    @Autowired
    DataProcessingFilesService dataProcessingFilesService

    @Autowired
    ExecutionService executionService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    SeqTrackService seqTrackService

    @Autowired
    ConfigService configService

    @Override
    public void execute() throws Exception {
        SeqTrack seqTrack = SeqTrack.get(Long.parseLong(getProcessParameterValue()))
        Individual ind = seqTrack.sample.individual
        Realm realm = configService.getRealmDataProcessing(ind.project)
        List<DataFile> seqFiles = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)

        String outDir = dataProcessingFilesService.getOutputDirectory(ind, DataProcessingFilesService.OutputDirectories.FASTX_QC)
        List<String> pbsIDs = []

        seqFiles.each { seq ->
            String rawSeq = lsdfFilesService.getFileFinalPath(seq)
            String cmd = "fastqc ${rawSeq} --noextract --nogroup -o ${outDir};chmod -R 440 ${outDir}/*.zip"
            pbsIDs.add(executionService.executeJob(realm, cmd))

            dataProcessingFilesService.createAndSaveProcessedFile(
                ProcessedFile.Type.FASTQC_ARCHIVE, seq, this, ind, MergingLog.Execution.SYSTEM)
        }
        addOutputParameter("__pbsIds", pbsIDs.join(","))
        addOutputParameter("__pbsRealm", realm.id.toString())
    }
}
