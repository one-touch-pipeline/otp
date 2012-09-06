package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class BwaAlignmentCompleteJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    AlignmentPassService alignmentPassService

    @Override
    public void execute() throws Exception {
        long alignmentPassId =  Long.parseLong(getProcessParameterValue())
        AlignmentPass alignmentPass = AlignmentPass.get(alignmentPassId)
        alignmentPassService.alignmentPassFinished(alignmentPass)
        succeed()
    }
}
