package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

@Component
@Scope("prototype")
@UseJobLog
class BwaAlignmentCompleteJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    AlignmentPassService alignmentPassService

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Override
    public void execute() throws Exception {
        long alignmentPassId =  Long.parseLong(getProcessParameterValue())
        AlignmentPass alignmentPass = AlignmentPass.get(alignmentPassId)
        AlignmentPass.withTransaction {
            alignmentPassService.alignmentPassFinished(alignmentPass)
            processedBamFileService.setNeedsProcessing(CollectionUtils.exactlyOneElement(ProcessedBamFile.findAllByAlignmentPass(alignmentPass)))
        }
        succeed()
    }
}
