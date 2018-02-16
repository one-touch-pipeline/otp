package de.dkfz.tbi.otp.job.jobs.indelCalling

import de.dkfz.tbi.otp.TestConfigService
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
        BedFile,
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
        ProcessingOption,
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


    void "prepareAndReturnWorkflowSpecificCValues, when all fine and WGS, return correct value list"() {
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

        TestConfigService configService = new TestConfigService(['otp.root.path': temporaryFolder.newFolder().path])

        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()

        AbstractMergedBamFile bamFileDisease = indelCallingInstance.sampleType1BamFile
        AbstractMergedBamFile bamFileControl = indelCallingInstance.sampleType2BamFile

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileDisease)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileControl)

        bamFileDisease.mergingWorkPackage.bamFileInProjectFolder = bamFileDisease
        assert bamFileDisease.mergingWorkPackage.save(flush: true)

        bamFileControl.mergingWorkPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.mergingWorkPackage.save(flush: true)

        String analysisMethodNameOnOutput = "indel_results/${indelCallingInstance.seqType.libraryLayoutDirName}/" +
                "${indelCallingInstance.sampleType1BamFile.sampleType.dirName}_${indelCallingInstance.sampleType2BamFile.sampleType.dirName}/" +
                "${indelCallingInstance.instanceName}"


        List<String> expectedList = [
                "bamfile_list:${bamFileControl.pathForFurtherProcessing.path};${bamFileDisease.pathForFurtherProcessing.path}",
                "sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}",
                "possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}",
                "REFERENCE_GENOME:${fasta.path}",
                "CHR_SUFFIX:${indelCallingInstance.referenceGenome.chromosomeSuffix}",
                "CHR_PREFIX:${indelCallingInstance.referenceGenome.chromosomePrefix}",
                "analysisMethodNameOnOutput:${analysisMethodNameOnOutput}",
                "VCF_NORMAL_HEADER_COL:${bamFileControl.sampleType.dirName}",
                "VCF_TUMOR_HEADER_COL:${bamFileDisease.sampleType.dirName}",
                "SEQUENCE_TYPE:${bamFileDisease.seqType.roddyName}",
        ]

        when:
        List<String> returnedList = job.prepareAndReturnWorkflowSpecificCValues(indelCallingInstance)

        then:
        expectedList == returnedList

        cleanup:
        configService.clean()
    }


    void "prepareAndReturnWorkflowSpecificCValues, when all fine and WES, return correct value list"() {
        given:
        File fasta = CreateFileHelper.createFile(new File(temporaryFolder.newFolder(), "fasta.fa"))
        File bedFile = CreateFileHelper.createFile(new File(temporaryFolder.newFolder(), "bed.txt"))

        ExecuteRoddyIndelJob job = new ExecuteRoddyIndelJob([
                indelCallingService     : Mock(IndelCallingService) {
                    1 * validateInputBamFiles(_) >> {}
                },
                referenceGenomeService: Mock(ReferenceGenomeService) {
                    1 * fastaFilePath(_) >> fasta
                    0 * _
                },
                bedFileService : Mock(BedFileService) {
                    1* filePath(_) >> bedFile
                },
        ])
        new TestConfigService(['otp.root.path': temporaryFolder.newFolder().path])
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()
        SeqType seqType = DomainFactory.createExomeSeqType()

        LibraryPreparationKit kit = DomainFactory.createLibraryPreparationKit()
        indelCallingInstance.containedSeqTracks*.libraryPreparationKit = kit
        assert indelCallingInstance.containedSeqTracks*.save(flush: true)
        indelCallingInstance.samplePair.mergingWorkPackage1.libraryPreparationKit = kit
        assert indelCallingInstance.samplePair.mergingWorkPackage1.save(flush: true)
        indelCallingInstance.samplePair.mergingWorkPackage2.libraryPreparationKit = kit
        assert indelCallingInstance.samplePair.mergingWorkPackage2.save(flush: true)

        DomainFactory.createBedFile(
                libraryPreparationKit: kit,
                referenceGenome: indelCallingInstance.sampleType1BamFile.referenceGenome
        )

        indelCallingInstance.samplePair.mergingWorkPackage1.seqType = seqType
        assert indelCallingInstance.samplePair.mergingWorkPackage1.save(flush: true)
        indelCallingInstance.samplePair.mergingWorkPackage2.seqType = seqType
        assert indelCallingInstance.samplePair.mergingWorkPackage2.save(flush: true)
        indelCallingInstance.containedSeqTracks*.seqType = seqType
        assert indelCallingInstance.containedSeqTracks*.save(flush: true)

        AbstractMergedBamFile bamFileDisease = indelCallingInstance.sampleType1BamFile
        AbstractMergedBamFile bamFileControl = indelCallingInstance.sampleType2BamFile

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileDisease)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileControl)

        bamFileDisease.mergingWorkPackage.bamFileInProjectFolder = bamFileDisease
        assert bamFileDisease.mergingWorkPackage.save(flush: true)

        bamFileControl.mergingWorkPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.mergingWorkPackage.save(flush: true)

        String analysisMethodNameOnOutput = "indel_results/${indelCallingInstance.seqType.libraryLayoutDirName}/" +
                "${indelCallingInstance.sampleType1BamFile.sampleType.dirName}_${indelCallingInstance.sampleType2BamFile.sampleType.dirName}/" +
                "${indelCallingInstance.instanceName}"


        List<String> expectedList = [
                "bamfile_list:${bamFileControl.pathForFurtherProcessing.path};${bamFileDisease.pathForFurtherProcessing.path}",
                "sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}",
                "possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}",
                "REFERENCE_GENOME:${fasta.path}",
                "CHR_SUFFIX:${indelCallingInstance.referenceGenome.chromosomeSuffix}",
                "CHR_PREFIX:${indelCallingInstance.referenceGenome.chromosomePrefix}",
                "analysisMethodNameOnOutput:${analysisMethodNameOnOutput}",
                "VCF_NORMAL_HEADER_COL:${bamFileControl.sampleType.dirName}",
                "VCF_TUMOR_HEADER_COL:${bamFileDisease.sampleType.dirName}",
                "SEQUENCE_TYPE:${bamFileDisease.seqType.roddyName}",
                "EXOME_CAPTURE_KIT_BEDFILE:${bedFile}"
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
                configService             : new TestConfigService(['otp.root.path': temporaryFolder.newFolder().path]),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {}
                },
                indelCallingService         : Mock(IndelCallingService) {
                    1 * validateInputBamFiles(_) >> {}
                }
        ])
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()

        CreateRoddyFileHelper.createIndelResultFiles(indelCallingInstance)

        expect:
        job.validate(indelCallingInstance)
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
                configService             : new TestConfigService(['otp.root.path': temporaryFolder.newFolder().path]),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {
                        throw new AssertionError(md5sum)
                    }
                },
        ])
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()

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
                configService             : new TestConfigService(['otp.root.path': temporaryFolder.newFolder().path]),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {}
                },
        ])
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()

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
                { IndelCallingInstance it ->
                    it.getIndelQcJsonFile()
                },
                { IndelCallingInstance it ->
                    it.getSampleSwapJsonFile()
                },
        ]
    }
}
