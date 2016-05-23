package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.PanCanAlignmentConfiguration
import groovy.transform.TupleConstructor
import org.apache.commons.lang.StringEscapeUtils

class RoddyPanCanConfigTemplate {

    @TupleConstructor
    static enum SeqTypeOptions {
        WES('exomeAnalysis', '''
                <!-- BWA Binary -->
                <cvalue name="BWA_BINARY" value="${bwaBinary_bwa078}" type="filename"
                        description="The BWA version"/>
'''),
        WGS('qcAnalysis', '''
                <!-- BWA Binary -->
                <cvalue name="BWA_BINARY" value="${bwaBinary_bwa078}" type="filename"
                        description="The BWA version"/>
'''),
        WGBS('bisulfiteCoreAnalysis', '''
                <!-- BWA Binary -->
                <cvalue name="BWA_BINARY" value="bwa-0.7.8-bisulfite" type="filename"
                        description="The BWA version suffixed by -bisulfite has the FASTQ read identifier check turned off, compared to the version without the suffix."/>

                <cvalue name='IS_TAGMENTATION' value="false" type="boolean"
                        description="true: tagmentation; false: standard WGBS."/>
'''),
        WGBSTAG('bisulfiteCoreAnalysis', '''
                <!-- BWA Binary -->
                <cvalue name="BWA_BINARY" value="bwa-0.7.8-bisulfite" type="filename"
                        description="The BWA version suffixed by -bisulfite has the FASTQ read identifier check turned off, compared to the version without the suffix."/>

                <cvalue name='IS_TAGMENTATION' value="true" type="boolean"
                        description="true: tagmentation; false: standard WGBS."/>
''')

        final String analysis

        final String additionalProperties

        static SeqTypeOptions findByRoddyName(String roddyName) {
            SeqTypeOptions template = values().find {
                it.name().compareToIgnoreCase(roddyName) == 0
            }
            assert template: "No template found for ${roddyName}"
            return template
        }
    }

    static String createConfig(PanCanAlignmentConfiguration panCanAlignmentConfiguration) {
        SeqTypeOptions template = SeqTypeOptions.findByRoddyName(panCanAlignmentConfiguration.seqType.roddyName)

        return """
<configuration
        configurationType='project'
        name='${RoddyWorkflowConfig.getNameUsedInConfig(Pipeline.Name.PANCAN_ALIGNMENT, panCanAlignmentConfiguration.seqType,
                "${panCanAlignmentConfiguration.pluginName}:${panCanAlignmentConfiguration.pluginVersion}", panCanAlignmentConfiguration.configVersion)}'
        description='Alignment configuration for project ${panCanAlignmentConfiguration.project.name}.'
        imports='${panCanAlignmentConfiguration.baseProjectConfig}'>

    <subconfigurations>
        <configuration name='config' usedresourcessize='xl'>
            <availableAnalyses>
                <analysis id='${panCanAlignmentConfiguration.seqType.roddyName}' configuration='${template.analysis}' killswitches='FilenameSection'/>
            </availableAnalyses>
            <configurationvalues>

                <!-- Convey -->
                <cvalue name='useAcceleratedHardware' value='false' type='boolean'
                    description='Map reads with Convey BWA-MEM (true) or software BWA-MEM (false; PCAWF standard)'/>

                <!-- Merge tool -->
                <cvalue name='markDuplicatesVariant' value='${panCanAlignmentConfiguration.mergeTool}' type='string'
                    description='Allowed values: biobambam, picard, sambamba. Default: empty. If set, this option takes precedence over the older useBioBamBamMarkDuplicates option.'/>

${template.additionalProperties}

            </configurationvalues>
        </configuration>
    </subconfigurations>
</configuration>
"""
    }

    static String createConfigBashEscaped(PanCanAlignmentConfiguration panCanAlignmentConfiguration) {
        createConfig(panCanAlignmentConfiguration).replaceAll(/\$/, /\\\$/)
    }

}


