package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*

class RoddyAceseqConfigTemplate {

    static String createConfig(RoddyConfiguration aceseqPipelineConfiguration, Pipeline.Name pipelineName) {
        return """
<configuration configurationType="project"
               name="${RoddyWorkflowConfig.getNameUsedInConfig(pipelineName, aceseqPipelineConfiguration.seqType, aceseqPipelineConfiguration.pluginName, aceseqPipelineConfiguration.pluginVersion, aceseqPipelineConfiguration.configVersion)}"
               description="Project configuration for ${aceseqPipelineConfiguration.seqType.roddyName} in OTP."
               imports="${aceseqPipelineConfiguration.baseProjectConfig}">
    <subconfigurations>
        <configuration name="config" usedresourcessize="${aceseqPipelineConfiguration.resources}">
            <availableAnalyses>
                <analysis id="${aceseqPipelineConfiguration.seqType.roddyName}" configuration="copyNumberEstimationAnalysis"/>
            </availableAnalyses>
            <configurationvalues>
            </configurationvalues>
        </configuration>
    </subconfigurations>
</configuration>
"""
    }
}
