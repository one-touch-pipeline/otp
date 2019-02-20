package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.RoddyConfiguration

class RoddySnvConfigTemplate {

    static String createConfig(RoddyConfiguration snvPipelineConfiguration, Pipeline.Name pipelineName) {
        return """
<configuration configurationType="project"
               name="${RoddyWorkflowConfig.getNameUsedInConfig(pipelineName, snvPipelineConfiguration.seqType, snvPipelineConfiguration.pluginName, snvPipelineConfiguration.pluginVersion, snvPipelineConfiguration.configVersion)}"
               description="SNV project configuration for ${snvPipelineConfiguration.seqType.roddyName} in OTP."
               imports="${snvPipelineConfiguration.baseProjectConfig}">
    <subconfigurations>
        <configuration name="config" usedresourcessize="${snvPipelineConfiguration.resources}">
            <availableAnalyses>
                <analysis id="${snvPipelineConfiguration.seqType.roddyName}" configuration="snvCallingAnalysis"/>
            </availableAnalyses>
        </configuration>
    </subconfigurations>
</configuration>
"""
    }
}
