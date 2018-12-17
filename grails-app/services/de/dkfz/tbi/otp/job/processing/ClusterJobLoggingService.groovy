package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Realm

class ClusterJobLoggingService {

    final static CLUSTER_LOG_BASE_DIR = 'clusterLog'

    LsdfFilesService lsdfFilesService

    static File logDirectory(ProcessingStep processingStep) {
        assert processingStep: 'No processing step specified.'

        Date date = processingStep.firstProcessingStepUpdate.date
        String dateDirectory = date.format('yyyy-MM-dd')
        return new File("${ConfigService.getInstance().getLoggingRootPath()}/${CLUSTER_LOG_BASE_DIR}/${dateDirectory}")
    }

    File createAndGetLogDirectory(Realm realm, ProcessingStep processingStep) {
        assert realm: 'No realm specified.'
        File logDirectory = logDirectory(processingStep)
        if (!logDirectory.exists()) {
            //race condition between threads and within NFS can be ignored, since the command 'mkdir --parent' does not fail if the directory already exists.
            lsdfFilesService.createDirectory(logDirectory, realm)
            LsdfFilesService.ensureDirIsReadable(logDirectory)
        }
        return logDirectory
    }
}
