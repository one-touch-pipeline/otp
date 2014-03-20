package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class BwaAlignmentValidationJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    ProcessedSaiFileService processedSaiFileService

    @Override
    public void execute() throws Exception {
        long alignmentPassId = Long.parseLong(getProcessParameterValue())
        AlignmentPass alignmentPass = AlignmentPass.get(alignmentPassId)
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
}
