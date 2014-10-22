package de.dkfz.tbi.otp.ngsdata


import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import static org.springframework.util.Assert.*
import org.springframework.security.access.prepost.PreAuthorize
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SampleTypeCombinationPerIndividual
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvProcessingStates
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.Sample.*
import de.dkfz.tbi.otp.ngsdata.SeqTrack.*





class SampleTypeCombinationPerIndividualService {


    //TODO handle of ignored SNv-status according to OTP-1224
    @PreAuthorize("hasPermission(#individual.project, read) or hasRole('ROLE_OPERATOR')")
    Map<String, SampleTypeCombinationPerIndividual> samplePairsBySnvProcessingState(Individual individual) {
        assert individual

        List<SampleTypeCombinationPerIndividual> finishedSampleTypeCombinationPerIndividuals = []
        List<SampleTypeCombinationPerIndividual> progressingSampleTypeCombinationPerIndividuals = []
        List<SampleTypeCombinationPerIndividual> notStarted = []
        List<SampleTypeCombinationPerIndividual> processingDisabled = []


        List<SampleTypeCombinationPerIndividual> sampleTypeCombinationPerIndividuals = SampleTypeCombinationPerIndividual.findAllByIndividual(individual)
        sampleTypeCombinationPerIndividuals.each {
            List<SnvCallingInstance> snvCallingInstances = SnvCallingInstance.findAllBySampleTypeCombination(it)
            if (snvCallingInstances) {
                if (snvCallingInstances.find { it.processingState == SnvProcessingStates.FINISHED }) {
                    finishedSampleTypeCombinationPerIndividuals << it
                }
                if (snvCallingInstances.find { it.processingState == SnvProcessingStates.IN_PROGRESS }) {
                    progressingSampleTypeCombinationPerIndividuals << it
                }
                if (it.needsProcessing) {
                    notStarted << it
                }
            } else {
                if (it.needsProcessing) {
                    notStarted << it
                } else {
                    processingDisabled << it
                }
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
