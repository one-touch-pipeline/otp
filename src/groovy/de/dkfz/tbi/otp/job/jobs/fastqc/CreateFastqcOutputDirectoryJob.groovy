package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*

import org.springframework.beans.factory.annotation.Autowired

class CreateFastqcOutputDirectoryJob extends AbstractJobImpl {

    @Autowired
    ConfigService configService

    @Autowired
    DataProcessingFilesService dataProcessingFilesService

    @Autowired
    ExecutionService executionService

    @Override
    public void execute() throws Exception {
        long seqTrackId = Long.parseLong(getProcessParameterValue())
        SeqTrack seqTrack = SeqTrack.get(seqTrackId)
        Individual individual = seqTrack.sample.individual
        Realm realm = configService.getRealmDataProcessing(individual.project)

        def type = DataProcessingFilesService.OutputDirectories.FASTX_QC
        String dir = dataProcessingFilesService.getOutputDirectory(individual, type)
        execute(dir, realm)
    }

    private void execute(String directory, Realm realm) {
        String cmd = "mkdir -p " + directory
        String exitCode = executionService.executeCommand(realm, cmd)
        println "creating directory finished with exit code " + exitCode
    }
}
