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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import de.dkfz.tbi.otp.utils.ProcessOutput

import javax.sql.DataSource
import java.util.regex.Matcher

@Transactional
class ProjectOverviewService {

    ExecuteRoddyCommandService executeRoddyCommandService
    ProcessingOptionService processingOptionService
    AlignmentDeciderService alignmentDeciderService
    DataSource dataSource

    RemoteShellHelper remoteShellHelper

    @Autowired
    ApplicationContext applicationContext


    protected RoddyAlignmentInfo getRoddyAlignmentInformation(RoddyWorkflowConfig config) {
        assert config
        ProcessOutput output = getRoddyProcessOutput(config)
        return generateRoddyAlignmentInfo(output, config.seqType, config.programVersion)
    }

    /**
     * get the output check it and returns result if successful
     * @param workflowConfig
     */
    protected ProcessOutput getRoddyProcessOutput(RoddyWorkflowConfig workflowConfig) {
        Realm realm = workflowConfig.project.realm
        String nameInConfigFile = workflowConfig.getNameUsedInConfig()
        String cmd = executeRoddyCommandService.roddyGetRuntimeConfigCommand(workflowConfig, nameInConfigFile, workflowConfig.seqType.roddyName)

       ProcessOutput output = remoteShellHelper.executeCommandReturnProcessOutput(realm, cmd)

        if (output.exitCode != 0) {
            log?.debug("Alignment information can't be detected:\n${output}")
            throw new RuntimeException("Alignment information can't be detected. Is Roddy with support for printidlessruntimeconfig installed?")
        }

        if (output.stderr.contains("The project configuration \"${nameInConfigFile}.config\" could not be found")) {
            log?.debug("Error during output of roddy:\n${output}")
            throw new RuntimeException("Roddy could not find the configuration '${nameInConfigFile}'. Probably some access problem.")
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
    private RoddyAlignmentInfo generateRoddyAlignmentInfo(ProcessOutput output, SeqType seqType, String programVersion) {
        Map<String, String> res = extractConfigRoddyOutput(output)

        Map bwa = createAlignmentCommandOptionsMap(res, output, seqType)

        Map merge = createMergeCommandOptionsMap(res, output, seqType)

        return new RoddyAlignmentInfo(
                alignmentProgram  : bwa.command,
                alignmentParameter: bwa.options,
                samToolsCommand   : res.get("SAMTOOLS_VERSION") ? "Version ${res.get("SAMTOOLS_VERSION")}" : "",
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
    private Map createMergeCommandOptionsMap(Map<String, String> res, ProcessOutput output, SeqType seqType) {
        Map merge = [:]

        // Default empty
        merge.options = ''

        String tool = getMergingTool(res, seqType)
        switch (tool) {
            case MergeConstants.MERGE_TOOL_BIOBAMBAM:
                merge.command = res.get("BIOBAMBAM_VERSION") ? "Biobambam bammarkduplicates Version ${res.get("BIOBAMBAM_VERSION")}" : ""
                merge.options = res.get("mergeAndRemoveDuplicates_argumentList")
                break
            case MergeConstants.MERGE_TOOL_PICARD:
                merge.command = res.get("PICARD_VERSION") ? "Picard Version ${res.get("PICARD_VERSION")}" : ""
                break
            case MergeConstants.MERGE_TOOL_SAMBAMBA:
                merge.command = res.get('SAMBAMBA_MARKDUP_VERSION') ? "Sambamba Version ${res.get('SAMBAMBA_MARKDUP_VERSION')}" : ""
                merge.options = res.get('SAMBAMBA_MARKDUP_OPTS')
                break
            case MergeConstants.MERGE_TOOL_SAMBAMBA_RNA:
                merge.command = res.get('SAMBAMBA_VERSION') ? "Sambamba Version ${res.get('SAMBAMBA_VERSION')}" : ""
                break
            default:
                merge.command = "Unknown tool: ${tool}"
        }

        if (!merge.command) {
            log?.debug("Could not extract merging configuration from output:\n${output}")
            throw new RuntimeException("Could not extract merging configuration value from the roddy output")
        }

        return merge
    }

    /**
     * Generates and returns String that holds what Merging have to be done
     * @return String with the generated tool
     */
    private String getMergingTool(Map<String, String> res, SeqType seqType) {
        String tool = res.get('markDuplicatesVariant')

        if (seqType.isRna()) {
            tool = MergeConstants.MERGE_TOOL_SAMBAMBA_RNA
        }
        return tool ?: res.get("useBioBamBamMarkDuplicates") == 'true' ? MergeConstants.MERGE_TOOL_BIOBAMBAM : MergeConstants.MERGE_TOOL_PICARD
    }

    /**
     * Creates a Map bwa with command and options for the alignment
     * @return Map that holds command and options for the alignment
     */
    private Map createAlignmentCommandOptionsMap(Map<String, String> res, ProcessOutput output, SeqType seqType) {
        Map bwa = [:]

        if (seqType.isRna()) {
            bwa.command = res.get("STAR_VERSION") ? "STAR Version ${res.get("STAR_VERSION")}" : ""
            bwa.options = ['2PASS', 'OUT', 'CHIMERIC', 'INTRONS'].collect { name ->
                res.get("STAR_PARAMS_${name}".toString())
            }.join(' ')
        } else if (res.get("useAcceleratedHardware") == "true") {
            bwa.command = res.get("BWA_ACCELERATED_VERSION") ? "bwa-bb Version ${res.get("BWA_ACCELERATED_VERSION")}" : ""
            bwa.options = res.get("BWA_MEM_OPTIONS") + ' ' + res.get("BWA_MEM_CONVEY_ADDITIONAL_OPTIONS")
        } else {
            bwa.command = res.get("BWA_VERSION") ? "BWA Version ${res.get("BWA_VERSION")}" : ""
            bwa.options = res.get("BWA_MEM_OPTIONS")
        }

        if (!bwa.command) {
            log?.debug("Could not extract alignment configuration from output:\n${output}")
            throw new RuntimeException("Could not extract alignment configuration value from the roddy output")
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
            throw new RuntimeException("Could not extract any configuration value from the roddy output")
        }

        return res
    }

    AlignmentInfo getAlignmentInformationFromConfig(AlignmentConfig config) {
        assert config
        if (config.class == RoddyWorkflowConfig.class) {
            return getRoddyAlignmentInformation((RoddyWorkflowConfig) config)
        }
        return config.getAlignmentInformation()
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

    @PreAuthorize("hasRole('ROLE_OPERATOR') or hasPermission(#project, 'OTP_READ_ACCESS')")
    List overviewProjectQuery(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            property("individualId")
            property("mockPid")
            property("sampleTypeName")
            property("seqTypeDisplayName")
            property("libraryLayout")
            property("singleCell")
            property("seqPlatformId")
            property("seqCenterName")
            property("laneCount")
            property("sum_N_BasePairsGb")
            property("projectName")
            order("mockPid")
            order("sampleTypeName")
            order("seqTypeDisplayName")
            order("libraryLayout")
            order("seqPlatformId")
            order("seqCenterName")
            order("laneCount")
        }
        List queryList = []
        for (def track in seq) {
            def queryListSingleRow = [
                    track.mockPid,
                    track.sampleTypeName,
                    track.seqTypeDisplayName,
                    track.libraryLayout,
                    track.singleCell,
                    track.seqCenterName,
                    SeqPlatform.get(track.seqPlatformId).toString(),
                    track.laneCount,
                    track.sum_N_BasePairsGb,
            ]
            queryList.add(queryListSingleRow)
        }
        return queryList
    }

    /**
     * Returns the sampleIdentifier strings in a project, in a map keyed by Individual+SampleType+SeqType.
     * <p>
     * Key is the three-item list <pre>[ 'Individual.mockFullName', 'sampleType.name', 'SeqType layout single/bulk']</pre>
     * Value is a combined list of sample identifiers for this combination, taken from the SeqTracks.</p>
     * <p>
     * Example result:
     * <pre>
     * [
     *   [ 'indivA', 'tumor',   'WGS Paired bulk' ] : [ 'sampleIdA1', 'sampleIdA2', 'sampleIdA3' ]
     *   [ 'indivA', 'control', 'WGS Paired bulk' ] : [ 'sampleIdA4', 'sampleIdA5', 'sampleIdA6' ]
     *   [ 'indivB', 'tumor',   'WGS Paired bulk' ] : [ 'sampleIdB1', 'sampleIdB2', 'sampleIdB3' ]
     *   [ 'indivB', 'control', 'WGS Paired bulk' ] : [ 'sampleIdB4', 'sampleIdB5', 'sampleIdB6' ]
     * ]
     * </pre>
     * </p>
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Map<List<String>, List<String>> listSampleIdentifierByProject(Project project) {
        return SeqTrack.createCriteria().list {
            projections {
                sample {
                    individual {
                        eq('project', project)
                        property('mockFullName')
                    }
                    sampleType {
                        property('name')
                    }
                }
                property('seqType')
                property('sampleIdentifier')
            }
        }
        .groupBy { it[0..2] } // group by Individual, SampleType, SeqType
        .collectEntries { k, v ->
            // replace SeqType-object with desired display string
            List<String> newKey = [k[0], k[1], k[2].getDisplayNameWithLibraryLayout()]

            // keep only SampleId (last element) in the value; project+SampleType+SeqType are already in the key.
            List<String> newVal = v.collect { it[-1] }.sort().unique()

            [ (newKey): newVal]
        }
    }

    List patientsAndSamplesGBCountPerProject(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("seqTypeDisplayName")
                groupProperty("libraryLayout")
                groupProperty("singleCell")
                countDistinct("mockPid")
                count("sampleId")
                sum("sum_N_BasePairsGb")
            }
            order("seqTypeDisplayName")
        }
        return seq
    }

    Long individualCountByProject(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections { countDistinct("mockPid") }
        }
        return seq[0]
    }

    List sampleTypeNameCountBySample(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("sampleTypeName")
                countDistinct("sampleId")
            }
        }
        return seq
    }

    List centerNameRunId(Project project) {
        List seq = Sequence.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("seqCenterName")
                countDistinct("runId")
            }
            order("seqCenterName")
        }
        return seq
    }

    List centerNameRunIdLastMonth(Project project) {
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -6)
        Date date = cal.getTime()
        List seq = Sequence.withCriteria {
            eq("projectId", project?.id)
            gt("dateExecuted", date)
            projections {
                groupProperty("seqCenterName")
                countDistinct("runId")
            }
            order("seqCenterName")
        }
        return seq
    }

    /**
     * @param project the project for filtering the result
     * @return all SeqTypes used in the project
     */
    List<SeqType> seqTypeByProject(Project project) {
        List<Long> seqTypeIds = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("seqTypeId")
            }
        }
        List<SeqType> seqTypes = []
        if (seqTypeIds) {
            seqTypes = SeqType.withCriteria {
                'in'("id", seqTypeIds)
                order("name")
                order("libraryLayout")
            }
        }
        return seqTypes
    }

    /**
     * @param project the project for filtering the result
     * @return all MockPids used in the project
     */
    List<String> mockPidByProject(Project project) {
        List<String> mockPids = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("mockPid")
            }
        }
        return mockPids
    }

    List<String> sampleTypeByProject(Project project) {
        List<String> sampleTypes = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("sampleTypeName")
            }
            order("sampleTypeName", "asc")
        }
        return sampleTypes
    }

    /**
     * fetch and return all combination of individual(mockPid) and sampleTypeName as list.
     * <br> Example: [[patient1, sampleType1],[patient1, sampleType2]...]
     *
     * @param project the project for filtering the result
     * @return all combination of individual(mockPid) and sampleTypeName as list
     */
    List<List<String>> overviewMockPidSampleType(Project project) {
        List<List<String>> mockPidSampleTypes = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("mockPid")
                groupProperty("sampleTypeName")
            }
        }
        return mockPidSampleTypes
    }

    /**
     * fetch and return all combination of {@link Individual} (as mockpid) and of Sample type name with the number of lanes depend of {@link SeqType}
     * as list.
     * <br> Example:[
     * [mockPid: patient1, sampleTypeName: sampleTypeName1, seqType: sampleType1, laneCount: laneCount1],
     * [mockPid: patient1, sampleTypeName: sampleTypeName1, seqType: sampleType2, laneCount: laneCount2],
     * [mockPid: patient1, sampleTypeName: sampleTypeName2, seqType: sampleType1, laneCount: laneCount3],
     * [mockPid: patient2, sampleTypeName: sampleTypeName1, seqType: sampleType1, laneCount: laneCount4],
     * ...]
     * @param project the project for filtering the result
     * @return all combination of  name of {@link Individual}(mockPid) and sampleTypeName with with the number of lanes depend of {@link SeqType}  as list
     */
    List<Map> laneCountForSeqtypesPerPatientAndSampleType(Project project) {
        List lanes = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("mockPid")
                groupProperty("sampleTypeName")
                groupProperty("seqTypeId")
                sum("laneCount")
            }
        }

        Map<Long, SeqType> seqTypes = [:]

        List<Map> ret = lanes.collect {
            SeqType seqType = seqTypes[it[2]]
            if (!seqType) {
                seqType = SeqType.get(it[2])
                seqTypes.put(it[2], seqType)
            }
            [
                    mockPid       : it[0],
                    sampleTypeName: it[1],
                    seqType       : seqType,
                    laneCount     : it[3],
            ]
        }
        return ret
    }

    Collection<AbstractMergedBamFile> abstractMergedBamFilesInProjectFolder(Project project) {
        if (!project) {
            return []
        }
        return AbstractMergedBamFile.executeQuery("""
from
        AbstractMergedBamFile abstractMergedBamFile
where
        workPackage.sample.individual.project = :project
        and workPackage.bamFileInProjectFolder = abstractMergedBamFile
        and fileOperationStatus = :fileOperationStatus
""", [project: project, fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED])
    }

    List listReferenceGenome(Project project) {
        return ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project)
    }
}
