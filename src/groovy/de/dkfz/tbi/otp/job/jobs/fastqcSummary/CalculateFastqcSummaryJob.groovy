package de.dkfz.tbi.otp.job.jobs.fastqcSummary

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsqc.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import org.springframework.beans.factory.annotation.Autowired

class CalculateFastqcSummaryJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    FastqcResultsService fastqcResultsService

    @Override
    public void execute() throws Exception {
        Run run = Run.get(Long.parseLong(getProcessParameterValue()))
        run.dataQuality = calculateQuality(run)
        run.qualityEvaluated = true
        assert(run.save(flush: true))
        succeed()
    }

    private double calculateQuality(Run run) {
        final List statusList = [
            FastqcModuleStatus.Status.PASS,
            FastqcModuleStatus.Status.WARN,
            FastqcModuleStatus.Status.FAIL
        ]
        List<FastqcProcessedFile> fastqcs = fastqcResultsService.fastqcFilesForRun(run)
        List<Integer> N = []
        for (FastqcModuleStatus.Status status in statusList) {
            N << FastqcModuleStatus.countByStatusAndFastqcProcessedFileInList(status, fastqcs)
        }
        double total = N.get(0) + N.get(1) * 0.5 // heuristics
        double normalization = FastqcModuleStatus.countByFastqcProcessedFileInList(fastqcs)
        return total/normalization
    }
}
