/*
 * Copyright 2011-2020 The OTP authors
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

package migration

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.project.PanCanAlignmentConfiguration
import de.dkfz.tbi.otp.project.RoddyConfiguration

ProcessingOptionService processingOptionService = ctx.processingOptionService

RoddyWorkflowConfig.findAllByObsoleteDateIsNullAndMd5sumIsNull().each { RoddyWorkflowConfig config ->
    if (!config.seqType) {
        println "seqtype is null for ${config} ${config.project}"
    } else if (!(new File(config.configFilePath).exists())) {
        println "config does not exist ${config} ${config.project} (${config.configFilePath})"
    } else {
        String expectedConfigContent
        String configContent = new File(config.configFilePath).text

        Map commonConfig = [
                project          : config.project,
                seqType          : config.seqType,
                programVersion   : config.programVersion,
                configVersion    : config.version,
                baseProjectConfig: ".*",
        ]

        switch (config.pipeline.name) {
            case Pipeline.Name.PANCAN_ALIGNMENT:
                expectedConfigContent = RoddyPanCanConfigTemplate.createConfig(new PanCanAlignmentConfiguration(
                        commonConfig + [
                                referenceGenome      : ".*",
                                statSizeFileName     : ".*",
                                mergeTool            : processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_MERGE_TOOL, config.seqType.roddyName),
                                pluginName           : processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_NAME, config.seqType.roddyName),
                                bwaMemVersion        : ".*",
                                sambambaVersion      : ".*",
                                adapterTrimmingNeeded: config.adapterTrimmingNeeded,
                        ]))
                break;
            case Pipeline.Name.RODDY_RNA_ALIGNMENT:
                expectedConfigContent = RoddyRnaConfigTemplate.createConfig(new RoddyConfiguration(
                        commonConfig + [
                                pluginName: processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_NAME, config.seqType.roddyName),
                        ]), config.pipeline.name)
                break;
            case Pipeline.Name.RODDY_SNV:
                expectedConfigContent = RoddySnvConfigTemplate.createConfig(new RoddyConfiguration(
                        commonConfig + [
                                pluginName: processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_NAME, config.seqType.roddyName),
                        ]), config.pipeline.name)
                break;
            case Pipeline.Name.RODDY_INDEL:
                expectedConfigContent = RoddyIndelConfigTemplate.createConfig(new RoddyConfiguration(
                        commonConfig + [
                                pluginName: processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_INDEL_DEFAULT_PLUGIN_NAME, config.seqType.roddyName),
                        ]), config.pipeline.name)
                break;
            case Pipeline.Name.RODDY_SOPHIA:
                expectedConfigContent = RoddySophiaConfigTemplate.createConfig(new RoddyConfiguration(
                        commonConfig + [
                                pluginName: processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_SOPHIA_DEFAULT_PLUGIN_NAME, config.seqType.roddyName),
                        ]), config.pipeline.name)
                break;
            case Pipeline.Name.RODDY_ACESEQ:
                expectedConfigContent = RoddyAceseqConfigTemplate.createConfig(new RoddyConfiguration(
                        commonConfig + [
                                pluginName: processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_ACESEQ_DEFAULT_PLUGIN_NAME, config.seqType.roddyName),
                        ]), config.pipeline.name)
                break;
        }
        expectedConfigContent = expectedConfigContent.replaceAll(/name="${config.pipeline.name}.*"/, "name=\"${config.nameUsedInConfig}\"")
                .replaceAll(/description="Alignment configuration for project .*"/, "description=\"Alignment configuration for project .*\"")

        if (configContent =~ expectedConfigContent) {
            config.md5sum = configContent.encodeAsMD5()
            config.save(flush: true)
        }
    }
}
[]
