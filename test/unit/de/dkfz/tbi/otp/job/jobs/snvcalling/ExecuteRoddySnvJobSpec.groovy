package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

import java.nio.file.*

@Mock([
        DataFile,
        FileType,
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
        RoddySnvCallingInstance,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
])
class ExecuteRoddySnvJobSpec extends Specification {


    @Rule
    TemporaryFolder temporaryFolder


    void "prepareAndReturnWorkflowSpecificCValues, when roddySnvCallingInstance is null, throw assert"() {
        when:
        new ExecuteRoddySnvJob().prepareAndReturnWorkflowSpecificCValues(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert roddySnvCallingInstance')
    }


    void "prepareAndReturnWorkflowSpecificCValues, when all fine, return correct value list"() {
        given:
        File fasta = CreateFileHelper.createFile(new File(temporaryFolder.newFolder(), "fasta.fa"))
        File chromosomeLength = temporaryFolder.newFile()

        ExecuteRoddySnvJob job = new ExecuteRoddySnvJob([
                snvCallingService     : Mock(SnvCallingService) {
                    1 * validateInputBamFiles(_) >> {}
                },
                referenceGenomeService: Mock(ReferenceGenomeService) {
                    1 * fastaFilePath(_, _) >> fasta
                    1 * chromosomeLengthFile(_) >> chromosomeLength
                    0 * _
                },
        ])

        RoddySnvCallingInstance roddySnvCallingInstance = DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: roddySnvCallingInstance.project.realmName])

        AbstractMergedBamFile bamFileDisease = roddySnvCallingInstance.sampleType1BamFile
        AbstractMergedBamFile bamFileControl = roddySnvCallingInstance.sampleType2BamFile

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileDisease)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileControl)

        bamFileDisease.mergingWorkPackage.bamFileInProjectFolder = bamFileDisease
        assert bamFileDisease.mergingWorkPackage.save(flush: true)

        bamFileControl.mergingWorkPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.mergingWorkPackage.save(flush: true)

        String bamFileDiseasePath = bamFileDisease.pathForFurtherProcessing.path
        String bamFileControlPath = bamFileControl.pathForFurtherProcessing.path

        String analysisMethodNameOnOutput = "snv_results/${roddySnvCallingInstance.seqType.libraryLayoutDirName}/" +
                "${roddySnvCallingInstance.sampleType1BamFile.sampleType.dirName}_${roddySnvCallingInstance.sampleType2BamFile.sampleType.dirName}/" +
                "${roddySnvCallingInstance.instanceName}"

        List<String> chromosomeNames = ["1", "2", "3", "4", "5", "M", "X", "Y"]
        DomainFactory.createReferenceGenomeEntries(roddySnvCallingInstance.referenceGenome, chromosomeNames)

        List<String> expectedList = [
                "bamfile_list:${bamFileControlPath};${bamFileDiseasePath}",
                "sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}",
                "possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}",
                "REFERENCE_GENOME:${fasta.path}",
                "CHROMOSOME_LENGTH_FILE:${chromosomeLength.path}",
                "CHR_SUFFIX:${roddySnvCallingInstance.referenceGenome.chromosomeSuffix}",
                "CHR_PREFIX:${roddySnvCallingInstance.referenceGenome.chromosomePrefix}",
                "${job.getChromosomeIndexParameter(roddySnvCallingInstance.referenceGenome)}",
                "analysisMethodNameOnOutput:${analysisMethodNameOnOutput}",
        ]

        when:
        List<String> returnedList = job.prepareAndReturnWorkflowSpecificCValues(roddySnvCallingInstance)

        then:
        expectedList == returnedList
    }


    @Unroll
    void "prepareAndReturnWorkflowSpecificParameter, return always empty String"() {
        expect:
        new ExecuteRoddySnvJob().prepareAndReturnWorkflowSpecificParameter(value).empty

        where:
        value << [
                null,
                new RoddySnvCallingInstance(),
        ]
    }


    void "validate, when all fine, set processing state to finished"() {
        given:
        ExecuteRoddySnvJob job = new ExecuteRoddySnvJob([
                configService             : new ConfigService(),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {}
                },
                snvCallingService         : Mock(SnvCallingService) {
                    1 * validateInputBamFiles(_) >> {}
                }
        ])
        RoddySnvCallingInstance roddySnvCallingInstance = DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: roddySnvCallingInstance.project.realmName])

        CreateRoddyFileHelper.createRoddySnvResultFiles(roddySnvCallingInstance)

        when:
        job.validate(roddySnvCallingInstance)

        then:
        roddySnvCallingInstance.processingState == AnalysisProcessingStates.FINISHED
    }


    void "validate, when roddySnvCallingInstance is null, throw assert"() {
        when:
        new ExecuteRoddySnvJob().validate(null)

        then:
        AssertionError e = thrown()
        e.message.contains('The input roddyResult must not be null. Expression')
    }


    void "validate, when correctPermissionsAndGroups fail, throw assert"() {
        given:
        String md5sum = HelperUtils.uniqueString
        ExecuteRoddySnvJob job = new ExecuteRoddySnvJob([
                configService             : new ConfigService(),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {
                        throw new AssertionError(md5sum)
                    }
                },
        ])
        RoddySnvCallingInstance roddySnvCallingInstance = DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: roddySnvCallingInstance.project.realmName])

        CreateRoddyFileHelper.createRoddySnvResultFiles(roddySnvCallingInstance)

        when:
        job.validate(roddySnvCallingInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(md5sum)
        roddySnvCallingInstance.processingState != AnalysisProcessingStates.FINISHED
    }


    @Unroll
    void "validate, when file not exist, throw assert"() {
        given:
        ExecuteRoddySnvJob job = new ExecuteRoddySnvJob([
                configService             : new ConfigService(),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {}
                },
        ])
        RoddySnvCallingInstance roddySnvCallingInstance = DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()
        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: roddySnvCallingInstance.project.realmName])

        CreateRoddyFileHelper.createRoddySnvResultFiles(roddySnvCallingInstance)

        File fileToDelete = fileClousure(roddySnvCallingInstance)
        assert fileToDelete.delete() || fileToDelete.deleteDir()

        when:
        job.validate(roddySnvCallingInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(fileToDelete.path)
        roddySnvCallingInstance.processingState != AnalysisProcessingStates.FINISHED

        where:
        fileClousure << [
                { RoddySnvCallingInstance it ->
                    it.workExecutionStoreDirectory
                },
                { RoddySnvCallingInstance it ->
                    it.workExecutionDirectories.first()
                },
                { RoddySnvCallingInstance it ->
                    it.getAllSNVdiagnosticsPlots().absoluteDataManagementPath
                },
                { RoddySnvCallingInstance it ->
                    new OtpPath(it.snvInstancePath, SnvCallingStep.CALLING.getResultFileName(it.individual)).absoluteDataManagementPath
                },
                { RoddySnvCallingInstance it ->
                    new OtpPath(it.snvInstancePath, SnvCallingStep.SNV_DEEPANNOTATION.getResultFileName(it.individual)).absoluteDataManagementPath
                },
        ]
    }
}
