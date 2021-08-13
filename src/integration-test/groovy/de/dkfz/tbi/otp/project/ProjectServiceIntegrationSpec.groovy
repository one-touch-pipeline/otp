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
package de.dkfz.tbi.otp.project

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import grails.validation.ValidationException
import grails.web.mapping.LinkGenerator
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.springframework.mock.web.MockMultipartFile
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfigService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.DomainFactoryProcessingPriority
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.project.exception.unixGroup.UnixGroupIsInvalidException
import de.dkfz.tbi.otp.project.exception.unixGroup.UnixGroupIsSharedException
import de.dkfz.tbi.otp.project.exception.unixGroup.UnixGroupNotFoundException
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.nio.file.*
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.time.LocalDate

@Rollback
@Integration
class ProjectServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore, DomainFactoryProcessingPriority {

    RemoteShellHelper remoteShellHelper
    ProcessingOptionService processingOptionService
    ProjectService projectService
    FileService fileService
    FileSystemService fileSystemService
    ReferenceGenomeService referenceGenomeService
    RoddyWorkflowConfigService roddyWorkflowConfigService
    TestConfigService configService

    static final String FILE_NAME = "fileName"
    static final byte[] CONTENT = 0..3

    @Rule
    TemporaryFolder temporaryFolder

    void baseSetupData() {
        createUserAndRoles()
        configService.addOtpProperties(temporaryFolder.newFolder().toPath())
    }

    void setupData() {
        baseSetupData()

        createProject(name: 'testProject', nameInMetadataFiles: 'testProject2', dirName: 'testDir')
        createProject(name: 'testProject3', nameInMetadataFiles: null)
        ProjectGroup projectGroup = new ProjectGroup(name: 'projectGroup')
        projectGroup.save(flush: true)

        Realm realm = DomainFactory.createDefaultRealmWithProcessingOption()

        createProject(name: 'testProjectAlignment', realm: realm)
        createReferenceGenome(name: 'testReferenceGenome')
        createReferenceGenome(name: 'testReferenceGenome2')
        DomainFactory.createAllAlignableSeqTypes()
        DomainFactory.createPanCanPipeline()
        DomainFactory.createRnaPipeline()
        DomainFactory.createRoddySnvPipelineLazy()
        DomainFactory.createIndelPipelineLazy()
        DomainFactory.createSophiaPipelineLazy()
        DomainFactory.createAceseqPipelineLazy()

        int counter = 0
        projectService.remoteShellHelper = Stub(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { Realm realm2, String command ->
                File script = temporaryFolder.newFile('script' + counter++ + '.sh')
                script.text = command
                return LocalShellHelper.executeAndWait("bash ${script.absolutePath}").assertExitCodeZero()
            }
        }
        projectService.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            _ * executeCommandReturnProcessOutput(_, _) >> { Realm realm2, String command ->
                return new ProcessOutput(command, '', 0)
            }
        }

        projectService.fileSystemService = new TestFileSystemService()
        projectService.roddyWorkflowConfigService = new RoddyWorkflowConfigService()
        projectService.roddyWorkflowConfigService.fileSystemService = new TestFileSystemService()
        projectService.roddyWorkflowConfigService.workflowConfigService = new WorkflowConfigService()
        projectService.mailHelperService = Mock(MailHelperService) {
            0 * sendEmail(_, _, _)
        }
        projectService.configService = configService

        DomainFactory.createProcessingOptionLazy([
                name : OptionName.PIPELINE_RODDY_ALIGNMENT_BWA_VERSION_AVAILABLE,
                type : null,
                value: "bwa-mem",
        ])
        DomainFactory.createProcessingOptionLazy([
                name : OptionName.PIPELINE_RODDY_ALIGNMENT_SAMBAMBA_VERSION_AVAILABLE,
                type : null,
                value: "sambamba",
        ])

        DomainFactory.createProcessingOptionBasePathReferenceGenome(new File(configService.rootPath, "reference_genome").path)
    }

    void cleanup() {
        projectService.remoteShellHelper = remoteShellHelper
        projectService.roddyWorkflowConfigService = roddyWorkflowConfigService
        projectService.fileService = fileService
        projectService.fileSystemService = fileSystemService
        fileService.remoteShellHelper = remoteShellHelper
        configService.clean()
    }

    @SuppressWarnings("UnnecessaryObjectReferences")
    @Unroll
    void "test createProject valid input"() {
        given:
        setupData()
        String unixGroup = configService.testingGroup

        when:
        Project project
        if (dirAnalysis) {
            dirAnalysis = "${temporaryFolder.newFolder()}${dirAnalysis}"
        }
        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name                          : name,
                dirName                       : dirName,
                individualPrefix              : 'individualPrefix',
                dirAnalysis                   : dirAnalysis,
                relatedProjects               : relatedProjects,
                sampleIdentifierParserBeanName: sampleIdentifierParserBeanName,
                qcThresholdHandling           : qcThresholdHandling,
                unixGroup                     : unixGroup,
                projectGroup                  : projectGroup,
                nameInMetadataFiles           : nameInMetadataFiles,
                forceCopyFiles                : forceCopyFiles,
                description                   : description,
                processingPriority            : createProcessingPriority(priority: processingPriority),
                projectType                   : Project.ProjectType.SEQUENCING,
                storageUntil                  : LocalDate.now(),
                publiclyAvailable             : false,
        )
        SpringSecurityUtils.doWithAuth(ADMIN) {
            project = projectService.createProject(projectParams)
        }

        then:
        project.name == name
        project.dirName == dirName
        project.dirAnalysis == dirAnalysis
        project.relatedProjects == relatedProjects
        project.sampleIdentifierParserBeanName == sampleIdentifierParserBeanName
        project.qcThresholdHandling == qcThresholdHandling
        project.unixGroup == unixGroup
        project.projectGroup == ProjectGroup.findByName(projectGroup)
        project.nameInMetadataFiles == nameInMetadataFiles
        project.forceCopyFiles == forceCopyFiles
        project.description == description
        project.processingPriority.priority == processingPriority

        where:
        name      | dirName | dirAnalysis | relatedProjects | projectGroup   | nameInMetadataFiles | forceCopyFiles | description   | processingPriority            | sampleIdentifierParserBeanName           | qcThresholdHandling
        'project' | 'dir'   | ''          | ''              | ''             | null                | true           | ''            | ProcessingPriority.FAST_TRACK | SampleIdentifierParserBeanName.INFORM    | QcThresholdHandling.CHECK_AND_NOTIFY
        'project' | 'dir'   | ''          | ''              | 'projectGroup' | 'project'           | true           | 'description' | ProcessingPriority.NORMAL     | SampleIdentifierParserBeanName.HIPO      | QcThresholdHandling.NO_CHECK
        'project' | 'dir'   | ''          | ''              | ''             | 'project'           | true           | 'description' | ProcessingPriority.NORMAL     | SampleIdentifierParserBeanName.DEEP      | QcThresholdHandling.CHECK_AND_NOTIFY
        'project' | 'dir'   | '/dirA'     | ''              | ''             | 'project'           | true           | 'description' | ProcessingPriority.FAST_TRACK | SampleIdentifierParserBeanName.NO_PARSER | QcThresholdHandling.NO_CHECK
    }

    @Unroll
    void "test updateAllRelatedProjects updates all related projects with new project name"() {
        given:
        baseSetupData()
        Project baseProject = createProject(name: "P2", relatedProjects: baseRelatedProjects)
        Project baseProject2 = createProject(name: "P3", relatedProjects: "P4,P5")
        Project newProject = createProject(name: "P1", relatedProjects: "P2,P3,P10")

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.updateAllRelatedProjects(newProject)
        }

        then:
        baseProject.relatedProjects == expected
        baseProject2.relatedProjects == "P1,P4,P5"

        where:
        baseRelatedProjects || expected
        "P3"                || "P1,P3"
        ""                  || "P1"
        null                || "P1"
    }

    void "test createProject if directories are created"() {
        given:
        setupData()
        String group = configService.testingGroup

        when:
        Project project
        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name: 'project',
                dirName: 'dir',
                individualPrefix: 'individualPrefix',
                dirAnalysis: "${temporaryFolder.newFolder()}/dirA",
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: null,
                forceCopyFiles: false,
                description: '',
                processingPriority: createProcessingPriority(),
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                qcThresholdHandling: QcThresholdHandling.NO_CHECK,
                projectType: Project.ProjectType.SEQUENCING,
                storageUntil: LocalDate.now(),
        )
        SpringSecurityUtils.doWithAuth(ADMIN) {
            project = projectService.createProject(projectParams)
        }

        then:
        File projectDirectory = LsdfFilesService.getPath(
                configService.rootPath.absolutePath,
                project.dirName,
        )
        projectDirectory.exists()
        PosixFileAttributes attrs = Files.readAttributes(projectDirectory.toPath(), PosixFileAttributes, LinkOption.NOFOLLOW_LINKS)
        attrs.group().toString() == group
        TestCase.assertContainSame(attrs.permissions(),
                [PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE])

        and:
        File projectDirAnalysis = LsdfFilesService.getPath(
                project.dirAnalysis,
        )
        projectDirAnalysis.exists()
        PosixFileAttributes dirAnalysisAttrs = Files.readAttributes(projectDirAnalysis.toPath(), PosixFileAttributes, LinkOption.NOFOLLOW_LINKS)
        dirAnalysisAttrs.group().toString() == group
        TestCase.assertContainSame(dirAnalysisAttrs.permissions(),
                [PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE])
    }

    void "test createProject when dirAnalysis can not be created, sends email"() {
        given:
        setupData()
        String group = configService.testingGroup

        when:
        Project project
        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name: 'project',
                dirName: 'dir',
                individualPrefix: 'individualPrefix',
                dirAnalysis: "${temporaryFolder.newFolder()}/dirA",
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: null,
                forceCopyFiles: false,
                description: '',
                processingPriority: createProcessingPriority(),
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                qcThresholdHandling: QcThresholdHandling.NO_CHECK,
                projectType: Project.ProjectType.SEQUENCING,
                storageUntil: LocalDate.now(),
        )
        projectService.mailHelperService = Mock(MailHelperService) {
            1 * sendEmailToTicketSystem(_, _) >> { String emailSubject, String content ->
                assert emailSubject == "Could not automatically create analysisDir '${projectParams.dirAnalysis}' for Project '${projectParams.name}'."
                assert content.contains("mkdir: cannot create directory ‘${projectParams.dirAnalysis}’: Permission denied")
            }
        }
        projectService.remoteShellHelper = Stub(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { Realm realm2, String command ->
                File script = temporaryFolder.newFile('script.sh')
                script.text = command
                return LocalShellHelper.executeAndWait("bash ${script.absolutePath}").assertExitCodeZero()
            } >> { Realm realm2, String command ->
                return new ProcessOutput(stderr: "mkdir: cannot create directory ‘${projectParams.dirAnalysis}’: Permission denied".toString(), exitCode: 1)
            }
        }

        SpringSecurityUtils.doWithAuth(ADMIN) {
            project = projectService.createProject(projectParams)
        }

        then:
        File projectDirAnalysis = LsdfFilesService.getPath(
                project.dirAnalysis,
        )
        !projectDirAnalysis.exists()
    }

    @Unroll
    void "test createProject invalid input ('#name', '#dirName', '#nameInMetadataFiles')"() {
        given:
        setupData()
        String group = configService.testingGroup

        when:
        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name: name,
                dirName: dirName,
                individualPrefix: 'individualPrefix',
                dirAnalysis: "${temporaryFolder.newFolder()}/dirA",
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: nameInMetadataFiles,
                forceCopyFiles: true,
                description: '',
                processingPriority: createProcessingPriority(),
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                qcThresholdHandling: QcThresholdHandling.NO_CHECK,
                projectType: Project.ProjectType.SEQUENCING,
                storageUntil: LocalDate.now(),
        )
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        ValidationException ex = thrown()
        ex.message.contains(errorName) && ex.message.contains(errorLocaction)

        where:
        name           | dirName     | nameInMetadataFiles || errorName                       | errorLocaction
        'testProject'  | 'dir'       | 'project'           || 'unique'                        | 'on field \'name\': rejected value [testProject]'
        'testProject2' | 'dir'       | 'project'           || 'duplicate'                     | 'on field \'name\': rejected value [testProject2]'
        'project'      | 'dir'       | 'testProject'       || 'duplicate.name'                | 'on field \'nameInMetadataFiles\': rejected value [testProject]'
        'project'      | 'dir'       | 'testProject2'      || 'duplicate.nameInMetadataFiles' | 'on field \'nameInMetadataFiles\': rejected value [testProject2]'
        'project'      | 'testDir'   | ''                  || 'unique'                        | 'on field \'dirName\': rejected value [testDir]'
        'project'      | '/abs/path' | 'project'           || 'validator.relative.path'       | "on field 'dirName': rejected value [/abs/path];"
    }

    void "test createProject invalid unix group"() {
        when:
        setupData()
        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name: 'project',
                dirName: 'dir',
                individualPrefix: 'individualPrefix',
                dirAnalysis: "${temporaryFolder.newFolder()}/dirA",
                unixGroup: 'invalidValue',
                projectGroup: '',
                nameInMetadataFiles: null,
                forceCopyFiles: false,
                description: '',
                processingPriority: createProcessingPriority(),
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                qcThresholdHandling: QcThresholdHandling.NO_CHECK,
                projectType: Project.ProjectType.SEQUENCING,
                storageUntil: LocalDate.now(),
        )
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        AssertionError e = thrown()
        e.message.contains('Expected exit code to be 0, but it is 1')
    }

    void "test createProject with invalid dirAnalysis"() {
        given:
        setupData()
        String group = configService.testingGroup

        when:
        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name: 'project',
                dirName: 'dir',
                individualPrefix: 'individualPrefix',
                dirAnalysis: 'invalidDirA',
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: null,
                forceCopyFiles: false,
                description: '',
                processingPriority: createProcessingPriority(),
                qcThresholdHandling: QcThresholdHandling.NO_CHECK,
                projectType: Project.ProjectType.SEQUENCING,
                storageUntil: LocalDate.now(),
        )
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        ValidationException e = thrown()
        e.message.contains("dirAnalysis")
    }

    void "test createProject valid input, when directory with wrong unix group already exists"() {
        given:
        setupData()
        String group = configService.testingGroup
        File projectDirectory = LsdfFilesService.getPath(
                configService.rootPath.absolutePath,
                "/dir",
        )

        when:
        new File("${projectDirectory}").mkdirs()

        then:
        projectDirectory.exists()
        String mkdirGroup = Files.readAttributes(projectDirectory.toPath(), PosixFileAttributes, LinkOption.NOFOLLOW_LINKS).group()
        assert mkdirGroup != group : "Cannot test this if OTP property \"otp.testing.group\" is also the user's primary group." +
                " Please update your .otp.properties to use a different group that is not your primary group!"

        when:
        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name: 'project',
                dirName: 'dir',
                individualPrefix: 'individualPrefix',
                dirAnalysis: "${temporaryFolder.newFolder()}/dirA",
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: null,
                forceCopyFiles: false,
                description: '',
                processingPriority: createProcessingPriority(),
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                qcThresholdHandling: QcThresholdHandling.NO_CHECK,
                projectType: Project.ProjectType.SEQUENCING,
                storageUntil: LocalDate.now(),
        )
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        Files.readAttributes(projectDirectory.toPath(), PosixFileAttributes, LinkOption.NOFOLLOW_LINKS).group().toString() == group
    }

    void "test createProject valid input, when directory with correct unix group already exists and project file, then do not create the directory and upload file"() {
        given:
        setupData()
        File projectDirectory = LsdfFilesService.getPath(
                configService.rootPath.absolutePath,
                "/dir",
        )
        String analysisDir = "${temporaryFolder.newFolder()}/dirA"
        projectService.remoteShellHelper = Mock(RemoteShellHelper) {
            1 * executeCommandReturnProcessOutput(_, _) >> { Realm realm, String command ->
                assert command.contains("mkdir -p -m 2775 ${analysisDir}")

                File script = temporaryFolder.newFile('script.sh')
                script.text = command
                return LocalShellHelper.executeAndWait("bash ${script.absolutePath}").assertExitCodeZero()
            }
        }
        projectService.projectInfoService = Mock(ProjectInfoService) {
            1 * createProjectInfoAndUploadFile(_, _)
        }
        MockMultipartFile mockMultipartFile = new MockMultipartFile(FILE_NAME, CONTENT)
        mockMultipartFile.originalFilename = FILE_NAME

        when:
        new File("${projectDirectory}").mkdirs()
        String group = Files.readAttributes(projectDirectory.toPath(), PosixFileAttributes, LinkOption.NOFOLLOW_LINKS).group()

        then:
        projectDirectory.exists()

        when:
        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name: 'project',
                dirName: 'dir',
                individualPrefix: 'individualPrefix',
                dirAnalysis: analysisDir,
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: null,
                forceCopyFiles: false,
                description: '',
                processingPriority: createProcessingPriority(),
                projectInfoFile: mockMultipartFile,
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                qcThresholdHandling: QcThresholdHandling.NO_CHECK,
                projectType: Project.ProjectType.SEQUENCING,
                storageUntil: LocalDate.now(),
        )
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        Files.readAttributes(projectDirectory.toPath(), PosixFileAttributes, LinkOption.NOFOLLOW_LINKS).group().toString() == group
    }

    void "test updateNameInMetadata valid input"() {
        when:
        setupData()
        Project project = Project.findByName("testProject")
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.updateProjectField(name, "nameInMetadataFiles", project)
        }

        then:
        project.nameInMetadataFiles == name

        where:
        name             | _
        'testProject'    | _
        'testProject2'   | _
        'newTestProject' | _
        null             | _
    }

    void "test updateNameInMetadata invalid input"() {
        when:
        setupData()
        Project project = Project.findByName("testProject3")
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.updateProjectField(name, "nameInMetadataFiles", project)
        }

        then:
        ValidationException ex = thrown()
        ex.message.contains(errorName) && ex.message.contains(errorLocaction)

        where:
        name           || errorName                       | errorLocaction
        'testProject'  || 'duplicate.name'                | 'on field \'nameInMetadataFiles\': rejected value [testProject]'
        'testProject2' || 'duplicate.nameInMetadataFiles' | 'on field \'nameInMetadataFiles\': rejected value [testProject2]'
        ''             || 'blank'                         | 'on field \'nameInMetadataFiles\': rejected value []'
    }

    void "test createProject without project type should fail"() {
        given:
        setupData()
        String group = configService.testingGroup

        when:
        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name: 'project',
                dirName: 'dir',
                individualPrefix: 'individualPrefix',
                dirAnalysis: "${temporaryFolder.newFolder()}/dirA",
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: 'project',
                forceCopyFiles: true,
                description: '',
                qcThresholdHandling: QcThresholdHandling.NO_CHECK,
                processingPriority: createProcessingPriority(),
                storageUntil: LocalDate.now(),
        )
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        ValidationException e = thrown()
        e.message.contains("'projectType': rejected value [null]")
    }

    void "getUsersToCopyFromBaseProject only returns enabled PIs"() {
        given:
        baseSetupData()
        createAllBasicProjectRoles()

        Project project = createProject()
        ProjectRole pi = ProjectRole.findByName(ProjectRole.Basic.PI.name())
        ProjectRole bi = ProjectRole.findByName(ProjectRole.Basic.BIOINFORMATICIAN.name())

        Closure<UserProjectRole> createUserProjectRoleHelper = { ProjectRole projectRole, boolean enabled ->
            return DomainFactory.createUserProjectRole(project: project, projectRoles: [projectRole], enabled: enabled)
        }

        List<UserProjectRole> userProjectRoles = [
                createUserProjectRoleHelper(pi, true),
                createUserProjectRoleHelper(pi, false),
                createUserProjectRoleHelper(bi, true),
                createUserProjectRoleHelper(DomainFactory.createProjectRole(), true),
        ]

        expect:
        projectService.getUsersToCopyFromBaseProject(project) == [userProjectRoles[0]]
    }

    @Unroll
    void "test updateIndividualPrefix valid alias and valid user #username"() {
        given:
        setupData()
        String individualPrefix = "prefix"
        Project project = Project.findByName("testProject")
        addUserWithReadAccessToProject(User.findByUsername(USER), project)

        when:
        SpringSecurityUtils.doWithAuth(username) {
            projectService.updateProjectField(individualPrefix, 'individualPrefix', project)
        }

        then:
        project.individualPrefix == individualPrefix

        where:
        username | _
        ADMIN    | _
        OPERATOR | _
    }

    void "test updateIndividualPrefix valid alias and invalid user"() {
        given:
        setupData()
        String individualPrefix = "prefix"
        Project project = Project.findByName("testProject")

        when:
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            projectService.updateProjectField(individualPrefix, 'individualPrefix', project)
        }

        then:
        org.springframework.security.access.AccessDeniedException e = thrown()
        e.message.contains("Access is denied")
    }

    void "test updateAnalysisDirectory valid name"() {
        given:
        setupData()
        String analysisDirectory = "${temporaryFolder.newFolder()}/dirA"
        Project project = Project.findByName("testProject")
        project.unixGroup = configService.testingGroup
        project.save(flush: true)

        when:
        assert !project.dirAnalysis
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.updateProjectField(analysisDirectory, "dirAnalysis", project)
        }

        then:
        project.dirAnalysis == analysisDirectory
    }

    void "test updateProcessingPriority valid name"() {
        given:
        setupData()
        Project project = Project.findByName("testProject")
        ProcessingPriority fastTrackProcessingPriority = findOrCreateProcessingPriorityFastrack()

        when:
        assert project.processingPriority != fastTrackProcessingPriority
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.updateProjectField(fastTrackProcessingPriority, "processingPriority", project)
        }

        then:
        project.processingPriority == fastTrackProcessingPriority
    }

    void "test updateTumor valid name"() {
        given:
        setupData()
        Project project = Project.findByName("testProject")
        TumorEntity tumorEntity = DomainFactory.createTumorEntity()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.updateProjectField(tumorEntity, "tumorEntity", project)
        }

        then:
        project.tumorEntity == tumorEntity
    }

    void "test updateRelatedProjects valid name"() {
        given:
        setupData()
        Project project = Project.findByName("testProject")
        String relatedProjects = "testProject3"

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.updateProjectField(relatedProjects, "relatedProjects", project)
        }

        then:
        project.relatedProjects == relatedProjects
    }

    void "test updateInternalNotes valid name"() {
        given:
        setupData()
        Project project = Project.findByName("testProject")
        String internalNotes = "internalNotes"

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.updateProjectField(internalNotes, "internalNotes", project)
        }

        then:
        project.internalNotes == internalNotes
    }

    void "test configureNoAlignmentDeciderProject"() {
        given:
        setupData()
        Project project = Project.findByName("testProjectAlignment")

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configureNoAlignmentDeciderProject(project)
        }

        then:
        project.alignmentDeciderBeanName == AlignmentDeciderBeanName.NO_ALIGNMENT
        ReferenceGenomeProjectSeqType.findAllByProject(project).size == 0
    }

    void "test configureDefaultOtpAlignmentDecider valid input"() {
        given:
        setupData()
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configureDefaultOtpAlignmentDecider(project, referenceGenome.name)
        }

        then:
        project.alignmentDeciderBeanName == AlignmentDeciderBeanName.OTP_ALIGNMENT
        Set<ReferenceGenomeProjectSeqType> referenceGenomeProjectSeqTypes = ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project)
        referenceGenomeProjectSeqTypes.every { it.referenceGenome == referenceGenome }
        referenceGenomeProjectSeqTypes.size() == 2
    }

    void "test configureDefaultOtpAlignmentDecider valid input, twice"() {
        given:
        setupData()
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")
        ReferenceGenome referenceGenome2 = ReferenceGenome.findByName("testReferenceGenome2")

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configureDefaultOtpAlignmentDecider(project, referenceGenome.name)
            projectService.configureDefaultOtpAlignmentDecider(project, referenceGenome2.name)
        }

        then:
        project.alignmentDeciderBeanName == AlignmentDeciderBeanName.OTP_ALIGNMENT
        Set<ReferenceGenomeProjectSeqType> referenceGenomeProjectSeqTypes = ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project)
        referenceGenomeProjectSeqTypes.every { it.referenceGenome == referenceGenome2 }
        referenceGenomeProjectSeqTypes.size() == 2
    }

    void "test configureDefaultOtpAlignmentDecider invalid input"() {
        given:
        setupData()
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome.findByName("testReferenceGenome")

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configureDefaultOtpAlignmentDecider(project, "error")
        }

        then:
        AssertionError exception = thrown()
        exception.message == "Collection contains 0 elements. Expected 1"
    }

    void "test configurePanCanAlignmentDeciderProject valid input"() {
        given:
        setupData()
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        configuration.project.alignmentDeciderBeanName == AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT
        List<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineInListAndProgramVersionAndObsoleteDateIsNull(
                configuration.project,
                configuration.seqType,
                Pipeline.findAllByTypeAndName(Pipeline.Type.ALIGNMENT, Pipeline.Name.PANCAN_ALIGNMENT),
                "${configuration.pluginName}:${configuration.programVersion}"
        )
        roddyWorkflowConfigs.size() == 1
        File roddyWorkflowConfig = new File(roddyWorkflowConfigs.configFilePath.first())
        roddyWorkflowConfig.exists()
        PosixFileAttributes attributes = Files.readAttributes(roddyWorkflowConfig.toPath(), PosixFileAttributes, LinkOption.NOFOLLOW_LINKS)
        TestCase.assertContainSame(attributes.permissions(), [PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ])
    }

    void "test configurePanCanAlignmentDeciderProject valid input, twice"() {
        given:
        setupData()
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration()
        PanCanAlignmentConfiguration configuration2 = createPanCanAlignmentConfiguration([
                referenceGenome : "testReferenceGenome2",
                statSizeFileName: "testStatSizeFileName2.tab",
                configVersion   : 'v1_1',
        ])

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
            projectService.configurePanCanAlignmentDeciderProject(configuration2)
        }

        then:
        configuration.project.alignmentDeciderBeanName == AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT
        Set<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.findAllByProjectAndPipelineInListAndProgramVersion(
                configuration.project,
                Pipeline.findAllByTypeAndName(Pipeline.Type.ALIGNMENT, Pipeline.Name.PANCAN_ALIGNMENT),
                "${configuration2.pluginName}:${configuration2.programVersion}"
        )
        roddyWorkflowConfigs.size() == 2
        roddyWorkflowConfigs.findAll { it.obsoleteDate == null }.size() == 1
        ReferenceGenomeProjectSeqType.findAllByDeprecatedDateIsNull().size() == 1
    }

    void "test configurePanCanAlignmentDeciderProject valid input, multiple SeqTypes"() {
        given:
        setupData()
        List<PanCanAlignmentConfiguration> configurations = DomainFactory.createPanCanAlignableSeqTypes().collect {
            createPanCanAlignmentConfiguration(seqType: it)
        }
        int count = configurations.size()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            configurations.each {
                projectService.configurePanCanAlignmentDeciderProject(it)
            }
        }

        then:
        Set<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.list()
        roddyWorkflowConfigs.size() == count
        roddyWorkflowConfigs.findAll { it.obsoleteDate == null }.size() == count
        ReferenceGenomeProjectSeqType.findAllByDeprecatedDateIsNull().size() == count
    }

    void "test configurePanCanAlignmentDeciderProject invalid referenceGenome input"() {
        given:
        setupData()
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration()
        configuration.referenceGenome = 'invalidReferenceGenome'

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message == "Collection contains 0 elements. Expected 1"
    }

    void "test configurePanCanAlignmentDeciderProject invalid pluginName input"() {
        given:
        setupData()
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration(
                pluginName: 'invalid/name'
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /pluginName '.*' is an invalid path component\..*/
    }

    void "test configurePanCanAlignmentDeciderProject invalid programVersion input"() {
        given:
        setupData()
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration(
                programVersion: 'invalid/version'
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /programVersion '.*' is an invalid path component\..*/
    }

    @Unroll
    void "test configurePanCanAlignmentDeciderProject invalid baseProjectConfig input"() {
        given:
        setupData()
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration(
                baseProjectConfig: baseProjectConfig
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ message

        where:
        baseProjectConfig || message
        ''                || /baseProjectConfig '.*' is an invalid path component\..*/
        "invalid/path"    || /baseProjectConfig '.*' is an invalid path component\..*/
    }

    void "test configurePanCanAlignmentDeciderProject invalid statSizeFileName input"() {
        given:
        setupData()
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration()
        configuration.statSizeFileName = 'nonExistingFile.tab'

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /The statSizeFile '.*${configuration.statSizeFileName}' could not be found in .*/
    }

    void "test configurePanCanAlignmentDeciderProject invalid alignment version input"() {
        given:
        setupData()
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration(
                bwaMemVersion: 'invalidBwa_memVersion',
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /Invalid bwa_mem version: 'invalidBwa_memVersion',.*/
    }

    void "test configurePanCanAlignmentDeciderProject invalid mergeTool input"() {
        given:
        setupData()
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration(
                mergeTool: 'invalidMergeTool',
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /Invalid merge tool: 'invalidMergeTool',.*/
    }

    void "test configurePanCanAlignmentDeciderProject invalid sambamba version input"() {
        given:
        setupData()
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration(
                mergeTool: MergeTool.SAMBAMBA.name,
                sambambaVersion: 'invalidSambambaVersion',
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /Invalid sambamba version: 'invalidSambambaVersion',.*/
    }

    @Unroll
    void "test configurePanCanAlignmentDeciderProject phix reference genome require sambamba for merge"() {
        given:
        setupData()
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration(
                referenceGenome: createReferenceGenome(name: "referencegenome_${ProjectService.PHIX_INFIX}").name,
                mergeTool: tool,
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /Only sambamba supported for reference genome with Phix.*/

        where:
        tool << [MergeTool.PICARD, MergeTool.BIOBAMBAM]*.name
    }

    void "test configurePanCanAlignmentDeciderProject invalid configVersion input"() {
        given:
        setupData()
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration(
                configVersion: 'invalid/Version',
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /configVersion 'invalid\/Version' has not expected pattern.*/
    }

    void "test configurePanCanAlignmentDeciderProject to configureDefaultOtpAlignmentDecider"() {
        given:
        setupData()
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
            projectService.configureDefaultOtpAlignmentDecider(configuration.project, configuration.referenceGenome)
        }

        then:
        configuration.project.alignmentDeciderBeanName == AlignmentDeciderBeanName.OTP_ALIGNMENT
        Collection<ReferenceGenomeProjectSeqType> referenceGenomeProjectSeqTypes = ReferenceGenomeProjectSeqType.findAllByDeprecatedDateIsNull()
        referenceGenomeProjectSeqTypes.every {
            it.referenceGenome.name == configuration.referenceGenome && it.statSizeFileName == null
        }
        referenceGenomeProjectSeqTypes.size() == 2
    }

    void "test configurePanCanAlignmentDeciderProject to configureNoAlignmentDeciderProject"() {
        given:
        setupData()
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
            projectService.configureNoAlignmentDeciderProject(configuration.project)
        }

        then:
        configuration.project.alignmentDeciderBeanName == AlignmentDeciderBeanName.NO_ALIGNMENT
        ReferenceGenomeProjectSeqType.findAllByDeprecatedDateIsNull().size() == 0
    }

    void "test configureRnaAlignmentConfig valid input"() {
        given:
        setupData()
        RoddyConfiguration configuration = new RoddyConfiguration(
                project: Project.findByName("testProjectAlignment"),
                seqType: SeqTypeService.wholeGenomePairedSeqType,
                pluginName: 'plugin',
                programVersion: '1.2.3',
                baseProjectConfig: 'baseConfig',
                configVersion: 'v1_0',
        )
        File projectDirectory = LsdfFilesService.getPath(
                configService.rootPath.path,
                configuration.project.dirName,
        )
        assert projectDirectory.mkdirs()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configureRnaAlignmentConfig(configuration)
        }

        then:
        RoddyWorkflowConfig roddyWorkflowConfig = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineAndProgramVersionAndObsoleteDateIsNull(
                configuration.project,
                configuration.seqType,
                Pipeline.Name.RODDY_RNA_ALIGNMENT.pipeline,
                "${configuration.pluginName}:${configuration.programVersion}"
        ))
        File roddyWorkflowConfigFile = new File(roddyWorkflowConfig.configFilePath)
        roddyWorkflowConfigFile.exists()
        PosixFileAttributes attributes = Files.readAttributes(roddyWorkflowConfigFile.toPath(), PosixFileAttributes, LinkOption.NOFOLLOW_LINKS)
        TestCase.assertContainSame(attributes.permissions(), [PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ])
    }

    @Unroll
    void "test configureRnaAlignmentReferenceGenome valid input (mouseData = #mouseData)"() {
        given:
        setupData()
        RnaAlignmentReferenceGenomeConfiguration configuration = createRnaAlignmentConfiguration(
                mouseData: mouseData
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configureRnaAlignmentReferenceGenome(configuration)
        }

        then:
        configuration.project.alignmentDeciderBeanName == AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = CollectionUtils.exactlyOneElement(
                ReferenceGenomeProjectSeqType.list()
        )
        configuration.project == referenceGenomeProjectSeqType.project
        configuration.seqType == referenceGenomeProjectSeqType.seqType
        referenceGenomeProjectSeqType.sampleType == null
        configuration.referenceGenome == referenceGenomeProjectSeqType.referenceGenome.name
        TestCase.assertContainSame(entries, referenceGenomeProjectSeqType.alignmentProperties*.name)

        where:
        mouseData || entries
        false     || [ProjectService.ARRIBA_KNOWN_FUSIONS, ProjectService.ARRIBA_BLACKLIST, ProjectService.GENOME_STAR_INDEX,
                      ProjectService.GENOME_GATK_INDEX, ProjectService.GENOME_KALLISTO_INDEX,
                      GeneModel.GENE_MODELS, GeneModel.GENE_MODELS_DEXSEQ, GeneModel.GENE_MODELS_EXCLUDE, GeneModel.GENE_MODELS_GC]
        true      || [ProjectService.RUN_ARRIBA, ProjectService.RUN_FEATURE_COUNTS_DEXSEQ, ProjectService.GENOME_STAR_INDEX,
                      ProjectService.GENOME_GATK_INDEX, ProjectService.GENOME_KALLISTO_INDEX,
                      GeneModel.GENE_MODELS, GeneModel.GENE_MODELS_EXCLUDE, GeneModel.GENE_MODELS_GC]
    }

    @Unroll
    void "test configureRnaAlignmentReferenceGenome set general reference genome and deprecate = #deprecateConfigurations specific ones"() {
        given:
        setupData()
        int configuredSampleTypes = 3
        Project project = createProject()
        SeqType seqType = createSeqType()
        ReferenceGenome referenceGenome = createReferenceGenome()
        ReferenceGenome newReferenceGenome = createReferenceGenome()

        List<SampleType> sampleTypes = (1..configuredSampleTypes).collect { return DomainFactory.createSampleType() }
        sampleTypes.add(null)
        sampleTypes.each { SampleType sampleType ->
            DomainFactory.createReferenceGenomeProjectSeqType(
                    project: project,
                    seqType: seqType,
                    referenceGenome: referenceGenome,
                    sampleType: sampleType,
                    deprecatedDate: null,
            )
        }
        RnaAlignmentReferenceGenomeConfiguration configuration = createRnaAlignmentConfiguration(
                project: project,
                seqType: seqType,
                referenceGenome: newReferenceGenome.name,
                deprecateConfigurations: deprecateConfigurations,
                sampleTypes: [],
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configureRnaAlignmentReferenceGenome(configuration)
        }

        then:
        configuration.project.alignmentDeciderBeanName == AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT
        List<ReferenceGenomeProjectSeqType> leftOverConfigurations = ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndDeprecatedDateIsNull(project, seqType)
        leftOverConfigurations.size() == (deprecateConfigurations ? 1 : configuredSampleTypes + 1)
        ReferenceGenomeProjectSeqType generalRefGenConfig = leftOverConfigurations.find { it.sampleType == null }
        generalRefGenConfig.referenceGenome == newReferenceGenome

        where:
        deprecateConfigurations | _
        false                   | _
        true                    | _
    }

    void "test configureDefaultOtpAlignmentDecider to configurePanCanAlignmentDeciderProject"() {
        given:
        setupData()
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configureDefaultOtpAlignmentDecider(configuration.project, configuration.referenceGenome)
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        configuration.project.alignmentDeciderBeanName == AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT
        RoddyWorkflowConfig.findAllByProjectAndPipelineInListAndProgramVersionAndObsoleteDateIsNull(
                configuration.project,
                Pipeline.findAllByTypeAndName(Pipeline.Type.ALIGNMENT, Pipeline.Name.PANCAN_ALIGNMENT),
                "${configuration.pluginName}:${configuration.programVersion}"
        ).size() == 1
        ReferenceGenomeProjectSeqType.findAllByDeprecatedDateIsNull().size() == 1
    }

    void "copyPanCanAlignmentXml, target project has no previous config"() {
        given:
        setupData()
        Map<String, Object> setup = setupValidInputForCopyPanCanAlignmentXml()
        Project baseProject = setup['baseProject'] as Project
        Project targetProject = setup['targetProject'] as Project
        SeqType seqType = setup['seqType'] as SeqType

        expect: "No previous config or RGPST for targetProject"
        [] == RoddyWorkflowConfig.findAllByProjectAndSeqType(targetProject, seqType)
        [] == ReferenceGenomeProjectSeqType.findAllByProjectAndSeqType(targetProject, seqType)

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.copyPanCanAlignmentXml(baseProject, targetProject, seqType)
        }

        then: "RoddyWorkflowConfig created for target project"
        RoddyWorkflowConfig targetConfig = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.findAllByProjectAndSeqType(targetProject, seqType))
        targetConfig.configVersion == "v1_0"

        and: "target project uses PanCan AlignmentDecider"
        targetProject.alignmentDeciderBeanName == AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT

        and: "file has been copied and adapted"
        Path targetConfigFile = Paths.get(targetConfig.configFilePath)
        Files.exists(targetConfigFile)
        targetConfigFile.text == FileContentHelper.createXmlContentForRoddyWorkflowConfig(targetConfig.nameUsedInConfig)
        Files.getPosixFilePermissions(targetConfigFile) == fileService.DEFAULT_FILE_PERMISSION
    }

    void "copyPanCanAlignmentXml, target project has been configured before"() {
        given:
        setupData()
        Map<String, Object> setup = setupValidInputForCopyPanCanAlignmentXml()
        Project baseProject = setup['baseProject'] as Project
        Project targetProject = setup['targetProject'] as Project
        SeqType seqType = setup['seqType'] as SeqType
        Pipeline pipeline = setup['pipeline'] as Pipeline

        RoddyWorkflowConfig targetWorkflowConfig = DomainFactory.createRoddyWorkflowConfig(
                project: targetProject,
                seqType: seqType,
                pipeline: pipeline,
                configVersion: "v1_2",
        )

        ReferenceGenomeProjectSeqType targetRefGenProjectSeqType = DomainFactory.createReferenceGenomeProjectSeqType(
                project: targetProject,
                seqType: seqType
        )

        expect: "target project has already been configured once"
        [targetWorkflowConfig] == RoddyWorkflowConfig.findAllByProjectAndSeqType(targetProject, seqType)
        [targetRefGenProjectSeqType] == ReferenceGenomeProjectSeqType.findAllByProjectAndSeqType(targetProject, seqType)

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.copyPanCanAlignmentXml(baseProject, targetProject, seqType)
        }

        then: "there are multiple deprecated WorkflowConfigs, but only one active"
        RoddyWorkflowConfig.findAllByProject(targetProject).size() > 1
        RoddyWorkflowConfig targetConfig = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.findAllByProjectAndObsoleteDateIsNull(targetProject))
        targetConfig.configVersion == "v1_3"

        and: "target project uses PanCan AlignmentDecider"
        targetProject.alignmentDeciderBeanName == AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT

        and: "file has been copied and adapted"
        Path targetConfigFile = Paths.get(targetConfig.configFilePath)
        Files.exists(targetConfigFile)
        targetConfigFile.text == FileContentHelper.createXmlContentForRoddyWorkflowConfig(targetConfig.nameUsedInConfig)
        Files.getPosixFilePermissions(targetConfigFile) == fileService.DEFAULT_FILE_PERMISSION
    }

    void "adaptConfigurationNameInRoddyConfigFile, replaces name used in config properly"() {
        given:
        String oldName = "oldNameUsedInConfig"
        String newName = "newNameUsedInConfig"

        Path configFile = temporaryFolder.newFile().toPath()
        CreateFileHelper.createRoddyWorkflowConfig(configFile.toFile(), oldName)

        when:
        projectService.adaptConfigurationNameInRoddyConfigFile(configFile, oldName, newName)

        then:
        configFile.text == FileContentHelper.createXmlContentForRoddyWorkflowConfig(newName)
    }

    void "copyReferenceGenomeProjectSeqType, deprecates all RGPSTs of SeqType and copies values of base RGPST"() {
        given:
        SeqType seqType = DomainFactory.createSeqType()
        ReferenceGenomeProjectSeqType baseRGPST = DomainFactory.createReferenceGenomeProjectSeqType(seqType: seqType)

        Project targetProject = DomainFactory.createProject()
        DomainFactory.createReferenceGenomeProjectSeqType(project: targetProject, seqType: seqType)

        when:
        ReferenceGenomeProjectSeqType targetRGPST = projectService.copyReferenceGenomeProjectSeqType(baseRGPST.project, targetProject, seqType)

        then:
        targetRGPST.project == targetProject
        targetRGPST.seqType == baseRGPST.seqType
        targetRGPST.referenceGenome == baseRGPST.referenceGenome
        targetRGPST.sampleType == baseRGPST.sampleType
        targetRGPST.statSizeFileName == baseRGPST.statSizeFileName

        and: "The only active ReferenceGenomeProjectSeqType is the one we just created"
        targetRGPST == CollectionUtils.exactlyOneElement(ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndDeprecatedDateIsNull(targetProject, seqType))
    }

    private Map<String, Object> setupValidInputForCopyPanCanAlignmentXml() {
        SeqType seqType = DomainFactory.createExomeSeqType()

        Realm realm = DomainFactory.createRealm()
        Project baseProject = createProject(realm: realm)
        Project targetProject = createProject(realm: realm)

        String pluginName = "programVersion"
        String programVersion = "1.1.51"
        String configVersion = "v1_2"

        File baseXmlConfigFile = temporaryFolder.newFile("PANCAN_ALIGNMENT_WES_PAIRED_${programVersion}_${configVersion}.xml")
        CreateFileHelper.createRoddyWorkflowConfig(baseXmlConfigFile, "PANCAN_ALIGNMENT_WES_PAIRED_${pluginName}:${programVersion}_${configVersion}")

        Pipeline pipeline = Pipeline.findByTypeAndName(Pipeline.Type.ALIGNMENT, Pipeline.Name.PANCAN_ALIGNMENT)
        assert pipeline: "Pipeline could not be found"

        DomainFactory.createRoddyWorkflowConfig(
                project       : baseProject,
                seqType       : seqType,
                pipeline      : pipeline,
                configFilePath: baseXmlConfigFile,
                programVersion: "${pluginName}:${programVersion}",
                configVersion : configVersion,
        )

        DomainFactory.createReferenceGenomeProjectSeqType(
                project: baseProject,
                seqType: seqType,
        )

        Path baseProjectDirectory = projectService.getProjectDirectory(baseProject)
        assert Files.exists(baseProjectDirectory) || Files.createDirectories(baseProjectDirectory)

        Path targetProjectDirectory = projectService.getProjectDirectory(targetProject)
        assert Files.exists(targetProjectDirectory) || Files.createDirectories(targetProjectDirectory)

        return [
                "baseProject"  : baseProject,
                "targetProject": targetProject,
                "seqType"      : seqType,
                "pipeline"     : pipeline,
        ]
    }

    void "test configureDefaultOtpAlignmentDecider to configureNoAlignmentDeciderProject"() {
        given:
        setupData()
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configureDefaultOtpAlignmentDecider(project, referenceGenome.name)
            projectService.configureNoAlignmentDeciderProject(project)
        }

        then:
        project.alignmentDeciderBeanName == AlignmentDeciderBeanName.NO_ALIGNMENT
        ReferenceGenomeProjectSeqType.findAllByDeprecatedDateIsNull().size() == 0
    }

    @Unroll
    void "test configure #analysisName pipelineProject valid input"() {
        given:
        setupData()
        RoddyConfiguration configuration = "createRoddy${analysisName}Configuration"()
        if (analysisName in ["Sophia", "Aceseq"]) {
            ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = DomainFactory.createReferenceGenomeProjectSeqType(
                    project: configuration.project,
                    seqType: configuration.seqType,
                    referenceGenome: DomainFactory.createAceseqReferenceGenome()
            )
            referenceGenomeService.pathToChromosomeSizeFilesPerReference(referenceGenomeProjectSeqType.referenceGenome, false).mkdirs()
            SpringSecurityUtils.doWithAuth(ADMIN) {
                processingOptionService.createOrUpdate(
                        genomeOption,
                        referenceGenomeProjectSeqType.referenceGenome.name
                )
            }
        }

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService."configure${analysisName}PipelineProject"(configuration)
        }

        then:
        List<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineInListAndProgramVersionAndObsoleteDateIsNull(
                configuration.project,
                configuration.seqType,
                Pipeline.findAllByTypeAndName(Pipeline.Type."${analysisName.toUpperCase()}", Pipeline.Name."RODDY_${analysisName.toUpperCase()}"),
                "${configuration.pluginName}:${configuration.programVersion}"
        )
        roddyWorkflowConfigs.size() == 1
        File roddyWorkflowConfig = new File(roddyWorkflowConfigs.configFilePath.first())
        roddyWorkflowConfig.exists()
        PosixFileAttributes attributes = Files.readAttributes(roddyWorkflowConfig.toPath(), PosixFileAttributes, LinkOption.NOFOLLOW_LINKS)
        TestCase.assertContainSame(attributes.permissions(), [PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ])

        where:
        analysisName | service       | genomeOption
        "Aceseq"     | AceseqService | OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME
        "Indel"      | null          | null
        "Snv"        | null          | null
        "Sophia"     | SophiaService | OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME
    }

    @Unroll
    void "test configure #analysisName pipelineProject valid input, twice"() {
        given:
        setupData()
        RoddyConfiguration configuration = "createRoddy${analysisName}Configuration"()
        RoddyConfiguration configuration2 = "createRoddy${analysisName}Configuration"([
                configVersion: 'v1_1',
        ])
        if (analysisName in ["Sophia", "Aceseq"]) {
            ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = DomainFactory.createReferenceGenomeProjectSeqType(
                    project: configuration.project,
                    seqType: configuration.seqType,
                    referenceGenome: DomainFactory.createAceseqReferenceGenome()
            )
            referenceGenomeService.pathToChromosomeSizeFilesPerReference(referenceGenomeProjectSeqType.referenceGenome, false).mkdirs()
            SpringSecurityUtils.doWithAuth(ADMIN) {
                processingOptionService.createOrUpdate(
                        genomeOption,
                        referenceGenomeProjectSeqType.referenceGenome.name
                )
            }
        }

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService."configure${analysisName}PipelineProject"(configuration)
            projectService."configure${analysisName}PipelineProject"(configuration2)
        }

        then:
        Set<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.findAllByProjectAndPipelineInListAndProgramVersion(
                configuration.project,
                Pipeline.findAllByTypeAndName(Pipeline.Type."${analysisName.toUpperCase()}", Pipeline.Name."RODDY_${analysisName.toUpperCase()}"),
                "${configuration2.pluginName}:${configuration2.programVersion}"
        )
        roddyWorkflowConfigs.size() == 2
        roddyWorkflowConfigs.findAll { it.obsoleteDate == null }.size() == 1

        where:
        analysisName | service       | genomeOption
        "Aceseq"     | AceseqService | OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME
        "Indel"      | null          | null
        "Snv"        | null          | null
        "Sophia"     | SophiaService | OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME
    }

    void "test configure Snv PipelineProject valid input, old otp snv config exist"() {
        given:
        setupData()
        SnvConfig configuration = DomainFactory.createSnvConfig([
                project: Project.findByName("testProjectAlignment"),
                seqType: SeqTypeService.exomePairedSeqType,
        ])
        File projectDirectory = LsdfFilesService.getPath(
                configService.rootPath.absolutePath,
                configuration.project.dirName,
        )
        assert projectDirectory.exists() || projectDirectory.mkdirs()

        RoddyConfiguration configuration2 = createRoddySnvConfiguration([
                configVersion: 'v1_1',
        ])

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configureSnvPipelineProject(configuration2)
        }

        then:
        Set<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.findAllByProjectAndPipelineInListAndProgramVersion(
                configuration.project,
                Pipeline.findAllByTypeAndName(Pipeline.Type.SNV, Pipeline.Name.RODDY_SNV),
                "${configuration2.pluginName}:${configuration2.programVersion}"
        )
        roddyWorkflowConfigs.size() == 1
        roddyWorkflowConfigs[0].obsoleteDate == null

        SnvConfig snvConfig = CollectionUtils.exactlyOneElement(SnvConfig.list())
        snvConfig.obsoleteDate != null

        roddyWorkflowConfigs[0].previousConfig == snvConfig
    }

    @Unroll
    void "test configure #analysisName pipelineProject valid input, multiple SeqTypes"() {
        given:
        setupData()
        List<RoddyConfiguration> configurations = DomainFactory."create${analysisName}SeqTypes"().collect {
            "createRoddy${analysisName}Configuration"(seqType: it)
        }

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            configurations.each {
                projectService."configure${analysisName}PipelineProject"(it)
            }
        }

        then:
        Set<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.list()
        roddyWorkflowConfigs.size() == configurations.size()
        roddyWorkflowConfigs.findAll { it.obsoleteDate == null }.size() == configurations.size()

        where:
        analysisName << [
                "Indel",
                "Snv",
        ]
    }

    @Unroll
    void "test configure #analysisName PipelineProject invalid pluginName input"() {
        given:
        setupData()
        RoddyConfiguration configuration = "createRoddy${analysisName}Configuration"(
                pluginName: 'invalid/name'
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService."configure${analysisName}PipelineProject"(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /pluginName '.*' is an invalid path component\..*/

        where:
        analysisName << [
                "Aceseq",
                "Indel",
                "Snv",
        ]
    }

    @Unroll
    void "test configure #analysisName pipelineProject invalid programVersion input"() {
        given:
        setupData()
        RoddyConfiguration configuration = "createRoddy${analysisName}Configuration"(
                programVersion: 'invalid/version'
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService."configure${analysisName}PipelineProject"(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /programVersion '.*' is an invalid path component\..*/

        where:
        analysisName << [
                "Aceseq",
                "Indel",
                "Snv",
        ]
    }

    @Unroll
    void "test configure #analysisName pipelineProject invalid configVersion input"() {
        given:
        setupData()
        RoddyConfiguration configuration = "createRoddy${analysisName}Configuration"(
                configVersion: 'invalid/Version',
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService."configure${analysisName}PipelineProject"(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /configVersion 'invalid\/Version' has not expected pattern.*/

        where:
        analysisName << [
                "Aceseq",
                "Indel",
                "Snv",
        ]
    }

    @Unroll
    void "test configure #analysisName PipelineProject invalid baseProjectConfig input"() {
        given:
        setupData()
        RoddyConfiguration configuration = "createRoddy${analysisName}Configuration"(
                baseProjectConfig: baseProjectConfig
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService."configure${analysisName}PipelineProject"(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ message

        where:
        analysisName | baseProjectConfig || message
        "Aceseq"     | ''                || /baseProjectConfig '.*' is an invalid path component\..*/
        "Aceseq"     | "invalid/path"    || /baseProjectConfig '.*' is an invalid path component\..*/
        "Indel"      | ''                || /baseProjectConfig '.*' is an invalid path component\..*/
        "Indel"      | "invalid/path"    || /baseProjectConfig '.*' is an invalid path component\..*/
        "Snv"        | ''                || /baseProjectConfig '.*' is an invalid path component\..*/
        "Snv"        | "invalid/path"    || /baseProjectConfig '.*' is an invalid path component\..*/
    }

    @Unroll
    void "test getCountOfProjectsForSpecifiedPeriod for given date"() {
        given:
        setupData()
        Date baseDate = new Date(0, 0, 10)
        Date startDate = startDateOffset == null ? null : baseDate - startDateOffset
        Date endDate = endDateOffset == null ? null : baseDate - endDateOffset

        Project project = createProject()
        project.dateCreated = baseDate - 1
        project.save(flush: true)

        when:
        int projects = projectService.getCountOfProjectsForSpecifiedPeriod(startDate, endDate, [project])

        then:
        projects == expectedProjects

        where:
        startDateOffset | endDateOffset || expectedProjects
        2               | 0             || 1
        8               | 2             || 0
        null            | null          || 1
    }

    void "getAllProjectsWithSharedUnixGroup works without any projects"() {
        given:
        Map<String, List<Project>> result

        when:
        result = projectService.allProjectsWithSharedUnixGroup

        then:
        result == [:]
    }

    void "getAllProjectsWithSharedUnixGroup properly groups projects and ignores 1:1 unixgroup:project matches"() {
        given:
        Map<String, List<Project>> result
        String unixGroupA = "A"
        String unixGroupB = "B"
        String unixGroupC = "C"
        Project projectA = createProject(unixGroup: unixGroupA)
        Project projectB = createProject(unixGroup: unixGroupA)
        Project projectC = createProject(unixGroup: unixGroupB)
        Project projectD = createProject(unixGroup: unixGroupB)
        Project projectE = createProject(unixGroup: unixGroupB)
        createProject(unixGroup: unixGroupC)

        when:
        result = projectService.allProjectsWithSharedUnixGroup

        then:
        result == [
                (unixGroupA): [projectA, projectB],
                (unixGroupB): [projectC, projectD, projectE],
        ]
    }

    @Unroll
    void "test addProjectToRelatedProjects extends related projects with new project and cleans string"() {
        given:
        setupData()
        Project baseProject = createProject(relatedProjects: value)
        Project newProject = createProject(name: newName)

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.addProjectToRelatedProjects(baseProject, newProject)
        }

        then:
        baseProject.relatedProjects == expected

        where:
        value           | newName || expected
        null            | "P1"    || "P1"
        ""              | "P1"    || "P1"
        "P1,P2"         | "P3"    || "P1,P2,P3"
        "P1,P3"         | "P2"    || "P1,P2,P3"
        "P2,P1,P2"      | "P2"    || "P1,P2"
        ",P1,,P3,P2,P1" | "P4"    || "P1,P2,P3,P4"
    }

    void "sendProjectCreationNotificationEmail, test if email send to correct recipients"() {
        given:
        User userPi = DomainFactory.createUser()
        User userBioinf = DomainFactory.createUser()
        User userBioinfNoNotification = DomainFactory.createUser()

        createAllBasicProjectRoles()
        ProjectRole pi = ProjectRole.findByName(ProjectRole.Basic.PI.name())
        ProjectRole bioinf = ProjectRole.findByName(ProjectRole.Basic.BIOINFORMATICIAN.name())

        Project project = createProject()

        DomainFactory.createUserProjectRole(
                project: project,
                user: userPi,
                projectRoles: [pi] as Set<ProjectRole>,
                receivesNotifications: true,
        )

        DomainFactory.createUserProjectRole(
                project: project,
                user: userBioinf,
                projectRoles: [bioinf] as Set<ProjectRole>,
                receivesNotifications: true,
        )

        DomainFactory.createUserProjectRole(
                project: project,
                user: userBioinfNoNotification,
                projectRoles: [bioinf] as Set<ProjectRole>,
                receivesNotifications: false,
        )

        ProjectService projectService = new ProjectService(
                processingOptionService: processingOptionService,
                messageSourceService: Stub(MessageSourceService) {
                    createMessage("notification.projectCreation.subject", [projectName: project.displayName]) >> "subject"
                    createMessage("notification.projectCreation.message", [
                            projectName               : project.displayName,
                            linkProjectConfig         : 'link',
                            projectFolder             : LsdfFilesService.getPath(configService.rootPath.path, project.dirName),
                            analysisFolder            : project.dirAnalysis,
                            linkUserManagementConfig  : 'link',
                            clusterName               : processingOptionService.findOptionAsString(OptionName.CLUSTER_NAME),
                            teamSignature             : processingOptionService.findOptionAsString(OptionName.EMAIL_SENDER_SALUTATION),
                    ]) >> "body"
                },
                mailHelperService: Mock(MailHelperService),
                createNotificationTextService: new CreateNotificationTextService(
                        linkGenerator: Mock(LinkGenerator) {
                            _ * link(_) >> 'link'
                        },
                        lsdfFilesService: new LsdfFilesService(),
                        processingOptionService: processingOptionService,
                ),
                userProjectRoleService: new UserProjectRoleService(),
                configService: configService,
        )

        when:
        projectService.sendProjectCreationMailToUserAndTicketSystem(project)

        then:
        1 * projectService.mailHelperService.sendEmail('subject', 'body', [userPi.email, userBioinf.email].sort())
    }

    void "updateUnixGroup, should fail with UnixGroupIsInvalidException when it contains invalid chars"() {
        given:
        setupData()
        Project project = createProject()
        String invalidUnixGroup = "%group"

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            projectService.updateUnixGroup(project, invalidUnixGroup)
        }

        then:
        thrown(UnixGroupIsInvalidException)
    }

    void "updateUnixGroup, should fail with UnixGroupNotFoundException when it is not found on cluster"() {
        given:
        setupData()
        Project project = createProject()
        String unixGroup = "unknownGroup"

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            projectService.updateUnixGroup(project, unixGroup)
        }

        then:
        thrown(UnixGroupNotFoundException)
    }

    void "updateUnixGroup, should fail with UnixGroupIsSharedException when it is already used in another project"() {
        given:
        setupData()
        Project project = createProject()
        String unixGroup = createProject().unixGroup

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            projectService.updateUnixGroup(project, unixGroup)
        }

        then:
        thrown(UnixGroupIsSharedException)
    }

    void "updateUnixGroup, should succeed when unixGroup is valid and unshared"() {
        given:
        setupData()
        Project project = createProject()
        String unixGroup = configService.testingGroup

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            projectService.updateUnixGroup(project, unixGroup)
        }

        then:
        noExceptionThrown()
    }

    void "updateUnixGroup, should succeed in force mode when unixGroup is valid and shared"() {
        given:
        setupData()
        Project project = createProject()
        String unixGroup = configService.testingGroup

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            projectService.updateUnixGroup(project, unixGroup, true)
        }

        then:
        noExceptionThrown()
    }

    private File makeStatFile(ReferenceGenome referenceGenome, String statFileName) {
        File statDirectory = referenceGenomeService.pathToChromosomeSizeFilesPerReference(referenceGenome, false)
        assert statDirectory.exists() || statDirectory.mkdirs()
        File statFile = new File(statDirectory, statFileName)
        statFile.text = "someText"
        return statFile
    }

    private PanCanAlignmentConfiguration createPanCanAlignmentConfiguration(Map properties = [:]) {
        PanCanAlignmentConfiguration configuration = new PanCanAlignmentConfiguration([
                project              : Project.findByName("testProjectAlignment"),
                seqType              : SeqTypeService.wholeGenomePairedSeqType,
                referenceGenome      : "testReferenceGenome",
                statSizeFileName     : 'testStatSizeFileName.tab',
                bwaMemVersion        : "bwa-mem",
                sambambaVersion      : "sambamba",
                mergeTool            : MergeTool.PICARD.name,
                pluginName           : 'plugin',
                programVersion       : '1.2.3',
                baseProjectConfig    : 'baseConfig',
                configVersion        : 'v1_0',
                adapterTrimmingNeeded: true,
        ] + properties)
        File projectDirectory = LsdfFilesService.getPath(
                configService.rootPath.absolutePath,
                configuration.project.dirName,
        )
        assert projectDirectory.exists() || projectDirectory.mkdirs()

        makeStatFile(ReferenceGenome.findByName(configuration.referenceGenome), configuration.statSizeFileName)
        return configuration
    }

    private RnaAlignmentReferenceGenomeConfiguration createRnaAlignmentConfiguration(Map properties = [:]) {
        RnaAlignmentReferenceGenomeConfiguration configuration = new RnaAlignmentReferenceGenomeConfiguration([
                project             : Project.findByName("testProjectAlignment"),
                seqType             : SeqTypeService.rnaPairedSeqType,
                referenceGenome     : "testReferenceGenome",
                referenceGenomeIndex: [
                        createReferenceGenomeIndex(toolName: createToolName(name: "${ProjectService.GENOME_STAR_INDEX}_200")),
                        createReferenceGenomeIndex(toolName: createToolName(name: ProjectService.GENOME_KALLISTO_INDEX)),
                        createReferenceGenomeIndex(toolName: createToolName(name: ProjectService.GENOME_GATK_INDEX)),
                        createReferenceGenomeIndex(toolName: createToolName(name: ProjectService.ARRIBA_KNOWN_FUSIONS)),
                        createReferenceGenomeIndex(toolName: createToolName(name: ProjectService.ARRIBA_BLACKLIST)),
                ],
                geneModel           : DomainFactory.createGeneModel(),
                mouseData           : true,
        ] + properties)
        File projectDirectory = LsdfFilesService.getPath(
                configService.rootPath.absolutePath,
                configuration.project.dirName,
        )
        assert projectDirectory.exists() || projectDirectory.mkdirs()

        return configuration
    }

    private RoddyConfiguration createRoddySnvConfiguration(Map properties = [:]) {
        RoddyConfiguration configuration = new RoddyConfiguration([
                project          : Project.findByName("testProjectAlignment"),
                seqType          : SeqTypeService.exomePairedSeqType,
                pluginName       : 'SNVCallingWorkflow',
                programVersion   : '1.0.166-1',
                baseProjectConfig: 'otpSNVCallingWorkflowWES-1.0',
                configVersion    : 'v1_0',
        ] + properties)
        checkProjectDirectory(configuration)
        return configuration
    }

    @SuppressWarnings('UnusedPrivateMethod')
    //method name is constructed at runtime
    private RoddyConfiguration createRoddyIndelConfiguration(Map properties = [:]) {
        RoddyConfiguration configuration = new RoddyConfiguration([
                project          : Project.findByName("testProjectAlignment"),
                seqType          : SeqTypeService.exomePairedSeqType,
                pluginName       : 'IndelCallingWorkflow',
                programVersion   : '1.0.166-1',
                baseProjectConfig: 'otpIndelCallingWorkflowWES-1.0',
                configVersion    : 'v1_0',
        ] + properties)
        checkProjectDirectory(configuration)
        return configuration
    }

    @SuppressWarnings('UnusedPrivateMethod')
    //method name is constructed at runtime
    private RoddyConfiguration createRoddySophiaConfiguration(Map properties = [:]) {
        RoddyConfiguration configuration = new RoddyConfiguration([
                project          : Project.findByName("testProjectAlignment"),
                seqType          : SeqTypeService.wholeGenomePairedSeqType,
                pluginName       : 'SophiaWorkflow',
                programVersion   : '1.0.14',
                baseProjectConfig: 'otpSophia-1.0',
                configVersion    : 'v1_0',
        ] + properties)
        checkProjectDirectory(configuration)
        return configuration
    }

    @SuppressWarnings('UnusedPrivateMethod')
    //method name is constructed at runtime
    private RoddyConfiguration createRoddyAceseqConfiguration(Map properties = [:]) {
        RoddyConfiguration configuration = new RoddyConfiguration([
                project          : Project.findByName("testProjectAlignment"),
                seqType          : SeqTypeService.wholeGenomePairedSeqType,
                pluginName       : 'ACEseqWorkflow',
                programVersion   : '1.2.6',
                baseProjectConfig: 'otpACEseq-1.0',
                configVersion    : 'v1_0',
        ] + properties)
        checkProjectDirectory(configuration)
        return configuration
    }

    private void checkProjectDirectory(RoddyConfiguration configuration) {
        File projectDirectory = LsdfFilesService.getPath(
                configService.rootPath.absolutePath,
                configuration.project.dirName,
        )
        assert projectDirectory.exists() || projectDirectory.mkdirs()
    }
}
