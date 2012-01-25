package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService

public class CompareChecksumJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    String suffix = ".md5sum"

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)

        DataFile.findAllByRunAndProjectIsNotNull(run).each {DataFile file ->
            if (!compareMd5(file)) {
                fail()
            }
        }
        succeed()
    }

    private boolean compareMd5(DataFile file) {
        String path = pathToMd5File(file)
        File md5File = new File(path)
        if (md5File.canRead()) {
            return false
        }
        List<String> lines = md5File.readLines()
        List<String> tokens = lines.get(0).tokenize()
        String md5sum = tokens.get(0)
        return (md5sum == file.md5sum)
    }

    private String pathToMd5File(DataFile file) {
        String path = lsdfFilesService.getFileFinalPath(file)
        String md5file = path + suffix
        return md5file
    }
}
