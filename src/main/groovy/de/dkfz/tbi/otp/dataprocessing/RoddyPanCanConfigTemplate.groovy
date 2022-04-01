/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing

import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.project.PanCanAlignmentConfiguration

class RoddyPanCanConfigTemplate {

    @TupleConstructor
    static enum SeqTypeOptions {
        WES('exomeAnalysis', ''),
        WGS('qcAnalysis', ''),
        CHIPSEQ('qcAnalysis', ''),
        WGBS('bisulfiteCoreAnalysis', '''
                <!-- BWA Version -->
                <cvalue name="BWA_VERSION" value="0.7.8" type="string"
                        description="Use e.g. 0.7.8-r2.05 for a specific revision of bb-bwa. Suffix with -bisulfite to load a bisulfite patched version."/>

                <cvalue name="IS_TAGMENTATION" value="false" type="boolean"
                        description="true: tagmentation; false: standard WGBS."/>
'''),
        WGBSTAG('bisulfiteCoreAnalysis', '''
                <!-- BWA Version -->
                <cvalue name="BWA_VERSION" value="0.7.8" type="string"
                        description="Use e.g. 0.7.8-r2.05 for a specific revision of bb-bwa. Suffix with -bisulfite to load a bisulfite patched version."/>

                <cvalue name="IS_TAGMENTATION" value="true" type="boolean"
                        description="true: tagmentation; false: standard WGBS."/>
''')

        final String analysis

        final String additionalProperties

        static SeqTypeOptions getByRoddyName(String roddyName) {
            SeqTypeOptions template = values().find {
                it.name().compareToIgnoreCase(roddyName) == 0
            }
            assert template: "No template found for ${roddyName}"
            return template
        }
    }

    static String createConfig(PanCanAlignmentConfiguration panCanAlignmentConfiguration) {
        SeqTypeOptions template = SeqTypeOptions.getByRoddyName(panCanAlignmentConfiguration.seqType.roddyName)
        String additional = ""

        if (panCanAlignmentConfiguration.seqType.isWgbs()) {
            additional = template.additionalProperties
        } else {
            additional = """
                <!-- BWA Version -->
                <cvalue name="BWA_VERSION" value="${panCanAlignmentConfiguration.bwaMemVersion}" type="string"
                        description="Use e.g. 0.7.8-r2.05 for a specific revision of bb-bwa. Suffix with -bisulfite to load a bisulfite patched version."/>
                """
        }

        if (panCanAlignmentConfiguration.mergeTool == MergeTool.SAMBAMBA.name) {
            additional += """
                <!-- Merging and Markdup version of sambamba -->
                <cvalue name="SAMBAMBA_MARKDUP_VERSION" value="${panCanAlignmentConfiguration.sambambaVersion}" type="string"
                description="Only used for duplication marking."/>
                """
        }

        return """
<configuration
        configurationType="project"
        name="${RoddyWorkflowConfig.getNameUsedInConfig(Pipeline.Name.PANCAN_ALIGNMENT, panCanAlignmentConfiguration.seqType,
                "${panCanAlignmentConfiguration.pluginName}:${panCanAlignmentConfiguration.programVersion}", panCanAlignmentConfiguration.configVersion)}"
        description="Alignment configuration for project ${panCanAlignmentConfiguration.project.name}."
        imports="${panCanAlignmentConfiguration.baseProjectConfig}">

    <subconfigurations>
        <configuration name="config" usedresourcessize="${panCanAlignmentConfiguration.resources}">
            <availableAnalyses>
                <analysis id="${panCanAlignmentConfiguration.seqType.roddyName}" configuration="${template.analysis}" killswitches="FilenameSection"/>
            </availableAnalyses>
            <configurationvalues>

                <cvalue name="useAcceleratedHardware" value="false" type="boolean"
                    description=""/>

                <!-- Merge tool -->
                <cvalue name="markDuplicatesVariant" value="${panCanAlignmentConfiguration.mergeTool}" type="string"
                    description="Allowed values: biobambam, picard, sambamba. Default: empty. If set, this option takes precedence over the older
                    useBioBamBamMarkDuplicates option."/>

${additional}

            </configurationvalues>
        </configuration>
    </subconfigurations>
</configuration>
"""
    }
}
