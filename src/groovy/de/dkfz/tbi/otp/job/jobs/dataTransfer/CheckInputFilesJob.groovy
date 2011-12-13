package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("checkInputFiles")
@Scope("prototype")
class CheckInputFilesJob extends AbstractEndStateAwareJobImpl {

   /**
    * dependency injection of meta data service
    */
    @Autowired
    LsdfFilesService lsdfFilesService

    /**
     * Loop over all sequence and alignment files and check
     * if they exists in the initial location.
     * 
     * @throws Exception
     */
    @Override
    public void execute() throws Exception {

        long runId = Long.parseLong(getProcessParameterValue("run"))
        Run run = Run.get(runId)
        String path = run.dataPath
        succeed()

        Set<DataFile> dataFiles = run.dataFiles
        dataFiles.each {DataFile dataFile ->
            if (dataFile.fileType == FileType.Type.SEQUENCE ||
                dataFile.fileType == FileType.Type.ALIGNMENT) {
                String fullPath = path + "/" + run.name + "/" + dataFile.pathName + 
                    "/" + dataFile.fileName
                if (!lsdfFilesService.fileExists(fullPath)) {
                    fail()
                }
            }
        }
    }
}
