package de.dkfz.tbi.otp.job.jobs.unpacking

import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class SendUnpackJob extends AbstractJobImpl {

    @Autowired
    ConfigService configService

    @Autowired
    ExecutionHelperService executionHelperService

    String pbsIds = ""

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)
        List<RunSegment> segments =
                        RunSegment.findAllByRunAndFilesStatus(run, RunSegment.FilesStatus.PROCESSING_UNPACK)
        for(RunSegment segment in segments) {
            Realm realm = configService.getRealmForInitialFTPPath(segment.dataPath)
            String cmd = createUnpackCommand(segment)
            String pbsId = executionHelperService.sendScript(realm, cmd)
            addPbsId(pbsId)
        }
        addOutputParameter("__pbsIds", pbsIds)
        addOutputParameter("__pbsRealm", "DKFZ")
    }

    String createUnpackCommand(RunSegment segment) {
        String runName = segment.run.name
        switch (segment.initialFormat) {
            case RunSegment.DataFormat.TAR :
                return "cd ${segment.dataPath}; tar -xvf ${runName}.tar"
            case RunSegment.DataFormat.TAR_IN_DIRECTORY :
                return "cd ${segment.dataPath}/${runName}; tar -xvf ${runName}.tar"
        }
        throw new ProcessingException("Run initial format not a tar archive")
    }

    private void addPbsId(String pbsId) {
        pbsIds += pbsId + ","
    }
}
