package de.dkfz.tbi.otp.job.jobs.dataTransfer

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class CreateOutputDirectoryJob extends AbstractJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ExecutionService executionService

    @Autowired
    ConfigService configService

    @Override
    public void execute() throws Exception {

        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)

        Set<Project> projects = projects(run)
        projects.each {Project project ->
            String[] dirs = lsdfFilesService.getListOfRunDirecotries(run, project.name)
            dirs.each {String line ->
                String cmd = "mkdir -p -m 2750 " + line
                Realm realm = configService.getRealmDataManagement(project)
                String exitCode = executionService.executeCommand(realm, cmd)
                log.debug "creating directory [${line}].  exit code [${exitCode}]"
            }
        }
    }

    private Set<Project> projects(Run run) {
        Set<Project> projects = new HashSet<Project>()
        List<DataFile> files = DataFile.findAllByRun(run)
        for (DataFile file in files) {
            if (file.project) {
                projects << file.project
            }
        }
        return projects
    }
}
