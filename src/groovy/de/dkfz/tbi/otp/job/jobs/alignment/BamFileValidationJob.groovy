package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class BamFileValidationJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Override
    public void execute() throws Exception {
        ProcessedBamFile bamFile = parseInput()

        validateNumberOfReads(bamFile)

        final long fileSize = processedBamFileService.updateBamFile(bamFile)
        if (fileSize <= 0L) {
            throw new RuntimeException("File size of ${bamFile} is ${fileSize}.")
        }
        succeed()
    }

    private ProcessedBamFile parseInput() {
        long alignmentPassId = Long.parseLong(getProcessParameterValue())
        String type = getParameterValueOrClass("BamType")
        return processedBamFileService.findBamFile(alignmentPassId, type)
    }


    void validateNumberOfReads(ProcessedBamFile processedBamFile) {
        long fastQCReadLength = processedBamFile.seqTrack.getNReads()
        long alignmentReadLength = processedBamFileService.getAlignmentReadLength(processedBamFile)

        assert fastQCReadLength == alignmentReadLength: "Number of reads differs between FastQC (${fastQCReadLength}) and alignment (${alignmentReadLength})"
    }

}
