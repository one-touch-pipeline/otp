package de.dkfz.tbi.otp.job.jobs.aceseq

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
        AceseqInstance,
        AceseqQc,
        DataFile,
        FileType,
        IndelCallingInstance,
        Individual,
        LibraryPreparationKit,
        MergingWorkPackage,
        Pipeline,
        Project,
        ProjectSeqType,
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
class ExecuteRoddyAceseqJobSpec extends Specification {


    @Rule
    TemporaryFolder temporaryFolder

    AceseqInstance aceseqInstance

    void setup() {
        aceseqInstance = DomainFactory.createAceseqInstanceWithRoddyBamFiles()
        aceseqInstance.samplePair.mergingWorkPackage1.bamFileInProjectFolder = aceseqInstance.sampleType1BamFile
        assert aceseqInstance.samplePair.mergingWorkPackage1.save(flush: true)
        aceseqInstance.samplePair.mergingWorkPackage2.bamFileInProjectFolder = aceseqInstance.sampleType2BamFile
        assert aceseqInstance.samplePair.mergingWorkPackage2.save(flush: true)

        DomainFactory.createRealmDataManagement(temporaryFolder.newFolder(), [name: aceseqInstance.project.realmName])
        DomainFactory.createRealmDataProcessing(temporaryFolder.newFolder(), [name: aceseqInstance.project.realmName])
    }


    void "prepareAndReturnWorkflowSpecificCValues, when aceseqInstance is null, throw assert"() {
        when:
        new ExecuteRoddyAceseqJob().prepareAndReturnWorkflowSpecificCValues(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert aceseqInstance')
    }


    void "prepareAndReturnWorkflowSpecificCValues, when all fine, return correct value list"() {
        given:
        File fasta = CreateFileHelper.createFile(new File(temporaryFolder.newFolder(), "fasta.fa"))
        File chromosomeLength = CreateFileHelper.createFile(new File(temporaryFolder.newFolder(), "chrTotalLength.tsv"))
        File gcContent = CreateFileHelper.createFile(new File(temporaryFolder.newFolder(), "gcContentFile.tsv"))

        SophiaInstance sophiaInstance = DomainFactory.createSophiaInstance(aceseqInstance.samplePair)
        CreateRoddyFileHelper.createSophiaResultFiles(sophiaInstance)

        ExecuteRoddyAceseqJob job = new ExecuteRoddyAceseqJob([
                configService             : new ConfigService(),
                linkFileUtils             : Mock(LinkFileUtils) {
                    1 * createAndValidateLinks(_, _) >> { Map<File, File> sourceLinkMap, Realm realm ->
                        CreateFileHelper.createFile(aceseqInstance.instancePath.absoluteDataManagementPath, sophiaInstance.finalAceseqInputFile.name)
                    }
                },
                aceseqService     : Mock(AceseqService) {
                    1 * validateInputBamFiles(_) >> {}
                },
                referenceGenomeService: Mock(ReferenceGenomeService) {
                    1 * checkReferenceGenomeFilesAvailability(_) >> {}
                    1 * fastaFilePath(_) >> fasta
                    1 * chromosomeLengthFile(_) >> chromosomeLength
                    1 * gcContentFile(_) >> gcContent
                }
        ])

        ReferenceGenome referenceGenome = DomainFactory.createAceseqReferenceGenome()

        RoddyBamFile bamFileDisease = aceseqInstance.sampleType1BamFile as RoddyBamFile
        RoddyBamFile bamFileControl = aceseqInstance.sampleType2BamFile as RoddyBamFile

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileDisease)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFileControl)

        bamFileDisease.mergingWorkPackage.bamFileInProjectFolder = bamFileDisease
        bamFileDisease.mergingWorkPackage.referenceGenome = referenceGenome
        assert bamFileDisease.mergingWorkPackage.save(flush: true)

        bamFileControl.mergingWorkPackage.bamFileInProjectFolder = bamFileControl
        bamFileControl.mergingWorkPackage.referenceGenome = referenceGenome
        assert bamFileControl.mergingWorkPackage.save(flush: true)

        String bamFileDiseasePath = bamFileDisease.pathForFurtherProcessing.path
        String bamFileControlPath = bamFileControl.pathForFurtherProcessing.path


        List<String> expectedList = [
                "bamfile_list:${bamFileControlPath};${bamFileDiseasePath}",
                "sample_list:${bamFileControl.sampleType.dirName};${bamFileDisease.sampleType.dirName}",
                "possibleTumorSampleNamePrefixes:${bamFileDisease.sampleType.dirName}",
                "possibleControlSampleNamePrefixes:${bamFileControl.sampleType.dirName}",
                "REFERENCE_GENOME:${fasta}",
                "CHROMOSOME_LENGTH_FILE:${chromosomeLength}",
                "CHR_SUFFIX:${referenceGenome.chromosomeSuffix}",
                "CHR_PREFIX:${referenceGenome.chromosomePrefix}",
                "aceseqOutputDirectory:${aceseqInstance.workDirectory}",
                "svOutputDirectory:${aceseqInstance.workDirectory}",
                "MAPPABILITY_FILE:${referenceGenome.mappabilityFile}",
                "REPLICATION_TIME_FILE:${referenceGenome.replicationTimeFile}",
                "GC_CONTENT_FILE:${gcContent}",
                "GENETIC_MAP_FILE:${referenceGenome.geneticMapFile}",
                "KNOWN_HAPLOTYPES_FILE:${referenceGenome.knownHaplotypesFile}",
                "KNOWN_HAPLOTYPES_LEGEND_FILE:${referenceGenome.knownHaplotypesLegendFile}",
                "GENETIC_MAP_FILE_X:${referenceGenome.geneticMapFileX}",
                "KNOWN_HAPLOTYPES_FILE_X:${referenceGenome.knownHaplotypesFileX}",
                "KNOWN_HAPLOTYPES_LEGEND_FILE_X:${referenceGenome.knownHaplotypesLegendFileX}",
        ]

        when:
        List<String> returnedList = job.prepareAndReturnWorkflowSpecificCValues(aceseqInstance)

        then:
        expectedList == returnedList
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(sophiaInstance.getFinalAceseqInputFile())

    }


    @Unroll
    void "prepareAndReturnWorkflowSpecificParameter, return always empty String"() {
        expect:
        new ExecuteRoddyAceseqJob().prepareAndReturnWorkflowSpecificParameter(value).empty

        where:
        value << [
                null,
                new AceseqInstance(),
        ]
    }


    void "validate, when all fine, set processing state to finished"() {
        given:
        ExecuteRoddyAceseqJob job = new ExecuteRoddyAceseqJob([
                configService             : new ConfigService(),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {}
                },
                aceseqService         : Mock(AceseqService) {
                    1 * validateInputBamFiles(_) >> {}
                }
        ])

        DomainFactory.createAceseqQc([:], [:], [:], aceseqInstance)

        CreateRoddyFileHelper.createAceseqResultFiles(aceseqInstance)

        when:
        job.validate(aceseqInstance)

        then:
        aceseqInstance.processingState == AnalysisProcessingStates.FINISHED
    }


    void "validate, when aceseqInstance is null, throw assert"() {
        when:
        new ExecuteRoddyAceseqJob().validate(null)

        then:
        AssertionError e = thrown()
        e.message.contains('The input aceseqInstance must not be null. Expression')
    }


    void "validate, when correctPermissionsAndGroups fail, throw assert"() {
        given:
        String md5sum = HelperUtils.uniqueString
        ExecuteRoddyAceseqJob job = new ExecuteRoddyAceseqJob([
                configService             : new ConfigService(),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {
                        throw new AssertionError(md5sum)
                    }
                },
        ])

        DomainFactory.createAceseqQc([:], [:], [:], aceseqInstance)

        CreateRoddyFileHelper.createAceseqResultFiles(aceseqInstance)

        when:
        job.validate(aceseqInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(md5sum)
        aceseqInstance.processingState != AnalysisProcessingStates.FINISHED
    }


    @Unroll
    void "validate, when file not exist, throw assert"() {
        given:
        ExecuteRoddyAceseqJob job = new ExecuteRoddyAceseqJob([
                configService             : new ConfigService(),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * correctPermissionsAndGroups(_, _) >> {}
                },
        ])

        DomainFactory.createAceseqQc([:], [:], [:], aceseqInstance)

        CreateRoddyFileHelper.createAceseqResultFiles(aceseqInstance)

        File fileToDelete = fileClousure(aceseqInstance)
        assert fileToDelete.delete() || fileToDelete.deleteDir()

        when:
        job.validate(aceseqInstance)

        then:
        AssertionError e = thrown()
        e.message.contains(fileToDelete.path)
        aceseqInstance.processingState != AnalysisProcessingStates.FINISHED

        where:
        fileClousure << [
                { AceseqInstance it ->
                    it.workExecutionStoreDirectory
                },
                { AceseqInstance it ->
                    it.workExecutionDirectories.first()
                },
        ]
    }
}
