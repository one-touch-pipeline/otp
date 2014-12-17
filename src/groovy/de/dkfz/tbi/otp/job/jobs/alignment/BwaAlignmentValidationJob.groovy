package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import org.springframework.beans.factory.annotation.Autowired

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class BwaAlignmentValidationJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedSaiFileService processedSaiFileService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Override
    public void execute() throws Exception {
        long alignmentPassId = Long.parseLong(getProcessParameterValue())
        AlignmentPass alignmentPass = AlignmentPass.get(alignmentPassId)

        ProcessedBamFile processedBamFile = exactlyOneElement(ProcessedBamFile.findAllByAlignmentPass(alignmentPass))
        validateNumberOfReads(processedBamFile)

        List<ProcessedSaiFile> saiFiles = ProcessedSaiFile.findAllByAlignmentPass(alignmentPass)
        final List<String> problems = []
        for (ProcessedSaiFile saiFile in saiFiles) {
            final String problem = processedSaiFileService.updateSaiFileInfoFromDisk(saiFile)
            if (problem != null) {
                problems << problem
            }
        }
        if (problems.empty) {
            succeed()
        } else {
            throw new RuntimeException(problems.join("\n"))
        }
    }

    void validateNumberOfReads(ProcessedBamFile processedBamFile) {
        long fastQCReadLength = processedBamFileService.getFastQCReadLength(processedBamFile)
        long alignmentReadLength = processedBamFileService.getAlignmentReadLength(processedBamFile)

        assert fastQCReadLength == alignmentReadLength: "Number of reads differs between FastQC (${fastQCReadLength}) and alignment (${alignmentReadLength})"
    }
}
