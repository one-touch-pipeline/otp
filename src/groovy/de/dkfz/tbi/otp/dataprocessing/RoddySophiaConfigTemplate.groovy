package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*

class RoddySophiaConfigTemplate {

    static String createConfig(RoddyConfiguration sophiaPipelineConfiguration, Pipeline.Name pipelineName) {
        return """
<configuration configurationType='project'
               name='${RoddyWorkflowConfig.getNameUsedInConfig(pipelineName, sophiaPipelineConfiguration.seqType, sophiaPipelineConfiguration.pluginName, sophiaPipelineConfiguration.pluginVersion, sophiaPipelineConfiguration.configVersion)}'
               description='Project configuration for ${sophiaPipelineConfiguration.seqType.roddyName} in OTP.'
               imports="${sophiaPipelineConfiguration.baseProjectConfig}">

    <subconfigurations>
        <configuration name='config' usedresourcessize='xl'>
            <availableAnalyses>
                <analysis id='${sophiaPipelineConfiguration.seqType.roddyName}' configuration='sophiaAnalysis'/>
            </availableAnalyses>
        </configuration>
    </subconfigurations>
</configuration>
"""
    }
}
