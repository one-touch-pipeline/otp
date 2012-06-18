package de.dkfz.tbi.otp.job.jobs.unpacking

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class SendUnpackJob extends AbstractJobImpl {

    @Autowired
    ConfigService configService

    @Autowired
    ExecutionService executionService

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
            String pbsId = sendCommand(realm, cmd)
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

    private String sendCommand(Realm realm, String text) {
        String pbsResponse = executionService.executeJob(realm, text)
        List<String> extractedPbsIds = executionService.extractPbsIds(pbsResponse)
        if (extractedPbsIds.size() != 1) {
            println "Number of PBS is = ${extractedPbsIds.size()}"
        }
        return extractedPbsIds.get(0)
    }

    private void addPbsId(String pbsId) {
        pbsIds += pbsId + ","
    }
}
