package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.plugin.springsecurity.*
import grails.plugin.springsecurity.acl.*
import groovy.transform.*
import org.springframework.security.access.prepost.*
import org.springframework.security.acls.domain.*
import org.springframework.security.acls.model.*
import org.springframework.security.core.userdetails.*

import java.nio.file.*
import java.nio.file.attribute.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*


/**
 * Service providing methods to access information about Projects.
 *
 */
class ProjectService {

    static final String PHIX_INFIX = 'PhiX'
    static final List<String> processingPriorities = ["NORMAL","FAST_TRACK"]

    AclUtilService aclUtilService
    ExecutionService executionService
    GroupService groupService
    ReferenceGenomeService referenceGenomeService
    SpringSecurityService springSecurityService

    /**
     *
     * @return List of all available Projects
     */
    @PostFilter("hasRole('ROLE_OPERATOR') or hasPermission(filterObject, 'read')")
    public List<Project> getAllProjects() {
        return Project.list(sort: "name", order: "asc", fetch: [projectCategories: 'join', projectGroup: 'join'])
    }

    /**
     * Returns the Project in an acl aware manner
     * @param id The Id of the Project
     * @return The Project
     */
    @PostAuthorize("hasRole('ROLE_OPERATOR') or returnObject == null or hasPermission(returnObject, 'read')")
    public Project getProject(Long id) {
        return Project.get(id)
    }

    @PostAuthorize("hasRole('ROLE_OPERATOR') or returnObject == null or hasPermission(returnObject, 'read')")
    public Project getProjectByName(String name) {
        return Project.findByName(name)
    }

    public List<Project> projectByProjectGroup(ProjectGroup projectGroup) {
        return Project.findAllByProjectGroup(projectGroup, [sort: "name", order: "asc"])
    }

    /**
     * Creates a Project and grants permissions to Groups which have read/write privileges for Projects.
     * @param name
     * @param dirName
     * @param realmName
     * @return The created project
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public Project createProject(String name, String dirName, String realmName, String alignmentDeciderBeanName, List<String> categoryNames) {
        Project project = new Project(
                name: name,
                dirName: dirName,
                realmName: realmName,
                alignmentDeciderBeanName: alignmentDeciderBeanName,
                projectCategories: categoryNames.collect { exactlyOneElement(ProjectCategory.findAllByName(it)) },
        )
        project = project.save(flush: true)
        assert(project != null)
        // add to groups
        Group.list().each { Group group ->
            if (group.readProject) {
                Sid sid = new GrantedAuthoritySid(group.role.authority)
                aclUtilService.addPermission(project, sid, BasePermission.READ)
                if (group.writeProject) {
                    aclUtilService.addPermission(project, sid, BasePermission.WRITE)
                }
            }
        }
        return project
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public Project createProject(ProjectParams projectParams) {
        assert OtpPath.isValidPathComponent(projectParams.unixGroup): "unixGroup '${projectParams.unixGroup}' contains invalid characters"
        Project project = createProject(projectParams.name, projectParams.dirName, projectParams.realmName, projectParams.alignmentDeciderBeanName, projectParams.categoryNames)
        project.dirAnalysis = projectParams.dirAnalysis
        project.processingPriority = projectParams.processingPriority
        project.hasToBeCopied = projectParams.copyFiles
        project.fingerPrinting = projectParams.fingerPrinting
        project.nameInMetadataFiles = projectParams.nameInMetadataFiles
        project.setProjectGroup(ProjectGroup.findByName(projectParams.projectGroup))
        project.mailingListName = projectParams.mailingListName
        project.description = projectParams.description
        project.unixGroup = projectParams.unixGroup
        assert project.save(flush: true, failOnError: true)

        GroupCommand groupCommand = new GroupCommand(
                name: projectParams.name,
                description: "group for ${projectParams.name}",
                readProject: false,
                writeProject: false,
                readJobSystem: false,
                writeJobSystem: false,
                readSequenceCenter: false,
                writeSequenceCenter: false,
        )
        Group group = groupService.createGroup(groupCommand)
        groupService.aclUtilService.addPermission(project, new GrantedAuthoritySid(group.role.authority), BasePermission.READ)

        File projectDirectory = project.getProjectDirectory()
        if (projectDirectory.exists()) {
            PosixFileAttributes attrs = Files.readAttributes(projectDirectory.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attrs.group().toString() == projectParams.unixGroup) {
                return project
            }
        }
        executeScript(buildCreateProjectDirectory(projectParams.unixGroup, projectDirectory), project)
        WaitingFileUtils.waitUntilExists(projectDirectory)

        return project
    }

    @Immutable
    public static class ProjectParams {
        String name
        String dirName
        String dirAnalysis
        String realmName
        String alignmentDeciderBeanName
        List<String> categoryNames
        String unixGroup
        String projectGroup
        String nameInMetadataFiles
        boolean copyFiles
        boolean fingerPrinting
        String mailingListName
        String description
        short processingPriority
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public void updateAnalysisDirectory(String analysisDirectory, Project project) {
        project.dirAnalysis = analysisDirectory
        project.save(flush: true, failOnError: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public void updateNameInMetadata(String nameInMetadata, Project project) {
        project.nameInMetadataFiles = nameInMetadata
        project.save(flush: true, failOnError: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public void updateCategory(List<String> categoryNames, Project project) {
        project.projectCategories = categoryNames.collect { exactlyOneElement(ProjectCategory.findAllByName(it)) }
        project.save(flush: true, failOnError: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public void updateMailingListName(String mailingListName, Project project) {
        project.mailingListName = mailingListName
        project.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public void updateDescription(String description, Project project) {
        project.description = description
        project.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public void updateProcessingPriority(short processingPriority, Project project) {
        project.processingPriority = processingPriority
        project.save(flush: true)
    }

    /**
     * Discovers if a user has Projects.
     * @return <code>true</code> if the user has Project(s), false otherwise
     */
    public boolean projectsAvailable() {
        if (SpringSecurityUtils.ifAllGranted("ROLE_OPERATOR")) {
            // shortcut for operator
            if (Project.count() > 0) {
                return true
            }
            return false
        }
        // for normal users
        Set<String> roles = SpringSecurityUtils.authoritiesToRoles(SpringSecurityUtils.getPrincipalAuthorities())
        if (springSecurityService.isLoggedIn()) {
            // anonymous users do not have a principal
            roles.add((springSecurityService.getPrincipal() as UserDetails).getUsername())
        }
        String query = '''
SELECT count(p.id) FROM Project AS p, AclEntry AS ace
JOIN ace.aclObjectIdentity AS aoi
JOIN aoi.aclClass AS ac
JOIN ace.sid AS sid
WHERE
aoi.objectId = p.id
AND sid.sid IN (:roles)
AND ace.mask IN (:permissions)
AND ace.granting = true
'''
        Map params = [
            permissions: [
                BasePermission.READ.getMask(),
                BasePermission.ADMINISTRATION.getMask()
            ],
            roles: roles
        ]
        List result = Project.executeQuery(query, params)
        if (!result) {
            return false
        }
        if ((result[0] as Long) >= 1) {
            return true
        }
        return false
    }
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void setSnv(Project project, Project.Snv snv) {
        assert project: "the input project must not be null"
        assert snv: "the input snv must not be null"
        project.snv = snv
        project.save(flush: true, failOnError: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void configureNoAlignmentDeciderProject(Project project) {
        setReferenceGenomeProjectSeqTypeDeprecated(project)
        project.alignmentDeciderBeanName = "noAlignmentDecider"
        project.save(flush: true, failOnError: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void configureDefaultOtpAlignmentDecider(Project project, String referenceGenomeName) {
        setReferenceGenomeProjectSeqTypeDeprecated(project)
        project.alignmentDeciderBeanName = "defaultOtpAlignmentDecider"
        project.save(flush: true, failOnError: true)
        ReferenceGenome referenceGenome = exactlyOneElement(ReferenceGenome.findAllByName(referenceGenomeName))
        SeqType seqType_wgp = SeqType.getWholeGenomePairedSeqType()
        SeqType seqType_exome = SeqType.getExomePairedSeqType()
        [seqType_wgp, seqType_exome].each {seqType ->
            ReferenceGenomeProjectSeqType refSeqType = new ReferenceGenomeProjectSeqType()
            refSeqType.project = project
            refSeqType.seqType = seqType
            refSeqType.referenceGenome = referenceGenome
            refSeqType.sampleType = null
            refSeqType.save(flush: true, failOnError: true)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void configurePanCanAlignmentDeciderProject(PanCanAlignmentConfiguration panCanAlignmentConfiguration) {
        if (panCanAlignmentConfiguration.project.alignmentDeciderBeanName == AlignmentDeciderBeanNames.OTP_ALIGNMENT.bean) {
            setReferenceGenomeProjectSeqTypeDeprecated(panCanAlignmentConfiguration.project)
        } else {
            setReferenceGenomeProjectSeqTypeDeprecated(panCanAlignmentConfiguration.project, panCanAlignmentConfiguration.seqType)
        }
        panCanAlignmentConfiguration.project.alignmentDeciderBeanName = AlignmentDeciderBeanNames.PAN_CAN_ALIGNMENT.bean
        panCanAlignmentConfiguration.project.save(flush: true, failOnError: true)

        ReferenceGenome referenceGenome = exactlyOneElement(ReferenceGenome.findAllByName(panCanAlignmentConfiguration.referenceGenome))

        assert panCanAlignmentConfiguration.mergeTool in MergeConstants.ALL_MERGE_TOOLS: "Invalid merge tool: '${panCanAlignmentConfiguration.mergeTool}', possible values: ${MergeConstants.ALL_MERGE_TOOLS}"

        assert OtpPath.isValidPathComponent(panCanAlignmentConfiguration.pluginName): "pluginName '${panCanAlignmentConfiguration.pluginName}' is an invalid path component"
        assert OtpPath.isValidPathComponent(panCanAlignmentConfiguration.pluginVersion): "pluginVersion '${panCanAlignmentConfiguration.pluginVersion}' is an invalid path component"
        assert OtpPath.isValidPathComponent(panCanAlignmentConfiguration.baseProjectConfig): "baseProjectConfig '${panCanAlignmentConfiguration.baseProjectConfig}' is an invalid path component"
        assert panCanAlignmentConfiguration.configVersion ==~ RoddyWorkflowConfig.CONFIG_VERSION_PATTERN: "configVersion '${panCanAlignmentConfiguration.configVersion}' has not expected pattern: ${RoddyWorkflowConfig.CONFIG_VERSION_PATTERN}"

        //Reference genomes with PHIX_INFIX only works with sambamba
        if (referenceGenome.name.contains(PHIX_INFIX)) {
            assert panCanAlignmentConfiguration.mergeTool == MergeConstants.MERGE_TOOL_SAMBAMBA : "Only sambamba supported for reference genome with Phix"
        }

        File statDir = referenceGenomeService.pathToChromosomeSizeFilesPerReference(panCanAlignmentConfiguration.project, referenceGenome)
        File statSizeFile = new File(statDir, panCanAlignmentConfiguration.statSizeFileName)
        assert statSizeFile.exists(): "The statSizeFile '${panCanAlignmentConfiguration.statSizeFileName}' could not be found in ${statDir}"

        Pipeline pipeline = CollectionUtils.exactlyOneElement(Pipeline.findAllByTypeAndName(
                Pipeline.Type.ALIGNMENT,
                Pipeline.Name.PANCAN_ALIGNMENT,
        ))

        ReferenceGenomeProjectSeqType refSeqType = new ReferenceGenomeProjectSeqType()
        refSeqType.project = panCanAlignmentConfiguration.project
        refSeqType.seqType = panCanAlignmentConfiguration.seqType
        refSeqType.referenceGenome = referenceGenome
        refSeqType.sampleType = null
        refSeqType.statSizeFileName = panCanAlignmentConfiguration.statSizeFileName
        refSeqType.save(flush: true, failOnError: true)

        String xmlConfig = RoddyPanCanConfigTemplate.createConfigBashEscaped(panCanAlignmentConfiguration)

        File projectDirectory = panCanAlignmentConfiguration.project.getProjectDirectory()
        assert projectDirectory.exists()

        File configFilePath = RoddyWorkflowConfig.getStandardConfigFile(
                panCanAlignmentConfiguration.project,
                pipeline.name,
                panCanAlignmentConfiguration.seqType,
                panCanAlignmentConfiguration.pluginVersion,
                panCanAlignmentConfiguration.configVersion,
        )
        File configDirectory = configFilePath.parentFile

        executeScript(getScriptBash(configDirectory, xmlConfig, configFilePath), panCanAlignmentConfiguration.project)

        RoddyWorkflowConfig.importProjectConfigFile(
                panCanAlignmentConfiguration.project,
                panCanAlignmentConfiguration.seqType,
                "${panCanAlignmentConfiguration.pluginName}:${panCanAlignmentConfiguration.pluginVersion}",
                pipeline,
                configFilePath.path,
                panCanAlignmentConfiguration.configVersion,
        )
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void configureSnvPipelineProject(SnvPipelineConfiguration snvPipelineConfiguration) {
        assert OtpPath.isValidPathComponent(snvPipelineConfiguration.pluginName): "pluginName '${snvPipelineConfiguration.pluginName}' is an invalid path component"
        assert OtpPath.isValidPathComponent(snvPipelineConfiguration.pluginVersion): "pluginVersion '${snvPipelineConfiguration.pluginVersion}' is an invalid path component"
        assert OtpPath.isValidPathComponent(snvPipelineConfiguration.baseProjectConfig): "baseProjectConfig '${snvPipelineConfiguration.baseProjectConfig}' is an invalid path component"
        assert snvPipelineConfiguration.configVersion ==~ RoddyWorkflowConfig.CONFIG_VERSION_PATTERN: "configVersion '${snvPipelineConfiguration.configVersion}' has not expected pattern: ${RoddyWorkflowConfig.CONFIG_VERSION_PATTERN}"

        Pipeline pipeline = exactlyOneElement(Pipeline.findAllByTypeAndName(
                Pipeline.Type.SNV,
                Pipeline.Name.RODDY_SNV,
        ))

        String xmlConfig = RoddySnvConfigTemplate.createConfigBashEscaped(snvPipelineConfiguration, Pipeline.Name.RODDY_SNV)

        File projectDirectory = snvPipelineConfiguration.project.getProjectDirectory()
        assert projectDirectory.exists()

        File configFilePath = RoddyWorkflowConfig.getStandardConfigFile(
                snvPipelineConfiguration.project,
                pipeline.name,
                snvPipelineConfiguration.seqType,
                snvPipelineConfiguration.pluginVersion,
                snvPipelineConfiguration.configVersion,
        )
        File configDirectory = configFilePath.parentFile

        executeScript(getScriptBash(configDirectory, xmlConfig, configFilePath), snvPipelineConfiguration.project)

        RoddyWorkflowConfig.importProjectConfigFile(
                snvPipelineConfiguration.project,
                snvPipelineConfiguration.seqType,
                "${snvPipelineConfiguration.pluginName}:${snvPipelineConfiguration.pluginVersion}",
                pipeline,
                configFilePath.path,
                snvPipelineConfiguration.configVersion,
        )
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void configureIndelPipelineProject(IndelPipelineConfiguration indelPipelineConfiguration) {
        assert OtpPath.isValidPathComponent(indelPipelineConfiguration.pluginName): "pluginName '${indelPipelineConfiguration.pluginName}' is an invalid path component"
        assert OtpPath.isValidPathComponent(indelPipelineConfiguration.pluginVersion): "pluginVersion '${indelPipelineConfiguration.pluginVersion}' is an invalid path component"
        assert OtpPath.isValidPathComponent(indelPipelineConfiguration.baseProjectConfig): "baseProjectConfig '${indelPipelineConfiguration.baseProjectConfig}' is an invalid path component"
        assert indelPipelineConfiguration.configVersion ==~ RoddyWorkflowConfig.CONFIG_VERSION_PATTERN: "configVersion '${indelPipelineConfiguration.configVersion}' has not expected pattern: ${RoddyWorkflowConfig.CONFIG_VERSION_PATTERN}"

        Pipeline pipeline = exactlyOneElement(Pipeline.findAllByTypeAndName(
                Pipeline.Type.INDEL,
                Pipeline.Name.RODDY_INDEL,
        ))

        String xmlConfig = RoddyIndelConfigTemplate.createConfigBashEscaped(indelPipelineConfiguration, Pipeline.Name.RODDY_INDEL)

        File projectDirectory = indelPipelineConfiguration.project.getProjectDirectory()
        assert projectDirectory.exists()

        File configFilePath = RoddyWorkflowConfig.getStandardConfigFile(
                indelPipelineConfiguration.project,
                pipeline.name,
                indelPipelineConfiguration.seqType,
                indelPipelineConfiguration.pluginVersion,
                indelPipelineConfiguration.configVersion,
        )
        File configDirectory = configFilePath.parentFile

        executeScript(getScriptBash(configDirectory, xmlConfig, configFilePath), indelPipelineConfiguration.project)

        RoddyWorkflowConfig.importProjectConfigFile(
                indelPipelineConfiguration.project,
                indelPipelineConfiguration.seqType,
                "${indelPipelineConfiguration.pluginName}:${indelPipelineConfiguration.pluginVersion}",
                pipeline,
                configFilePath.path,
                indelPipelineConfiguration.configVersion,
        )
    }

    private String getScriptBash(File configDirectory, String xmlConfig, File configFilePath) {
        String md5 = HelperUtils.getRandomMd5sum()
        String createConfigDirectory = ''

        if (!configDirectory.exists()) {
            createConfigDirectory = """\
mkdir -p -m 2750 ${configDirectory}
"""
        }

        return """\

${createConfigDirectory}

cat <<${md5} > ${configFilePath}
${xmlConfig}
${md5}

chmod 0440 ${configFilePath}

"""
    }


    private String buildCreateProjectDirectory(String unixGroup, File projectDirectory) {
        return """\
mkdir -p -m 2750 ${projectDirectory}

chgrp ${unixGroup} ${projectDirectory}
chmod 2750 ${projectDirectory}
"""
    }

    private void executeScript(String input, Project project) {
        Realm realm = ConfigService.getRealm(project, Realm.OperationType.DATA_MANAGEMENT)
        String script = """\
#!/bin/bash
set -evx

umask 0027

${input}

echo 'OK'
"""
        LogThreadLocal.withThreadLog(System.out) {
            assert executionService.executeCommand(realm, script).trim() == "OK"
        }
    }

    private void setReferenceGenomeProjectSeqTypeDeprecated(Project project) {
        Set<ReferenceGenomeProjectSeqType> referenceGenomeProjectSeqTypes = ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project)
        referenceGenomeProjectSeqTypes*.deprecatedDate = new Date()
        referenceGenomeProjectSeqTypes*.save(flush: true, failOnError: true)
    }

    private void setReferenceGenomeProjectSeqTypeDeprecated(Project project, SeqType seqType) {
        Set<ReferenceGenomeProjectSeqType> referenceGenomeProjectSeqTypes = ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndDeprecatedDateIsNull(project, seqType)
        referenceGenomeProjectSeqTypes*.deprecatedDate = new Date()
        referenceGenomeProjectSeqTypes*.save(flush: true, failOnError: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateFingerPrinting(Project project, boolean value) {
        project.fingerPrinting = value
        assert project.save(flush: true)
    }
}

class RoddyConfiguration {
    Project project
    SeqType seqType
    String pluginName
    String pluginVersion
    String baseProjectConfig
    String configVersion
}

class PanCanAlignmentConfiguration extends RoddyConfiguration {
    String referenceGenome
    String statSizeFileName
    String mergeTool
}

class SnvPipelineConfiguration extends RoddyConfiguration {
}

class IndelPipelineConfiguration extends RoddyConfiguration {
}
