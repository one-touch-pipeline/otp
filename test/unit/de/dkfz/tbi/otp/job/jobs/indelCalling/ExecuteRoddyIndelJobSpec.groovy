package de.dkfz.tbi.otp.job.jobs.indelCalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
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
        ReferenceGenome,
        ReferenceGenomeEntry,
        ReferenceGenomeProjectSeqType,
        Realm,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
])
class ExecuteRoddyIndelJobSpec extends Specification {


    @Rule
    TemporaryFolder temporaryFolder


    void "prepareAndReturnWorkflowSpecificCValues, when roddyIndelCallingInstance is null, throw assert"() {
        when:
        new ExecuteRoddyIndelJob().prepareAndReturnWorkflowSpecificCValues(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert indelCallingInstance')
    }


    void "prepareAndReturnWorkflowSpecificCValues, when all fine, return correct value list"() {
        given:
        File fasta = CreateFileHelper.createFile(new File(temporaryFolder.newFolder(), "fasta.fa"))

        ExecuteRoddyIndelJob job = new ExecuteRoddyIndelJob([
                indelCallingService     : Mock(IndelCallingService) {
                    1 * validateInputBamFiles(_) >> {}
                },
                referenceGenomeService: Mock(ReferenceGenomeService) {
                    1 * fastaFilePath(_) >> fasta
                    0 * _
                },
        ])

        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: indelCallingInstance.project.realmName])

        AbstractMergedBamFile bamFileDisease = indelCallingInstance.sampleType1BamFile
        AbstractMergedBamFile bamFileControl = indelCallingInstance.sampleType2BamFile

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileDisease)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileControl)

        bamFileDisease.mergingWorkPackage.bamFileInProjectFolder = bamFileDisease
        assert bamFileDisease.mergingWorkPackage.save(flush: true)

        bamFileControl.mergingWorkPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.mergingWorkPackage.save(flush: true)

        String bamFileDiseasePath = bamFileDisease.pathForFurtherProcessing.path
        String bamFileControlPath = bamFileControl.pathForFurtherProcessing.path

        String analysisMethodNameOnOutput = "indel_results/${indelCallingInstance.seqType.libraryLayoutDirName}/" +
                "${indelCallingInstance.sampleType1BamFile.sampleType.dirName}_${indelCallingInstance.sampleType2BamFile.sampleType.dirName}/" +
                "${indelCallingInstance.instanceName}"


        List<String> expectedList = [
                "bamfile_list:${bamFileControlPath};${bamFileDiseasePath}",
                "sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}",
                "possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}",
                "REFERENCE_GENOME:${fasta.path}",
                "CHR_SUFFIX:${indelCallingInstance.referenceGenome.chromosomeSuffix}",
                "CHR_PREFIX:${indelCallingInstance.referenceGenome.chromosomePrefix}",
                "analysisMethodNameOnOutput:${analysisMethodNameOnOutput}",
        ]

        when:
        List<String> returnedList = job.prepareAndReturnWorkflowSpecificCValues(indelCallingInstance)

        then:
        expectedList == returnedList
    }


    @Unroll
    void "prepareAndReturnWorkflowSpecificParameter, return always empty String"() {
        expect:
        new ExecuteRoddyIndelJob().prepareAndReturnWorkflowSpecificParameter(value).empty

        where:
        value << [
                null,
                new IndelCallingInstance(),
        ]
    }


    void "validate, when all fine, set processing state to finished"() {
        given:
        ExecuteRoddyIndelJob job = new ExecuteRoddyIndelJob([
                configService             : new ConfigService(),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {}
                },
                indelCallingService         : Mock(IndelCallingService) {
                    1 * validateInputBamFiles(_) >> {}
                }
        ])
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: indelCallingInstance.project.realmName])

        CreateRoddyFileHelper.createIndelResultFiles(indelCallingInstance)

        when:
        job.validate(indelCallingInstance)

        then:
        indelCallingInstance.processingState == AnalysisProcessingStates.FINISHED
    }


    void "validate, when indelCallingInstance is null, throw assert"() {
        when:
        new ExecuteRoddyIndelJob().validate(null)

        then:
        AssertionError e = thrown()
        e.message.contains('The input indelCallingInstance must not be null. Expression')
    }


    void "validate, when correctPermissionsAndGroups fail, throw assert"() {
        given:
        String md5sum = HelperUtils.uniqueString
        ExecuteRoddyIndelJob job = new ExecuteRoddyIndelJob([
                configService             : new ConfigService(),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {
                        throw new AssertionError(md5sum)
                    }
                },
        ])
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: indelCallingInstance.project.realmName])

        CreateRoddyFileHelper.createIndelResultFiles(indelCallingInstance)

        when:
        job.validate(indelCallingInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(md5sum)
        indelCallingInstance.processingState != AnalysisProcessingStates.FINISHED
    }


    @Unroll
    void "validate, when file not exist, throw assert"() {
        given:
        ExecuteRoddyIndelJob job = new ExecuteRoddyIndelJob([
                configService             : new ConfigService(),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {}
                },
        ])
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: indelCallingInstance.project.realmName])

        CreateRoddyFileHelper.createIndelResultFiles(indelCallingInstance)

        File fileToDelete = fileClousure(indelCallingInstance)
        assert fileToDelete.delete() || fileToDelete.deleteDir()

        when:
        job.validate(indelCallingInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(fileToDelete.path)
        indelCallingInstance.processingState != AnalysisProcessingStates.FINISHED

        where:
        fileClousure << [
                { IndelCallingInstance it ->
                    it.workExecutionStoreDirectory
                },
                { IndelCallingInstance it ->
                    it.workExecutionDirectories.first()
                },
                { IndelCallingInstance it ->
                    it.getCombinedPlotPath()
                },
                { IndelCallingInstance it ->
                    it.resultFilePathsToValidate.first()
                },
                { IndelCallingInstance it ->
                    it.resultFilePathsToValidate.last()
                },
        ]
    }
}
