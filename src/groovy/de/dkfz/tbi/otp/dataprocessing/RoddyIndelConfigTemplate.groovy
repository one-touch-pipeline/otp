package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*

class RoddyIndelConfigTemplate {

    static String createConfig(RoddyConfiguration indelPipelineConfiguration, Pipeline.Name pipelineName) {
        return """
<configuration configurationType="project"
               name="${RoddyWorkflowConfig.getNameUsedInConfig(pipelineName, indelPipelineConfiguration.seqType, indelPipelineConfiguration.pluginName, indelPipelineConfiguration.pluginVersion, indelPipelineConfiguration.configVersion)}"
               description="Indel project configuration for ${indelPipelineConfiguration.seqType.roddyName} in OTP."
               imports="${indelPipelineConfiguration.baseProjectConfig}">
    <subconfigurations>
        <configuration name="config" usedresourcessize="xl">
            <availableAnalyses>
                <analysis id="${indelPipelineConfiguration.seqType.roddyName}" configuration="indelCallingAnalysis"/>
            </availableAnalyses>
        </configuration>
    </subconfigurations>
</configuration>
"""
    }
}
