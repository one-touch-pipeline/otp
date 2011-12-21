package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("copyFiles")
@Scope("prototype")
class CopyFilesJob extends AbstractJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    private String projectName

    @Override
    public void execute() throws Exception {

        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)

        String cmd = ""
        DataFile.findAllByRun(run).each {DataFile file ->
            if (file.project == null) {
                return
            }
            String from = lsdfFilesService.getFileInitialPath(file)
            String to = lsdfFilesService.getFileFinalPath(file)
            println from + " " + to
            cmd += "cp ${from} ${to};\n"
        }
        println cmd
    }
}
