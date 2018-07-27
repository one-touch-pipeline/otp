package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.utils.*
import groovy.sql.*
import groovy.transform.*
import org.springframework.beans.factory.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.*
import org.springframework.security.access.prepost.*

import javax.sql.*
import java.util.regex.*

import static de.dkfz.tbi.otp.utils.LocalShellHelper.*

class ProjectOverviewService {

    ExecuteRoddyCommandService executeRoddyCommandService
    ProcessingOptionService processingOptionService
    @Autowired
    ApplicationContext applicationContext
    DataSource dataSource

    static final Collection PROJECT_TO_HIDE_SAMPLE_IDENTIFIER = [
            "MMML",
            "MMML_XP",
            "MMML_RARE_LYMPHOMA_XP",
            "MMML_RARE_LYMPHOMA_EXOMES",
    ].asImmutable()

    /**
     * determine, if the column sample identifier should be hide in the view
     */
    static boolean hideSampleIdentifier(Project project) {
        return PROJECT_TO_HIDE_SAMPLE_IDENTIFIER.contains(project?.name)
    }

    protected AlignmentInfo getRoddyAlignmentInformation(RoddyWorkflowConfig workflowConfig) {
        assert workflowConfig

        ProcessOutput output = getRoddyProcessOutput(workflowConfig)

        return generateAlignmentInfo(output, workflowConfig.seqType, workflowConfig.pluginVersion)
    }

    /**
     * get the output check it and returns result if successful
     * @param workflowConfig
     */
    protected ProcessOutput getRoddyProcessOutput(RoddyWorkflowConfig workflowConfig) {

        String nameInConfigFile = workflowConfig.getNameUsedInConfig()

        ProcessOutput output = executeAndWait(
                executeRoddyCommandService.roddyGetRuntimeConfigCommand(workflowConfig, nameInConfigFile, workflowConfig.seqType.roddyName)
        )

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
     * Generate Alignment Info by calling necessary Methods
     * for res, bwa and merge Map and creating a new ALignment Info
     * on the basic of this Maps
     * @param output ProcessOutput of Roddy File
     * @param seqType
     * @return new Alignment info
     */
    private AlignmentInfo generateAlignmentInfo(ProcessOutput output, SeqType seqType, String pluginVersion) {

        Map<String, String> res = extractConfigRoddyOutput(output)

        Map bwa = createAlignmentCommandOptionsMap(res, output, seqType)

        Map merge = createMergeCommandOptionsMap(res, output, seqType)

        return new AlignmentInfo(
                bwaCommand: bwa.command,
                bwaOptions: bwa.options,
                samToolsCommand: res.get("SAMTOOLS_VERSION") ? "Version ${res.get("SAMTOOLS_VERSION")}" : "",
                mergeCommand: merge.command,
                mergeOptions: merge.options,
                pluginVersion: pluginVersion,
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

        } else if (!tool) {

            tool = res.get("useBioBamBamMarkDuplicates") == 'true' ? MergeConstants.MERGE_TOOL_BIOBAMBAM : MergeConstants.MERGE_TOOL_PICARD

        }

        return tool
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
     * Extracts Configurations from the roddy Output into the Map res
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

        if (config instanceof RoddyWorkflowConfig) {
            return getRoddyAlignmentInformation(config)
        } else {
            throw new UnsupportedOperationException("${config} is not a ${RoddyWorkflowConfig.simpleName}.")
        }
    }

    Map<String, AlignmentInfo> getAlignmentInformation(Project project) throws Exception {
        try {
            switch (applicationContext.getBean(project.alignmentDeciderBeanName).class) {
                case PanCanAlignmentDecider:
                    List<ReferenceGenomeProjectSeqType> rgpst = ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project)
                    Map<String, AlignmentInfo> result = [:]
                    rgpst*.seqType.unique().sort { it.displayNameWithLibraryLayout }.each { SeqType seqType ->
                        RoddyWorkflowConfig workflowConfig = RoddyWorkflowConfig.getLatestForProject(project, seqType, Pipeline.findByNameAndType(Pipeline.Name.forSeqType(seqType), Pipeline.Type.ALIGNMENT))
                        if (!workflowConfig) {
                            return //pancan not configured for this seq type, skipped
                        }
                        result.put(seqType.displayNameWithLibraryLayout, getRoddyAlignmentInformation(workflowConfig))
                    }
                    return result
                case NoAlignmentDecider:
                    return null
                default:
                    throw new Exception("Unknown Alignment configured.")
            }
        } catch (NoSuchBeanDefinitionException e) {
            throw new Exception("Alignment is configured wrong!")
        }
    }

    @Immutable
    static class AlignmentInfo {
        String bwaCommand
        String bwaOptions
        String samToolsCommand
        String mergeCommand
        String mergeOptions
        String pluginVersion
    }

    List overviewProjectQuery(projectName) {
        Project project = Project.findByName(projectName)
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
                    track.sum_N_BasePairsGb
            ]
            queryList.add(queryListSingleRow)
        }
        return queryList
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public Map<String, Map<String, List<String>>> listSampleIdentifierByProject(Project project) {
        return SampleIdentifier.createCriteria().list {
            projections {
                sample {
                    individual {
                        eq('project', project)
                        property("mockFullName")
                    }
                    sampleType {
                        property("name")
                    }
                }
                property("name")
            }
        }.groupBy([{ it[0] }, { it[1] }])
    }

    public List patientsAndSamplesGBCountPerProject(Project project) {
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

    public Long individualCountByProject(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections { countDistinct("mockPid") }
        }
        return seq[0]
    }

    public List sampleTypeNameCountBySample(Project project) {
        List seq = AggregateSequences.withCriteria {
            eq("projectId", project?.id)
            projections {
                groupProperty("sampleTypeName")
                countDistinct("sampleId")
            }
        }
        return seq
    }

    public List centerNameRunId(Project project) {
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

    public List centerNameRunIdLastMonth(Project project) {
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
    public List<SeqType> seqTypeByProject(Project project) {
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
    public List<String> mockPidByProject(Project project) {
        List<String> mockPids = AggregateSequences.withCriteria {
            eq("projectId", project.id)
            projections {
                groupProperty("mockPid")
            }
        }
        return mockPids
    }

    public List<String> sampleTypeByProject(Project project) {
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
    public List<List<String>> overviewMockPidSampleType(Project project) {
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
     * fetch and return all combination of {@link Individual} (as mockpid) and {@link SampleType} with the first {@link SampleIdentifier}
     * as list.
     * <br> Example:[[patient1, sampleType1, SampleIdentifier1],[patient1, sampleType2, SampleIdentifier2],[patient1, sampleType3, SampleIdentifier3]...]
     * @param project the project for filtering the result
     * @return all combination of name of individual(mockPid) and sampleTypeName with the first SampleIdentifier as list
     *
     */
    public List<Object> overviewSampleIdentifier(Project project) {
        List<Object> sampleIdentifiers = SampleIdentifier.withCriteria {
            projections {
                sample {
                    individual {
                        eq("project", project)
                        groupProperty("mockPid")
                    }
                    sampleType {
                        groupProperty("name")
                    }
                }
                min("name")
            }
        }
        return sampleIdentifiers
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
    public List<Map> laneCountForSeqtypesPerPatientAndSampleType(Project project) {
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

    public Collection<AbstractMergedBamFile> abstractMergedBamFilesInProjectFolder(Project project) {
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

    public List listReferenceGenome(Project project) {
        return ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project)
    }

    @PreAuthorize("hasRole('ROLE_MMML_MAPPING')")
    public List tableForMMMLMapping() {
        def seq = Individual.withCriteria {
            project {
                'in'("name", PROJECT_TO_HIDE_SAMPLE_IDENTIFIER)
            }
            projections {
                property("id")
                property("mockFullName")
                property("internIdentifier")
            }
            order("id", "desc")
        }
        return seq
    }

    public List getAccessPersons(Project project) {
        getAccessPersons([project])
    }

    public List getAccessPersons(List<Project> projects) {
        String query = """\
        SELECT username
        FROM users
        JOIN user_role ON users.id = user_role.user_id
        JOIN role AS r ON user_role.role_id = r.id
        JOIN acl_sid ON r.authority = acl_sid.sid
        JOIN acl_entry ON acl_sid.id = acl_entry.sid
        JOIN acl_object_identity ON acl_entry.acl_object_identity = acl_object_identity.id
        JOIN acl_class ON acl_object_identity.object_id_class = acl_class.id
        WHERE object_id_identity in ( ${projects*.id.join(', ')} ) AND acl_class.class = :className
        GROUP BY username ORDER BY username
        """
        List accessPersons = []
        def sql = new Sql(dataSource)
        sql.eachRow(query, [className: Project.name]) {
            accessPersons.add(it.username)
        }
        return accessPersons.unique()
    }
}

