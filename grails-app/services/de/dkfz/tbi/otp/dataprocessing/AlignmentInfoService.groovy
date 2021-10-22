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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.NoSuchBeanDefinitionException

import de.dkfz.tbi.otp.FileNotFoundException
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import de.dkfz.tbi.otp.utils.ProcessOutput

import java.util.regex.Matcher

@Transactional
class AlignmentInfoService {

    ExecuteRoddyCommandService executeRoddyCommandService
    RemoteShellHelper remoteShellHelper


    protected RoddyAlignmentInfo getRoddyAlignmentInformation(RoddyWorkflowConfig workflowConfig) {
        assert workflowConfig
        ProcessOutput output = getRoddyProcessOutput(workflowConfig)
        Map<String, String> config = extractConfigRoddyOutput(output)
        return generateRoddyAlignmentInfo(config, workflowConfig.seqType, workflowConfig.programVersion)
    }

    /**
     * get the output check it and returns result if successful
     * @param workflowConfig
     */
    protected ProcessOutput getRoddyProcessOutput(RoddyWorkflowConfig workflowConfig) {
        Realm realm = workflowConfig.project.realm
        String nameInConfigFile = workflowConfig.nameUsedInConfig
        String cmd = executeRoddyCommandService.roddyGetRuntimeConfigCommand(workflowConfig, nameInConfigFile, workflowConfig.seqType.roddyName)

        ProcessOutput output = remoteShellHelper.executeCommandReturnProcessOutput(realm, cmd)

        if (output.exitCode != 0) {
            log?.debug("Alignment information can't be detected:\n${output}")
            throw new NotSupportedException("Alignment information can't be detected. Is Roddy with support for printidlessruntimeconfig installed?")
        }

        if (output.stderr.contains("The project configuration \"${nameInConfigFile}.config\" could not be found")) {
            log?.debug("Error during output of roddy:\n${output}")
            throw new FileNotFoundException("Roddy could not find the configuration '${nameInConfigFile}'. Probably some access problem.")
        }

        return output
    }

    /**
     * Generate a new RoddyAlignmentInfo by calling necessary Methods
     * for res, bwa and merge
     * @param output ProcessOutput of Roddy File
     * @param seqType
     * @return new Alignment info
     */
    private RoddyAlignmentInfo generateRoddyAlignmentInfo(Map<String, String> config, SeqType seqType, String programVersion) {
        Map bwa = createAlignmentCommandOptionsMap(config, seqType)
        Map merge = createMergeCommandOptionsMap(config, seqType)

        return new RoddyAlignmentInfo(
                alignmentProgram  : bwa.command,
                alignmentParameter: bwa.options,
                samToolsCommand   : config.get("SAMTOOLS_VERSION") ? "Version ${config.get("SAMTOOLS_VERSION")}" : "",
                mergeCommand      : merge.command,
                mergeOptions      : merge.options,
                programVersion    : programVersion,
        )
    }

    /**
     * Generates Merge Map that holds Command and Options
     * for the required MergeTool
     * @param res Map with roddy config output values
     * @param seqType
     * @return Map that holds command and options for the Merging
     */
    private Map createMergeCommandOptionsMap(Map<String, String> config, SeqType seqType) {
        Map merge = [:]

        // Default empty
        merge.options = ''

        MergeTool tool = getMergeTool(config, seqType)
        switch (tool) {
            case MergeTool.BIOBAMBAM:
                merge.command = config.get("BIOBAMBAM_VERSION") ? "Biobambam bammarkduplicates Version ${config.get("BIOBAMBAM_VERSION")}" : ""
                merge.options = config.get("mergeAndRemoveDuplicates_argumentList")
                break
            case MergeTool.PICARD:
                merge.command = config.get("PICARD_VERSION") ? "Picard Version ${config.get("PICARD_VERSION")}" : ""
                break
            case MergeTool.SAMBAMBA:
                merge.command = config.get('SAMBAMBA_MARKDUP_VERSION') ? "Sambamba Version ${config.get('SAMBAMBA_MARKDUP_VERSION')}" : ""
                merge.options = config.get('SAMBAMBA_MARKDUP_OPTS')
                break
            case MergeTool.SAMBAMBA_RNA:
                merge.command = config.get('SAMBAMBA_VERSION') ? "Sambamba Version ${config.get('SAMBAMBA_VERSION')}" : ""
                break
            default:
                merge.command = "Unknown tool: ${tool}"
        }

        if (!merge.command) {
            log?.debug("Could not extract merging configuration from config:\n${config}")
            throw new ParsingException("Could not extract merging configuration value from Roddy config")
        }

        return merge
    }

    /**
     * Generates and returns String that holds what Merging have to be done
     * @return String with the generated tool
     */
    private MergeTool getMergeTool(Map<String, String> config, SeqType seqType) {
        if (seqType.isRna()) {
            return MergeTool.SAMBAMBA_RNA
        }

        String tool = config.get('markDuplicatesVariant')
        return MergeTool.getByName(tool) ?: config.get("useBioBamBamMarkDuplicates") == 'true' ? MergeTool.BIOBAMBAM : MergeTool.PICARD
    }

    /**
     * Creates a Map bwa with command and options for the alignment
     * @return Map that holds command and options for the alignment
     */
    private Map createAlignmentCommandOptionsMap(Map<String, String> config, SeqType seqType) {
        Map bwa = [:]

        if (seqType.isRna()) {
            bwa.command = config.get("STAR_VERSION") ? "STAR Version ${config.get("STAR_VERSION")}" : ""
            bwa.options = ['2PASS', 'OUT', 'CHIMERIC', 'INTRONS'].collect { name ->
                config.get("STAR_PARAMS_${name}".toString())
            }.join(' ')
        } else if (config.get("useAcceleratedHardware") == "true") {
            bwa.command = config.get("BWA_ACCELERATED_VERSION") ? "bwa-bb Version ${config.get("BWA_ACCELERATED_VERSION")}" : ""
            bwa.options = config.get("BWA_MEM_OPTIONS") + ' ' + config.get("BWA_MEM_CONVEY_ADDITIONAL_OPTIONS")
        } else {
            bwa.command = config.get("BWA_VERSION") ? "BWA Version ${config.get("BWA_VERSION")}" : ""
            bwa.options = config.get("BWA_MEM_OPTIONS")
        }

        if (!bwa.command) {
            log?.debug("Could not extract alignment configuration from config:\n${config}")
            throw new ParsingException("Could not extract alignment configuration value from Roddy config")
        }

        return bwa
    }

    /**
     * Extracts Configurations from the roddy Output as a map
     * @param output of Roddy
     * @return Map with Roddy config value output
     */
    private Map<String, String> extractConfigRoddyOutput(ProcessOutput output) {
        Map<String, String> res = [:]
        output.stdout.eachLine { String line ->
            Matcher matcher = line =~ /(?:declare +-x +(?:-i +)?)?([^ =]*)=(.*)/
            if (matcher.matches()) {
                String key = matcher.group(1)
                String value = matcher.group(2)
                res[key] = value.startsWith("\"") && value.length() > 2 ? value.substring(1, value.length() - 1) : value
            }
        }

        if (res.isEmpty()) {
            log?.debug("Could not extract any configuration value from the roddy output:\n${output}")
            throw new ParsingException("Could not extract any configuration value from the roddy output")
        }

        return res
    }

    AlignmentInfo getAlignmentInformationFromConfig(AlignmentConfig config) {
        assert config
        if (config.class == RoddyWorkflowConfig.class) {
            return getRoddyAlignmentInformation((RoddyWorkflowConfig) config)
        }
        return config.alignmentInformation
    }

    Map<String, AlignmentInfo> getAlignmentInformation(Project project) throws Exception {
        try {
            switch (project.alignmentDeciderBeanName) {
                case AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT:
                    List<ReferenceGenomeProjectSeqType> rgpst = ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project)
                    Map<String, AlignmentInfo> result = [:]
                    rgpst*.seqType.unique().sort { it.displayNameWithLibraryLayout }.each { SeqType seqType ->
                        RoddyWorkflowConfig workflowConfig = RoddyWorkflowConfig.getLatestForProject(
                                project, seqType, Pipeline.findByNameAndType(Pipeline.Name.forSeqType(seqType), Pipeline.Type.ALIGNMENT))
                        if (!workflowConfig) {
                            return //pancan not configured for this seq type, skipped
                        }
                        result.put(seqType.displayNameWithLibraryLayout, getRoddyAlignmentInformation(workflowConfig))
                    }
                    return result
                case AlignmentDeciderBeanName.NO_ALIGNMENT:
                    return null
                default:
                    throw new IllegalArgumentException("Unknown Alignment configured: \"${project.alignmentDeciderBeanName}\"")
            }
        } catch (NoSuchBeanDefinitionException e) {
            throw new IllegalArgumentException("Alignment is configured wrong!", e)
        }
    }

    List listReferenceGenome(Project project) {
        return ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project)
    }
}
