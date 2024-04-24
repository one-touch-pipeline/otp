/*
 * Copyright 2011-2024 The OTP authors
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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.validation.ValidationException
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.springframework.mock.web.MockMultipartFile
import spock.lang.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfigService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaService
import de.dkfz.tbi.otp.domainFactory.*
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.FastqcWorkflowDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.parser.SampleIdentifierParserBeanName
import de.dkfz.tbi.otp.project.exception.unixGroup.*
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.*
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.time.*
import java.time.temporal.ChronoUnit

@Rollback
@Integration
class ProjectServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore, DomainFactoryProcessingPriority,
        WorkflowSystemDomainFactory, FastqcWorkflowDomainFactory, UserDomainFactory, CellRangerFactory {

    RemoteShellHelper remoteShellHelper
    ProcessingOptionService processingOptionService
    ProjectService projectService
    FileService fileService
    FileSystemService fileSystemService
    ReferenceGenomeService referenceGenomeService
    RoddyWorkflowConfigService roddyWorkflowConfigService
    TestConfigService configService
    AutoTimestampEventListener autoTimestampEventListener

    static final String FILE_NAME = "fileName"
    static final byte[] CONTENT = 0..3

    @TempDir
    Path tempDir

    void baseSetupData() {
        createUserAndRoles()
        configService.addOtpProperties(tempDir)
    }

    void setupData() {
        baseSetupData()

        createProject(name: 'testProject', nameInMetadataFiles: 'testProject2', dirName: 'testDir')
        createProject(name: 'testProject3', nameInMetadataFiles: null)
        ProjectGroup projectGroup = new ProjectGroup(name: 'projectGroup')
        projectGroup.save(flush: true)

        createProject(name: 'testProjectAlignment')
        createReferenceGenome(name: 'testReferenceGenome')
        createReferenceGenome(name: 'testReferenceGenome2')
        DomainFactory.createAllAlignableSeqTypes()
        DomainFactory.createPanCanPipeline()
        DomainFactory.createRnaPipeline()
        DomainFactory.createRoddySnvPipelineLazy()
        DomainFactory.createIndelPipelineLazy()
        DomainFactory.createSophiaPipelineLazy()
        DomainFactory.createAceseqPipelineLazy()
        createWorkflow(name: WgbsWorkflow.WORKFLOW)

        int counter = 0
        projectService.remoteShellHelper = Stub(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String command ->
                File script = CreateFileHelper.createFile(tempDir.resolve('script' + counter++ + '.sh'), command).toFile()
                return LocalShellHelper.executeAndWait("bash ${script.absolutePath}").assertExitCodeZero()
            }
        }
        projectService.fileService = new FileService()
        projectService.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            _ * executeCommandReturnProcessOutput(_) >> { String command ->
                return new ProcessOutput(command, '', 0)
            }
        }

        projectService.fileSystemService = new TestFileSystemService()
        projectService.roddyWorkflowConfigService = new RoddyWorkflowConfigService()
        projectService.roddyWorkflowConfigService.fileSystemService = new TestFileSystemService()
        projectService.roddyWorkflowConfigService.workflowConfigService = new WorkflowConfigService()
        projectService.roddyWorkflowConfigService.configService = Mock(ConfigService)
        projectService.roddyWorkflowConfigService.fileService = new FileService()
        projectService.roddyWorkflowConfigService.fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_) >> { String cmd -> LocalShellHelper.executeAndWait(cmd) }
        }
        projectService.mailHelperService = Mock(MailHelperService) {
            0 * saveMail(_, _, _)
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
        findOrCreateProcessingOption([name: OptionName.DEFAULT_FASTQC_TYPE, value: "BASH"])
    }

    void cleanup() {
        projectService.remoteShellHelper = remoteShellHelper
        projectService.roddyWorkflowConfigService = roddyWorkflowConfigService
        projectService.fileService = fileService
        projectService.fileSystemService = fileSystemService
        fileService.remoteShellHelper = remoteShellHelper
        configService.clean()
    }

    @Unroll
    void "test getAllProjects, user #user can access all projects"() {
        given:
        createUserAndRoles()
        List<Project> projects = [createProject(), createProject(), createProject([state: Project.State.CLOSED])]

        projects.each {
            addUserWithReadAccessToProject(getUser(TESTUSER), it)
        }

        expect:
        TestCase.assertContainSame(projects, doWithAuth(user) {
            projectService.allProjects
        })

        where:
        user << [OPERATOR, ADMIN, TESTUSER]
    }

    void "test getAllProjects, user can only access projects to which they have access and are not deleted"() {
        given:
        createUserAndRoles()
        List<Project> projects = [createProject(), createProject()]
        projects.each {
            addUserWithReadAccessToProject(getUser(TESTUSER), it)
        }
        Project deletedProject = createProject([state: Project.State.DELETED])
        addUserWithReadAccessToProject(getUser(TESTUSER), deletedProject)
        createProject()

        expect:
        TestCase.assertContainSame(projects, doWithAuth(TESTUSER) {
            projectService.allProjects
        })
    }

    void "test getAllProjects, user doesn't have access"() {
        given:
        createUserAndRoles()
        Project projectWithRoleNotEnabled = createProject()
        Project projectWithNoAccessToOtp = createProject()
        createProject() // project with no UserProjectRole
        addUserWithReadAccessToProject(getUser(TESTUSER), projectWithRoleNotEnabled, false)
        createUserProjectRole(
                user: getUser(TESTUSER),
                project: projectWithNoAccessToOtp,
                enabled: true,
                accessToOtp: false,
        )

        expect:
        TestCase.assertContainSame([], doWithAuth(TESTUSER) {
            projectService.allProjects
        })
    }

    void "test getAllProjects, user is disabled"() {
        given:
        createUserAndRoles()
        List<Project> projects = [createProject(), createProject()]
        projects.each {
            addUserWithReadAccessToProject(getUser(TESTUSER), it)
        }
        getUser(TESTUSER).enabled = false
        getUser(TESTUSER).save(flush: true)

        expect:
        TestCase.assertContainSame([], doWithAuth(TESTUSER) {
            projectService.allProjects
        })
    }

    @Unroll
    void "test createProject valid input"() {
        given:
        setupData()

        WorkflowVersion workflowVersion = createBashFastqcWorkflowVersion()
        Workflow workflow = workflowVersion.workflow
        workflow.defaultSeqTypesForWorkflowVersions = SeqType.findAll()

        String unixGroup = configService.testingGroup
        Path projectPath = configService.rootPath.toPath().resolve(dirName)
        projectService.fileService = Mock(FileService) {
            1 * createDirectoryRecursivelyAndSetPermissionsViaBash(projectPath.parent, '', FileService.DIRECTORY_WITH_OTHER_PERMISSION_STRING)
            1 * createDirectoryRecursivelyAndSetPermissionsViaBash(projectPath, unixGroup)
            0 * _
        }

        if (dirAnalysis) {
            dirAnalysis = "${tempDir}${dirAnalysis}"
            Path analysisPath = Paths.get(dirAnalysis)
            1 * projectService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(analysisPath.parent, '', FileService.DIRECTORY_WITH_OTHER_PERMISSION_STRING)
            1 * projectService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(analysisPath, unixGroup, FileService.OWNER_AND_GROUP_DIRECTORY_PERMISSION_STRING)
        }

        when:
        Project project

        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name: name,
                dirName: dirName,
                individualPrefix: 'individualPrefix',
                dirAnalysis: dirAnalysis,
                relatedProjects: relatedProjects,
                sampleIdentifierParserBeanName: sampleIdentifierParserBeanName,
                unixGroup: unixGroup,
                projectGroup: projectGroup,
                nameInMetadataFiles: nameInMetadataFiles,
                description: description,
                processingPriority: createProcessingPriority(priority: processingPriority),
                projectType: Project.ProjectType.SEQUENCING,
                storageUntil: LocalDate.now(),
                publiclyAvailable: false,
        )
        project = doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        project.name == name
        project.dirName == dirName
        project.dirAnalysis == dirAnalysis
        project.relatedProjects == relatedProjects
        project.sampleIdentifierParserBeanName == sampleIdentifierParserBeanName
        project.unixGroup == unixGroup
        project.projectGroup == CollectionUtils.atMostOneElement(ProjectGroup.findAllByName(projectGroup))
        project.nameInMetadataFiles == nameInMetadataFiles
        project.description == description
        project.processingPriority.priority == processingPriority

        WorkflowVersionSelector.findAll().first().project == project
        WorkflowVersionSelector.findAll().first().workflowVersion == workflowVersion

        where:
        name      | dirName | dirAnalysis | relatedProjects | projectGroup   | nameInMetadataFiles | description   | processingPriority            | sampleIdentifierParserBeanName
        'project' | 'dir'   | ''          | ''              | ''             | null                | ''            | ProcessingPriority.FAST_TRACK | SampleIdentifierParserBeanName.INFORM
        'project' | 'dir'   | ''          | ''              | 'projectGroup' | 'project'           | 'description' | ProcessingPriority.NORMAL     | SampleIdentifierParserBeanName.HIPO
        'project' | 'dir'   | ''          | ''              | ''             | 'project'           | 'description' | ProcessingPriority.NORMAL     | SampleIdentifierParserBeanName.DEEP
        'project' | 'dir'   | '/dirA'     | ''              | ''             | 'project'           | 'description' | ProcessingPriority.FAST_TRACK | SampleIdentifierParserBeanName.NO_PARSER
    }

    @Unroll
    void "test updateAllRelatedProjects updates all related projects with new project name"() {
        given:
        baseSetupData()
        Project baseProject = createProject(name: "P2", relatedProjects: baseRelatedProjects)
        Project baseProject2 = createProject(name: "P3", relatedProjects: "P4,P5")
        Project newProject = createProject(name: "P1", relatedProjects: "P2,P3,P10")

        when:
        doWithAuth(ADMIN) {
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

    void "test createProject, when directories does not exist yet, create it"() {
        given:
        setupData()
        String unixGroup = configService.testingGroup

        WorkflowVersion workflowVersion = createBashFastqcWorkflowVersion()
        Workflow workflow = workflowVersion.workflow
        workflow.defaultSeqTypesForWorkflowVersions = SeqType.findAll()

        String dirName = 'projectDir/projectSubDir'
        Path projectPath = configService.rootPath.toPath().resolve(dirName)
        Path analysisPath = tempDir.resolve('analysisDir/analysisSubDir')
        projectService.fileService = Mock(FileService)

        Project project

        when:
        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name: 'project',
                dirName: dirName,
                individualPrefix: 'individualPrefix',
                dirAnalysis: analysisPath.toString(),
                unixGroup: unixGroup,
                projectGroup: '',
                nameInMetadataFiles: null,
                description: '',
                processingPriority: createProcessingPriority(),
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                projectType: Project.ProjectType.SEQUENCING,
                storageUntil: LocalDate.now(),
        )
        project = doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        1 * projectService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(projectPath.parent, '', FileService.DIRECTORY_WITH_OTHER_PERMISSION_STRING)
        1 * projectService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(projectPath, unixGroup)
        1 * projectService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(analysisPath.parent, '', FileService.DIRECTORY_WITH_OTHER_PERMISSION_STRING)
        1 * projectService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(analysisPath, unixGroup, FileService.OWNER_AND_GROUP_DIRECTORY_PERMISSION_STRING)
        0 * projectService.fileService._

        project
    }

    void "test createProject, when directories already exist, then change permission and group"() {
        given:
        setupData()
        String unixGroup = configService.testingGroup

        WorkflowVersion workflowVersion = createBashFastqcWorkflowVersion()
        Workflow workflow = workflowVersion.workflow
        workflow.defaultSeqTypesForWorkflowVersions = SeqType.findAll()

        String dirName = 'projectDir/subDir'
        Path projectPath = configService.rootPath.toPath().resolve(dirName)
        Path analysisPath = tempDir.resolve('analysisDir/subDir')
        projectService.fileService = Mock(FileService)

        Files.createDirectories(projectPath)
        Files.createDirectories(analysisPath)

        Project project

        when:
        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name: 'project',
                dirName: dirName,
                individualPrefix: 'individualPrefix',
                dirAnalysis: analysisPath.toString(),
                unixGroup: unixGroup,
                projectGroup: '',
                nameInMetadataFiles: null,
                description: '',
                processingPriority: createProcessingPriority(),
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                projectType: Project.ProjectType.SEQUENCING,
                storageUntil: LocalDate.now(),
        )
        project = doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        1 * projectService.fileService.setGroupViaBash(projectPath, unixGroup)
        1 * projectService.fileService.setPermissionViaBash(projectPath, FileService.DEFAULT_DIRECTORY_PERMISSION_STRING)
        1 * projectService.fileService.setGroupViaBash(analysisPath, unixGroup)
        1 * projectService.fileService.setPermissionViaBash(analysisPath, FileService.OWNER_AND_GROUP_DIRECTORY_PERMISSION_STRING)
        0 * projectService.fileService._

        project
    }

    void "test createProject when dirAnalysis can not be created, sends email"() {
        given:
        setupData()
        String unixGroup = configService.testingGroup

        WorkflowVersion workflowVersion = createBashFastqcWorkflowVersion()
        Workflow workflow = workflowVersion.workflow
        workflow.defaultSeqTypesForWorkflowVersions = SeqType.findAll()

        String exceptionMessage = "message ${nextId}"
        String dirName = 'projectDir'
        Path projectPath = configService.rootPath.toPath().resolve(dirName)
        Path analysisPath = tempDir.resolve('analysisDir')
        projectService.fileService = Mock(FileService)

        Project project

        when:
        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name: 'project',
                dirName: dirName,
                individualPrefix: 'individualPrefix',
                dirAnalysis: analysisPath.toString(),
                unixGroup: unixGroup,
                projectGroup: '',
                nameInMetadataFiles: null,
                description: '',
                processingPriority: createProcessingPriority(),
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                projectType: Project.ProjectType.SEQUENCING,
                storageUntil: LocalDate.now(),
        )
        projectService.mailHelperService = Mock(MailHelperService) {
            1 * saveErrorMailInNewTransaction(_, _) >> { String emailSubject, String content ->
                assert emailSubject == "Could not automatically create analysisDir '${projectParams.dirAnalysis}' for Project '${projectParams.name}'."
                assert content.contains(exceptionMessage)
            }
        }

        project = doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        1 * projectService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(projectPath.parent, '', FileService.DIRECTORY_WITH_OTHER_PERMISSION_STRING)
        1 * projectService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(projectPath, unixGroup)
        1 * projectService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(analysisPath.parent, '', FileService.DIRECTORY_WITH_OTHER_PERMISSION_STRING)
        1 * projectService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(analysisPath, unixGroup, FileService.OWNER_AND_GROUP_DIRECTORY_PERMISSION_STRING) >> {
            throw new OtpFileSystemException(exceptionMessage)
        }
        0 * projectService.fileService._

        project
    }

    @Unroll
    void "test createProject invalid input ('#name', '#dirName', '#nameInMetadataFiles')"() {
        given:
        setupData()
        String group = configService.testingGroup

        projectService.fileService = Mock(FileService) {
            0 * _
        }

        when:
        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name: name,
                dirName: dirName,
                individualPrefix: 'individualPrefix',
                dirAnalysis: "${tempDir}/dirA",
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: nameInMetadataFiles,
                description: '',
                processingPriority: createProcessingPriority(),
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                projectType: Project.ProjectType.SEQUENCING,
                storageUntil: LocalDate.now(),
        )
        doWithAuth(ADMIN) {
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
        given:
        setupData()
        String unixGroup = configService.testingGroup
        String exceptionMessage = "message ${nextId}"
        String dirName = 'projectDir'
        Path projectPath = configService.rootPath.toPath().resolve(dirName)
        Path analysisPath = tempDir.resolve('analysisDir')
        projectService.fileService = Mock(FileService)

        when:
        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name: 'project',
                dirName: dirName,
                individualPrefix: 'individualPrefix',
                dirAnalysis: analysisPath.toString(),
                unixGroup: unixGroup,
                projectGroup: '',
                nameInMetadataFiles: null,
                description: '',
                processingPriority: createProcessingPriority(),
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                projectType: Project.ProjectType.SEQUENCING,
                storageUntil: LocalDate.now(),
        )
        doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        1 * projectService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(projectPath.parent, '', FileService.DIRECTORY_WITH_OTHER_PERMISSION_STRING) >> {
            throw new ChangeFileGroupException(exceptionMessage)
        }
        0 * projectService.fileService._
        then:
        thrown(ChangeFileGroupException)
    }

    void "test createProject with invalid dirAnalysis"() {
        given:
        setupData()
        String group = configService.testingGroup
        projectService.fileService = Mock(FileService) {
            0 * _
        }

        when:
        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name: 'project',
                dirName: 'dir',
                individualPrefix: 'individualPrefix',
                dirAnalysis: 'invalidDirA',
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: null,
                description: '',
                processingPriority: createProcessingPriority(),
                projectType: Project.ProjectType.SEQUENCING,
                storageUntil: LocalDate.now(),
        )
        doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        ValidationException e = thrown()
        e.message.contains("dirAnalysis")
    }

    void "test createProject, when a project file is given, then create the directory and upload the file"() {
        given:
        setupData()
        String unixGroup = configService.testingGroup

        WorkflowVersion workflowVersion = createBashFastqcWorkflowVersion()
        Workflow workflow = workflowVersion.workflow
        workflow.defaultSeqTypesForWorkflowVersions = SeqType.findAll()

        String dirName = 'projectDir/projectSubDir'
        Path projectPath = configService.rootPath.toPath().resolve(dirName)
        Path analysisPath = tempDir.resolve('analysisDir/analysisSubDir')
        projectService.fileService = Mock(FileService)

        projectService.projectInfoService = Mock(ProjectInfoService) {
            1 * createProjectInfoAndUploadFile(_, _)
        }
        MockMultipartFile mockMultipartFile = new MockMultipartFile(FILE_NAME, FILE_NAME, null, CONTENT)

        when:
        ProjectCreationCommand projectParams = new ProjectCreationCommand(
                name: 'project',
                dirName: dirName,
                individualPrefix: 'individualPrefix',
                dirAnalysis: analysisPath.toString(),
                unixGroup: unixGroup,
                projectGroup: '',
                nameInMetadataFiles: null,
                description: '',
                processingPriority: createProcessingPriority(),
                projectInfoFile: mockMultipartFile,
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                projectType: Project.ProjectType.SEQUENCING,
                storageUntil: LocalDate.now(),
        )
        doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        1 * projectService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(projectPath.parent, '', FileService.DIRECTORY_WITH_OTHER_PERMISSION_STRING)
        1 * projectService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(projectPath, unixGroup)
        1 * projectService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(analysisPath.parent, '', FileService.DIRECTORY_WITH_OTHER_PERMISSION_STRING)
        1 * projectService.fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(analysisPath, unixGroup, FileService.OWNER_AND_GROUP_DIRECTORY_PERMISSION_STRING)
        0 * projectService.fileService._
    }

    void "test updateNameInMetadata valid input"() {
        when:
        setupData()
        Project project = CollectionUtils.atMostOneElement(Project.findAllByName("testProject"))
        doWithAuth(ADMIN) {
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
        Project project = CollectionUtils.atMostOneElement(Project.findAllByName("testProject3"))
        doWithAuth(ADMIN) {
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
                dirAnalysis: "${tempDir}/dirA",
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: 'project',
                description: '',
                processingPriority: createProcessingPriority(),
                storageUntil: LocalDate.now(),
        )
        doWithAuth(ADMIN) {
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
        ProjectRole pi = CollectionUtils.atMostOneElement(ProjectRole.findAllByName(ProjectRole.Basic.PI.name()))
        ProjectRole bi = CollectionUtils.atMostOneElement(ProjectRole.findAllByName(ProjectRole.Basic.BIOINFORMATICIAN.name()))

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
        Project project = CollectionUtils.atMostOneElement(Project.findAllByName("testProject"))
        addUserWithReadAccessToProject(CollectionUtils.atMostOneElement(User.findAllByUsername(USER)), project)

        when:
        doWithAuth(username) {
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
        Project project = CollectionUtils.atMostOneElement(Project.findAllByName("testProject"))

        when:
        doWithAuth(TESTUSER) {
            projectService.updateProjectField(individualPrefix, 'individualPrefix', project)
        }

        then:
        org.springframework.security.access.AccessDeniedException e = thrown()
        e.message.contains("Access Denied")
    }

    void "test updateProcessingPriority valid name"() {
        given:
        setupData()
        Project project = CollectionUtils.atMostOneElement(Project.findAllByName("testProject"))
        ProcessingPriority fastTrackProcessingPriority = findOrCreateProcessingPriorityFastrack()

        when:
        assert project.processingPriority != fastTrackProcessingPriority
        doWithAuth(ADMIN) {
            projectService.updateProjectField(fastTrackProcessingPriority, "processingPriority", project)
        }

        then:
        project.processingPriority == fastTrackProcessingPriority
    }

    void "test updateTumor valid name"() {
        given:
        setupData()
        Project project = CollectionUtils.atMostOneElement(Project.findAllByName("testProject"))
        TumorEntity tumorEntity = DomainFactory.createTumorEntity()

        when:
        doWithAuth(ADMIN) {
            projectService.updateProjectField(tumorEntity, "tumorEntity", project)
        }

        then:
        project.tumorEntity == tumorEntity
    }

    void "test updateRelatedProjects valid name"() {
        given:
        setupData()
        Project project = CollectionUtils.atMostOneElement(Project.findAllByName("testProject"))
        String relatedProjects = "testProject3"

        when:
        doWithAuth(ADMIN) {
            projectService.updateProjectField(relatedProjects, "relatedProjects", project)
        }

        then:
        project.relatedProjects == relatedProjects
    }

    void "test updateInternalNotes valid name"() {
        given:
        setupData()
        Project project = CollectionUtils.atMostOneElement(Project.findAllByName("testProject"))
        String internalNotes = "internalNotes"

        when:
        doWithAuth(ADMIN) {
            projectService.updateProjectField(internalNotes, "internalNotes", project)
        }

        then:
        project.internalNotes == internalNotes
    }

    @Unroll
    void "test configure #analysisName pipelineProject valid input"() {
        given:
        setupData()
        RoddyConfiguration configuration = "createRoddy${analysisName}Configuration"()
        if (analysisName in ["Sophia", "Aceseq"]) {
            ReferenceGenomeSelector referenceGenomeSelector = createReferenceGenomeSelector([
                    project        : configuration.project,
                    seqType        : configuration.seqType,
                    species        : [findOrCreateHumanSpecies()] as Set,
                    referenceGenome: DomainFactory.createAceseqReferenceGenome(),
            ])
            referenceGenomeService.pathToChromosomeSizeFilesPerReference(referenceGenomeSelector.referenceGenome, false).mkdirs()
            doWithAuth(ADMIN) {
                processingOptionService.createOrUpdate(
                        genomeOption,
                        referenceGenomeSelector.referenceGenome.name
                )
            }
        }

        when:
        doWithAuth(ADMIN) {
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
            ReferenceGenomeSelector referenceGenomeSelector = createReferenceGenomeSelector([
                    project        : configuration.project,
                    seqType        : configuration.seqType,
                    species        : [findOrCreateHumanSpecies()] as Set,
                    referenceGenome: DomainFactory.createAceseqReferenceGenome(),
            ])
            referenceGenomeService.pathToChromosomeSizeFilesPerReference(referenceGenomeSelector.referenceGenome, false).mkdirs()
            doWithAuth(ADMIN) {
                processingOptionService.createOrUpdate(
                        genomeOption,
                        referenceGenomeSelector.referenceGenome.name
                )
            }
        }

        when:
        doWithAuth(ADMIN) {
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
                project: CollectionUtils.atMostOneElement(Project.findAllByName("testProjectAlignment")),
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
        doWithAuth(ADMIN) {
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
        doWithAuth(ADMIN) {
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
        doWithAuth(ADMIN) {
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
        doWithAuth(ADMIN) {
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
        doWithAuth(ADMIN) {
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
        doWithAuth(ADMIN) {
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
        Instant baseDate = LocalDate.of(2022, 1, 10).atStartOfDay().toInstant(ZoneOffset.UTC)
        Date startDate = startDateOffset == null ? null : Date.from(baseDate.minus(startDateOffset, ChronoUnit.DAYS))
        Date endDate = endDateOffset == null ? null : Date.from(baseDate.minus(endDateOffset, ChronoUnit.DAYS))

        Project project

        autoTimestampEventListener.withoutDateCreated(Project) {
            project = createProject(dateCreated: Date.from(baseDate.minus(1, ChronoUnit.DAYS)))
        }

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
        result.size() == 2
        result.containsKey(unixGroupA)
        result.containsKey(unixGroupB)
        TestCase.assertContainSame(result[unixGroupA], [projectA, projectB])
        TestCase.assertContainSame(result[unixGroupB], [projectC, projectD, projectE])
    }

    @Unroll
    void "test addProjectToRelatedProjects extends related projects with new project and cleans string"() {
        given:
        setupData()
        Project baseProject = createProject(relatedProjects: value)
        Project newProject = createProject(name: newName)

        when:
        doWithAuth(ADMIN) {
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

    @Unroll
    void "updateAnalysisDirectory, should succeed and send no mail, when unix group has permission to create directory and force #force"() {
        given:
        setupData()
        String analysisDirectory = "${tempDir}/dirA"
        Project project = CollectionUtils.atMostOneElement(Project.findAllByName("testProject"))
        project.unixGroup = configService.testingGroup
        project.save(flush: true)

        when:
        doWithAuth(ADMIN) {
            projectService.updateAnalysisDirectory(project, analysisDirectory, force)
        }

        then:
        project.dirAnalysis == analysisDirectory
        0 * projectService.mailHelperService.saveErrorMailInNewTransaction(*_)

        where:
        force << [true, false]
    }

    void "updateAnalysisDirectory, should fail with OtpFileSystemException and send no mail, when unix group has no permission and force mode is false"() {
        given:
        setupData()
        String newDirAnalysis = "/directory/without/permission"
        String oldDirAnalysis = "/old/directory"
        Project project = CollectionUtils.atMostOneElement(Project.findAllByName("testProject"))
        project.dirAnalysis = oldDirAnalysis
        project.save(flush: true)
        projectService.mailHelperService = Mock(MailHelperService)

        when:
        doWithAuth(OPERATOR) {
            projectService.updateAnalysisDirectory(project, newDirAnalysis, false)
        }

        then:
        0 * projectService.mailHelperService.saveErrorMailInNewTransaction(*_)

        then:
        thrown(OtpFileSystemException)
    }

    void "updateAnalysisDirectory, should send email and update, when unix group has no permission and force mode is true"() {
        given:
        setupData()
        String newDirAnalysis = "/directory/without/permission"
        String oldDirAnalysis = "/old/directory"
        Project project = createProject(dirAnalysis: oldDirAnalysis)
        projectService.mailHelperService = Mock(MailHelperService)

        when:
        doWithAuth(OPERATOR) {
            projectService.updateAnalysisDirectory(project, newDirAnalysis, true)
        }

        then:
        project.dirAnalysis == newDirAnalysis
        1 * projectService.mailHelperService.saveErrorMailInNewTransaction(_, _)
    }

    void "updateUnixGroup, should fail with UnixGroupIsInvalidException when it contains invalid chars"() {
        given:
        setupData()
        Project project = createProject()
        String invalidUnixGroup = "%group"

        when:
        doWithAuth(OPERATOR) {
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
        doWithAuth(OPERATOR) {
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
        doWithAuth(OPERATOR) {
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
        doWithAuth(OPERATOR) {
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
        doWithAuth(OPERATOR) {
            projectService.updateUnixGroup(project, unixGroup, true)
        }

        then:
        noExceptionThrown()
    }

    void "getExpiredProjectsWithPIs, when an expired project with PI user exists, it should be found"() {
        given:
        setupData()
        createProject([storageUntil: LocalDate.of(3000, 1, 1)])
        Project expiredProject = createProject([storageUntil: LocalDate.of(1970, 1, 1)])
        ProjectRole projectRolePI = createProjectRole([name: ProjectRole.Basic.PI.name(),])
        User user = createUser()
        createUserProjectRole([
                project     : expiredProject,
                user        : user,
                projectRoles: [projectRolePI],
        ])

        when:
        Map<Project, List<User>> projectUsers = projectService.expiredProjectsWithPIs

        then:
        projectUsers.each { Project resultProject, List<User> users ->
            assert resultProject == expiredProject
            assert users == [user]
        }
    }

    void "getExpiredProjectsWithPIs, when all projects are not expired or deleted or archived, it should return an empty map"() {
        given:
        setupData()
        createProject([storageUntil: LocalDate.of(3000, 1, 1)])
        createProject([storageUntil: LocalDate.of(3010, 2, 2)])
        createProject([storageUntil: LocalDate.of(3020, 3, 3)])
        createProject([storageUntil: LocalDate.of(1970, 1, 1), state: Project.State.DELETED])
        createProject([storageUntil: LocalDate.of(1970, 1, 1), state: Project.State.ARCHIVED])

        when:
        Map<Project, List<User>> projectUsers = projectService.expiredProjectsWithPIs

        then:
        projectUsers == [:]
    }

    void "getExpiredProjectsWithPIs, when an expired project without PI users exists, it should return the project with empty user list"() {
        given:
        setupData()
        createProject([storageUntil: LocalDate.of(3000, 1, 1)])
        Project expiredProject = createProject([storageUntil: LocalDate.of(1970, 1, 1)])
        User user = createUser()
        createUserProjectRole([
                project: expiredProject,
                user   : user,
        ])

        when:
        Map<Project, List<User>> projectUsers = projectService.expiredProjectsWithPIs

        then:
        projectUsers.each { Project resultProject, List<User> users ->
            assert resultProject == expiredProject
            assert users == []
        }
    }

    void "createOrUpdateCellRangerConfig, should not change saved CellRangerConfig when invalid"() {
        given:
        setupData()
        projectService.workflowConfigService = new WorkflowConfigService()
        Project project = createProject()
        SeqType seqType = createSeqType()
        Pipeline pipeline = findOrCreatePipeline()
        CellRangerConfig cellRangerConfig = createCellRangerConfig([
                project : project,
                seqType : seqType,
                pipeline: pipeline,
        ])

        when:
        doWithAuth(ADMIN) {
            SessionUtils.withNewTransaction {
                projectService.createOrUpdateCellRangerConfig(project, seqType, "", null)
            }
        }

        then:
        thrown(ValidationException)
        cellRangerConfig.obsoleteDate == null
        cellRangerConfig.project == project
        cellRangerConfig.seqType == seqType
        cellRangerConfig.pipeline == pipeline
    }

    private RoddyConfiguration createRoddySnvConfiguration(Map properties = [:]) {
        RoddyConfiguration configuration = new RoddyConfiguration([
                project          : CollectionUtils.atMostOneElement(Project.findAllByName("testProjectAlignment")),
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
    // method name is constructed at runtime
    private RoddyConfiguration createRoddyIndelConfiguration(Map properties = [:]) {
        RoddyConfiguration configuration = new RoddyConfiguration([
                project          : CollectionUtils.atMostOneElement(Project.findAllByName("testProjectAlignment")),
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
    // method name is constructed at runtime
    private RoddyConfiguration createRoddySophiaConfiguration(Map properties = [:]) {
        RoddyConfiguration configuration = new RoddyConfiguration([
                project          : CollectionUtils.atMostOneElement(Project.findAllByName("testProjectAlignment")),
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
    // method name is constructed at runtime
    private RoddyConfiguration createRoddyAceseqConfiguration(Map properties = [:]) {
        RoddyConfiguration configuration = new RoddyConfiguration([
                project          : CollectionUtils.atMostOneElement(Project.findAllByName("testProjectAlignment")),
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
