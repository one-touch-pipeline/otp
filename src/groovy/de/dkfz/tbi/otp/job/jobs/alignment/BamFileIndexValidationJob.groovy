package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class BamFileIndexValidationJob  extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    AlignmentPassService alignmentPassService

    @Override
    public void execute() throws Exception {
        ProcessedBamFile bamFile = parseInput()
        boolean status = processedBamFileService.updateBamFileIndex(bamFile)
        if (status) {
            succeed()
        } else {
            fail()
        }
    }

    private ProcessedBamFile parseInput() {
        long alignmentPassId = Long.parseLong(getProcessParameterValue())
        String type = getParameterValueOrClass("BamType")
        return processedBamFileService.findBamFile(alignmentPassId, type)
    }
}
