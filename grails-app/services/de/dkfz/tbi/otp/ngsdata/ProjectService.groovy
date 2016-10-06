package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.administration.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.plugin.springsecurity.*
import jdk.nashorn.internal.ir.annotations.*
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

    /**
     * Dependency Injection for aclUtilService
     */
    def aclUtilService
    /**
     * Dependency Injection of Spring Security Service - needed for ACL checks
     */
    def springSecurityService

    GroupService groupService
    ReferenceGenomeService referenceGenomeService
    ExecutionService executionService

    /**
     *
     * @return List of all available Projects
     */
    @PostFilter("hasRole('ROLE_OPERATOR') or hasPermission(filterObject, 'read')")
    public List<Project> getAllProjects() {
        return Project.list(sort: "name", order: "asc")
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
    public Project createProject(String name, String dirName, String realmName, String alignmentDeciderBeanName, String categoryName) {
        Project project = new Project(
                name: name,
                dirName: dirName,
                realmName: realmName,
                alignmentDeciderBeanName: alignmentDeciderBeanName,
                category: exactlyOneElement(ProjectCategory.findAllByName(categoryName)),
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
    public Project createProject(String name, String dirName, String realmName, String alignmentDeciderBeanName, String categoryName, String unixGroup, String projectGroup, String nameInMetadataFiles, boolean copyFiles, String mailingListName) {
        assert OtpPath.isValidPathComponent(unixGroup): "unixGroup '${unixGroup}' contains invalid characters"
        Project project = createProject(name, dirName, realmName, alignmentDeciderBeanName, categoryName)
        project.hasToBeCopied = copyFiles
        project.nameInMetadataFiles = nameInMetadataFiles
        project.setProjectGroup(ProjectGroup.findByName(projectGroup))
        project.mailingListName = mailingListName
        assert project.save(flush: true, failOnError: true)

        GroupCommand groupCommand = new GroupCommand(
                name: name,
                description: "group for ${name}",
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
            if (attrs.group().toString() == unixGroup) {
                return project
            }
        }
        executeScript(buildCreateProjectDirectory(unixGroup, projectDirectory), project)
        WaitingFileUtils.waitUntilExists(projectDirectory)

        return project
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
    public void updateCategory(String categoryName, Project project) {
        ProjectCategory categoryEntry = exactlyOneElement(ProjectCategory.findAllByName(categoryName))
        project.category = categoryEntry
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
        String md5 = HelperUtils.getRandomMd5sum()

        String createConfigDirectory = ''
        if (!configDirectory.exists()) {
            createConfigDirectory = """\
mkdir -p -m 2750 ${configDirectory}
"""
        }

        String script = """\

${createConfigDirectory}

cat <<${md5} > ${configFilePath}
${xmlConfig}
${md5}

chmod 0440 ${configFilePath}

"""
        executeScript(script, panCanAlignmentConfiguration.project)

        RoddyWorkflowConfig.importProjectConfigFile(
                panCanAlignmentConfiguration.project,
                panCanAlignmentConfiguration.seqType,
                "${panCanAlignmentConfiguration.pluginName}:${panCanAlignmentConfiguration.pluginVersion}",
                pipeline,
                configFilePath.path,
                panCanAlignmentConfiguration.configVersion,
        )
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
}

@Immutable
class PanCanAlignmentConfiguration {
    Project project
    SeqType seqType
    String referenceGenome
    String statSizeFileName
    String mergeTool
    String pluginName
    String pluginVersion
    String baseProjectConfig
    String configVersion
}
