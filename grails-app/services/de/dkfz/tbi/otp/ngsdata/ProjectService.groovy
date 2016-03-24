package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.administration.GroupCommand
import de.dkfz.tbi.otp.administration.GroupService
import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.Workflow
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import grails.plugin.springsecurity.SpringSecurityUtils
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.model.Sid
import org.springframework.security.core.userdetails.UserDetails
import de.dkfz.tbi.otp.security.Group
import de.dkfz.tbi.otp.job.processing.ExecutionService


import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

/**
 * Service providing methods to access information about Projects.
 *
 */
class ProjectService {
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
    static final String PICARD = "Picard"
    static final String BIOBAMBAM = "BioBamBam"

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
    public Project createProject(String name, String dirName, String realmName, String alignmentDeciderBeanName) {
        Project project = new Project(
                name: name,
                dirName: dirName,
                realmName: realmName,
                alignmentDeciderBeanName: alignmentDeciderBeanName,
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
    public Project createProject(String name, String dirName, String realmName, String alignmentDeciderBeanName, String projectGroup, String nameInMetadataFiles, boolean copyFiles) {
        Project project = createProject(name, dirName, realmName, alignmentDeciderBeanName)
        project.hasToBeCopied = copyFiles
        project.nameInMetadataFiles = nameInMetadataFiles
        project.setProjectGroup(ProjectGroup.findByName(projectGroup))
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
        return project
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public void updateNameInMetadata(String nameInMetadata, Project project) {
        project.nameInMetadataFiles = nameInMetadata
        project.save(flush: true, failOnError: true)
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
    void configurePanCanAlignmentDeciderProject(Project project, String referenceGenomeName, String pluginVersion, String statSizeFileName, String unixGroup, String mergeTool, String configVersion) {
        setReferenceGenomeProjectSeqTypeDeprecated(project)
        project.alignmentDeciderBeanName = "panCanAlignmentDecider"
        project.save(flush: true, failOnError: true)
        ReferenceGenome referenceGenome = exactlyOneElement(ReferenceGenome.findAllByName(referenceGenomeName))
        assert mergeTool in [PICARD, BIOBAMBAM]: "Merge Tool must be '${PICARD}' or '${BIOBAMBAM}'"
        boolean useBioBamBam = mergeTool == BIOBAMBAM
        boolean useConvey = false
        String pluginName = 'QualityControlWorkflows'
        assert OtpPath.isValidPathComponent(pluginVersion): "pluginVersion is invalid path component"
        assert OtpPath.isValidPathComponent(unixGroup): "unixGroup contains invalid characters"
        Workflow workflow = CollectionUtils.exactlyOneElement(Workflow.findAllByTypeAndName(
                Workflow.Type.ALIGNMENT,
                Workflow.Name.PANCAN_ALIGNMENT,
        ))

        SeqType seqType_wgp = SeqType.getWholeGenomePairedSeqType()
        SeqType seqType_exome = SeqType.getExomePairedSeqType()
        [seqType_wgp, seqType_exome].each {seqType ->
            ReferenceGenomeProjectSeqType refSeqType = new ReferenceGenomeProjectSeqType()
            refSeqType.project = project
            refSeqType.seqType = seqType
            refSeqType.referenceGenome = referenceGenome
            refSeqType.sampleType = null
            refSeqType.statSizeFileName = statSizeFileName
            refSeqType.save(flush: true, failOnError: true)
        }

        File statDir = referenceGenomeService.pathToChromosomeSizeFilesPerReference(project, referenceGenome)
        File statSizeFile = new File(statDir, statSizeFileName)
        assert statSizeFile.exists(): "The statSizeFile ${statSizeFile} could not be found in ${statDir}"

        String xmlConfig = """
<configuration
        configurationType='project'
        name='${Workflow.Name.PANCAN_ALIGNMENT.name()}_${pluginName}:${pluginVersion}_${configVersion}'
        description='Align with BWA-MEM (${useConvey ? 'convey' : 'software'}) and mark duplicates with ${mergeTool}.' imports="otpPanCanAlignmentWorkflow-1.3 ">
    <subconfigurations>

        <configuration name="config" usedresourcessize="xl">
            <availableAnalyses>
                <analysis id='WGS' configuration='qcAnalysis' killswitches='FilenameSection'/>
                <analysis id='WES' configuration='exomeAnalysis' killswitches='FilenameSection'/>
            </availableAnalyses>
            <configurationvalues>

                <!-- Unix group -->
                <cvalue name='outputFileGroup' value='${unixGroup}'
                    description="For OTP this needs to be set to the Unix group of the project."/>

                <!-- Using of Convey -->
                <cvalue name='useAcceleratedHardware' value='${useConvey}' type="boolean"
                    description='Map reads with Convey BWA-MEM (true) or software BWA-MEM (false; PCAWF standard)'/>

                <!-- Picard / BioBamBam -->
                <cvalue name='useBioBamBamMarkDuplicates' value='${useBioBamBam}' type="boolean"
                    description='Merge and mark duplicates with biobambam (true; PCAWF standard) or Picard (false).'/>

            </configurationvalues>
        </configuration>

    </subconfigurations>
</configuration>
"""

        Realm realm = ConfigService.getRealm(project, Realm.OperationType.DATA_MANAGEMENT)

        File projectDirectory = LsdfFilesService.getPath(
                realm.rootPath,
                project.dirName,
        )

        File configDirectory = LsdfFilesService.getPath(
                projectDirectory.path,
                'configFiles',
                Workflow.Name.PANCAN_ALIGNMENT.name(),
        )
        File configFilePath = new File(configDirectory, "${Workflow.Name.PANCAN_ALIGNMENT.name()}_${pluginVersion}_${configVersion}.xml")
        String md5 = HelperUtils.getRandomMd5sum()

        String createProjectDirectory = ''
        if (!projectDirectory.exists()) {
            createProjectDirectory = """\
mkdir -p -m 2750 ${projectDirectory}

chgrp ${unixGroup} ${projectDirectory}
"""
        }

        String createConfigDirectory = ''
        if (!configDirectory.exists()) {
            createConfigDirectory = """\
mkdir -p -m 2750 ${configDirectory}
"""
        }

        String script = """\
#!/bin/bash
set -evx

umask 0027

${createProjectDirectory}

${createConfigDirectory}

cat <<${md5} > ${configFilePath}
${xmlConfig}
${md5}

chmod 0440 ${configFilePath}

echo 'OK'
"""

        LogThreadLocal.withThreadLog(System.out) {
            assert executionService.executeCommand(realm, script).trim() == "OK"
        }

        RoddyWorkflowConfig.importProjectConfigFile(
                project,
                "${pluginName}:${pluginVersion}",
                workflow,
                configFilePath.path,
                configVersion,
        )
    }

    private void setReferenceGenomeProjectSeqTypeDeprecated(Project project) {
        Set<ReferenceGenomeProjectSeqType> referenceGenomeProjectSeqTypes = ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project)
        referenceGenomeProjectSeqTypes*.deprecatedDate = new Date()
        referenceGenomeProjectSeqTypes*.save(flush: true, failOnError: true)
    }
}
