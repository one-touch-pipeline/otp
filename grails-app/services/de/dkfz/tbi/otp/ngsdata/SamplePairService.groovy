package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PreAuthorize
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvProcessingStates

class SamplePairService {

    @PreAuthorize("hasPermission(#individual.project, read) or hasRole('ROLE_OPERATOR')")
    Map<String, SamplePair> samplePairsBySnvProcessingState(Individual individual) {
        assert individual

        List<SamplePair> finishedSamplePairs = []
        List<SamplePair> progressingSamplePairs = []
        List<SamplePair> notStarted = []
        List<SamplePair> processingDisabled = []


        List<SamplePair> samplePairs = SamplePair.findAllByIndividual(individual)
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
            if (snvCallingInstances.find { it.processingState == SnvProcessingStates.FINISHED }) {
                finishedSamplePairs << it
            }
            if (snvCallingInstances.find { it.processingState == SnvProcessingStates.IN_PROGRESS }) {
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


    @PreAuthorize("hasPermission(#individual.project, read) or hasRole('ROLE_OPERATOR')")
    List<SamplePair> finishedSamplePairs(Individual individual) {
        return samplePairsBySnvProcessingState(individual)['finished'] ?: []
    }

    @PreAuthorize("hasPermission(#individual.project, read) or hasRole('ROLE_OPERATOR')")
    List<SamplePair> progressingSamplePairs(Individual individual) {
        return samplePairsBySnvProcessingState(individual)['inprogress'] ?: []
    }

    @PreAuthorize("hasPermission(#individual.project, read) or hasRole('ROLE_OPERATOR')")
    List<SamplePair> notStartedSamplePairs(Individual individual) {
        return samplePairsBySnvProcessingState(individual)['notStarted'] ?: []
    }

    @PreAuthorize("hasPermission(#individual.project, read) or hasRole('ROLE_OPERATOR')")
    List<SamplePair> disablesSamplePairs(Individual individual) {
        return samplePairsBySnvProcessingState(individual)['disabled'] ?: []
    }

}
