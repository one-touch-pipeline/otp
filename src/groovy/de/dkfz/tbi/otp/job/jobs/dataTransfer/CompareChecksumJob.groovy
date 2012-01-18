package de.dkfz.tbi.otp.job.jobs.dataTransfer

import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService

public class CompareChecksumJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    LsdfFilesService lsdfFilesService

    @Override
    public void execute() throws Exception {
        long runId = Long.parseLong(getProcessParameterValue())
        Run run = Run.get(runId)

        String[] directories = lsdfFilesService.getAllPathsForRun(run)
        for(String directory in directories) {
            if (!checkMd5File(run, directory)) {
                fail()
            }
        }
        succeed()
    }

    /**
     * 
     * @param run
     * @param directory
     * @return
     */
    private boolean checkMd5File(Run run, String directory) {
        String fileName = directory + "/run" + run.name + "/files.md5sum"
        println fileName
        File md5file = new File(fileName)
        if (!md5file.canRead()) {
            return false
        }
        boolean allOK = true
        md5file.eachLine {String line ->
            println line
            if (!checkMd5Line(run, line)) {
                allOK = false
            }
        }
        return allOK
    }

    /**
     * 
     * @param line
     * @return
     */
    private boolean checkMd5Line(Run run, String line) {
        List<String> tokens = line.tokenize()
        String md5sum = tokens.get(0)
        String fileName = tokens.get(1)
        DataFile dataFile = DataFile.findByRunAndFileName(run, fileName)
        String origMd5sum = dataFile.md5sum
        return (origMd5sum == md5sum)
    }
}
