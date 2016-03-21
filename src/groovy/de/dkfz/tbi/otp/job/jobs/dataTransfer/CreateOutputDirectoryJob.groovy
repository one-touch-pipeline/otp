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

    @Autowired
    RunProcessingService runProcessingService

    @Override
    public void execute() throws Exception {

        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)

        Set<Project> projects = projects(run)
        projects.each {Project project ->
            String[] dirs = lsdfFilesService.getListOfRunDirecotries(run, project.name)
            dirs.each {String directoryPath ->
                String cmd = "umask 027; mkdir -p -m 2750 ${directoryPath}; echo \$?"
                Realm realm = configService.getRealmDataManagement(project)
                assert executionService.executeCommand(realm, cmd) ==~ /^0\s*$/
                assert new File(directoryPath).isDirectory()
            }
        }
    }

    private Set<Project> projects(Run run) {
        Set<Project> projects = new HashSet<Project>()
        List<RunSegment> runSegments = runProcessingService.runSegmentsWithFilesInProcessing(run)
        List<DataFile> files = DataFile.findAllByRunSegmentInList(runSegments)
        for (DataFile file in files) {
            if (file.project) {
                projects << file.project
            }
        }
        return projects
    }
}
