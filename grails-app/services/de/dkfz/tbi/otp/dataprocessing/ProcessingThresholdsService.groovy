package de.dkfz.tbi.otp.dataprocessing


import org.springframework.security.access.prepost.PreAuthorize
import de.dkfz.tbi.otp.ngsdata.*

class ProcessingThresholdsService {

    /**
     *
     * @return List of ProcessingThresolds for an project
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public List<ProcessingThresholds> findByProject(Project project) {
        return ProcessingThresholds.findAllByProject(project)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ProcessingThresholds createOrUpdate(Project project, SampleType sampleType, SeqType seqType, Long numberOfLanes, Double coverage) {
        ProcessingThresholds processingThresholds = ProcessingThresholds.findByProjectAndSampleTypeAndSeqType(project, sampleType, seqType)
        if (processingThresholds) {
            processingThresholds.numberOfLanes = numberOfLanes
            processingThresholds.coverage = coverage
        } else {
            processingThresholds = new ProcessingThresholds(
                            project: project,
                            sampleType: sampleType,
                            seqType: seqType,
                            numberOfLanes: numberOfLanes,
                            coverage: coverage,
                            )
        }
        processingThresholds.save()
        return processingThresholds
    }
}
