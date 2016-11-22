package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import org.springframework.security.access.prepost.*

class SamplePairService {

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#lookupIndividual.project, read)")
    Map<String, List<SamplePair>> samplePairsBySnvProcessingState(Individual lookupIndividual) {


        List<SamplePair> finishedSamplePairs = []
        List<SamplePair> progressingSamplePairs = []
        List<SamplePair> notStarted = []
        List<SamplePair> processingDisabled = []

        List<SamplePair> samplePairs = SamplePair.createCriteria().list {
            mergingWorkPackage1 {
                sample {
                    individual {
                        eq('id', lookupIndividual.id)
                    }
                }
            }
        }
        samplePairs.each {
            switch (it.processingStatus) {
                case ProcessingStatus.NEEDS_PROCESSING:
                    notStarted << it
                    break
                case ProcessingStatus.DISABLED:
                    processingDisabled << it
                    break
                case ProcessingStatus.NO_PROCESSING_NEEDED:
                    break
                default:
                    throw new UnsupportedOperationException("Handling processing status ${it.processingStatus} is not implemented.")
            }
            List<SnvCallingInstance> snvCallingInstances = SnvCallingInstance.findAllBySamplePair(it)
            if (snvCallingInstances.find { it.processingState == AnalysisProcessingStates.FINISHED }) {
                finishedSamplePairs << it
            }
            if (snvCallingInstances.find { it.processingState == AnalysisProcessingStates.IN_PROGRESS && !it.withdrawn }) {
                progressingSamplePairs << it
            }
        }

        return [
            finished: finishedSamplePairs,
            inprogress: progressingSamplePairs,
            notStarted: notStarted,
            disabled: processingDisabled
        ]
    }


    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#individual.project, read)")
    List<SamplePair> finishedSamplePairs(Individual individual) {
        return samplePairsBySnvProcessingState(individual)['finished'] ?: []
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#individual.project, read)")
    List<SamplePair> progressingSamplePairs(Individual individual) {
        return samplePairsBySnvProcessingState(individual)['inprogress'] ?: []
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#individual.project, read)")
    List<SamplePair> notStartedSamplePairs(Individual individual) {
        return samplePairsBySnvProcessingState(individual)['notStarted'] ?: []
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#individual.project, read)")
    List<SamplePair> disablesSamplePairs(Individual individual) {
        return samplePairsBySnvProcessingState(individual)['disabled'] ?: []
    }

}
