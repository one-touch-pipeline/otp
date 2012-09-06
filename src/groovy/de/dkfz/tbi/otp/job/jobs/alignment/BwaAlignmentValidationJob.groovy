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
        boolean totalState = true
        for (ProcessedSaiFile saiFile in saiFiles) {
            boolean state = processedSaiFileService.updateSaiFile(saiFile)
            totalState = state ? totalState : false
        }
        totalState ? succeed() : fail()
    }
}
