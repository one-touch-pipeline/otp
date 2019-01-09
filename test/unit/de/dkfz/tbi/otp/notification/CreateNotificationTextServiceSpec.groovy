package de.dkfz.tbi.otp.notification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.codehaus.groovy.grails.web.mapping.*
import spock.lang.*
import org.codehaus.groovy.grails.context.support.*

import static de.dkfz.tbi.otp.tracking.OtrsTicket.ProcessingStep.*

@Mock([
        AbstractMergedBamFile,
        AntibodyTarget,
        ChipSeqSeqTrack,
        DataFile,
        ExomeSeqTrack,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        MetaDataEntry,
        MetaDataKey,
        OtrsTicket,
        Pipeline,
        ProcessingOption,
        Project,
        ProjectCategory,
        Realm,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
        Sample,
        SamplePair,
        SampleType,
        SampleTypePerProject,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SoftwareTool,
        SoftwareToolIdentifier,
])
//TODO refactor classes SNV INDEL ACESEQSpec abstract from BamFilePairAnalyses abstract
@TestMixin(GrailsUnitTestMixin)
class CreateNotificationTextServiceSpec extends Specification {

    TestConfigService configService

    static List listPairAnalyses = [
            [
                    type                  : "indel",
                    processingStep        : INDEL,
                    customProcessingStatus: 'indelProcessingStatus',
                    notification          : "indelNotification",
            ], [
                    type                  : "snv",
                    processingStep        : SNV,
                    customProcessingStatus: "snvProcessingStatus",
                    notification          : "snvNotification",
            ], [
                    type                  : "sophia",
                    processingStep        : SOPHIA,
                    customProcessingStatus: "sophiaProcessingStatus",
                    notification          : "sophiaNotification",
            ], [
                    type                  : "aceseq",
                    processingStep        : ACESEQ,
                    customProcessingStatus: "aceseqProcessingStatus",
                    notification          : "aceseqNotification",
            ], [
                    type                  : "runYapsa",
                    processingStep        : RUN_YAPSA,
                    customProcessingStatus: "runYapsaProcessingStatus",
                    notification          : "runYapsaNotification",
            ],
    ]

    static List processingStatusMultipleProjects = [
            [
                    multipleProjects: false,
                    processingStatus: ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
            ],
            [
                    multipleProjects: true,
                    processingStatus: ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
            ],
            [
                    multipleProjects: false,
                    processingStatus: ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
            ],
            [
                    multipleProjects: false,
                    processingStatus: ProcessingStatus.WorkflowProcessingStatus.PARTLY_DONE_MIGHT_DO_MORE,
            ],
    ]

    static List pairAnalysisContentsPermutation

    /**
     * Permutation of the listPairAnalyses and the processingStatusMultipleProjects
     * to have any combination in a List of Maps for later Testing
     */
    void setupSpec() {
        List result = []
        listPairAnalyses.each { var1 ->
            processingStatusMultipleProjects.each { var2 ->
                Map tmp = var1 + var2
                result.add(tmp)
            }
        }
        pairAnalysisContentsPermutation = result.asImmutable()
    }


    void "createMessage, when template is null, throw assert"() {
        when:
        new CreateNotificationTextService().createMessage(null, [:])

        then:
        AssertionError e = thrown()
        e.message.contains('assert templateName')
    }

    void "createMessage, when template exist, return notification text"() {
        given:
        String templateName = "notification.template.base"
        CreateNotificationTextService createNotificationTextService = new CreateNotificationTextService(
                messageSource: Mock(PluginAwareResourceBundleMessageSource) {
                    _ * getMessageInternal("notification.template.base", [], _) >> 'Some text ${placeholder} some text'
                }
        )

        when:
        String message = createNotificationTextService.createMessage(templateName, [placeholder: 'information'])

        then:
        'Some text information some text' == message
    }

    void "getSampleName, when seqTracks is null, throw assert"() {
        when:
        new CreateNotificationTextService().getSampleName(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert seqTrack')
    }


    void "getSampleName, when seqTracks exist, return name to display"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        when:
        String name = new CreateNotificationTextService().getSampleName(seqTrack)

        then:
        name.contains(seqTrack.individual.displayName)
    }


    void "getSampleIdentifiers, when no seqTracks exist, throw assert"() {
        when:
        new CreateNotificationTextService().getSampleIdentifiers(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert seqTracks')
    }


    void "getSampleIdentifiers, when no metadataEntries exist, return only brackets"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles()

        when:
        String value = new CreateNotificationTextService().getSampleIdentifiers([seqTrack])

        then:
        ' ()' == value
    }


    void "getSampleIdentifiers, when metadataEntries exist and sample identifier should be hidden, return empty string"() {
        given:
        String identifier = 'SomeName'
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles()
        seqTrack.project.name = ProjectOverviewService.PROJECT_TO_HIDE_SAMPLE_IDENTIFIER.first()
        seqTrack.dataFiles.each {
            DomainFactory.createMetaDataKeyAndEntry(it, MetaDataColumn.SAMPLE_ID, identifier)
        }

        when:
        String value = new CreateNotificationTextService().getSampleIdentifiers([seqTrack])

        then:
        '' == value
    }


    void "getSampleIdentifiers, when metadataEntries exist and sample identifier should be shown, return sample identifier in brackets"() {
        given:
        String identifier = 'SomeName'
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles()
        seqTrack.dataFiles.each {
            DomainFactory.createMetaDataKeyAndEntry(it, MetaDataColumn.SAMPLE_ID, identifier)
        }
        String expected = " (${identifier})"

        when:
        String value = new CreateNotificationTextService().getSampleIdentifiers([seqTrack])

        then:
        expected == value
    }


    void "getSampleIdentifiers, when multiple seqtracks exist with multiple sample identifiers, return unique and sorted sample identifiers in brackets"() {
        given:
        Project project = DomainFactory.createProject()
        List<String> identifiers = [
                'sample6',
                'sample3',
                'sample8',
                'sample3',
                'sample6',
        ]
        List<SeqTrack> seqTracks = identifiers.collect { String identifier ->
            SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles()
            seqTrack.individual.project = project
            seqTrack.dataFiles.each {
                DomainFactory.createMetaDataKeyAndEntry(it, MetaDataColumn.SAMPLE_ID, identifier)
            }
            return seqTrack
        }
        String expected = " (sample3, sample6, sample8)"

        when:
        String value = new CreateNotificationTextService().getSampleIdentifiers(seqTracks)

        then:
        expected == value
    }


    void "getSampleIdentifiers, when samples are mixed, throw assert"() {
        given:
        SeqTrack seqTrack1 = DomainFactory.createSeqTrackWithTwoDataFiles()
        and:
        SeqTrack seqTrack2 = DomainFactory.createSeqTrackWithTwoDataFiles()
        seqTrack2.project.name = ProjectOverviewService.PROJECT_TO_HIDE_SAMPLE_IDENTIFIER.first()

        when:
        new CreateNotificationTextService().getSampleIdentifiers([seqTrack1, seqTrack2])

        then:
        AssertionError e = thrown()
        e.message.contains('seqtracks must be of the same project')
    }


    void "getSeqTypeDirectories, when seqTracks is null, throw assert"() {
        when:
        new CreateNotificationTextService().getSeqTypeDirectories(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert seqTracks')
    }


    void "getSeqTypeDirectories, return correct paths"() {
        given:
        SeqTrack seqTrack1 = DomainFactory.createSeqTrackWithTwoDataFiles()
        SeqTrack seqTrack2 = DomainFactory.createSeqTrackWithTwoDataFiles()

        configService = new TestConfigService()

        when:
        String fileNameString = new CreateNotificationTextService(
                lsdfFilesService: new LsdfFilesService(),
        ).getSeqTypeDirectories([seqTrack1, seqTrack2])
        String expected = [
                new File("${configService.getRootPath()}/${seqTrack1.project.dirName}/sequencing/${seqTrack1.seqType.dirName}"),
                new File("${configService.getRootPath()}/${seqTrack2.project.dirName}/sequencing/${seqTrack2.seqType.dirName}"),
        ].sort().join('\n')

        then:
        expected == fileNameString
    }


    void "getMergingDirectories, when bamFiles is null, throw assert"() {
        when:
        new CreateNotificationTextService().getMergingDirectories(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert bamFiles')
    }


    void "getMergingDirectories, return correct paths"() {
        given:
        RoddyBamFile roddyBamFile1 = DomainFactory.createRoddyBamFile()
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile()
        // roddyBamFile3 has the same path with placeholders as roddyBamFile1
        RoddyBamFile roddyBamFile3 = DomainFactory.createRoddyBamFile([
                workPackage: DomainFactory.createMergingWorkPackage([
                        sample  : DomainFactory.createSample([
                                individual: DomainFactory.createIndividual([
                                        project: roddyBamFile1.project,
                                ]),
                        ]),
                        seqType : roddyBamFile1.seqType,
                        pipeline: roddyBamFile1.pipeline,
                ])
        ])
        configService = new TestConfigService()

        when:
        String fileNameString = new CreateNotificationTextService().getMergingDirectories([roddyBamFile1, roddyBamFile2, roddyBamFile3])
        String expected = [
                new File("${configService.getRootPath()}/${roddyBamFile1.project.dirName}/sequencing/${roddyBamFile1.seqType.dirName}/view-by-pid/\${PID}/\${SAMPLE_TYPE}/${roddyBamFile1.seqType.libraryLayoutDirName}/merged-alignment"),
                new File("${configService.getRootPath()}/${roddyBamFile2.project.dirName}/sequencing/${roddyBamFile2.seqType.dirName}/view-by-pid/\${PID}/\${SAMPLE_TYPE}/${roddyBamFile2.seqType.libraryLayoutDirName}/merged-alignment"),
        ].sort().join('\n')

        then:
        expected == fileNameString
    }


    void "getMergingDirectories, when seqtype is chipseq, then the path should contain antibody"() {
        given:
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                workPackage: DomainFactory.createMergingWorkPackage([
                        seqType : DomainFactory.createChipSeqType(),
                        pipeline: DomainFactory.createPanCanPipeline(),
                ])
        ])
        configService = new TestConfigService()

        when:
        String fileNameString = new CreateNotificationTextService().getMergingDirectories([roddyBamFile])
        String expected = new File("${configService.getRootPath()}/${roddyBamFile.project.dirName}/sequencing/${roddyBamFile.seqType.dirName}/view-by-pid/\${PID}/\${SAMPLE_TYPE}-\${ANTI_BODY_TARGET}/${roddyBamFile.seqType.libraryLayoutDirName}/merged-alignment").path

        then:
        expected == fileNameString
    }


    void "variantCallingDirectories, when samplePairsFinished is null, return empty string"() {
        when:
        new CreateNotificationTextService().variantCallingDirectories(null, SNV)

        then:
        thrown(AssertionError)
    }


    void "variantCallingDirectories, return correct paths"() {
        given:
        SamplePair samplePair1 = DomainFactory.createSamplePair()
        SamplePair samplePair2 = DomainFactory.createSamplePair()

        configService = new TestConfigService()

        when:
        String fileNameString = new CreateNotificationTextService().variantCallingDirectories([samplePair1, samplePair2], analysis)
        String expected = [
                new File("${configService.getRootPath()}/${samplePair1.project.dirName}/sequencing/${samplePair1.seqType.dirName}/view-by-pid/${samplePair1.individual.pid}/${pathSegment}/${samplePair1.seqType.libraryLayoutDirName}/${samplePair1.sampleType1.dirName}_${samplePair1.sampleType2.dirName}"),
                new File("${configService.getRootPath()}/${samplePair2.project.dirName}/sequencing/${samplePair2.seqType.dirName}/view-by-pid/${samplePair2.individual.pid}/${pathSegment}/${samplePair2.seqType.libraryLayoutDirName}/${samplePair2.sampleType1.dirName}_${samplePair2.sampleType2.dirName}"),
        ].sort().join('\n')

        then:
        expected == fileNameString

        where:
        analysis || pathSegment
        SNV      || "snv_results"
        INDEL    || "indel_results"
        SOPHIA   || "sv_results"
        ACESEQ   || "cnv_results"
    }


    void "getSamplePairRepresentation, when empty sample pair list, should return empty string"() {
        when:
        String samplePairs = new CreateNotificationTextService().getSamplePairRepresentation([])

        then:
        '' == samplePairs
    }


    void "getSamplePairRepresentation, when sample pair list is not empty, should return sample pair representations"() {
        given:
        SamplePair samplePair1 = DomainFactory.createSamplePair()
        SamplePair samplePair2 = DomainFactory.createSamplePair()

        when:
        String samplePairs = new CreateNotificationTextService().getSamplePairRepresentation([samplePair1, samplePair2])
        String expectedSamplePair = [
                "${samplePair1.individual.displayName} ${samplePair1.sampleType1.displayName} ${samplePair1.sampleType2.displayName} ${samplePair1.seqType.displayNameWithLibraryLayout}",
                "${samplePair2.individual.displayName} ${samplePair2.sampleType1.displayName} ${samplePair2.sampleType2.displayName} ${samplePair2.seqType.displayNameWithLibraryLayout}",
        ].sort().join('\n')

        then:
        expectedSamplePair == samplePairs
    }


    void "installationNotification, when status is null, throw assert"() {
        when:
        new CreateNotificationTextService().installationNotification(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert status')
    }


    @Unroll
    void "installationNotification, return message"() {
        given:
        DomainFactory.createRoddyAlignableSeqTypes()

        Map data1 = createData([
                sampleId1: 'sampleId1',
                pid      : 'patient_1',
        ])
        Map data2 = createData([
                sampleId1                   : 'sampleId2a',
                sampleId2                   : 'sampleId2b',
                pid                         : 'patient_2',
                project                     : multipleProjects ? DomainFactory.createProject() : data1.seqTrack.project,
                seqType                     : multipleSeqTypes ? DomainFactory.createSeqTypePaired() : data1.seqTrack.seqType,
                run                         : multipleRuns ? DomainFactory.createRun() : data1.seqTrack.run,
                installationProcessingStatus: installationProcessingStatus,
                alignmentProcessingStatus   : align ? ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_MIGHT_DO : ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
        ])

        ProcessingStatus processingStatus = new ProcessingStatus([
                data1.seqTrackProcessingStatus,
                data2.seqTrackProcessingStatus,
        ])

        int projectCount = multipleProjects && installationProcessingStatus == ProcessingStatus.WorkflowProcessingStatus.ALL_DONE ? 2 : 1

        CreateNotificationTextService createNotificationTextService = new CreateNotificationTextService(
                linkGenerator: Mock(LinkGenerator) {
                    projectCount * link(_) >> 'link'
                },
                lsdfFilesService: new LsdfFilesService(),
                messageSource: getMessageSource(),
        )

        List<SeqTrack> seqTracks = [data1.seqTrack]
        List<String> samples = ["[-] ${createNotificationTextService.getSampleName(data1.seqTrack)} (${data1.sampleId1})"]
        if (installationProcessingStatus == ProcessingStatus.WorkflowProcessingStatus.ALL_DONE) {
            seqTracks.add(data2.seqTrack)
            samples.add("${align ? '[A]' : '[-]'} ${createNotificationTextService.getSampleName(data2.seqTrack)} (${data2.sampleId1}, ${data2.sampleId2})")
        }

        String expectedPaths = createNotificationTextService.getSeqTypeDirectories(seqTracks)
        String expectedRuns = seqTracks*.run*.name.sort().unique().join(', ')
        String expectedLinks = seqTracks*.project.unique().collect { 'link' }.join('\n')
        String expectedSamples = samples.join('\n')
        String expectedAlign = align ? '\nfurther processing' : ''

        String expected = """
data installation finished
runs: ${expectedRuns}
paths: ${expectedPaths}
samples: ${expectedSamples}
links: ${expectedLinks}
${expectedAlign}"""

        when:
        String message = createNotificationTextService.installationNotification(processingStatus)

        then:
        expected == message

        where:
        multipleRuns | multipleSeqTypes | multipleProjects | installationProcessingStatus                                       | align
        false        | false            | false            | ProcessingStatus.WorkflowProcessingStatus.ALL_DONE                 | false
        true         | false            | false            | ProcessingStatus.WorkflowProcessingStatus.ALL_DONE                 | false
        false        | true             | false            | ProcessingStatus.WorkflowProcessingStatus.ALL_DONE                 | false
        false        | false            | true             | ProcessingStatus.WorkflowProcessingStatus.ALL_DONE                 | false
        false        | false            | false            | ProcessingStatus.WorkflowProcessingStatus.PARTLY_DONE_WONT_DO_MORE | false
        false        | false            | false            | ProcessingStatus.WorkflowProcessingStatus.ALL_DONE                 | true
    }

    void "alignmentNotification, when seqTracks is null, throw assert"() {
        when:
        new CreateNotificationTextService().alignmentNotification(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert status')
    }

    @Unroll
    void "alignmentNotification, return message"() {
        given:
        DomainFactory.createRoddyAlignableSeqTypes()
        DomainFactory.createProcessingOptionForNotificationRecipient()
        DomainFactory.createProcessingOptionForEmailSenderSalutation()

        Map data1 = createData([
                sampleId1                : 'sampleId1',
                alignmentProcessingStatus: ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
        ])
        Map data2 = createData(
                sampleId1: 'sampleId2a',
                sampleId2: 'sampleId2b',
                project: multipleProjects ? DomainFactory.createProject() : data1.seqTrack.project,
                seqType: multipleSeqTypes ? DomainFactory.createSeqTypePaired() : data1.seqTrack.seqType,
                run: data1.seqTrack.run,
                alignmentProcessingStatus: secondSampleAligned ? ProcessingStatus.WorkflowProcessingStatus.ALL_DONE : ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
                snvProcessingStatus: snv ? ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_MIGHT_DO : ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
                indelProcessingStatus: indel ? ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_MIGHT_DO : ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
                sophiaProcessingStatus: sophia ? ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_MIGHT_DO : ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
                aceseqProcessingStatus: aceseq ? ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_MIGHT_DO : ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
                runYapsaProcessingStatus: runYapsa ? ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_MIGHT_DO : ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
        )

        ProcessingStatus processingStatus = new ProcessingStatus([
                data1.seqTrackProcessingStatus,
                data2.seqTrackProcessingStatus,
        ])

        int projectCount = multipleProjects && secondSampleAligned ? 2 : 1
        int alignmentCount = (multipleProjects || multipleSeqTypes) && secondSampleAligned ? 2 : 1

        CreateNotificationTextService createNotificationTextService = new CreateNotificationTextService(
                projectOverviewService: Mock(ProjectOverviewService) {
                    alignmentCount * getAlignmentInformationFromConfig(_) >> data1.alignmentInfo >> data2.alignmentInfo
                    0 * _
                },
                linkGenerator: Mock(LinkGenerator) {
                    projectCount * link(_) >> 'link'
                },
                messageSource: getMessageSource(),
        )
        createNotificationTextService.processingOptionService = new ProcessingOptionService()

        List<SeqTrack> seqTracks = [data1.seqTrack]
        List<SamplePair> samplePairWithoutVariantCalling = [data1.samplePair]
        List<SamplePair> samplePairWithVariantCalling = []
        List<String> expectedSamples = ["${createNotificationTextService.getSampleName(data1.seqTrack)} (${data1.sampleId1})"]
        List<String> variantCallingPipelines = []
        if (secondSampleAligned) {
            seqTracks.add(data2.seqTrack)
            expectedSamples << "${createNotificationTextService.getSampleName(data2.seqTrack)} (${data2.sampleId1}, ${data2.sampleId2})"
            if (indel || snv || sophia || aceseq || runYapsa) {
                samplePairWithVariantCalling.add(data2.samplePair)
                // the If-cases have to be ordered alphabetically
                if (aceseq) {
                    variantCallingPipelines << 'CNV (from ACEseq)'
                }
                if (indel) {
                    variantCallingPipelines << 'Indel'
                }
                if (runYapsa) {
                    variantCallingPipelines << 'RunYapsa'
                }
                if (snv) {
                    variantCallingPipelines << 'SNV'
                }
                if (sophia) {
                    variantCallingPipelines << 'SV (from SOPHIA)'
                }
            } else {
                samplePairWithoutVariantCalling.add(data2.samplePair)
            }
        }

        String expectedLinks = seqTracks*.project.unique().collect { 'link' }.join('\n')
        List<String> alignments = []
        if (projectCount == 2) {
            alignments << "***********************"
            alignments << data1.seqTrack.project.name
        }
        alignments << createAlignmentInfoString(data1) + "\n" + createRoddyAlignmentInfoString(data1)
        if (alignmentCount == 2) {
            alignments << ''
            if (projectCount == 2) {
                alignments << "***********************"
                alignments << data2.seqTrack.project.name
            }
            alignments << createAlignmentInfoString(data2) + "\n" + createRoddyAlignmentInfoString(data2)
        }
        String expectedPaths = createNotificationTextService.getMergingDirectories(seqTracks)
        String expectedAlignment = alignments.join('\n').trim()
        String expectedVariantCallingRunning = samplePairWithVariantCalling ? """\n
run variant calling
variantCallingPipelines: ${variantCallingPipelines.join(', ')}
samplePairsWillProcess: ${createNotificationTextService.getSamplePairRepresentation(samplePairWithVariantCalling)}
""" : ''

        String expectedVariantCallingNotRunning = """\n
no variant calling
samplePairsWontProcess: ${createNotificationTextService.getSamplePairRepresentation(samplePairWithoutVariantCalling)}
"""

        String expected = """
alignment finished
samples: ${expectedSamples.sort().join('\n')}
links: ${expectedLinks}
processingValues: ${expectedAlignment}
paths: ${expectedPaths}
${expectedVariantCallingRunning}${expectedVariantCallingNotRunning}"""

        when:
        String message = createNotificationTextService.alignmentNotification(processingStatus)

        then:
        expected == message

        where:
        multipleSeqTypes | multipleProjects | secondSampleAligned | snv   | indel | sophia | aceseq | runYapsa
        false            | false            | false               | false | false | false  | false  | false
        false            | false            | true                | false | false | false  | false  | false
        false            | true             | true                | false | false | false  | false  | false
        true             | false            | true                | false | false | false  | false  | false
        false            | false            | true                | false | true  | false  | false  | false
        false            | false            | true                | true  | true  | false  | false  | false
        false            | false            | true                | false | false | true   | true   | true
        false            | false            | true                | false | true  | true   | false  | false
        false            | false            | true                | true  | true  | true   | true   | true
        false            | false            | true                | false | false | true   | true   | false
        true             | true             | true                | true  | true  | true   | true   | true
    }

    @Unroll
    void "alignmentNotification, test difference between Single Cell and Roddy"() {
        given:
        DomainFactory.createRoddyAlignableSeqTypes()
        DomainFactory.createProcessingOptionForNotificationRecipient()
        DomainFactory.createProcessingOptionForEmailSenderSalutation()

        Map data1 = createData([
                sampleId1                : "sampleId1",
                alignmentProcessingStatus: ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
                singleCell               : singleCell,
        ])

        ProcessingStatus processingStatus = new ProcessingStatus([
                data1.seqTrackProcessingStatus,
        ])

        CreateNotificationTextService createNotificationTextService = new CreateNotificationTextService(
                projectOverviewService: Mock(ProjectOverviewService) {
                    1 * getAlignmentInformationFromConfig(_) >> data1.alignmentInfo
                    0 * _
                },
                linkGenerator: Mock(LinkGenerator) {
                    1 * link(_) >> "link"
                },
        )
        createNotificationTextService.processingOptionService = new ProcessingOptionService()
        createNotificationTextService.messageSource = Mock(PluginAwareResourceBundleMessageSource) {
            1 * getMessageInternal("notification.template.alignment.base", [], _) >> ""
            1 * getMessageInternal("notification.template.alignment.processing", [], _) >> ""
            1 * getMessageInternal("notification.template.alignment.noFurtherProcessing", [], _) >> ""
            (singleCell ? 0 : 1) * getMessageInternal("notification.template.alignment.processing.roddy", [], _) >> ""
            (singleCell ? 1 : 0) * getMessageInternal("notification.template.alignment.processing.singleCell", [], _) >> ""
        }

        when:
        createNotificationTextService.alignmentNotification(processingStatus)

        then:
        true

        where:
        singleCell | _
        true       | _
        false      | _
    }

    @Unroll("#pairAnalysisList.type, when ProcessingStatus is null, throw assert")
    void "instanceNotification, when ProcessingStatus is null, throw assert"() {
        when:
        new CreateNotificationTextService()."${pairAnalysisList.notification}"(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert status')

        where:
        pairAnalysisList << listPairAnalyses
    }

    @Unroll("#pairAnalysisContentsPermutationList.type, return message")
    void "instanceNotification, return message"() {
        given:
        DomainFactory.createRoddyAlignableSeqTypes()
        DomainFactory.createProcessingOptionForEmailSenderSalutation()

        Map data1 = createData([
                (pairAnalysisContentsPermutationList.customProcessingStatus): ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
                seqType: SeqTypeService."${pairAnalysisContentsPermutationList.type}PipelineSeqTypes".first(),
        ])

        Map data2 = createData(
                project: pairAnalysisContentsPermutationList.multipleProjects ? DomainFactory.createProject() : data1.seqTrack.project,
                (pairAnalysisContentsPermutationList.customProcessingStatus): pairAnalysisContentsPermutationList.processingStatus,
                seqType: SeqTypeService."${pairAnalysisContentsPermutationList.type}PipelineSeqTypes".first(),
        )

        Map data3 = createData(
                project: pairAnalysisContentsPermutationList.multipleProjects ? DomainFactory.createProject() : data1.seqTrack.project,
                (pairAnalysisContentsPermutationList.customProcessingStatus): pairAnalysisContentsPermutationList.processingStatus,
        )

        ProcessingStatus processingStatus = new ProcessingStatus([
                data1.seqTrackProcessingStatus,
                data2.seqTrackProcessingStatus,
                data3.seqTrackProcessingStatus,
        ])

        OtrsTicket.ProcessingStep processingStep = (OtrsTicket.ProcessingStep) pairAnalysisContentsPermutationList.processingStep
        boolean expectsLinks = processingStep.controllerName && processingStep.actionName

        int projectCount = 0
        if (expectsLinks) {
            projectCount = pairAnalysisContentsPermutationList.multipleProjects && pairAnalysisContentsPermutationList.processingStatus == ProcessingStatus.WorkflowProcessingStatus.ALL_DONE ? 2 : 1
        }

        CreateNotificationTextService createNotificationTextService = new CreateNotificationTextService(
                linkGenerator: Mock(LinkGenerator) {
                    projectCount * link(_) >> 'link'
                },
                messageSource: getMessageSource(),
        )
        createNotificationTextService.processingOptionService = new ProcessingOptionService()

        List<SamplePair> samplePairWithAnalysis = [data1.samplePair]
        List<SamplePair> samplePairWithoutAnalysis = []


        switch (pairAnalysisContentsPermutationList.processingStatus) {
            case ProcessingStatus.WorkflowProcessingStatus.ALL_DONE:
                samplePairWithAnalysis.add(data2.samplePair)
                break
            case ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO:
                samplePairWithoutAnalysis.add(data2.samplePair)
                break
            default:
                //ignore samplepair
                break
        }

        String expectedSamplePairsFinished = createNotificationTextService.getSamplePairRepresentation(samplePairWithAnalysis)
        String expectedSamplePairsNotProcessed = createNotificationTextService.getSamplePairRepresentation(samplePairWithoutAnalysis)
        String expectedDirectories = createNotificationTextService.variantCallingDirectories(samplePairWithAnalysis, processingStep)
        String expectedLinks = expectsLinks ? samplePairWithAnalysis*.project.unique().collect { 'link' }.join('\n') : ""

        String expected = """
${processingStep.displayName} finished
samplePairsFinished: ${expectedSamplePairsFinished}
"""
        if (expectsLinks) {
            expected += "otpLinks: ${expectedLinks}\n"
        }
        expected += "directories: ${expectedDirectories}\n"

        if (expectedSamplePairsNotProcessed) {
            expected += """\n
${processingStep.notificationSubject} not processed
samplePairsNotProcessed: ${expectedSamplePairsNotProcessed}
"""
        }

        when:
        String message = createNotificationTextService."${pairAnalysisContentsPermutationList.notification}"(processingStatus)

        then:
        expected == message

        where:
        pairAnalysisContentsPermutationList << pairAnalysisContentsPermutation

    }

    void "notification, when an argument is null, throw assert"() {
        when:
        new CreateNotificationTextService().notification(
                ticket ? DomainFactory.createOtrsTicket() : null,
                status ? new ProcessingStatus() : null,
                processingStep ? SNV : null,
                project ? DomainFactory.createProject() : null)
        then:
        AssertionError e = thrown()
        e.message.contains("assert ${text}")

        where:
        ticket | status | processingStep | project || text
        true   | true   | true           | false   || 'project'
        true   | true   | false          | true    || 'processingStep'
        true   | false  | true           | true    || 'status'
        false  | true   | true           | true    || 'otrsTicket'
    }


    void "notification, when call for ProcessingStep FASTQC, throw an exception"() {
        when:
        new CreateNotificationTextService().notification(DomainFactory.createOtrsTicket(), new ProcessingStatus(), FASTQC, new Project())

        then:
        thrown(MissingMethodException)
    }

    private Map createData(Map properties = [:]) {
        Project project = properties.project ?: DomainFactory.createProject()
        String pid = properties.pid ?: "pid_${DomainFactory.counter++}"
        SeqType seqType = properties.seqType ?: DomainFactory.createSeqTypePaired()
        ProcessingStatus.WorkflowProcessingStatus installationProcessingStatus = properties.installationProcessingStatus ?: ProcessingStatus.WorkflowProcessingStatus.ALL_DONE
        ProcessingStatus.WorkflowProcessingStatus alignmentProcessingStatus = properties.alignmentProcessingStatus ?: ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        ProcessingStatus.WorkflowProcessingStatus snvProcessingStatus = properties.snvProcessingStatus ?: ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        ProcessingStatus.WorkflowProcessingStatus indelProcessingStatus = properties.indelProcessingStatus ?: ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        ProcessingStatus.WorkflowProcessingStatus sophiaProcessingStatus = properties.sophiaProcessingStatus ?: ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        ProcessingStatus.WorkflowProcessingStatus aceseqProcessingStatus = properties.aceseqProcessingStatus ?: ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        ProcessingStatus.WorkflowProcessingStatus runYapsaProcessingStatus = properties.runYapsaProcessingStatus ?: ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        Run run = properties.run ?: DomainFactory.createRun()
        String sampleId1 = properties.sampleId1 ?: "sampleId_${DomainFactory.counter++}"
        String sampleId2 = properties.sampleId2

        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles([
                seqType: seqType,
                sample : DomainFactory.createSample([
                        individual: DomainFactory.createIndividual([
                                project     : project,
                                pid         : pid,
                                mockFullName: pid,
                        ])
                ]),
                run    : run,
        ])
        seqTrack.dataFiles.each {
            DomainFactory.createMetaDataKeyAndEntry(it, MetaDataColumn.SAMPLE_ID, sampleId1)
            if (sampleId2) {
                DomainFactory.createMetaDataKeyAndEntry(it, MetaDataColumn.SAMPLE_ID, sampleId2)
            }
        }

        ReferenceGenome referenceGenome = DomainFactory.createReferenceGenomeProjectSeqTypeLazy([
                project: seqTrack.project,
                seqType: seqTrack.seqType,
        ]).referenceGenome

        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                sample          : seqTrack.sample,
                seqType         : seqType,
                referenceGenome : referenceGenome,
                statSizeFileName: "statSizeFileName_${DomainFactory.counter++}.tab",
                pipeline        : DomainFactory.createPanCanPipeline(),
        ])

        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([workPackage: mergingWorkPackage])

        SamplePair samplePair = DomainFactory.createDisease(mergingWorkPackage)

        AlignmentInfo alignmentInfo
        if (properties.singleCell == true) {
            alignmentInfo = createSingleCellAlignmentInfo(seqTrack)
        } else {
            alignmentInfo = createRoddyAlignmentInfo(seqTrack)
        }

        SeqTrackProcessingStatus seqTrackProcessingStatus = new SeqTrackProcessingStatus(
                seqTrack,
                installationProcessingStatus,
                ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
                [
                        new MergingWorkPackageProcessingStatus(
                                mergingWorkPackage,
                                alignmentProcessingStatus,
                                roddyBamFile,
                                [
                                        new SamplePairProcessingStatus(
                                                samplePair,
                                                snvProcessingStatus,
                                                null,
                                                indelProcessingStatus,
                                                null,
                                                sophiaProcessingStatus,
                                                null,
                                                aceseqProcessingStatus,
                                                null,
                                                runYapsaProcessingStatus,
                                                null,
                                        )
                                ]
                        )
                ]
        )

        return [
                sampleId1               : sampleId1,
                sampleId2               : sampleId2,
                seqTrack                : seqTrack,
                referenceGenome         : referenceGenome,
                alignmentInfo           : alignmentInfo,
                seqTrackProcessingStatus: seqTrackProcessingStatus,
                samplePair              : samplePair,
        ]
    }

    private RoddyAlignmentInfo createRoddyAlignmentInfo(SeqTrack seqTrack) {
        String prefix = "${seqTrack.project.name}_${seqTrack.seqType.displayNameWithLibraryLayout}"
        return new RoddyAlignmentInfo([
                alignmentProgram  : "${prefix}_alignmentProgram",
                alignmentParameter: "${prefix}_alignmentParameter",
                mergeCommand      : "${prefix}_mergeCommand",
                mergeOptions      : "${prefix}_mergeOptions",
                samToolsCommand   : "${prefix}_samTools",
        ])
    }

    private SingleCellAlignmentInfo createSingleCellAlignmentInfo(SeqTrack seqTrack) {
        String prefix = "${seqTrack.project.name}_${seqTrack.seqType.displayNameWithLibraryLayout}"
        return new SingleCellAlignmentInfo([
                alignmentProgram  : "${prefix}_alignmentProgram",
                alignmentParameter: "${prefix}_alignmentParameter",
        ])
    }

    private String createAlignmentInfoString(Map data) {
        return """\
alignment information
seqType: ${data.seqTrack.seqType.displayNameWithLibraryLayout}
referenceGenome: ${data.referenceGenome}
alignmentProgram: ${data.alignmentInfo.alignmentProgram}
alignmentParameter: ${data.alignmentInfo.alignmentParameter}"""
    }

    private String createRoddyAlignmentInfoString(Map data) {
        return """
mergingProgram: ${data.alignmentInfo.mergeCommand}
mergingParameter: ${data.alignmentInfo.mergeOptions}
samtoolsProgram: ${data.alignmentInfo.samToolsCommand}"""
    }

    PluginAwareResourceBundleMessageSource getMessageSource() {
        return Mock(PluginAwareResourceBundleMessageSource) {
            _ * getMessageInternal("notification.template.installation.base", [], _) >> '''
data installation finished
runs: ${runs}
paths: ${paths}
samples: ${samples}
links: ${links}
'''
            _ * getMessageInternal("notification.template.installation.furtherProcessing", [], _) >> '''further processing'''
            _ * getMessageInternal("notification.template.alignment.base", [], _) >> '''
alignment finished
samples: ${samples}
links: ${links}
processingValues: ${processingValues}
paths: ${paths}
'''
            _ * getMessageInternal("notification.template.alignment.furtherProcessing", [], _) >> '''
run variant calling
variantCallingPipelines: ${variantCallingPipelines}
samplePairsWillProcess: ${samplePairsWillProcess}
'''
            _ * getMessageInternal("notification.template.alignment.noFurtherProcessing", [], _) >> '''
no variant calling
samplePairsWontProcess: ${samplePairsWontProcess}
'''
            _ * getMessageInternal("notification.template.alignment.processing", [], _) >> '''
alignment information
seqType: ${seqType}
referenceGenome: ${referenceGenome}
alignmentProgram: ${alignmentProgram}
alignmentParameter: ${alignmentParameter}
'''
            _ * getMessageInternal("notification.template.alignment.processing.roddy", [], _) >> '''
mergingProgram: ${mergingProgram}
mergingParameter: ${mergingParameter}
samtoolsProgram: ${samtoolsProgram}
'''
            _ * getMessageInternal("notification.template.step.processed", [], _) >> '''
${displayName} finished
samplePairsFinished: ${samplePairsFinished}
'''
            _ * getMessageInternal("notification.template.step.processed.results.links", [], _) >> '''otpLinks: ${otpLinks}\n'''

            _ * getMessageInternal("notification.template.step.processed.results.directories", [], _) >> '''directories: ${directories}\n'''

            _ * getMessageInternal("notification.template.step.notProcessed", [], _) >> '''
${notificationSubject} not processed
samplePairsNotProcessed: ${samplePairsNotProcessed}
'''
        }
    }
}
