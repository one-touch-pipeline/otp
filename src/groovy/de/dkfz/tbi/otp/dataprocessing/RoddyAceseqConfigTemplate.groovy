package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*

class RoddyAceseqConfigTemplate {

    static String createConfig(RoddyConfiguration aceseqPipelineConfiguration, Pipeline.Name pipelineName, ReferenceGenome refGenome, ReferenceGenomeService refGenomeService) {
        return """
<configuration configurationType='project'
               name='${RoddyWorkflowConfig.getNameUsedInConfig(pipelineName, aceseqPipelineConfiguration.seqType, aceseqPipelineConfiguration.pluginName, aceseqPipelineConfiguration.pluginVersion, aceseqPipelineConfiguration.configVersion)}'
               description='Project configuration for ${aceseqPipelineConfiguration.seqType.roddyName} in OTP.'
               imports="${aceseqPipelineConfiguration.baseProjectConfig}">
    <subconfigurations>
        <configuration name='config' usedresourcessize='xl'>
            <availableAnalyses>
                <analysis id='${aceseqPipelineConfiguration.seqType.roddyName}' configuration='copyNumberEstimationAnalysis'/>
            </availableAnalyses>
            <configurationvalues>
                <cvalue name="MAPPABILITY_FILE" value="${new File(refGenome.mappabilityFile).absolutePath}" type="path"/>
                <cvalue name="REPLICATION_TIME_FILE" value="${new File(refGenome.replicationTimeFile).absolutePath}" type="path"/>
                <cvalue name="GC_CONTENT_FILE" value="${new File(refGenomeService.pathToChromosomeSizeFilesPerReference(refGenome), refGenome.gcContentFile).absolutePath}" type="path"/>
                <cvalue name='GENETIC_MAP_FILE' value="${new File(refGenome.geneticMapFile).absolutePath}" type="path"/>
                <cvalue name='KNOWN_HAPLOTYPES_FILE' value="${new File(refGenome.knownHaplotypesFile).absolutePath}" type="path"/>
                <cvalue name='KNOWN_HAPLOTYPES_LEGEND_FILE' value="${new File(refGenome.knownHaplotypesLegendFile).absolutePath}" type="path"/>
                <cvalue name='GENETIC_MAP_FILE_X' value="${new File(refGenome.geneticMapFileX).absolutePath}" type="path"/>
                <cvalue name='KNOWN_HAPLOTYPES_FILE_X' value="${new File(refGenome.knownHaplotypesFileX).absolutePath}" type="path"/>
                <cvalue name='KNOWN_HAPLOTYPES_LEGEND_FILE_X' value="${new File(refGenome.knownHaplotypesLegendFileX).absolutePath}" type="path"/>
            </configurationvalues>
        </configuration>
    </subconfigurations>
</configuration>
"""
    }
}
