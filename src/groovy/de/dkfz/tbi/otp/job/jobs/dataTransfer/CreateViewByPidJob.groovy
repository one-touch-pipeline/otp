package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("createViewByPidJob")
@Scope("prototype")
class CreateViewByPidJob extends AbstractJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    private String projectName

    @Override
    public void execute() throws Exception {

        long runId = Long.parseLong(getProcessParameterValue("run"))
        Run run = Run.get(runId)
        projectName = getParameterValueOrClass("project")
        String cmd = ""
        DataFile[] dataFiles = (DataFile[])run.dataFiles.toArray()
        for(int iFile=0; iFile<dataFiles.length; iFile++) {
            DataFile dataFile = dataFiles[iFile]
            if (dataFile.project.toString() != projectName) {
                continue
            }
            String target = lsdfFilesService.getFileFinalPath(dataFile)
            String linkName = lsdfFilesService.getFileViewByPidPath(dataFile)
            if (linkName == null || target == null) {
                continue
            }
            cmd += "ln -s " + target + " " + linkName + ";"
        }
        println cmd
    }
}
