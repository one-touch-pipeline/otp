package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*

import org.springframework.beans.factory.annotation.Autowired

class CreateFastqcOutputDirectoryJob extends AbstractJobImpl {

    @Autowired
    FastqcDataFilesService fastqcDataFilesService

    @Autowired
    ExecutionService executionService

    @Override
    public void execute() throws Exception {
        long seqTrackId = Long.parseLong(getProcessParameterValue())
        SeqTrack seqTrack = SeqTrack.get(seqTrackId)
        String dir = fastqcDataFilesService.fastqcOutputDirectory(seqTrack)
        Realm realm = fastqcDataFilesService.fastqcRealm(seqTrack)
        execute(dir, realm)
    }

    private void execute(String directory, Realm realm) {
        String cmd = "mkdir -p " + directory
        String exitCode = executionService.executeCommand(realm, cmd)
        log.debug "creating directory finished with exit code " + exitCode
    }
}
