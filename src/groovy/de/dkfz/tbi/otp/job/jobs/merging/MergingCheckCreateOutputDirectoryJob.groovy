package de.dkfz.tbi.otp.job.jobs.merging

import org.springframework.beans.factory.annotation.Autowired;
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class MergingCheckCreateOutputDirectoryJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Override
    public void execute() throws Exception {
        long mergedPassId = Long.parseLong(getProcessParameterValue())
        MergingPass mergingPass = MergingPass.get(mergingPassId)
        File file = new File(processedMergedBamFileService.getDirectory(mergingPass))
        if (!file.canRead()) {
            log.debug "directory not readable ${file}"
            fail()
            throw new FileNotReadableException(file)
        } else {
            succeed()
        }
    }
}
