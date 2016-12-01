package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class SnvCallingService extends BamFileAnalysisService {

    void markSnvCallingInstanceAsFailed(SnvCallingInstance instance, List<SnvCallingStep> stepsToWithdrawSnvJobResults) {
        assert stepsToWithdrawSnvJobResults : 'at least one of SnvCallingStep must be provided'
        stepsToWithdrawSnvJobResults.each { step ->
            def snvJobResult = exactlyOneElement(SnvJobResult.findAllBySnvCallingInstanceAndStep(instance, step))
            snvJobResult.withdrawn = true
            assert snvJobResult.save([flush: true])
        }
        instance.withdrawn = true
        assert instance.save(flush: true)
    }

    SnvJobResult getLatestValidJobResultForStep(SnvCallingInstance instance, SnvCallingStep step) {
        SnvJobResult snvJobResult = SnvJobResult.createCriteria().get {
            snvCallingInstance {
                eq('samplePair', instance.samplePair)
            }
            eq('step', step)
            eq('withdrawn', false)
            eq('processingState', AnalysisProcessingStates.FINISHED)
            order("id", "desc")
            maxResults(1)
        }

        assert snvJobResult : "There is no valid previous result file for sample pair ${instance.samplePair} and step ${step}."

        AbstractMergedBamFile firstBamFile = snvJobResult.sampleType1BamFile
        AbstractMergedBamFile secondBamFile = snvJobResult.sampleType2BamFile

        assert firstBamFile  == instance.sampleType1BamFile : "The first bam file has changed between instance ${snvJobResult.snvCallingInstance} and ${instance}."
        assert secondBamFile == instance.sampleType2BamFile : "The second bam file has changed between instance ${snvJobResult.snvCallingInstance} and ${instance}."
        assert !firstBamFile.withdrawn  : "The bam file ${firstBamFile} of the previous result is withdrawn."
        assert !secondBamFile.withdrawn : "The bam file ${secondBamFile} of the previous result is withdrawn."

        return snvJobResult
    }

    protected String getProcessingStateCheck() {
        return "sp.snvProcessingStatus = :needsProcessing "
    }

    protected Class<SnvCallingInstance> getAnalysisClass() {
        return SnvCallingInstance.class
    }

    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.SNV
    }
}
