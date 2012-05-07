package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CreateOutputDirectoryJob extends AbstractJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ExecutionService executionService

    @Override
    public void execute() throws Exception {

        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)

        Set<Project> projects = projects(run)
        projects.each {Project project ->
            String[] dirs = lsdfFilesService.getListOfRunDirecotries(run, project.name)
            dirs.each {String line ->
                String cmd = "mkdir -p " + line
                String exitCode = executionService.executeCommand(project.realm, cmd)
                println "creating directory finished with exit code " + exitCode
            }
        }
    }

    private Set<Project> projects(Run run) {
        Set<Project> projects = new HashSet<Project>()
        List<DataFile> files = DataFile.findAllByRun(run)
        for(DataFile file in files) {
            if (file.project) {
                projects << file.project
            }
        }
        return projects
    }
}
