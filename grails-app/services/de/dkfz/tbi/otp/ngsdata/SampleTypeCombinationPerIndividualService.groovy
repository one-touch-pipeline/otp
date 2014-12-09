package de.dkfz.tbi.otp.ngsdata

import org.springframework.security.access.prepost.PreAuthorize
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SampleTypeCombinationPerIndividual
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SampleTypeCombinationPerIndividual.ProcessingStatus
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvProcessingStates

class SampleTypeCombinationPerIndividualService {

    @PreAuthorize("hasPermission(#individual.project, read) or hasRole('ROLE_OPERATOR')")
    Map<String, SampleTypeCombinationPerIndividual> samplePairsBySnvProcessingState(Individual individual) {
        assert individual

        List<SampleTypeCombinationPerIndividual> finishedSampleTypeCombinationPerIndividuals = []
        List<SampleTypeCombinationPerIndividual> progressingSampleTypeCombinationPerIndividuals = []
        List<SampleTypeCombinationPerIndividual> notStarted = []
        List<SampleTypeCombinationPerIndividual> processingDisabled = []


        List<SampleTypeCombinationPerIndividual> sampleTypeCombinationPerIndividuals = SampleTypeCombinationPerIndividual.findAllByIndividual(individual)
        sampleTypeCombinationPerIndividuals.each {
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
            List<SnvCallingInstance> snvCallingInstances = SnvCallingInstance.findAllBySampleTypeCombination(it)
            if (snvCallingInstances.find { it.processingState == SnvProcessingStates.FINISHED }) {
                finishedSampleTypeCombinationPerIndividuals << it
            }
            if (snvCallingInstances.find { it.processingState == SnvProcessingStates.IN_PROGRESS }) {
                progressingSampleTypeCombinationPerIndividuals << it
            }
        }

        return [
            finished: finishedSampleTypeCombinationPerIndividuals,
            inprogress: progressingSampleTypeCombinationPerIndividuals,
            notStarted: notStarted,
            disabled: processingDisabled
        ]
    }


    @PreAuthorize("hasPermission(#individual.project, read) or hasRole('ROLE_OPERATOR')")
    List<SampleTypeCombinationPerIndividual> finishedSamplePairs(Individual individual) {
        return samplePairsBySnvProcessingState(individual)['finished'] ?: []
    }

    @PreAuthorize("hasPermission(#individual.project, read) or hasRole('ROLE_OPERATOR')")
    List<SampleTypeCombinationPerIndividual> progressingSamplePairs(Individual individual) {
        return samplePairsBySnvProcessingState(individual)['inprogress'] ?: []
    }

    @PreAuthorize("hasPermission(#individual.project, read) or hasRole('ROLE_OPERATOR')")
    List<SampleTypeCombinationPerIndividual> notStartedSamplePairs(Individual individual) {
        return samplePairsBySnvProcessingState(individual)['notStarted'] ?: []
    }

    @PreAuthorize("hasPermission(#individual.project, read) or hasRole('ROLE_OPERATOR')")
    List<SampleTypeCombinationPerIndividual> disablesSamplePairs(Individual individual) {
        return samplePairsBySnvProcessingState(individual)['disabled'] ?: []
    }

}
