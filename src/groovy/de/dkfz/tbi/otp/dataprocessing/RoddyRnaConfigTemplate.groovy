package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*

class RoddyRnaConfigTemplate {

    static String createConfig(RnaAlignmentConfiguration rnaAlignmentConfiguration, Pipeline.Name pipelineName, ReferenceGenomeIndexService referenceGenomeIndexService, GeneModelService geneModelService) {
        return """
<configuration configurationType='project'
               name='${RoddyWorkflowConfig.getNameUsedInConfig(pipelineName, rnaAlignmentConfiguration.seqType, rnaAlignmentConfiguration.pluginName, rnaAlignmentConfiguration.pluginVersion, rnaAlignmentConfiguration.configVersion)}'
               description='Project configuration for ${rnaAlignmentConfiguration.seqType.roddyName} in OTP.'
               imports="${rnaAlignmentConfiguration.baseProjectConfig}">
    <subconfigurations>
        <configuration name='config' usedresourcessize='${rnaAlignmentConfiguration.resources}'>
            <availableAnalyses>
                <analysis id='${rnaAlignmentConfiguration.seqType.roddyName}' configuration='RNAseqAnalysis'/>
            </availableAnalyses>
            <configurationvalues>
                ${getConfigurationValues(rnaAlignmentConfiguration, referenceGenomeIndexService, geneModelService)}
            </configurationvalues>
        </configuration>
    </subconfigurations>
</configuration>
"""
    }

    static private String getConfigurationValues(RnaAlignmentConfiguration rnaAlignmentConfiguration, ReferenceGenomeIndexService referenceGenomeIndexService, GeneModelService geneModelService) {
        List<String> configurationValues = []

        for (int i = 0; i < rnaAlignmentConfiguration.referenceGenomeIndex.size(); i++) {
            ReferenceGenomeIndex referenceGenomeIndex = rnaAlignmentConfiguration.referenceGenomeIndex.get(i)
            configurationValues.add("<cvalue name=\"${referenceGenomeIndex.toolName.name}\" value=\"${referenceGenomeIndexService.getFile(referenceGenomeIndex).absolutePath}\" type=\"string\"/>")
        }
        for (int i = 0; i < rnaAlignmentConfiguration.geneModels.size(); i++) {
            GeneModel geneModel = rnaAlignmentConfiguration.geneModels.get(i)
            if (geneModel.fileName) {
                configurationValues.add("<cvalue name=\"GENE_MODELS\" value=\"${geneModelService.getFile(geneModel).absolutePath}\" type=\"string\"/>")
            }
            if (geneModel.dexSeqFileName) {
                configurationValues.add("<cvalue name=\"GENE_MODELS_DEXSEQ\" value=\"${geneModelService.getDexSeqFile(geneModel).absolutePath}\" type=\"string\"/>")
            }
            if (geneModel.excludeFileName) {
                configurationValues.add("<cvalue name=\"GENE_MODELS_EXCLUDE\" value=\"${geneModelService.getExcludeFile(geneModel).absolutePath}\" type=\"string\"/>")
            }
            if (geneModel.gcFileName) {
                configurationValues.add("<cvalue name=\"GENE_MODELS_GC\" value=\"${geneModelService.getGcFile(geneModel).absolutePath}\" type=\"string\"/>")
            }
        }

        return configurationValues.join("\n                ")
    }
}
