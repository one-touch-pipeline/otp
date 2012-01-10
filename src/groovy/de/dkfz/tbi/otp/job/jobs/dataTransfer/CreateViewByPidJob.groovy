package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CreateViewByPidJob extends AbstractJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    private String projectName

    @Override
    public void execute() throws Exception {

        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)
        projectName = "PROJECT_NAME" //getParameterValueOrClass("project")
        String cmd = ""
        DataFile[] dataFiles = DataFile.findAllByRun(run).toArray()
        //(DataFile[])run.dataFiles.toArray()
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
            String dirName = linkName.substring(0, linkName.lastIndexOf('/'))
            cmd += "mkdir -p ${dirName};\n"
            cmd += "ln -s " + target + " " + linkName + ";\n"
        }
        println cmd
    }
}
