package de.dkfz.tbi.otp.job.jobs.alignment

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CreateAlignmentOutputDirectoryJob extends AbstractJobImpl {

    @Autowired
    ProcessedAlignmentFileService processedAlignmentFileService

    @Autowired
    AlignmentPassService alignmentPassService

    @Autowired
    ExecutionService executionService

    @Override
    public void execute() throws Exception {
        long alignmentPassId = Long.parseLong(getProcessParameterValue())
        AlignmentPass alignmentPass = AlignmentPass.get(alignmentPassId)

        // TODO remove this hack when multiple start is solved
        alignmentPassService.alignmentPassStarted(alignmentPass)

        String directory = processedAlignmentFileService.getDirectory(alignmentPass)
        Realm realm = alignmentPassService.realmForDataProcessing(alignmentPass)
        String cmd = "umask 027; mkdir -p -m 2750 " + directory
        String exitCode = executionService.executeCommand(realm, cmd)
        log.debug "creating directory finished with exit code " + exitCode
    }
}
