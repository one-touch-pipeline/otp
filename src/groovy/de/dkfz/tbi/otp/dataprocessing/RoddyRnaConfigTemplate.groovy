package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*

class RoddyRnaConfigTemplate {

    static final String ARRIBA_KNOWN_FUSIONS = "ARRIBA_KNOWN_FUSIONS"
    static final String ARRIBA_BLACKLIST = "ARRIBA_BLACKLIST"
    static final String GENE_MODELS = "GENE_MODELS"
    static final String GENE_MODELS_DEXSEQ = "GENE_MODELS_DEXSEQ"
    static final String GENE_MODELS_EXCLUDE = "GENE_MODELS_EXCLUDE"
    static final String GENE_MODELS_GC = "GENE_MODELS_GC"
    static final String GENOME_GATK = "GENOME_GATK"
    static final String GENOME_KALLISTO_INDEX = "GENOME_KALLISTO_INDEX"
    static final String GENOME_STAR_INDEX = "GENOME_STAR_INDEX"
    static final String RUN_ARRIBA = "RUN_ARRIBA"
    static final String RUN_FEATURE_COUNTS_DEXSEQ = "RUN_FEATURE_COUNTS_DEXSEQ"

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
        boolean mouseData = rnaAlignmentConfiguration.mouseData
        GeneModel geneModel = rnaAlignmentConfiguration.geneModel

        if (mouseData) {
            configurationValues.add("<cvalue name=\"${RUN_ARRIBA}\" value=\"false\" type=\"boolean\"/>")
            configurationValues.add("<cvalue name=\"${RUN_FEATURE_COUNTS_DEXSEQ}\" value=\"false\" type=\"boolean\"/>")
        }
        rnaAlignmentConfiguration.referenceGenomeIndex.each {
            if (!(mouseData && (it.toolName.name == ARRIBA_KNOWN_FUSIONS || it.toolName.name == ARRIBA_BLACKLIST))) {
                configurationValues.add(
                        "<cvalue name=\"${it.toolName.name.contains(GENOME_STAR_INDEX) ? GENOME_STAR_INDEX : it.toolName.name}\" " +
                                "value=\"${referenceGenomeIndexService.getFile(it).absolutePath}\" " +
                                "type=\"string\"/>"
                )
            }
        }
        configurationValues.add("<cvalue name=\"${GENE_MODELS}\" value=\"${geneModelService.getFile(geneModel).absolutePath}\" type=\"string\"/>")
        if (!mouseData) {
            configurationValues.add("<cvalue name=\"${GENE_MODELS_DEXSEQ}\" value=\"${geneModelService.getDexSeqFile(geneModel).absolutePath}\" type=\"string\"/>")
        }
        if (geneModel.excludeFileName) {
            configurationValues.add("<cvalue name=\"${GENE_MODELS_EXCLUDE}\" value=\"${geneModelService.getExcludeFile(geneModel).absolutePath}\" type=\"string\"/>")
        }
        if (geneModel.gcFileName) {
            configurationValues.add("<cvalue name=\"${GENE_MODELS_GC}\" value=\"${geneModelService.getGcFile(geneModel).absolutePath}\" type=\"string\"/>")
        }

        return configurationValues.join("\n                ")
    }
}
