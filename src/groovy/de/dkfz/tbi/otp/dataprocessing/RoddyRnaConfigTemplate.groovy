package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*

class RoddyRnaConfigTemplate {


    static String createConfig(RoddyConfiguration rnaAlignmentConfiguration, Pipeline.Name pipelineName) {
        return """
<configuration configurationType="project"
               name="${RoddyWorkflowConfig.getNameUsedInConfig(pipelineName, rnaAlignmentConfiguration.seqType, rnaAlignmentConfiguration.pluginName, rnaAlignmentConfiguration.pluginVersion, rnaAlignmentConfiguration.configVersion)}"
               description="Project configuration for ${rnaAlignmentConfiguration.seqType.roddyName} in OTP."
               imports="${rnaAlignmentConfiguration.baseProjectConfig}">
    <subconfigurations>
        <configuration name="config" usedresourcessize="${rnaAlignmentConfiguration.resources}">
            <availableAnalyses>
                <analysis id="${rnaAlignmentConfiguration.seqType.roddyName}" configuration="RNAseqAnalysis"/>
            </availableAnalyses>
        </configuration>
    </subconfigurations>
</configuration>
"""
    }
}
