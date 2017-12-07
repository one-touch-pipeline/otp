package de.dkfz.tbi.otp.job.jobs.sophia

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

@Mock([
        DataFile,
        FileType,
        IndelCallingInstance,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        Pipeline,
        Project,
        ProjectCategory,
        Sample,
        SamplePair,
        SampleType,
        SampleTypePerProject,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SequencingKitLabel,
        SeqTrack,
        SeqType,
        SoftwareTool,
        SophiaInstance,
        Realm,
        ReferenceGenome,
        ReferenceGenomeEntry,
        ReferenceGenomeProjectSeqType,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
])
class ExecuteRoddySophiaJobSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    void "prepareAndReturnWorkflowSpecificCValues, when roddySophiaInstance is null, throw assert"() {
        when:
        new ExecuteRoddySophiaJob().prepareAndReturnWorkflowSpecificCValues(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert sophiaInstance')
    }


    void "prepareAndReturnWorkflowSpecificCValues, when all fine, return correct value list"() {
        given:
        ExecuteRoddySophiaJob job = new ExecuteRoddySophiaJob([
                sophiaService     : Mock(SophiaService) {
                    1 * validateInputBamFiles(_) >> {}
                },
        ])

        SophiaInstance sophiaInstance = DomainFactory.createSophiaInstanceWithRoddyBamFiles()
        TestConfigService configService = new TestConfigService(['otp.root.path': temporaryFolder.newFolder().path])
        DomainFactory.createRealmDataManagement(name: sophiaInstance.project.realmName)

        RoddyBamFile bamFileDisease = sophiaInstance.sampleType1BamFile as RoddyBamFile
        RoddyBamFile bamFileControl = sophiaInstance.sampleType2BamFile as RoddyBamFile

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileDisease)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileControl)
        CreateRoddyFileHelper.createInsertSizeFiles(sophiaInstance)

        bamFileDisease.mergingWorkPackage.bamFileInProjectFolder = bamFileDisease
        assert bamFileDisease.mergingWorkPackage.save(flush: true)

        bamFileControl.mergingWorkPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.mergingWorkPackage.save(flush: true)

        String bamFileDiseasePath = bamFileDisease.pathForFurtherProcessing.path
        String bamFileControlPath = bamFileControl.pathForFurtherProcessing.path


        List<String> expectedList = [
                "bamfile_list:${bamFileControlPath};${bamFileDiseasePath}",
                "sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}",
                "insertsizesfile_list:${bamFileDisease.finalInsertSizeFile};${bamFileControl.finalInsertSizeFile}",
                "possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}",
                "possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}",
                "controlDefaultReadLength:${bamFileDisease.getMaximalReadLength()}",
                "tumorDefaultReadLength:${bamFileControl.getMaximalReadLength()}",
        ]

        when:
        List<String> returnedList = job.prepareAndReturnWorkflowSpecificCValues(sophiaInstance)

        then:
        expectedList == returnedList

        cleanup:
        configService.clean()
    }


    @Unroll
    void "prepareAndReturnWorkflowSpecificParameter, return always empty String"() {
        expect:
        new ExecuteRoddySophiaJob().prepareAndReturnWorkflowSpecificParameter(value).empty

        where:
        value << [
                null,
                new SophiaInstance(),
        ]
    }


    void "validate, when all fine, set processing state to finished"() {
        given:
        TestConfigService configService = new TestConfigService(['otp.root.path': temporaryFolder.newFolder().path])
        ExecuteRoddySophiaJob job = new ExecuteRoddySophiaJob([
                configService             : configService,
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {}
                },
                sophiaService         : Mock(SophiaService) {
                    1 * validateInputBamFiles(_) >> {}
                }
        ])
        SophiaInstance sophiaInstance = DomainFactory.createSophiaInstanceWithRoddyBamFiles()
        DomainFactory.createRealmDataManagement([name: sophiaInstance.project.realmName])

        CreateRoddyFileHelper.createSophiaResultFiles(sophiaInstance)

        when:
        job.validate(sophiaInstance)

        then:
        sophiaInstance.processingState == AnalysisProcessingStates.FINISHED

        cleanup:
        configService.clean()
    }


    void "validate, when sophiaInstance is null, throw assert"() {
        when:
        new ExecuteRoddySophiaJob().validate(null)

        then:
        AssertionError e = thrown()
        e.message.contains('The input sophiaInstance must not be null. Expression')
    }


    void "validate, when correctPermissionsAndGroups fail, throw assert"() {
        given:
        String md5sum = HelperUtils.uniqueString
        TestConfigService configService = new TestConfigService(['otp.root.path': temporaryFolder.newFolder().path])
        ExecuteRoddySophiaJob job = new ExecuteRoddySophiaJob([
                configService             : configService,
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {
                        throw new AssertionError(md5sum)
                    }
                },
        ])
        SophiaInstance sophiaInstance = DomainFactory.createSophiaInstanceWithRoddyBamFiles()
        DomainFactory.createRealmDataManagement([name: sophiaInstance.project.realmName])

        CreateRoddyFileHelper.createSophiaResultFiles(sophiaInstance)

        when:
        job.validate(sophiaInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(md5sum)
        sophiaInstance.processingState != AnalysisProcessingStates.FINISHED

        cleanup:
        configService.clean()
    }


    @Unroll
    void "validate, when file not exist, throw assert"() {
        given:
        TestConfigService configService = new TestConfigService(['otp.root.path': temporaryFolder.newFolder().path])
        ExecuteRoddySophiaJob job = new ExecuteRoddySophiaJob([
                configService             : configService,
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {}
                },
        ])

        SophiaInstance sophiaInstance = DomainFactory.createSophiaInstanceWithRoddyBamFiles()
        DomainFactory.createRealmDataManagement([name: sophiaInstance.project.realmName])

        CreateRoddyFileHelper.createSophiaResultFiles(sophiaInstance)

        File fileToDelete = fileClousure(sophiaInstance)
        assert fileToDelete.delete() || fileToDelete.deleteDir()

        when:
        job.validate(sophiaInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(fileToDelete.path)
        sophiaInstance.processingState != AnalysisProcessingStates.FINISHED

        cleanup:
        configService.clean()

        where:
        fileClousure << [
                { SophiaInstance it ->
                    it.workExecutionStoreDirectory
                },
                { SophiaInstance it ->
                    it.workExecutionDirectories.first()
                },
                { SophiaInstance it ->
                    it.finalAceseqInputFile
                },
        ]
    }
}
