package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddy.RoddyConstants
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.testing.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.*
import grails.test.spock.*
import grails.validation.*
import org.codehaus.groovy.grails.commons.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

import java.nio.file.*
import java.nio.file.attribute.*

class ProjectServiceIntegrationSpec extends IntegrationSpec implements UserAndRoles {

    ProjectService projectService

    ReferenceGenomeService referenceGenomeService

    GrailsApplication grailsApplication

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
        Realm realm = DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: Realm.LATEST_DKFZ_REALM])
        DomainFactory.createRealmDataProcessing(temporaryFolder.newFolder(), [name: realm.name])
        DomainFactory.createProject(name: 'testProjectAlignment', realmName: realm.name, alignmentDeciderBeanName: 'test')
        DomainFactory.createReferenceGenome(name: 'testReferenceGenome')
        DomainFactory.createReferenceGenome(name: 'testReferenceGenome2')
        DomainFactory.createAllAlignableSeqTypes()
        DomainFactory.createPanCanPipeline()
        DomainFactory.createRoddySnvPipelineLazy()
        DomainFactory.createIndelPipelineLazy()
        projectService.executionService = Stub(ExecutionService) {
            executeCommand(_, _) >> { Realm realm2, String command ->
                File script = temporaryFolder.newFile('script' + counter++ + '.sh')
                script.text = command
                return ProcessHelperService.executeAndWait("bash ${script.absolutePath}").assertExitCodeZero().stdout
            }
        }

        DomainFactory.createProcessingOption([
                name: RoddyConstants.OPTION_KEY_BWA_VERSION_AVAILABLE,
                type: null,
                value: "bwa-mem",
        ])
        DomainFactory.createProcessingOption([
                name: RoddyConstants.OPTION_KEY_BWA_PATHS,
                type: "bwa-mem",
                value: temporaryFolder.newFile(),
        ])
        DomainFactory.createProcessingOption([
                name: RoddyConstants.OPTION_KEY_SAMBAMBA_VERSION_AVAILABLE,
                type: null,
                value: "sambamba",
        ])
        DomainFactory.createProcessingOption([
                name: RoddyConstants.OPTION_KEY_SAMBAMBA_PATHS,
                type: "sambamba",
                value: temporaryFolder.newFile(),
        ])
    }

    void "test createProject valid input"() {
        given:
        String group = TestCase.testingGroup(grailsApplication)

        when:
        Project project
        ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                name: name,
                dirName: dirName,
                dirAnalysis: dirAnalysis,
                realmName: Realm.LATEST_DKFZ_REALM,
                alignmentDeciderBeanName: AlignmentDeciderBeanNames.NO_ALIGNMENT.bean,
                categoryNames: category,
                unixGroup: group,
                projectGroup: projectGroup,
                nameInMetadataFiles: nameInMetadataFiles,
                copyFiles: copyFiles,
                mailingListName: mailingListName,
                description: description,
                processingPriority: processingPriority,
        )
        SpringSecurityUtils.doWithAuth("admin") {
            project = projectService.createProject(projectParams)
        }

        then:
        project.name == name
        project.dirName == dirName
        project.dirAnalysis == dirAnalysis
        project.processingPriority == processingPriority
        project.projectGroup == ProjectGroup.findByName(projectGroup)
        project.nameInMetadataFiles == nameInMetadataFiles
        project.hasToBeCopied == copyFiles
        project.projectCategories == category.collect { ProjectCategory.findByName(it) } as Set
        project.mailingListName == mailingListName
        project.description == description

        where:
        name      | dirName | dirAnalysis | projectGroup    | nameInMetadataFiles | copyFiles | mailingListName          | description   | category     | processingPriority
        'project' | 'dir'   | ''          | ''              | 'project'           | true      | "tr_project@MailingList" | 'description' | ["category"] | ProcessingPriority.NORMAL_PRIORITY
        'project' | 'dir'   | ''          | ''              | null                | true      | "tr_project@MailingList" | ''            | ["category"] | ProcessingPriority.FAST_TRACK_PRIORITY
        'project' | 'dir'   | ''          | 'projectGroup'  | 'project'           | true      | "tr_project@MailingList" | 'description' | ["category"] | ProcessingPriority.NORMAL_PRIORITY
        'project' | 'dir'   | ''          | ''              | 'project'           | false     | "tr_project@MailingList" | ''            | ["category"] | ProcessingPriority.FAST_TRACK_PRIORITY
        'project' | 'dir'   | ''          | ''              | 'project'           | true      | ""                       | 'description' | ["category"] | ProcessingPriority.NORMAL_PRIORITY
        'project' | 'dir'   | '/dirA'     | ''              | 'project'           | true      | ""                       | 'description' | []           | ProcessingPriority.FAST_TRACK_PRIORITY
    }

    void "test createProject if directory is created"() {
        given:
        String group = TestCase.testingGroup(grailsApplication)

        when:
        Project project
        ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                name: 'project',
                dirName: 'dir',
                dirAnalysis: '/dirA',
                realmName: Realm.LATEST_DKFZ_REALM,
                alignmentDeciderBeanName: AlignmentDeciderBeanNames.NO_ALIGNMENT.bean,
                categoryNames: ['category'],
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: null,
                copyFiles: false,
                mailingListName: "tr_Mailing@ListName",
                description: '',
                processingPriority: ProcessingPriority.NORMAL_PRIORITY,
        )
        SpringSecurityUtils.doWithAuth("admin") {
            project = projectService.createProject(projectParams)
        }

        then:
        Realm realm = ConfigService.getRealm(project, Realm.OperationType.DATA_MANAGEMENT)

        File projectDirectory = LsdfFilesService.getPath(
                realm.rootPath,
                project.dirName,
        )
        assert projectDirectory.exists()
        PosixFileAttributes attrs = Files.readAttributes(projectDirectory.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        attrs.group().toString() == group
        TestCase.assertContainSame(attrs.permissions(),
                [PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE])
    }

    void "test createProject invalid input"() {
        given:
        String group = TestCase.testingGroup(grailsApplication)

        when:
        Project project
        ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                name: name,
                dirName: dirName,
                dirAnalysis: '/dirA',
                realmName: Realm.LATEST_DKFZ_REALM,
                alignmentDeciderBeanName: AlignmentDeciderBeanNames.NO_ALIGNMENT.bean,
                categoryNames: ['category'],
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: nameInMetadataFiles,
                copyFiles: true,
                mailingListName: "tr_Mailing@ListName",
                description: '',
                processingPriority: ProcessingPriority.NORMAL_PRIORITY,
        )
        SpringSecurityUtils.doWithAuth("admin") {
            project = projectService.createProject(projectParams)
        }

        then:
        ValidationException ex = thrown()
        ex.message.contains(errorName) && ex.message.contains(errorLocaction)


        where:
        name           | dirName   | nameInMetadataFiles || errorName | errorLocaction
        'testProject'  | 'dir'     | 'project'           || 'unique' | 'on field \'name\': rejected value [testProject]'
        'testProject2' | 'dir'     | 'project'           || 'this name is already used in another project as nameInMetadataFiles entry' | 'on field \'name\': rejected value [testProject2]'
        'project'      | 'dir'     | 'testProject'       || 'this nameInMetadataFiles is already used in another project as name entry' | 'on field \'nameInMetadataFiles\': rejected value [testProject]'
        'project'      | 'dir'     | 'testProject2'      || 'this nameInMetadataFiles is already used in another project as nameInMetadataFiles entry' | 'on field \'nameInMetadataFiles\': rejected value [testProject2]'
        'project'      | 'dir'     | ''                  || 'blank' | 'on field \'nameInMetadataFiles\': rejected value []'
        'project'      | 'testDir' | ''                  || 'unique' | 'on field \'dirName\': rejected value [testDir]'
    }

    void "test createProject invalid unix group"() {
        when:
        Project project
        ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                name: 'project',
                dirName: 'dir',
                dirAnalysis: '/dirA',
                realmName: Realm.LATEST_DKFZ_REALM,
                alignmentDeciderBeanName: AlignmentDeciderBeanNames.NO_ALIGNMENT.bean,
                categoryNames: ['category'],
                unixGroup: 'invalidValue',
                projectGroup: '',
                nameInMetadataFiles: null,
                copyFiles: false,
                mailingListName: "tr_Mailing@ListName",
                description: '',
                processingPriority: ProcessingPriority.NORMAL_PRIORITY,
        )
        SpringSecurityUtils.doWithAuth("admin") {
            project = projectService.createProject(projectParams)
        }

        then:
        AssertionError ex = thrown()
        ex.message.contains('Expected exit code to be 0, but it is 1')
    }

    void "test createProject with invalid mailingListName"() {
        given:
        String group = TestCase.testingGroup(grailsApplication)

        when:
        Project project
        ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                name: 'project',
                dirName: 'dir',
                dirAnalysis: '/dirA',
                realmName: Realm.LATEST_DKFZ_REALM,
                alignmentDeciderBeanName: AlignmentDeciderBeanNames.NO_ALIGNMENT.bean,
                categoryNames: ['category'],
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: null,
                copyFiles: false,
                mailingListName: "invalidMailingListName",
                description: '',
                processingPriority: ProcessingPriority.NORMAL_PRIORITY,
        )
        SpringSecurityUtils.doWithAuth("admin") {
            project = projectService.createProject(projectParams)
        }

        then:
        ValidationException exception = thrown()
        exception.message.contains("mailingListName")
    }

    void "test createProject with invalid dirAnalysis"() {
        given:
        String group = TestCase.testingGroup(grailsApplication)

        when:
        Project project
        ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                name: 'project',
                dirName: 'dir',
                dirAnalysis: 'invalidDirA',
                realmName: Realm.LATEST_DKFZ_REALM,
                alignmentDeciderBeanName: AlignmentDeciderBeanNames.NO_ALIGNMENT.bean,
                categoryNames: ['category'],
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: null,
                copyFiles: false,
                mailingListName: "tr_Mailing@ListName",
                description: '',
                processingPriority: ProcessingPriority.NORMAL_PRIORITY,
        )
        SpringSecurityUtils.doWithAuth("admin") {
            project = projectService.createProject(projectParams)
        }

        then:
        ValidationException exception = thrown()
        exception.message.contains("dirAnalysis")
    }

    void "test createProject valid input, when directory with wrong unix group already exists"() {
        given:
        String group = TestCase.testingGroup(grailsApplication)
        Realm realm = CollectionUtils.exactlyOneElement(Realm.findAllByOperationType(Realm.OperationType.DATA_MANAGEMENT))
        File projectDirectory = LsdfFilesService.getPath(
                realm.rootPath,
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
                realmName: Realm.LATEST_DKFZ_REALM,
                alignmentDeciderBeanName: AlignmentDeciderBeanNames.NO_ALIGNMENT.bean,
                categoryNames: ['category'],
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: null,
                copyFiles: false,
                mailingListName: "tr_Mailing@ListName",
                description: '',
                processingPriority: ProcessingPriority.NORMAL_PRIORITY,
        )
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.createProject(projectParams)
        }
        then:
        Files.readAttributes(projectDirectory.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).group().toString() == group
    }

    void "test updateNameInMetadata valid input"() {
        when:
        Project project = Project.findByName("testProject")
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.updateNameInMetadata(name, project)
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
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.updateNameInMetadata(name, project)
        }

        then:
        ValidationException ex = thrown()
        ex.message.contains(errorName) && ex.message.contains(errorLocaction)


        where:
        name           || errorName | errorLocaction
        'testProject'  || 'this nameInMetadataFiles is already used in another project as name entry' | 'on field \'nameInMetadataFiles\': rejected value [testProject]'
        'testProject2' || 'this nameInMetadataFiles is already used in another project as nameInMetadataFiles entry' | 'on field \'nameInMetadataFiles\': rejected value [testProject2]'
        ''             || 'blank' | 'on field \'nameInMetadataFiles\': rejected value []'
    }

    void "test createProject invalid project category should fail"() {
        given:
        String group = TestCase.testingGroup(grailsApplication)

        when:
        Project project
        ProjectService.ProjectParams projectParams = new ProjectService.ProjectParams(
                name: 'project',
                dirName: 'dir',
                dirAnalysis: '/dirA',
                realmName: Realm.LATEST_DKFZ_REALM,
                alignmentDeciderBeanName: AlignmentDeciderBeanNames.NO_ALIGNMENT.bean,
                categoryNames: ['invalid category'],
                unixGroup: group,
                projectGroup: '',
                nameInMetadataFiles: 'project',
                copyFiles: true,
                mailingListName: "tr_Mailing@ListName",
                description: '',
                processingPriority: ProcessingPriority.NORMAL_PRIORITY,
        )
        SpringSecurityUtils.doWithAuth("admin") {
            project = projectService.createProject(projectParams)
        }

        then:
        AssertionError ex = thrown()
        ex.message.contains("Collection contains 0 elements. Expected 1.")
    }

    void "test updateCategory invalid project category should fail"() {
        given:
        Project project = Project.findByName("testProject")

        when:
        SpringSecurityUtils.doWithAuth("admin") {
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
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.updateCategory([projectCategory.name], project)
        }

        then:
        project.projectCategories == [projectCategory] as Set
    }

    void "test updateMailingListName valid name"() {
        given:
        String mailingListName = "tr_test@Name"
        Project project = Project.findByName("testProject")

        when:
        assert !project.mailingListName
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.updateMailingListName(mailingListName, project)
        }

        then:
        project.mailingListName == mailingListName
    }

    void "test updateAnalysisDirectory valid name"() {
        given:
        String analysisDirectory = '/dirA'
        Project project = Project.findByName("testProject")

        when:
        assert !project.dirAnalysis
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.updateAnalysisDirectory(analysisDirectory, project)
        }

        then:
        project.dirAnalysis == analysisDirectory
    }

    void "test updateProcessingPriority valid name"() {
        given:
        Project project = Project.findByName("testProject")

        when:
        assert !project.processingPriority
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.updateProcessingPriority(ProcessingPriority.FAST_TRACK_PRIORITY, project)
        }

        then:
        project.processingPriority == ProcessingPriority.FAST_TRACK_PRIORITY
    }

    void "test configureNoAlignmentDeciderProject"() {
        setup:
        Project project = Project.findByName("testProjectAlignment")

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureNoAlignmentDeciderProject(project)
        }

        then:
        project.alignmentDeciderBeanName == "noAlignmentDecider"
        ReferenceGenomeProjectSeqType.findAllByProject(project).size == 0
    }

    void "test configureDefaultOtpAlignmentDecider valid input"() {
        setup:
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureDefaultOtpAlignmentDecider(project, referenceGenome.name)
        }

        then:
        project.alignmentDeciderBeanName == AlignmentDeciderBeanNames.OTP_ALIGNMENT.bean
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
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureDefaultOtpAlignmentDecider(project, referenceGenome.name)
            projectService.configureDefaultOtpAlignmentDecider(project, referenceGenome2.name)
        }

        then:
        project.alignmentDeciderBeanName == AlignmentDeciderBeanNames.OTP_ALIGNMENT.bean
        Set<ReferenceGenomeProjectSeqType> referenceGenomeProjectSeqTypes = ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project)
        referenceGenomeProjectSeqTypes.every { it.referenceGenome == referenceGenome2 }
        referenceGenomeProjectSeqTypes.size() == 2
    }

    void "test configureDefaultOtpAlignmentDecider invalid input"() {
        setup:
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")

        when:
        SpringSecurityUtils.doWithAuth("admin") {
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
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        configuration.project.alignmentDeciderBeanName == AlignmentDeciderBeanNames.PAN_CAN_ALIGNMENT.bean
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
                configVersion   : 'v1_1'
        ])

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
            projectService.configurePanCanAlignmentDeciderProject(configuration2)
        }

        then:
        configuration.project.alignmentDeciderBeanName == AlignmentDeciderBeanNames.PAN_CAN_ALIGNMENT.bean
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
        SpringSecurityUtils.doWithAuth("admin") {
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
        SpringSecurityUtils.doWithAuth("admin") {
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
        SpringSecurityUtils.doWithAuth("admin") {
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
        SpringSecurityUtils.doWithAuth("admin") {
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
        SpringSecurityUtils.doWithAuth("admin") {
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
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /The statSizeFile '.*${configuration.statSizeFileName}' could not be found in .*/
    }

    void "test configurePanCanAlignmentDeciderProject invalid alignment version input"() {
        setup:
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration(
                bwaMemVersion:  'invalidBwa_memVersion',
        )

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /Invalid bwa_mem version: 'invalidBwa_memVersion',.*/
    }

    void "test configurePanCanAlignmentDeciderProject alignment version path does not exist"() {
        setup:
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration()
        assert new File(ProcessingOption.findByName(RoddyConstants.OPTION_KEY_BWA_PATHS).value).delete()

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /.* does not exist.*/
    }


    void "test configurePanCanAlignmentDeciderProject invalid mergeTool input"() {
        setup:
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration(
                mergeTool: 'invalidMergeTool',
        )

        when:
        SpringSecurityUtils.doWithAuth("admin") {
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
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /Invalid sambamba version: 'invalidSambambaVersion',.*/
    }


    void "test configurePanCanAlignmentDeciderProject sambamba version path does not exist"() {
        setup:
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration(
                mergeTool: MergeConstants.MERGE_TOOL_SAMBAMBA,
        )
        assert new File(ProcessingOption.findByName(RoddyConstants.OPTION_KEY_SAMBAMBA_PATHS).value).delete()

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /.* does not exist.*/
    }


    @Unroll
    void "test configurePanCanAlignmentDeciderProject phix reference genome require sambamba for merge"() {
        setup:
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration(
                referenceGenome: DomainFactory.createReferenceGenome(name: "referencegenome_${ProjectService.PHIX_INFIX}").name,
                mergeTool: tool,
        )

        when:
        SpringSecurityUtils.doWithAuth("admin") {
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
        SpringSecurityUtils.doWithAuth("admin") {
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
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
            projectService.configureDefaultOtpAlignmentDecider(configuration.project, configuration.referenceGenome)
        }

        then:
        configuration.project.alignmentDeciderBeanName == AlignmentDeciderBeanNames.OTP_ALIGNMENT.bean
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
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(configuration)
            projectService.configureNoAlignmentDeciderProject(configuration.project)
        }

        then:
        configuration.project.alignmentDeciderBeanName == AlignmentDeciderBeanNames.NO_ALIGNMENT.bean
        ReferenceGenomeProjectSeqType.findAllByDeprecatedDateIsNull().size() == 0
    }

    void "test configureDefaultOtpAlignmentDecider to configurePanCanAlignmentDeciderProject"() {
        setup:
        PanCanAlignmentConfiguration configuration = createPanCanAlignmentConfiguration()

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureDefaultOtpAlignmentDecider(configuration.project, configuration.referenceGenome)
            projectService.configurePanCanAlignmentDeciderProject(configuration)
        }

        then:
        configuration.project.alignmentDeciderBeanName == AlignmentDeciderBeanNames.PAN_CAN_ALIGNMENT.bean
        RoddyWorkflowConfig.findAllByProjectAndPipelineInListAndPluginVersionAndObsoleteDateIsNull(
                configuration.project,
                Pipeline.findAllByTypeAndName(Pipeline.Type.ALIGNMENT, Pipeline.Name.PANCAN_ALIGNMENT),
                "${configuration.pluginName}:${configuration.pluginVersion}"
        ).size() == 1
        ReferenceGenomeProjectSeqType.findAllByDeprecatedDateIsNull().size() == 1
    }

    void "test configureDefaultOtpAlignmentDecider to configureNoAlignmentDeciderProject"() {
        setup:
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")


        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureDefaultOtpAlignmentDecider(project, referenceGenome.name)
            projectService.configureNoAlignmentDeciderProject(project)
        }

        then:
        project.alignmentDeciderBeanName == AlignmentDeciderBeanNames.NO_ALIGNMENT.bean
        ReferenceGenomeProjectSeqType.findAllByDeprecatedDateIsNull().size() == 0
    }

    void "test configureSnvPipelineProject valid input"() {
        setup:
        SnvPipelineConfiguration configuration = createSnvPipelineConfiguration()

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureSnvPipelineProject(configuration)
        }

        then:
        List<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineInListAndPluginVersionAndObsoleteDateIsNull(
                configuration.project,
                configuration.seqType,
                Pipeline.findAllByTypeAndName(Pipeline.Type.SNV, Pipeline.Name.RODDY_SNV),
                "${configuration.pluginName}:${configuration.pluginVersion}"
        )
        roddyWorkflowConfigs.size() == 1
        File roddyWorkflowConfig = new File(roddyWorkflowConfigs.configFilePath.first())
        roddyWorkflowConfig.exists()
        PosixFileAttributes attributes = Files.readAttributes(roddyWorkflowConfig.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        TestCase.assertContainSame(attributes.permissions(), [PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ])
    }

    void "test configureSnvPipelineProject valid input, twice"() {
        setup:
        SnvPipelineConfiguration configuration = createSnvPipelineConfiguration()
        SnvPipelineConfiguration configuration2 = createSnvPipelineConfiguration([
                configVersion   : 'v1_1'
        ])

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureSnvPipelineProject(configuration)
            projectService.configureSnvPipelineProject(configuration2)
        }

        then:
        Set<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.findAllByProjectAndPipelineInListAndPluginVersion(
                configuration.project,
                Pipeline.findAllByTypeAndName(Pipeline.Type.SNV, Pipeline.Name.RODDY_SNV),
                "${configuration2.pluginName}:${configuration2.pluginVersion}"
        )
        roddyWorkflowConfigs.size() == 2
        roddyWorkflowConfigs.findAll({ it.obsoleteDate == null }).size() == 1
    }

    void "test configureSnvPipelineProject valid input, multiple SeqTypes"() {
        setup:
        List<SnvPipelineConfiguration> configurations = DomainFactory.createSnvSeqTypes().collect {
            createSnvPipelineConfiguration(seqType: it)
        }
        int count = configurations.size()

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            configurations.each {
                projectService.configureSnvPipelineProject(it)
            }
        }

        then:
        Set<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.list()
        roddyWorkflowConfigs.size() == count
        roddyWorkflowConfigs.findAll({ it.obsoleteDate == null }).size() == count
    }

    void "test configureSnvPipelineProject invalid pluginName input"() {
        setup:
        SnvPipelineConfiguration configuration = createSnvPipelineConfiguration(
                pluginName: 'invalid/name'
        )

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureSnvPipelineProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /pluginName '.*' is an invalid path component\..*/
    }

    void "test configureSnvPipelineProject invalid pluginVersion input"() {
        setup:
        SnvPipelineConfiguration configuration = createSnvPipelineConfiguration(
                pluginVersion: 'invalid/version'
        )

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureSnvPipelineProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /pluginVersion '.*' is an invalid path component\..*/
    }

    @Unroll
    void "test configureSnvPipelineProject invalid baseProjectConfig input"() {
        setup:
        SnvPipelineConfiguration configuration = createSnvPipelineConfiguration(
                baseProjectConfig: baseProjectConfig
        )

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureSnvPipelineProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ message

        where:
        baseProjectConfig || message
        ''                || /baseProjectConfig '.*' is an invalid path component\..*/
        "invalid/path"    || /baseProjectConfig '.*' is an invalid path component\..*/
    }

    void "test configureSnvPipelineProject invalid configVersion input"() {
        setup:
        SnvPipelineConfiguration configuration = createSnvPipelineConfiguration(
                configVersion: 'invalid/Version',
        )

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureSnvPipelineProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /configVersion 'invalid\/Version' has not expected pattern.*/
    }

    void "test configureIndelPipelineProject valid input"() {
        setup:
        IndelPipelineConfiguration configuration = createIndelPipelineConfiguration()

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureIndelPipelineProject(configuration)
        }

        then:
        List<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineInListAndPluginVersionAndObsoleteDateIsNull(
                configuration.project,
                configuration.seqType,
                Pipeline.findAllByTypeAndName(Pipeline.Type.INDEL, Pipeline.Name.RODDY_INDEL),
                "${configuration.pluginName}:${configuration.pluginVersion}"
        )
        roddyWorkflowConfigs.size() == 1
        File roddyWorkflowConfig = new File(roddyWorkflowConfigs.configFilePath.first())
        roddyWorkflowConfig.exists()
        PosixFileAttributes attributes = Files.readAttributes(roddyWorkflowConfig.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        TestCase.assertContainSame(attributes.permissions(), [PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ])
    }

    void "test configureIndelPipelineProject valid input, twice"() {
        setup:
        IndelPipelineConfiguration configuration = createIndelPipelineConfiguration()
        IndelPipelineConfiguration configuration2 = createIndelPipelineConfiguration([
                configVersion   : 'v1_1'
        ])

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureIndelPipelineProject(configuration)
            projectService.configureIndelPipelineProject(configuration2)
        }

        then:
        Set<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.findAllByProjectAndPipelineInListAndPluginVersion(
                configuration.project,
                Pipeline.findAllByTypeAndName(Pipeline.Type.INDEL, Pipeline.Name.RODDY_INDEL),
                "${configuration2.pluginName}:${configuration2.pluginVersion}"
        )
        roddyWorkflowConfigs.size() == 2
        roddyWorkflowConfigs.findAll({ it.obsoleteDate == null }).size() == 1
    }

    void "test configureIndelPipelineProject valid input, multiple SeqTypes"() {
        setup:
        List<IndelPipelineConfiguration> configurations = DomainFactory.createIndelSeqTypes().collect {
            createIndelPipelineConfiguration(seqType: it)
        }
        int count = configurations.size()

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            configurations.each {
                projectService.configureIndelPipelineProject(it)
            }
        }

        then:
        Set<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.list()
        roddyWorkflowConfigs.size() == count
        roddyWorkflowConfigs.findAll({ it.obsoleteDate == null }).size() == count
    }

    void "test configureIndelPipelineProject invalid pluginName input"() {
        setup:
        IndelPipelineConfiguration configuration = createIndelPipelineConfiguration(
                pluginName: 'invalid/name'
        )

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureIndelPipelineProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /pluginName '.*' is an invalid path component\..*/
    }

    void "test configureIndelPipelineProject invalid pluginVersion input"() {
        setup:
        IndelPipelineConfiguration configuration = createIndelPipelineConfiguration(
                pluginVersion: 'invalid/version'
        )

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureIndelPipelineProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /pluginVersion '.*' is an invalid path component\..*/
    }

    @Unroll
    void "test configureIndelPipelineProject invalid baseProjectConfig input"() {
        setup:
        IndelPipelineConfiguration configuration = createIndelPipelineConfiguration(
                baseProjectConfig: baseProjectConfig
        )

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureIndelPipelineProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ message

        where:
        baseProjectConfig || message
        ''                || /baseProjectConfig '.*' is an invalid path component\..*/
        "invalid/path"    || /baseProjectConfig '.*' is an invalid path component\..*/
    }

    void "test configureIndelPipelineProject invalid configVersion input"() {
        setup:
        IndelPipelineConfiguration configuration = createIndelPipelineConfiguration(
                configVersion: 'invalid/Version',
        )

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureIndelPipelineProject(configuration)
        }

        then:
        AssertionError exception = thrown()
        exception.message ==~ /configVersion 'invalid\/Version' has not expected pattern.*/
    }

    private File makeStatFile(Project project, ReferenceGenome referenceGenome, String statFileName) {
        File statDirectory = referenceGenomeService.pathToChromosomeSizeFilesPerReference(project, referenceGenome, false)
        assert statDirectory.exists() || statDirectory.mkdirs()
        File statFile = new File(statDirectory, statFileName)
        statFile.text = "someText"
        return statFile
    }

    private PanCanAlignmentConfiguration createPanCanAlignmentConfiguration(Map properties = [:]) {
        PanCanAlignmentConfiguration configuration = new PanCanAlignmentConfiguration([
                project          : Project.findByName("testProjectAlignment"),
                seqType          : SeqType.wholeGenomePairedSeqType,
                referenceGenome  : "testReferenceGenome",
                statSizeFileName : 'testStatSizeFileName.tab',
                bwaMemVersion    : "bwa-mem",
                sambambaVersion  : "sambamba",
                mergeTool        : MergeConstants.MERGE_TOOL_PICARD,
                pluginName       : 'plugin',
                pluginVersion    : '1.2.3',
                baseProjectConfig: 'baseConfig',
                configVersion    : 'v1_0',
        ] + properties)
        Realm realm = ConfigService.getRealm(configuration.project, Realm.OperationType.DATA_MANAGEMENT)
        File projectDirectory = LsdfFilesService.getPath(
                realm.rootPath,
                configuration.project.dirName,
        )
        assert projectDirectory.exists() || projectDirectory.mkdirs()

        makeStatFile(configuration.project, ReferenceGenome.findByName(configuration.referenceGenome), configuration.statSizeFileName)
        return configuration
    }

    private SnvPipelineConfiguration createSnvPipelineConfiguration(Map properties = [:]) {
        SnvPipelineConfiguration configuration = new SnvPipelineConfiguration([
                project          : Project.findByName("testProjectAlignment"),
                seqType          : SeqType.exomePairedSeqType,
                pluginName       : 'SNVCallingWorkflow',
                pluginVersion    : '1.0.166-1',
                baseProjectConfig: 'otpSNVCallingWorkflowWES-1.0',
                configVersion    : 'v1_0',
        ] + properties)
        Realm realm = ConfigService.getRealm(configuration.project, Realm.OperationType.DATA_MANAGEMENT)
        File projectDirectory = LsdfFilesService.getPath(
                realm.rootPath,
                configuration.project.dirName,
        )
        assert projectDirectory.exists() || projectDirectory.mkdirs()
        return configuration
    }

    private IndelPipelineConfiguration createIndelPipelineConfiguration(Map properties = [:]) {
        IndelPipelineConfiguration configuration = new IndelPipelineConfiguration([
                project          : Project.findByName("testProjectAlignment"),
                seqType          : SeqType.exomePairedSeqType,
                pluginName       : 'IndelCallingWorkflow',
                pluginVersion    : '1.0.166-1',
                baseProjectConfig: 'otpIndelCallingWorkflowWES-1.0',
                configVersion    : 'v1_0',
        ] + properties)
        Realm realm = ConfigService.getRealm(configuration.project, Realm.OperationType.DATA_MANAGEMENT)
        File projectDirectory = LsdfFilesService.getPath(
                realm.rootPath,
                configuration.project.dirName,
        )
        assert projectDirectory.exists() || projectDirectory.mkdirs()
        return configuration
    }
}
