package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.ngsdata.*

class ClusterJobLoggingService {

    final static CLUSTER_LOG_BASE_DIR = 'clusterLog'

    LsdfFilesService lsdfFilesService

    static File logDirectory(Realm realm, ProcessingStep processingStep) {
        assert realm: 'No realm specified.'
        assert processingStep: 'No processing step specified.'

        Date date = processingStep.firstProcessingStepUpdate.date
        String dateDirectory = date.format('yyyy-MM-dd')
        return new File("${realm.loggingRootPath}/${CLUSTER_LOG_BASE_DIR}/${dateDirectory}")
    }

    File createAndGetLogDirectory(Realm realm, ProcessingStep processingStep) {
        File logDirectory = logDirectory(realm, processingStep)
        if (!logDirectory.exists()) {
            //race condition between threads and within NFS can be ignored, since the command 'mkdir --parent' does not fail if the directory already exists.
            lsdfFilesService.createDirectory(logDirectory, realm)
            LsdfFilesService.ensureDirIsReadable(logDirectory)
        }
        return logDirectory
    }
}
