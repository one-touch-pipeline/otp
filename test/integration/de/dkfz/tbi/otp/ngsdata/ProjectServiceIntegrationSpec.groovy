package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.testing.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.*
import grails.test.spock.*
import grails.validation.*
import org.junit.*
import org.junit.rules.*
import org.springframework.mock.web.*
import spock.lang.*

import java.nio.file.*
import java.nio.file.attribute.*

class ProjectServiceIntegrationSpec extends IntegrationSpec implements UserAndRoles {

    RemoteShellHelper remoteShellHelper
    ProcessingOptionService processingOptionService
    ProjectService projectService
    FileService fileService
    ReferenceGenomeService referenceGenomeService
    RoddyWorkflowConfigService roddyWorkflowConfigService
    TestConfigService configService

    static final String FILE_NAME = "fileName"
    static final byte[] CONTENT = 0..3


    @Rule
    TemporaryFolder temporaryFolder


    def setup() {
        createUserAndRoles()
        DomainFactory.createProject(name: 'testProject', nameInMetadataFiles: 'testProject2', dirName: 'testDir')
        DomainFactory.createProject(name: 'testProject3', nameInMetadataFiles: null)
        ProjectGroup projectGroup = new ProjectGroup(name: 'projectGroup')
        projectGroup.save(flush: true, failOnError: true)
        DomainFactory.createProjectCategory(name: 'category')

        int counter = 0
        Realm realm = DomainFactory.createDefaultRealmWithProcessingOption()

        configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT)   : temporaryFolder.newFolder().path,
                (OtpProperty.PATH_PROCESSING_ROOT): temporaryFolder.newFolder().path,
        ])
        configService.processingOptionService = new ProcessingOptionService()

        DomainFactory.createProject(name: 'testProjectAlignment', realm: realm)
        DomainFactory.createReferenceGenome(name: 'testReferenceGenome')
        DomainFactory.createReferenceGenome(name: 'testReferenceGenome2')
        DomainFactory.createAllAlignableSeqTypes()
        DomainFactory.createPanCanPipeline()
        DomainFactory.createRnaPipeline()
        DomainFactory.createRoddySnvPipelineLazy()
        DomainFactory.createIndelPipelineLazy()
        DomainFactory.createSophiaPipelineLazy()
        DomainFactory.createAceseqPipelineLazy()
        projectService.remoteShellHelper = Stub(RemoteShellHelper) {
            executeCommand(_, _) >> { Realm realm2, String command ->
                File script = temporaryFolder.newFile('script' + counter++ + '.sh')
                script.text = command
                return LocalShellHelper.executeAndWait("bash ${script.absolutePath}").assertExitCodeZero().stdout
            }
        }
        projectService.roddyWorkflowConfigService = new RoddyWorkflowConfigService()
        projectService.roddyWorkflowConfigService.fileSystemService = new TestFileSystemService()

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

        DomainFactory.createProcessingOptionBasePathReferenceGenome(new File(configService.getRootPath(), "reference_genome").path)
    }

    def cleanup() {
        projectService.remoteShellHelper = remoteShellHelper
        projectService.roddyWorkflowConfigService = roddyWorkflowConfigService
        projectService.fileService = fileService
        configService.clean()
    }

    void "test createProject valid input"() {
        given:
        String unixGroup = configService.getTestingGroup()

        when:
        Project project
        ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                name: name,
                dirName: dirName,
                dirAnalysis: dirAnalysis,
                realm: configService.getDefaultRealm(),
                sampleIdentifierParserBeanName: sampleIdentifierParserBeanName,
                qcThresholdHandling: qcThresholdHandling,
                categoryNames: categoryNames,
                unixGroup: unixGroup,
                projectGroup: projectGroup,
                nameInMetadataFiles: nameInMetadataFiles,
                copyFiles: copyFiles,
                description: description,
                processingPriority: processingPriority,
        )
        SpringSecurityUtils.doWithAuth(ADMIN) {
            project = projectService.createProject(projectParams)
        }

        then:
        project.name == name
        project.dirName == dirName
        project.dirAnalysis == dirAnalysis
        project.sampleIdentifierParserBeanName == sampleIdentifierParserBeanName
        project.qcThresholdHandling == qcThresholdHandling
        project.projectCategories == categoryNames.collect { ProjectCategory.findByName(it) } as Set
        project.unixGroup == unixGroup
        project.projectGroup == ProjectGroup.findByName(projectGroup)
        project.nameInMetadataFiles == nameInMetadataFiles
        project.hasToBeCopied == copyFiles
        project.description == description
        project.processingPriority == processingPriority.priority

        where:
        name      | dirName | dirAnalysis | projectGroup   | nameInMetadataFiles | copyFiles | description   | categoryNames | processingPriority            | sampleIdentifierParserBeanName           | qcThresholdHandling
        'project' | 'dir'   | ''          | ''             | 'project'           | true      | 'description' | ["category"]  | ProcessingPriority.NORMAL     | SampleIdentifierParserBeanName.NO_PARSER | QcThresholdHandling.CHECK_NOTIFY_AND_BLOCK
        'project' | 'dir'   | ''          | ''             | null                | true      | ''            | ["category"]  | ProcessingPriority.FAST_TRACK | SampleIdentifierParserBeanName.INFORM    | QcThresholdHandling.CHECK_AND_NOTIFY
        'project' | 'dir'   | ''          | 'projectGroup' | 'project'           | true      | 'description' | ["category"]  | ProcessingPriority.NORMAL     | SampleIdentifierParserBeanName.HIPO      | QcThresholdHandling.NO_CHECK
        'project' | 'dir'   | ''          | ''             | 'project'           | false     | ''            | ["category"]  | ProcessingPriority.FAST_TRACK | SampleIdentifierParserBeanName.HIPO2     | QcThresholdHandling.CHECK_NOTIFY_AND_BLOCK
        'project' | 'dir'   | ''          | ''             | 'project'           | true      | 'description' | ["category"]  | ProcessingPriority.NORMAL     | SampleIdentifierParserBeanName.DEEP      | QcThresholdHandling.CHECK_AND_NOTIFY
        'project' | 'dir'   | '/dirA'     | ''             | 'project'           | true      | 'description' | []            | ProcessingPriority.FAST_TRACK | SampleIdentifierParserBeanName.NO_PARSER | QcThresholdHandling.NO_CHECK
    }

    void "test createProject if directory is created"() {
        given:
        String group = configService.getTestingGroup()

        when:
        Project project
        ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                name: 'project',
                dirName: 'dir',
                dirAnalysis: '/dirA',
                realm: configService.getDefaultRealm(),
                categoryNames: ['category'],
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: null,
                copyFiles: false,
                description: '',
                processingPriority: ProcessingPriority.NORMAL,
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                qcThresholdHandling: QcThresholdHandling.NO_CHECK,
        )
        SpringSecurityUtils.doWithAuth(ADMIN) {
            project = projectService.createProject(projectParams)
        }

        then:
        File projectDirectory = LsdfFilesService.getPath(
                configService.getRootPath().absolutePath,
                project.dirName,
        )
        assert projectDirectory.exists()
        PosixFileAttributes attrs = Files.readAttributes(projectDirectory.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        attrs.group().toString() == group
        TestCase.assertContainSame(attrs.permissions(),
                [PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE])
    }

    @Unroll
    void "test createProject invalid input ('#name', '#dirName', '#nameInMetadataFiles')"() {
        given:
        String group = configService.getTestingGroup()

        when:
        ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                name: name,
                dirName: dirName,
                dirAnalysis: '/dirA',
                realm: configService.getDefaultRealm(),
                categoryNames: ['category'],
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: nameInMetadataFiles,
                copyFiles: true,
                description: '',
                processingPriority: ProcessingPriority.NORMAL,
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                qcThresholdHandling: QcThresholdHandling.NO_CHECK,
        )
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        ValidationException ex = thrown()
        ex.message.contains(errorName) && ex.message.contains(errorLocaction)


        where:
        name           | dirName     | nameInMetadataFiles || errorName                                                                                  | errorLocaction
        'testProject'  | 'dir'       | 'project'           || 'unique'                                                                                   | 'on field \'name\': rejected value [testProject]'
        'testProject2' | 'dir'       | 'project'           || 'this name is already used in another project as nameInMetadataFiles entry'                | 'on field \'name\': rejected value [testProject2]'
        'project'      | 'dir'       | 'testProject'       || 'this nameInMetadataFiles is already used in another project as name entry'                | 'on field \'nameInMetadataFiles\': rejected value [testProject]'
        'project'      | 'dir'       | 'testProject2'      || 'this nameInMetadataFiles is already used in another project as nameInMetadataFiles entry' | 'on field \'nameInMetadataFiles\': rejected value [testProject2]'
        'project'      | 'dir'       | ''                  || 'blank'                                                                                    | 'on field \'nameInMetadataFiles\': rejected value []'
        'project'      | 'testDir'   | ''                  || 'unique'                                                                                   | 'on field \'dirName\': rejected value [testDir]'
        'project'      | '/abs/path' | 'project'           || 'custom validation'                                                                        | "on field 'dirName': rejected value [/abs/path];"
    }

    void "test createProject invalid unix group"() {
        when:
        ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                name: 'project',
                dirName: 'dir',
                dirAnalysis: '/dirA',
                realm: configService.getDefaultRealm(),
                categoryNames: ['category'],
                unixGroup: 'invalidValue',
                projectGroup: '',
                nameInMetadataFiles: null,
                copyFiles: false,
                description: '',
                processingPriority: ProcessingPriority.NORMAL,
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                qcThresholdHandling: QcThresholdHandling.NO_CHECK,
        )
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        AssertionError ex = thrown()
        ex.message.contains('Expected exit code to be 0, but it is 1')
    }

    void "test createProject with invalid dirAnalysis"() {
        given:
        String group = configService.getTestingGroup()

        when:
        ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                name: 'project',
                dirName: 'dir',
                dirAnalysis: 'invalidDirA',
                realm: configService.getDefaultRealm(),
                categoryNames: ['category'],
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: null,
                copyFiles: false,
                description: '',
                processingPriority: ProcessingPriority.NORMAL,
                qcThresholdHandling: QcThresholdHandling.NO_CHECK,
        )
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        ValidationException exception = thrown()
        exception.message.contains("dirAnalysis")
    }

    void "test createProject valid input, when directory with wrong unix group already exists"() {
        given:
        String group = configService.getTestingGroup()
        File projectDirectory = LsdfFilesService.getPath(
                configService.getRootPath().absolutePath,
                "/dir",
        )

        when:
        new File("${projectDirectory}").mkdirs()

        then:
        projectDirectory.exists()
        Files.readAttributes(projectDirectory.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).group().toString() != group

        when:
        ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                name: 'project',
                dirName: 'dir',
                dirAnalysis: '/dirA',
                realm: configService.getDefaultRealm(),
                categoryNames: ['category'],
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: null,
                copyFiles: false,
                description: '',
                processingPriority: ProcessingPriority.NORMAL,
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                qcThresholdHandling: QcThresholdHandling.NO_CHECK,
        )
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }
        then:
        Files.readAttributes(projectDirectory.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).group().toString() == group
    }

    void "test createProject valid input, when directory with correct unix group already exists and project file, then do not create the directory and upload file"() {
        given:
        File projectDirectory = LsdfFilesService.getPath(
                configService.getRootPath().absolutePath,
                "/dir",
        )
        projectService.remoteShellHelper = Mock(RemoteShellHelper) {
            0 * executeCommand(_, _)
        }
        projectService.fileService = Mock(FileService) {
            1 * createFileWithContent(_, _, _)
        }
        MockMultipartFile mockMultipartFile = new MockMultipartFile(FILE_NAME, CONTENT)
        mockMultipartFile.originalFilename = FILE_NAME

        when:
        new File("${projectDirectory}").mkdirs()
        String group = Files.readAttributes(projectDirectory.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).group().toString()

        then:
        projectDirectory.exists()

        when:
        ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                name: 'project',
                dirName: 'dir',
                dirAnalysis: '/dirA',
                realm: configService.getDefaultRealm(),
                categoryNames: ['category'],
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: null,
                copyFiles: false,
                description: '',
                processingPriority: ProcessingPriority.NORMAL,
                projectInfoFile: mockMultipartFile,
                sampleIdentifierParserBeanName: SampleIdentifierParserBeanName.NO_PARSER,
                qcThresholdHandling: QcThresholdHandling.NO_CHECK,
        )
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }
        then:
        Files.readAttributes(projectDirectory.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).group().toString() == group
    }

    void "test updateNameInMetadata valid input"() {
        when:
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
        Project project = Project.findByName("testProject3")
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.updateProjectField(name, "nameInMetadataFiles", project)
        }

        then:
        ValidationException ex = thrown()
        ex.message.contains(errorName) && ex.message.contains(errorLocaction)


        where:
        name           || errorName                                                                                  | errorLocaction
        'testProject'  || 'this nameInMetadataFiles is already used in another project as name entry'                | 'on field \'nameInMetadataFiles\': rejected value [testProject]'
        'testProject2' || 'this nameInMetadataFiles is already used in another project as nameInMetadataFiles entry' | 'on field \'nameInMetadataFiles\': rejected value [testProject2]'
        ''             || 'blank'                                                                                    | 'on field \'nameInMetadataFiles\': rejected value []'
    }

    void "test createProject invalid project category should fail"() {
        given:
        String group = configService.getTestingGroup()

        when:
        ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                name: 'project',
                dirName: 'dir',
                dirAnalysis: '/dirA',
                realm: configService.getDefaultRealm(),
                categoryNames: ['invalid category'],
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: 'project',
                copyFiles: true,
                description: '',
                processingPriority: ProcessingPriority.NORMAL,
        )
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.createProject(projectParams)
        }

        then:
        AssertionError ex = thrown()
        ex.message.contains("Collection contains 0 elements. Expected 1.")
    }

    @Unroll
    void "test updatePhabricatorAlias valid alias and valid user #username"() {
        given:
        String phabricatorAlias = "some alias"
        Project project = Project.findByName("testProject")
        addUserWithReadAccessToProject(User.findByUsername(USER), project)

        when:
        SpringSecurityUtils.doWithAuth(username) {
            projectService.updatePhabricatorAlias(phabricatorAlias, project)
        }

        then:
        project.phabricatorAlias == phabricatorAlias

        where:
        username | _
        ADMIN    | _
        OPERATOR | _
        USER     | _
    }

    void "test updatePhabricatorAlias valid alias and invalide user"() {
        given:
        String phabricatorAlias = "some alias"
        Project project = Project.findByName("testProject")

        when:
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            projectService.updatePhabricatorAlias(phabricatorAlias, project)
        }

        then:
        org.springframework.security.access.AccessDeniedException e = thrown()
        e.message.contains("Access is denied")
    }

    void "test updateCategory invalid project category should fail"() {
        given:
        Project project = Project.findByName("testProject")

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.updateCategory(['not available'], project)
        }

        then:
        AssertionError ex = thrown()
        ex.message.contains("Collection contains 0 elements. Expected 1.")
    }

    void "test updateCategory valid project category"() {
        given:
        Project project = Project.findByName("testProject")
        ProjectCategory projectCategory = DomainFactory.createProjectCategory(name: 'valid category')

        when:
        assert !project.projectCategories
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.updateCategory([projectCategory.name], project)
        }

        then:
        project.projectCategories == [projectCategory] as Set
    }

    void "test updateAnalysisDirectory valid name"() {
        given:
        String analysisDirectory = '/dirA'
        Project project = Project.findByName("testProject")

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
        Project project = Project.findByName("testProject")

        when:
        assert !project.processingPriority
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.updateProjectField(ProcessingPriority.FAST_TRACK.priority, "processingPriority", project)
        }

        then:
        project.processingPriority == ProcessingPriority.FAST_TRACK.priority
    }

    void "test updateTumor valid name"() {
        given:
        Project project = Project.findByName("testProject")
        TumorEntity tumorEntity = DomainFactory.createTumorEntity()

        when:
        assert !project.processingPriority
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.updateProjectField(tumorEntity, "tumorEntity", project)
        }

        then:
        project.tumorEntity == tumorEntity
    }

    void "test configureNoAlignmentDeciderProject"() {
        setup:
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
        setup:
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
        setup:
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
        setup:
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome.findByName("testReferenceGenome")

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configureDefaultOtpAlignmentDecider(project, "error")
        }

        then:
        AssertionError exception = thrown()
        exception.message == "Collection contains 0 elements. Expected 1."
    }

    void "test configurePanCanAlignmentDeciderProject valid input"() {
        setup:
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        configuration.project.alignmentDeciderBeanName == AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT
        List<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineInListAndPluginVersionAndObsoleteDateIsNull(
                configuration.project,
                configuration.seqType,
                Pipeline.findAllByTypeAndName(Pipeline.Type.ALIGNMENT, Pipeline.Name.PANCAN_ALIGNMENT),
                "${configuration.pluginName}:${configuration.pluginVersion}"
        )
        roddyWorkflowConfigs.size() == 1
        File roddyWorkflowConfig = new File(roddyWorkflowConfigs.configFilePath.first())
        roddyWorkflowConfig.exists()
        PosixFileAttributes attributes = Files.readAttributes(roddyWorkflowConfig.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        TestCase.assertContainSame(attributes.permissions(), [PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ])
    }

    void "test configurePanCanAlignmentDeciderProject valid input, twice"() {
        setup:
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
        Set<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.findAllByProjectAndPipelineInListAndPluginVersion(
                configuration.project,
                Pipeline.findAllByTypeAndName(Pipeline.Type.ALIGNMENT, Pipeline.Name.PANCAN_ALIGNMENT),
                "${configuration2.pluginName}:${configuration2.pluginVersion}"
        )
        roddyWorkflowConfigs.size() == 2
        roddyWorkflowConfigs.findAll({ it.obsoleteDate == null }).size() == 1
        ReferenceGenomeProjectSeqType.findAllByDeprecatedDateIsNull().size() == 1
    }

    void "test configurePanCanAlignmentDeciderProject valid input, multiple SeqTypes"() {
        setup:
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
        roddyWorkflowConfigs.findAll({ it.obsoleteDate == null }).size() == count
        ReferenceGenomeProjectSeqType.findAllByDeprecatedDateIsNull().size() == count
    }

    void "test configurePanCanAlignmentDeciderProject invalid referenceGenome input"() {
        setup:
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration()
        configuration.referenceGenome = 'invalidReferenceGenome'

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message == "Collection contains 0 elements. Expected 1."
    }

    void "test configurePanCanAlignmentDeciderProject invalid pluginName input"() {
        setup:
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

    void "test configurePanCanAlignmentDeciderProject invalid pluginVersion input"() {
        setup:
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration(
                pluginVersion: 'invalid/version'
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /pluginVersion '.*' is an invalid path component\..*/
    }

    @Unroll
    void "test configurePanCanAlignmentDeciderProject invalid baseProjectConfig input"() {
        setup:
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
        setup:
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
        setup:
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
        setup:
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
        setup:
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration(
                mergeTool: MergeConstants.MERGE_TOOL_SAMBAMBA,
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
        setup:
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration(
                referenceGenome: DomainFactory.createReferenceGenome(name: "referencegenome_${ProjectService.PHIX_INFIX}").name,
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
        tool << [MergeConstants.MERGE_TOOL_PICARD, MergeConstants.MERGE_TOOL_BIOBAMBAM]
    }

    void "test configurePanCanAlignmentDeciderProject invalid configVersion input"() {
        setup:
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
        setup:
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
        setup:
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
        setup:
        RoddyConfiguration configuration = new RoddyConfiguration(
                project: Project.findByName("testProjectAlignment"),
                seqType: SeqTypeService.wholeGenomePairedSeqType,
                pluginName: 'plugin',
                pluginVersion: '1.2.3',
                baseProjectConfig: 'baseConfig',
                configVersion: 'v1_0',
        )
        File projectDirectory = LsdfFilesService.getPath(
                configService.getRootPath().path,
                configuration.project.dirName,
        )
        assert projectDirectory.mkdirs()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configureRnaAlignmentConfig(configuration)
        }

        then:
        RoddyWorkflowConfig roddyWorkflowConfig = CollectionUtils.exactlyOneElement(RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineAndPluginVersionAndObsoleteDateIsNull(
                configuration.project,
                configuration.seqType,
                Pipeline.Name.RODDY_RNA_ALIGNMENT.pipeline,
                "${configuration.pluginName}:${configuration.pluginVersion}"
        ))
        File roddyWorkflowConfigFile = new File(roddyWorkflowConfig.configFilePath)
        roddyWorkflowConfigFile.exists()
        PosixFileAttributes attributes = Files.readAttributes(roddyWorkflowConfigFile.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        TestCase.assertContainSame(attributes.permissions(), [PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ])
    }

    @Unroll
    void "test configureRnaAlignmentReferenceGenome valid input (mouseData = #mouseData)"() {
        setup:
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
        null == referenceGenomeProjectSeqType.sampleType
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
        int configuredSampleTypes = 3
        Project project = DomainFactory.createProject()
        SeqType seqType = DomainFactory.createSeqType()
        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenome()
        ReferenceGenome newReferenceGenome = DomainFactory.createReferenceGenome()

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
        setup:
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration()

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.configureDefaultOtpAlignmentDecider(configuration.project, configuration.referenceGenome)
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        configuration.project.alignmentDeciderBeanName == AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT
        RoddyWorkflowConfig.findAllByProjectAndPipelineInListAndPluginVersionAndObsoleteDateIsNull(
                configuration.project,
                Pipeline.findAllByTypeAndName(Pipeline.Type.ALIGNMENT, Pipeline.Name.PANCAN_ALIGNMENT),
                "${configuration.pluginName}:${configuration.pluginVersion}"
        ).size() == 1
        ReferenceGenomeProjectSeqType.findAllByDeprecatedDateIsNull().size() == 1
    }

    void "test copyPanCanAlignmentXml valid input, no reference genome configured already"() {
        setup:
        List<Project, SeqType, Project> setupOutput = createValidInputForCopyPanCanAlignmentXml()
        Project basedProject = setupOutput[0]
        SeqType seqType = setupOutput[1]
        Project project = setupOutput[2]

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.copyPanCanAlignmentXml(basedProject, seqType, project)
        }

        then:
        validateCopyPanCanAlignmentXml(project, seqType)
    }

    void "test copyPanCanAlignmentXml valid input, reference genome configured already"() {
        setup:
        List setupOutput = createValidInputForCopyPanCanAlignmentXml()
        Project basedProject = setupOutput[0]
        SeqType seqType = setupOutput[1]
        Project project = setupOutput[2]
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = DomainFactory.createReferenceGenomeProjectSeqType(
                project: project,
                seqType: seqType
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.copyPanCanAlignmentXml(basedProject, seqType, project)
        }

        then:
        validateCopyPanCanAlignmentXml(project, seqType)
        assert referenceGenomeProjectSeqType.deprecatedDate
    }


    private List createValidInputForCopyPanCanAlignmentXml() {
        SeqType seqType = DomainFactory.createExomeSeqType()
        Project project = Project.findByName("testProjectAlignment")

        Realm realm = project.realm
        Project basedProject = DomainFactory.createProject(name: 'basedTestProjectAlignment', realm: realm)

        File tempFile = temporaryFolder.newFile("PANCAN_ALIGNMENT_WES_PAIRED_1.1.51_v1_0.xml")
        CreateFileHelper.createRoddyWorkflowConfig(tempFile, "PANCAN_ALIGNMENT_WES_PAIRED_pluginVersion:1.1.51_v1_0")

        Pipeline pipeline = CollectionUtils.exactlyOneElement(Pipeline.findAllByTypeAndName(
                Pipeline.Type.ALIGNMENT,
                Pipeline.Name.PANCAN_ALIGNMENT,
        ))

        DomainFactory.createRoddyWorkflowConfig(
                project: basedProject,
                seqType: seqType,
                pipeline: pipeline,
                configFilePath: tempFile,
                pluginVersion: 'pluginVersion:1.1.51',
                configVersion: 'v1_0',
        )

        DomainFactory.createReferenceGenomeProjectSeqType(
                project: basedProject,
                seqType: seqType,
        )

        File projectDirectory = basedProject.getProjectDirectory()
        assert projectDirectory.exists() || projectDirectory.mkdirs()

        File projectDirectory1 = project.getProjectDirectory()
        assert projectDirectory1.exists() || projectDirectory1.mkdirs()

        return [basedProject, seqType, project]
    }

    private void validateCopyPanCanAlignmentXml(Project project, SeqType seqType) {
        List<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineInListAndPluginVersionAndObsoleteDateIsNull(
                project,
                seqType,
                Pipeline.findAllByTypeAndName(Pipeline.Type.ALIGNMENT, Pipeline.Name.PANCAN_ALIGNMENT),
                "pluginVersion:1.1.51"
        )
        assert roddyWorkflowConfigs.size() == 1
        File roddyWorkflowConfig = new File(roddyWorkflowConfigs.configFilePath.first())
        assert roddyWorkflowConfig.exists()
        assert project.alignmentDeciderBeanName == AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT
    }


    void "test configureDefaultOtpAlignmentDecider to configureNoAlignmentDeciderProject"() {
        setup:
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
        setup:
        RoddyConfiguration configuration = "createRoddy${analysisName}Configuration"()
        if (analysisName in ["Sophia", "Aceseq"]) {
            ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = DomainFactory.createReferenceGenomeProjectSeqType(
                    project: configuration.project,
                    seqType: configuration.seqType,
                    referenceGenome: DomainFactory.createAceseqReferenceGenome()
            )
            referenceGenomeService.pathToChromosomeSizeFilesPerReference(referenceGenomeProjectSeqType.referenceGenome, false).mkdirs()
            SpringSecurityUtils.doWithAuth(ADMIN, {
                processingOptionService.createOrUpdate(
                        genomeOption,
                        referenceGenomeProjectSeqType.referenceGenome.name
                )
            })
        }

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService."configure${analysisName}PipelineProject"(configuration)
        }

        then:
        List<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineInListAndPluginVersionAndObsoleteDateIsNull(
                configuration.project,
                configuration.seqType,
                Pipeline.findAllByTypeAndName(Pipeline.Type."${analysisName.toUpperCase()}", Pipeline.Name."RODDY_${analysisName.toUpperCase()}"),
                "${configuration.pluginName}:${configuration.pluginVersion}"
        )
        roddyWorkflowConfigs.size() == 1
        File roddyWorkflowConfig = new File(roddyWorkflowConfigs.configFilePath.first())
        roddyWorkflowConfig.exists()
        PosixFileAttributes attributes = Files.readAttributes(roddyWorkflowConfig.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        TestCase.assertContainSame(attributes.permissions(), [PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ])

        where:
        analysisName | service       | genomeOption
        "Aceseq"     | AceseqService | OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME
        "Indel"      | null          | null
        "Snv"        | null          | null
        "Sophia"     | SophiaService | ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME
    }

    @Unroll
    void "test configure #analysisName pipelineProject valid input, twice"() {
        setup:
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
            SpringSecurityUtils.doWithAuth(ADMIN, {
                processingOptionService.createOrUpdate(
                        genomeOption,
                        referenceGenomeProjectSeqType.referenceGenome.name
                )
            })
        }

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService."configure${analysisName}PipelineProject"(configuration)
            projectService."configure${analysisName}PipelineProject"(configuration2)
        }

        then:
        Set<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.findAllByProjectAndPipelineInListAndPluginVersion(
                configuration.project,
                Pipeline.findAllByTypeAndName(Pipeline.Type."${analysisName.toUpperCase()}", Pipeline.Name."RODDY_${analysisName.toUpperCase()}"),
                "${configuration2.pluginName}:${configuration2.pluginVersion}"
        )
        roddyWorkflowConfigs.size() == 2
        roddyWorkflowConfigs.findAll({ it.obsoleteDate == null }).size() == 1

        where:
        analysisName | service       | genomeOption
        "Aceseq"     | AceseqService | OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME
        "Indel"      | null          | null
        "Snv"        | null          | null
        "Sophia"     | SophiaService | ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME
    }

    void "test configure Snv PipelineProject valid input, old otp snv config exist"() {
        setup:
        SnvConfig configuration = DomainFactory.createSnvConfig([
                project: Project.findByName("testProjectAlignment"),
                seqType: SeqTypeService.exomePairedSeqType,
        ])
        File projectDirectory = LsdfFilesService.getPath(
                configService.getRootPath().absolutePath,
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
        Set<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.findAllByProjectAndPipelineInListAndPluginVersion(
                configuration.project,
                Pipeline.findAllByTypeAndName(Pipeline.Type.SNV, Pipeline.Name.RODDY_SNV),
                "${configuration2.pluginName}:${configuration2.pluginVersion}"
        )
        roddyWorkflowConfigs.size() == 1
        roddyWorkflowConfigs[0].obsoleteDate == null

        SnvConfig snvConfig = CollectionUtils.exactlyOneElement(SnvConfig.list())
        snvConfig.obsoleteDate != null

        roddyWorkflowConfigs[0].previousConfig == snvConfig
    }

    @Unroll
    void "test configure #analysisName pipelineProject valid input, multiple SeqTypes"() {
        setup:
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
        roddyWorkflowConfigs.findAll({ it.obsoleteDate == null }).size() == configurations.size()

        where:
        analysisName << [
                "Indel",
                "Snv",
        ]
    }

    @Unroll
    void "test configure #analysisName PipelineProject invalid pluginName input"() {
        setup:
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
    void "test configure #analysisName pipelineProject invalid pluginVersion input"() {
        setup:
        RoddyConfiguration configuration = "createRoddy${analysisName}Configuration"(
                pluginVersion: 'invalid/version'
        )

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService."configure${analysisName}PipelineProject"(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /pluginVersion '.*' is an invalid path component\..*/

        where:
        analysisName << [
                "Aceseq",
                "Indel",
                "Snv",
        ]
    }

    void "test configure #analysisName pipelineProject invalid configVersion input"() {
        setup:
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
        setup:
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

    void "test createProjectInfo, succeeds"() {
        given:
        Project project = DomainFactory.createProject()

        when:
        ProjectInfo projectInfo = projectService.createProjectInfo(project, FILE_NAME)

        then:
        projectInfo.fileName == FILE_NAME
    }

    void "test createProjectInfo, with same fileName for different projects, succeeds"() {
        given:
        Project project1 = DomainFactory.createProject()
        Project project2 = DomainFactory.createProject()

        when:
        ProjectInfo projectInfo1 = projectService.createProjectInfo(project1, FILE_NAME)
        ProjectInfo projectInfo2 = projectService.createProjectInfo(project2, FILE_NAME)

        then:
        projectInfo1.fileName == FILE_NAME
        projectInfo2.fileName == FILE_NAME
    }

    void "test createProjectInfo, with same fileName for same project, fails"() {
        given:
        Project project = DomainFactory.createProject()

        when:
        projectService.createProjectInfo(project, FILE_NAME)
        projectService.createProjectInfo(project, FILE_NAME)

        then:
        ValidationException ex = thrown()
        ex.message.contains('unique')
    }

    void "test createProjectInfoAndUploadFile, succeeds"() {
        given:
        Project project = DomainFactory.createProject()
        MockMultipartFile mockMultipartFile = new MockMultipartFile(FILE_NAME, CONTENT)
        mockMultipartFile.originalFilename = FILE_NAME

        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.createProjectInfoAndUploadFile(project, mockMultipartFile)
        }

        then:
        Path projectInfoFile = Paths.get("${project.getProjectDirectory()}/${projectService.PROJECT_INFO}/${FILE_NAME}")
        PosixFileAttributes attrs = Files.readAttributes(projectInfoFile, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS)

        projectInfoFile.bytes == CONTENT
        TestCase.assertContainSame(attrs.permissions(), [PosixFilePermission.OWNER_READ])
    }

    void "test copyprojectInfoToProjectFolder, succeeds"() {
        given:
        Project project = DomainFactory.createProject()
        byte[] projectInfoContent = []
        MockMultipartFile mockMultipartFile = new MockMultipartFile(FILE_NAME, CONTENT)
        mockMultipartFile.originalFilename = FILE_NAME


        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.createProjectInfoAndUploadFile(project, mockMultipartFile)
            projectInfoContent = projectService.getProjectInfoContent(CollectionUtils.exactlyOneElement(project.projectInfos))
        }

        then:
        projectInfoContent == CONTENT
    }

    void "test copyprojectInfoToProjectFolder, when no file exists, returns []"() {
        given:
        Project project = DomainFactory.createProject()
        byte[] projectInfoContent = []
        MockMultipartFile mockMultipartFile = new MockMultipartFile(FILE_NAME, CONTENT)
        mockMultipartFile.originalFilename = FILE_NAME


        when:
        SpringSecurityUtils.doWithAuth(ADMIN) {
            projectService.createProjectInfoAndUploadFile(project, mockMultipartFile)
            ProjectInfo projectInfo = CollectionUtils.exactlyOneElement(project.projectInfos)
            FileSystem fs = projectService.fileSystemService.getFilesystemForConfigFileChecksForRealm(projectInfo.project.realm)
            Path file = fs.getPath(projectInfo.getPath())
            Files.delete(file)

            projectInfoContent = projectService.getProjectInfoContent(CollectionUtils.exactlyOneElement(project.projectInfos))
        }


        then:
        projectInfoContent == [] as byte[]
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
                mergeTool            : MergeConstants.MERGE_TOOL_PICARD,
                pluginName           : 'plugin',
                pluginVersion        : '1.2.3',
                baseProjectConfig    : 'baseConfig',
                configVersion        : 'v1_0',
                adapterTrimmingNeeded: true,
        ] + properties)
        File projectDirectory = LsdfFilesService.getPath(
                configService.getRootPath().absolutePath,
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
                        DomainFactory.createReferenceGenomeIndex(toolName: DomainFactory.createToolName(name: "${ProjectService.GENOME_STAR_INDEX}_200")),
                        DomainFactory.createReferenceGenomeIndex(toolName: DomainFactory.createToolName(name: ProjectService.GENOME_KALLISTO_INDEX)),
                        DomainFactory.createReferenceGenomeIndex(toolName: DomainFactory.createToolName(name: ProjectService.GENOME_GATK_INDEX)),
                        DomainFactory.createReferenceGenomeIndex(toolName: DomainFactory.createToolName(name: ProjectService.ARRIBA_KNOWN_FUSIONS)),
                        DomainFactory.createReferenceGenomeIndex(toolName: DomainFactory.createToolName(name: ProjectService.ARRIBA_BLACKLIST)),
                ],
                geneModel           : DomainFactory.createGeneModel(),
                mouseData           : true,
        ] + properties)
        File projectDirectory = LsdfFilesService.getPath(
                configService.getRootPath().absolutePath,
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
                pluginVersion    : '1.0.166-1',
                baseProjectConfig: 'otpSNVCallingWorkflowWES-1.0',
                configVersion    : 'v1_0',
        ] + properties)
        checkProjectDirectory(configuration)
        return configuration
    }

    private RoddyConfiguration createRoddyIndelConfiguration(Map properties = [:]) {
        RoddyConfiguration configuration = new RoddyConfiguration([
                project          : Project.findByName("testProjectAlignment"),
                seqType          : SeqTypeService.exomePairedSeqType,
                pluginName       : 'IndelCallingWorkflow',
                pluginVersion    : '1.0.166-1',
                baseProjectConfig: 'otpIndelCallingWorkflowWES-1.0',
                configVersion    : 'v1_0',
        ] + properties)
        checkProjectDirectory(configuration)
        return configuration
    }

    private RoddyConfiguration createRoddySophiaConfiguration(Map properties = [:]) {
        RoddyConfiguration configuration = new RoddyConfiguration([
                project          : Project.findByName("testProjectAlignment"),
                seqType          : SeqTypeService.wholeGenomePairedSeqType,
                pluginName       : 'SophiaWorkflow',
                pluginVersion    : '1.0.14',
                baseProjectConfig: 'otpSophia-1.0',
                configVersion    : 'v1_0',
        ] + properties)
        checkProjectDirectory(configuration)
        return configuration
    }

    private RoddyConfiguration createRoddyAceseqConfiguration(Map properties = [:]) {
        RoddyConfiguration configuration = new RoddyConfiguration([
                project          : Project.findByName("testProjectAlignment"),
                seqType          : SeqTypeService.wholeGenomePairedSeqType,
                pluginName       : 'ACEseqWorkflow',
                pluginVersion    : '1.2.6',
                baseProjectConfig: 'otpACEseq-1.0',
                configVersion    : 'v1_0',
        ] + properties)
        checkProjectDirectory(configuration)
        return configuration
    }

    private checkProjectDirectory(RoddyConfiguration configuration) {
        File projectDirectory = LsdfFilesService.getPath(
                configService.getRootPath().absolutePath,
                configuration.project.dirName,
        )
        assert projectDirectory.exists() || projectDirectory.mkdirs()
    }
}
