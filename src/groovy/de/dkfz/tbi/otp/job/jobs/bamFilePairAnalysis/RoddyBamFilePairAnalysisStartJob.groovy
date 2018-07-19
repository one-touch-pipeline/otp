package de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import org.hibernate.*

trait RoddyBamFilePairAnalysisStartJob implements BamFilePairAnalysisStartJobTrait {

    @Override
    String getInstanceName(ConfigPerProjectAndSeqType config) {
        assert RoddyWorkflowConfig.isAssignableFrom(Hibernate.getClass(config)): "Roddy startjobs should only ever be started with a RoddyWorkFlow, not something else; got ${ config.class }"
        return "results_${config.pluginVersion.replaceAll(":", "-")}_${config.configVersion}_${ getFormattedDate() }"
    }

    @Override
    ConfigPerProjectAndSeqType getConfig(SamplePair samplePair) {
        Pipeline pipeline = getBamFileAnalysisService().getPipeline()
        RoddyWorkflowConfig config = (RoddyWorkflowConfig)RoddyWorkflowConfig.getLatestForIndividual(
                samplePair.individual, samplePair.seqType, pipeline)

        if (config == null) {
            throw new RuntimeException("No ${RoddyWorkflowConfig.simpleName} found for ${Pipeline.simpleName} ${pipeline}, ${Individual.simpleName} ${samplePair.individual} (${Project.simpleName} ${samplePair.project}), ${SeqType.simpleName} ${samplePair.seqType}")
        }
        return config
    }
}
