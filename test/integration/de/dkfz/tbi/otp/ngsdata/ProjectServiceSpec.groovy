package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
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

import java.nio.file.*
import java.nio.file.attribute.*


class ProjectServiceSpec extends IntegrationSpec implements UserAndRoles {

    ProjectService projectService

    ReferenceGenomeService referenceGenomeService

    GrailsApplication grailsApplication

    @Rule
    TemporaryFolder temporaryFolder

    final static String REALM_NAME = 'DKFZ_13.1'

    def setup() {
        createUserAndRoles()
        DomainFactory.createProject(name: 'testProject', nameInMetadataFiles: 'testProject2', dirName: 'testDir')
        DomainFactory.createProject(name: 'testProject3', nameInMetadataFiles: null)
        ProjectGroup projectGroup = new ProjectGroup(name: 'projectGroup')
        projectGroup.save(flush: true, failOnError: true)

        int counter = 0
        Realm realm = DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: REALM_NAME])
        DomainFactory.createRealmDataProcessing(temporaryFolder.newFolder(), [name: realm.name])
        DomainFactory.createProject(name: 'testProjectAlignment', realmName: realm.name, alignmentDeciderBeanName: 'test')
        DomainFactory.createReferenceGenome(name: 'testReferenceGenome')
        DomainFactory.createReferenceGenome(name: 'testReferenceGenome2')
        DomainFactory.createWholeGenomeSeqType()
        DomainFactory.createExomeSeqType()
        DomainFactory.createPanCanPipeline()
        projectService.executionService = Stub(ExecutionService) {
            executeCommand(_, _) >> { Realm realm2, String command ->
                File script = temporaryFolder.newFile('script'+ counter++ +'.sh')
                script.text = command
                return ProcessHelperService.executeCommandAndAssertExistCodeAndReturnProcessOutput("bash ${script.absolutePath}").stdout
            }
        }
    }

    void "test createProject valid input"() {
        given:
        String group = grailsApplication.config.otp.testing.group

        when:
        Project project
        SpringSecurityUtils.doWithAuth("admin") {
            project = projectService.createProject(name,  dirName,  REALM_NAME, 'noAlignmentDecider', group, projectGroup,  nameInMetadataFiles,  copyFiles)
        }

        then:
        project.name == name
        project.dirName == dirName
        project.projectGroup == ProjectGroup.findByName(projectGroup)
        project.nameInMetadataFiles == nameInMetadataFiles
        project.hasToBeCopied == copyFiles



        where:
        name        | dirName   | projectGroup      | nameInMetadataFiles   | copyFiles
        'project'   | 'dir'     | ''                | 'project'             | true
        'project'   | 'dir'     | ''                | null                  | true
        'project'   | 'dir'     | 'projectGroup'    | 'project'             | true
        'project'   | 'dir'     | ''                | 'project'             | false
    }

    void "test createProject if directory is created"() {
        given:
        String group = grailsApplication.config.otp.testing.group

        when:
        Project project
        SpringSecurityUtils.doWithAuth("admin") {
            project = projectService.createProject('project',  'dir',  REALM_NAME, 'noAlignmentDecider', group, '', null,  false)
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
        String group = grailsApplication.config.otp.testing.group

        when:
        Project project
        SpringSecurityUtils.doWithAuth("admin") {
            project = projectService.createProject(name, dirName, REALM_NAME, 'noAlignmentDecider', group, projectGroup, nameInMetadataFiles, copyFiles)
        }

        then:
            ValidationException ex = thrown()
            ex.message.contains(errorName) && ex.message.contains(errorLocaction)


        where:
        name            | dirName   | projectGroup  | nameInMetadataFiles   | copyFiles || errorName                            | errorLocaction
        'testProject'   | 'dir'     | ''            | 'project'             | true      || 'unique'                                                                                     | 'on field \'name\': rejected value [testProject]'
        'testProject2'  | 'dir'     | ''            | 'project'             | true      || 'this name is already used in another project as nameInMetadataFiles entry'                  | 'on field \'name\': rejected value [testProject2]'
        'project'       | 'dir'     | ''            | 'testProject'         | true      || 'this nameInMetadataFiles is already used in another project as name entry'                  | 'on field \'nameInMetadataFiles\': rejected value [testProject]'
        'project'       | 'dir'     | ''            | 'testProject2'        | true      || 'this nameInMetadataFiles is already used in another project as nameInMetadataFiles entry'   | 'on field \'nameInMetadataFiles\': rejected value [testProject2]'
        'project'       | 'dir'     | ''            | ''                    | true      || 'blank'                                                                                      | 'on field \'nameInMetadataFiles\': rejected value []'
        'project'       | 'testDir' | ''            | ''                    | true      || 'unique'                                                                                     | 'on field \'dirName\': rejected value [testDir]'
    }

    void "test createProject invalid unix group"() {
        when:
        Project project
        SpringSecurityUtils.doWithAuth("admin") {
            project = projectService.createProject('project',  'dir',  REALM_NAME, 'noAlignmentDecider', 'invalidValue', '', null,  false)
        }

        then:
        AssertionError ex = thrown()
        ex.message.contains('The exit value is not 0, but 1')
    }

    void "test createProject valid input, when directory with wrong unix group already exists"() {
        given:
        String group = grailsApplication.config.otp.testing.group
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
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.createProject('project',  'dir',  REALM_NAME, 'noAlignmentDecider', group, '', null,  false)
        }
        then:
        Files.readAttributes(projectDirectory.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS).group().toString() == group
    }

    void "test updateNameInMetadata valid input"() {
        when:
        Project project = Project.findByName("testProject")
        SpringSecurityUtils.doWithAuth("admin"){
            projectService.updateNameInMetadata(name, project)
        }

        then:
        project.nameInMetadataFiles == name

        where:
        name                | _
        'testProject'       | _
        'testProject2'      | _
        'newTestProject'    | _
        null                | _
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
        project.alignmentDeciderBeanName == "defaultOtpAlignmentDecider"
        Set <ReferenceGenomeProjectSeqType> referenceGenomeProjectSeqTypes = ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project)
        referenceGenomeProjectSeqTypes.every {it.referenceGenome == referenceGenome}
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
        project.alignmentDeciderBeanName == "defaultOtpAlignmentDecider"
        Set <ReferenceGenomeProjectSeqType> referenceGenomeProjectSeqTypes = ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project)
        referenceGenomeProjectSeqTypes.every {it.referenceGenome == referenceGenome2}
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

    @spock.lang.Ignore('Will be fixed with OTP-2142')
    void "test configurePanCanAlignmentDeciderProject valid input"() {
        setup:
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")
        String statFileName = "testStatSizeFileName.tab"
        File statFile = makeStatFile(project, referenceGenome, statFileName)
        String group = grailsApplication.config.otp.testing.group


        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(project, referenceGenome.name, "1.0.182", statFileName, group, ProjectService.PICARD, "v1_0")
        }

        then:
        project.alignmentDeciderBeanName == "panCanAlignmentDecider"
        RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineInListAndPluginVersionAndObsoleteDateIsNull(
                project,
                seqType,
                Pipeline.findAllByTypeAndName(Pipeline.Type.ALIGNMENT,Pipeline.Name.PANCAN_ALIGNMENT,),
                "QualityControlWorkflows:1.0.182"
        ).size == 1
        File roddyWorkflowConfig = getRoddyWorkflowConfig(project)
        roddyWorkflowConfig.exists()
        PosixFileAttributes attrs = Files.readAttributes(roddyWorkflowConfig.toPath(), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        attrs.group().toString() == group
        TestCase.assertContainSame(attrs.permissions(), [PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ])
    }

    @spock.lang.Ignore('Will be fixed with OTP-2142')
    void "test configurePanCanAlignmentDeciderProject valid input, twice"() {
        setup:
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")
        String statFileName = "testStatSizeFileName.tab"
        makeStatFile(project, referenceGenome, statFileName)

        ReferenceGenome referenceGenome2 = ReferenceGenome.findByName("testReferenceGenome2")
        String statFileName2 = "testStatSizeFileName2.tab"
        File statFile2 = makeStatFile(project, referenceGenome2, statFileName2)
        String group = grailsApplication.config.otp.testing.group


        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(project, referenceGenome.name, "1.0.182", statFileName, group, ProjectService.PICARD, "v1_0")
            projectService.configurePanCanAlignmentDeciderProject(project, referenceGenome2.name, "1.0.182", statFileName2, group, ProjectService.PICARD, "v1_1")
        }

        then:
        project.alignmentDeciderBeanName == "panCanAlignmentDecider"
        Set<RoddyWorkflowConfig> roddyWorkflowConfigs = RoddyWorkflowConfig.findAllByProjectAndPipelineInListAndPluginVersion(
                project,
                Pipeline.findAllByTypeAndName(Pipeline.Type.ALIGNMENT,Pipeline.Name.PANCAN_ALIGNMENT,),
                "QualityControlWorkflows:1.0.182"
        )
        roddyWorkflowConfigs.size() == 2
        roddyWorkflowConfigs.findAll({it.obsoleteDate == null}).size() == 1
        statFile2.exists()
    }

    void "test configurePanCanAlignmentDeciderProject invalid referenceGenome input"() {
        setup:
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")
        String statFileName = "testStatSizeFileName.tab"
        makeStatFile(project, referenceGenome, statFileName)
        String group = grailsApplication.config.otp.testing.group


        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(project, "invalidReferenceGenome", "1.0.182", statFileName, group, ProjectService.PICARD, "v1_0")
        }

        then:
        AssertionError exception = thrown()
        exception.message == "Collection contains 0 elements. Expected 1."
    }

    void "test configurePanCanAlignmentDeciderProject invalid pluginVersion input"() {
        setup:
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")
        String statFileName = "testStatSizeFileName.tab"
        makeStatFile(project, referenceGenome, statFileName)
        String group = grailsApplication.config.otp.testing.group


        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(project, referenceGenome.name, "1.0.182/", statFileName, group, ProjectService.PICARD, "v1_0")
        }

        then:
        AssertionError exception = thrown()
        exception.message == "pluginVersion is invalid path component. Expression: de.dkfz.tbi.otp.dataprocessing.OtpPath.isValidPathComponent(pluginVersion)"
    }


    void "test configurePanCanAlignmentDeciderProject invalid statSizeFileName input"() {
        setup:
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")
        File statDirectory = referenceGenomeService.pathToChromosomeSizeFilesPerReference(project, referenceGenome, false)
        assert statDirectory.mkdirs()
        String group = grailsApplication.config.otp.testing.group

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(project, referenceGenome.name, "1.0.182", "invalidStatSizeFileName.tab", group, ProjectService.PICARD, "v1_0")
        }

        then:
        AssertionError exception = thrown()
        exception.message == "The statSizeFile " + statDirectory + "/invalidStatSizeFileName.tab could not be found in " + statDirectory + ". Expression: statSizeFile.exists()"
    }

    void "test configurePanCanAlignmentDeciderProject invalid unixGroup input"() {
        setup:
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")
        String statFileName = "testStatSizeFileName.tab"
        makeStatFile(project, referenceGenome, statFileName)

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(project, referenceGenome.name, "1.0.182", statFileName, groupName, ProjectService.PICARD, "v1_0")
        }

        then:
        AssertionError exception = thrown()
        exception.message == message

        where:
        groupName       || message
        "invalidGroup"  || "The exit value is not 0, but 1. Expression: (process.exitValue() == 0)"
        "invalidGroup/" || "unixGroup contains invalid characters. Expression: de.dkfz.tbi.otp.dataprocessing.OtpPath.isValidPathComponent(unixGroup)"
    }

    void "test configurePanCanAlignmentDeciderProject invalid mergeTool input"() {
        setup:
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")
        String statFileName = "testStatSizeFileName.tab"
        makeStatFile(project, referenceGenome, statFileName)
        String group = grailsApplication.config.otp.testing.group

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(project, referenceGenome.name, "1.0.182", statFileName, group, "invalidMergeTool", "v1_0")
        }

        then:
        AssertionError exception = thrown()
        exception.message == "Merge Tool must be '" + ProjectService.PICARD + "' or '" + ProjectService.BIOBAMBAM + "'. Expression: (mergeTool in [PICARD, BIOBAMBAM]). Values: mergeTool = invalidMergeTool"
    }

    @spock.lang.Ignore('Will be fixed with OTP-2142')
    void "test configurePanCanAlignmentDeciderProject invalid configVersion input"() {
        setup:
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")
        String statFileName = "testStatSizeFileName.tab"
        makeStatFile(project, referenceGenome, statFileName)
        String group = grailsApplication.config.otp.testing.group

        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(project, referenceGenome.name, "1.0.182", statFileName, group, ProjectService.PICARD, "v1.0")
        }

        then:
        ValidationException exception = thrown()
        exception.message.contains("[Property [{0}] of class [{1}] with value [{2}] does not match the required pattern [{3}]]")
    }

    @spock.lang.Ignore('Will be fixed with OTP-2142')
    void "test configurePanCanAlignmentDeciderProject to configureDefaultOtpAlignmentDecider"() {
        setup:
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")
        String statFileName = "testStatSizeFileName.tab"
        makeStatFile(project, referenceGenome, statFileName)
        String group = grailsApplication.config.otp.testing.group


        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(project, referenceGenome.name, "1.0.182", statFileName, group, ProjectService.PICARD, "v1_0")
            projectService.configureDefaultOtpAlignmentDecider(project, referenceGenome.name)
        }

        then:
        project.alignmentDeciderBeanName == "defaultOtpAlignmentDecider"
        Set <ReferenceGenomeProjectSeqType> referenceGenomeProjectSeqTypes = ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project)
        referenceGenomeProjectSeqTypes.every {it.referenceGenome == referenceGenome}
        referenceGenomeProjectSeqTypes.size() == 2
    }

    @spock.lang.Ignore('Will be fixed with OTP-2142')
    void "test configurePanCanAlignmentDeciderProject to configureNoAlignmentDeciderProject"() {
        setup:
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")
        String statFileName = "testStatSizeFileName.tab"
        makeStatFile(project, referenceGenome, statFileName)
        String group = grailsApplication.config.otp.testing.group


        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configurePanCanAlignmentDeciderProject(project, referenceGenome.name, "1.0.182", statFileName, group, ProjectService.PICARD, "v1_0")
            projectService.configureNoAlignmentDeciderProject(project)
        }

        then:
        project.alignmentDeciderBeanName == "noAlignmentDecider"
        ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project).size == 0
    }

    @spock.lang.Ignore('Will be fixed with OTP-2142')
    void "test configureDefaultOtpAlignmentDecider to configurePanCanAlignmentDeciderProject"() {
        setup:
        Project project = Project.findByName("testProjectAlignment")
        ReferenceGenome referenceGenome = ReferenceGenome.findByName("testReferenceGenome")
        String statFileName = "testStatSizeFileName.tab"
        File statFile = makeStatFile(project, referenceGenome, statFileName)
        String group = grailsApplication.config.otp.testing.group


        when:
        SpringSecurityUtils.doWithAuth("admin") {
            projectService.configureDefaultOtpAlignmentDecider(project, referenceGenome.name)
            projectService.configurePanCanAlignmentDeciderProject(project, referenceGenome.name, "1.0.182", statFileName, group, ProjectService.PICARD, "v1_0")
        }

        then:
        project.alignmentDeciderBeanName == "panCanAlignmentDecider"
        RoddyWorkflowConfig.findAllByProjectAndPipelineInListAndPluginVersionAndObsoleteDateIsNull(
                project,
                Pipeline.findAllByTypeAndName(Pipeline.Type.ALIGNMENT,Pipeline.Name.PANCAN_ALIGNMENT,),
                "QualityControlWorkflows:1.0.182"
        ).size == 1
        statFile.exists()
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
        project.alignmentDeciderBeanName == "noAlignmentDecider"
        ReferenceGenomeProjectSeqType.findAllByProjectAndDeprecatedDateIsNull(project).size == 0
    }

    private File makeStatFile(Project project, ReferenceGenome referenceGenome, String statFileName) {
        File statDirectory = referenceGenomeService.pathToChromosomeSizeFilesPerReference(project, referenceGenome, false)
        assert statDirectory.mkdirs()
        File statFile = new File(statDirectory, statFileName)
        statFile.text = "someText"
        return statFile
    }

    private File getRoddyWorkflowConfig(Project project) {
        Realm realm = ConfigService.getRealm(project, Realm.OperationType.DATA_MANAGEMENT)
        File configDirectory = LsdfFilesService.getPath(
                LsdfFilesService.getPath(
                        realm.rootPath,
                        project.dirName,
                ).path,
                'configFiles',
                Pipeline.Name.PANCAN_ALIGNMENT.name(),
        )
        return new File(configDirectory, "${Pipeline.Name.PANCAN_ALIGNMENT.name()}_1.0.182_v1_0.xml")
    }
}
