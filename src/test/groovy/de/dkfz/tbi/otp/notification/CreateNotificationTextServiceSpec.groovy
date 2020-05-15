/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.notification

import grails.testing.gorm.DataTest
import grails.web.mapping.LinkGenerator
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.MessageSourceService

import static de.dkfz.tbi.otp.tracking.OtrsTicket.ProcessingStep.*

//TODO refactor classes SNV INDEL ACESEQSpec abstract from BamFilePairAnalyses abstract
@SuppressWarnings(["ClassSize"])
class CreateNotificationTextServiceSpec extends Specification implements AlignmentPipelineFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                AbstractMergedBamFile,
                AntibodyTarget,
                CellRangerConfig,
                CellRangerMergingWorkPackage,
                DataFile,
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
                Realm,
                ReferenceGenome,
                ReferenceGenomeIndex,
                ReferenceGenomeProjectSeqType,
                RnaRoddyBamFile,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Run,
                FastqImportInstance,
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
                SingleCellBamFile,
                SoftwareTool,
                SoftwareToolIdentifier,
                ToolName,
        ]
    }

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
    @SuppressWarnings('AssignmentToStaticFieldFromInstanceMethod')
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

    void setup() {
        configService = new TestConfigService()
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
                it.project = project
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

        when:
        String fileNameString = new CreateNotificationTextService().getMergingDirectories([roddyBamFile1, roddyBamFile2, roddyBamFile3])
        String expected = [
                new File("${configService.getRootPath()}/${roddyBamFile1.project.dirName}/sequencing/${roddyBamFile1.seqType.dirName}/" +
                        "view-by-pid/\${PID}/\${SAMPLE_TYPE}/${roddyBamFile1.seqType.libraryLayoutDirName}/merged-alignment"),
                new File("${configService.getRootPath()}/${roddyBamFile2.project.dirName}/sequencing/${roddyBamFile1.seqType.dirName}/" +
                        "view-by-pid/\${PID}/\${SAMPLE_TYPE}/${roddyBamFile2.seqType.libraryLayoutDirName}/merged-alignment"),
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

        when:
        String fileNameString = new CreateNotificationTextService().getMergingDirectories([roddyBamFile])
        String expected = new File("${configService.getRootPath()}/${roddyBamFile.project.dirName}/sequencing/" +
                "${roddyBamFile.seqType.dirName}/view-by-pid/\${PID}/\${SAMPLE_TYPE}-\${ANTI_BODY_TARGET}/" +
                "${roddyBamFile.seqType.libraryLayoutDirName}/merged-alignment").path

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

        when:
        String fileNameString = new CreateNotificationTextService().variantCallingDirectories([samplePair1, samplePair2], analysis)

        String expected = [
                new File("${configService.getRootPath()}/${samplePair1.project.dirName}/sequencing/${samplePair1.seqType.dirName}/" +
                        "view-by-pid/${samplePair1.individual.pid}/${pathSegment}/${samplePair1.seqType.libraryLayoutDirName}/" +
                        "${samplePair1.sampleType1.dirName}_${samplePair1.sampleType2.dirName}"),
                new File("${configService.getRootPath()}/${samplePair2.project.dirName}/sequencing/${samplePair2.seqType.dirName}/" +
                        "view-by-pid/${samplePair2.individual.pid}/${pathSegment}/${samplePair2.seqType.libraryLayoutDirName}/" +
                        "${samplePair2.sampleType1.dirName}_${samplePair2.sampleType2.dirName}"),
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
                "${samplePair1.individual.displayName} ${samplePair1.sampleType1.displayName} " +
                        "${samplePair1.sampleType2.displayName} ${samplePair1.seqType.displayNameWithLibraryLayout}",
                "${samplePair2.individual.displayName} ${samplePair2.sampleType1.displayName} " +
                        "${samplePair2.sampleType2.displayName} ${samplePair2.seqType.displayNameWithLibraryLayout}",
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
        DomainFactory.createCellRangerAlignableSeqTypes()
        DomainFactory.createProcessingOptionLazy(ProcessingOption.OptionName.NOTIFICATION_TEMPLATE_FAQ_LINK, "faq")

        Map data1 = createData([
                sampleId1: 'sampleId1',
                pid      : 'patient_1',
                seqType  : singleCell ? DomainFactory.createCellRangerAlignableSeqTypes().first() : null,
        ])
        Map data2 = createData([
                sampleId1                   : 'sampleId2a',
                sampleId2                   : 'sampleId2b',
                pid                         : 'patient_2',
                project                     : multipleProjects ? DomainFactory.createProject() : data1.seqTrack.project,
                seqType                     : multipleSeqTypes ? DomainFactory.createSeqTypePaired() : data1.seqTrack.seqType,
                run                         : multipleRuns ? DomainFactory.createRun() : data1.seqTrack.run,
                installationProcessingStatus: installationProcessingStatus,
                alignmentProcessingStatus   : align ?
                        ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_MIGHT_DO :
                        ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
        ])

        ProcessingStatus processingStatus = new ProcessingStatus([
                data1.seqTrackProcessingStatus,
                data2.seqTrackProcessingStatus,
        ])

        int projectCount = multipleProjects && installationProcessingStatus == ProcessingStatus.WorkflowProcessingStatus.ALL_DONE ? 2 : 1

        CreateNotificationTextService createNotificationTextService = new CreateNotificationTextService(
                linkGenerator: Mock(LinkGenerator) {
                    (projectCount + (singleCell ? 1 : 0)) * link(_) >> 'link'
                },
                lsdfFilesService: new LsdfFilesService(),
                messageSourceService: getMessageSourceServiceWithMockedMessageSource(),
                processingOptionService: new ProcessingOptionService(),
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
        String expectedAlign = align ? "\nfurther processing${singleCell ? " cell ranger faq" : ""} further notification" : ""

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
        multipleRuns | multipleSeqTypes | multipleProjects | installationProcessingStatus                                       | align | singleCell
        false        | false            | false            | ProcessingStatus.WorkflowProcessingStatus.ALL_DONE                 | false | false
        true         | false            | false            | ProcessingStatus.WorkflowProcessingStatus.ALL_DONE                 | false | false
        false        | true             | false            | ProcessingStatus.WorkflowProcessingStatus.ALL_DONE                 | false | false
        false        | false            | true             | ProcessingStatus.WorkflowProcessingStatus.ALL_DONE                 | false | false
        false        | false            | false            | ProcessingStatus.WorkflowProcessingStatus.PARTLY_DONE_WONT_DO_MORE | false | false
        false        | false            | false            | ProcessingStatus.WorkflowProcessingStatus.ALL_DONE                 | true  | false
        false        | false            | false            | ProcessingStatus.WorkflowProcessingStatus.ALL_DONE                 | true  | true
    }

    @Unroll
    void "#methodName Notification when not ALL_DONE, returns empty String"() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        CreateNotificationTextService createNotificationTextService = new CreateNotificationTextService()
        ProcessingStatus status = new ProcessingStatus([new SeqTrackProcessingStatus(DomainFactory.createSeqTrack())])
        status.metaClass.getSamplePairProcessingStatuses = { return [] }
        String message

        when:
        message = createNotificationTextService."${methodName}Notification"(status)

        then:
        message == ''

        where:
        methodName     | _
        'installation' | _
        'alignment'    | _
        'snv'          | _
        'indel'        | _
        'aceseq'       | _
        'runYapsa'     | _
    }

    void "alignmentNotification, when seqTracks is null, throw assert"() {
        when:
        new CreateNotificationTextService().alignmentNotification(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert status')
    }

    @Unroll
    void "alignmentNotification, return message (#name)"() {
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
                alignmentProcessingStatus: secondSampleAligned ? ProcessingStatus.WorkflowProcessingStatus.ALL_DONE :
                        ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
                snvProcessingStatus: snv ? ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_MIGHT_DO :
                        ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
                indelProcessingStatus: indel ? ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_MIGHT_DO :
                        ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
                sophiaProcessingStatus: sophia ? ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_MIGHT_DO :
                        ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
                aceseqProcessingStatus: aceseq ? ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_MIGHT_DO :
                        ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
                runYapsaProcessingStatus: runYapsa ? ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_MIGHT_DO :
                        ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
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
                messageSourceService: getMessageSourceServiceWithMockedMessageSource(),
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
${expectedVariantCallingRunning}${expectedVariantCallingNotRunning}
pancan alignment infos
"""

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

        name = [
                multipleProjects ? 'two projects' : '',
                multipleSeqTypes ? 'two seq types' : '',
                secondSampleAligned ? 'two alignments' : '',
                snv ? 'snv' : '',
                indel ? 'indel' : '',
                sophia ? 'sophia' : '',
                aceseq ? 'aceseq' : '',
                runYapsa ? 'runYapsa' : '',
        ].findAll().join(', ')
    }

    @Unroll
    void "alignmentNotification, test difference between Cell Ranger and Roddy (#name)"() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
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

        int crOffset = (name == "Cell Ranger") ? 1 : 0
        CreateNotificationTextService createNotificationTextService = new CreateNotificationTextService(
                projectOverviewService: Mock(ProjectOverviewService) {
                    1 * getAlignmentInformationFromConfig(_) >> data1.alignmentInfo
                    0 * _
                },
                linkGenerator: Mock(LinkGenerator) {
                    (1 + crOffset) * link(_) >> "link"
                },
        )
        createNotificationTextService.processingOptionService = new ProcessingOptionService()
        createNotificationTextService.messageSourceService = new MessageSourceService(
                messageSource: Mock(PluginAwareResourceBundleMessageSource) {
                    1 * getMessageInternal("notification.template.alignment.base", [], _) >> ""
                    1 * getMessageInternal("notification.template.alignment.processing", [], _) >> ""
                    1 * getMessageInternal("notification.template.alignment.noFurtherProcessing", [], _) >> ""
                    crOffset * getMessageInternal("notification.template.annotation.cellRanger.selfservice", [], _) >> ""
                    0 * getMessageInternal("notification.template.annotation.cellRanger.selfservice.alreadyFinal", [], _) >> ""

                    (singleCell ? 0 : 1) * getMessageInternal("notification.template.alignment.processing.roddy", [], _) >> ""
                    (singleCell ? 1 : 0) * getMessageInternal("notification.template.alignment.processing.singleCell", [], _) >> ""
                    (singleCell ? 0 : 1) * getMessageInternal("notification.template.references.alignment.pancan", [], _) >> ""
                    (singleCell ? 1 : 0) * getMessageInternal("notification.template.references.alignment.cellRanger", [], _) >> ""
                }
        )

        when:
        createNotificationTextService.alignmentNotification(processingStatus)

        then:
        true

        where:
        singleCell || name
        true       || 'Cell Ranger'
        false      || 'Roddy'
    }

    @Unroll
    void "alignmentNotification, test for pipeline #pipeline"() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        DomainFactory.createProcessingOptionForNotificationRecipient()
        DomainFactory.createProcessingOptionForEmailSenderSalutation()

        AbstractMergedBamFile abstractMergedBamFile = createBamFileForPipelineName(pipeline)

        ProcessingStatus processingStatus = new ProcessingStatus([
                new SeqTrackProcessingStatus(
                        abstractMergedBamFile.getContainedSeqTracks().first(),
                        ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
                        ProcessingStatus.WorkflowProcessingStatus.ALL_DONE, [
                        new MergingWorkPackageProcessingStatus(
                                abstractMergedBamFile.mergingWorkPackage,
                                ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
                                abstractMergedBamFile,
                                []
                        ),
                ]
                ),
        ])

        int countPanCan = pipeline == Pipeline.Name.PANCAN_ALIGNMENT ? 1 : 0
        int countRoddyRna = pipeline == Pipeline.Name.RODDY_RNA_ALIGNMENT ? 1 : 0
        int countCellRanger = pipeline == Pipeline.Name.CELL_RANGER ? 1 : 0
        int countRoddy = countPanCan + countRoddyRna

        CreateNotificationTextService createNotificationTextService = new CreateNotificationTextService(
                projectOverviewService: Mock(ProjectOverviewService) {
                    countRoddy * getAlignmentInformationFromConfig(_ as RoddyWorkflowConfig) >> new RoddyAlignmentInfo()
                    countCellRanger * getAlignmentInformationFromConfig(_ as CellRangerConfig) >> new SingleCellAlignmentInfo()
                    0 * _
                },
                linkGenerator: Mock(LinkGenerator) {
                    (1 + countCellRanger) * link(_) >> "link"
                },
                processingOptionService: new ProcessingOptionService(),
        )

        createNotificationTextService.messageSourceService = new MessageSourceService(
                messageSource: Mock(PluginAwareResourceBundleMessageSource) {
                    1 * getMessageInternal("notification.template.alignment.base", [], _) >> ""
                    1 * getMessageInternal("notification.template.alignment.processing", [], _) >> ""

                    countCellRanger * getMessageInternal("notification.template.alignment.processing.singleCell", [], _) >> ""
                    countRoddy * getMessageInternal("notification.template.alignment.processing.roddy", [], _) >> ""
                    countPanCan * getMessageInternal("notification.template.references.alignment.pancan", [], _) >> ""
                    countCellRanger * getMessageInternal("notification.template.references.alignment.cellRanger", [], _) >> ""

                    countCellRanger * getMessageInternal("notification.template.annotation.cellRanger.selfservice", [], _) >> ""

                    0 * _
                }
        )

        when:
        createNotificationTextService.alignmentNotification(processingStatus)

        then:
        true

        where:
        pipeline << Pipeline.Name.getAlignmentPipelineNames()
    }

    void "alignmentNotification, test for multiple pipelines"() {
        given:
        DomainFactory.createAllAlignableSeqTypes()
        DomainFactory.createProcessingOptionForNotificationRecipient()
        DomainFactory.createProcessingOptionForEmailSenderSalutation()

        List<AbstractMergedBamFile> abstractMergedBamFiles = Pipeline.Name.getAlignmentPipelineNames().collect {
            createBamFileForPipelineName(it)
        }

        ProcessingStatus processingStatus = new ProcessingStatus(abstractMergedBamFiles.collect {
            new SeqTrackProcessingStatus(
                    it.getContainedSeqTracks().first(),
                    ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
                    ProcessingStatus.WorkflowProcessingStatus.ALL_DONE, [
                    new MergingWorkPackageProcessingStatus(
                            it.mergingWorkPackage,
                            ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
                            it,
                            []
                    ),
            ]
            )
        })

        CreateNotificationTextService createNotificationTextService = new CreateNotificationTextService(
                projectOverviewService: Mock(ProjectOverviewService) {
                    2 * getAlignmentInformationFromConfig(_ as RoddyWorkflowConfig) >> new RoddyAlignmentInfo()
                    1 * getAlignmentInformationFromConfig(_ as CellRangerConfig) >> new SingleCellAlignmentInfo()
                    0 * _
                },
                linkGenerator: Mock(LinkGenerator) {
                    4 * link(_) >> "link"
                },
                processingOptionService: new ProcessingOptionService(),
        )

        createNotificationTextService.messageSourceService = new MessageSourceService(
                messageSource: Mock(PluginAwareResourceBundleMessageSource) {
                    1 * getMessageInternal("notification.template.alignment.base", [], _) >> ""
                    3 * getMessageInternal("notification.template.alignment.processing", [], _) >> ""
                    1 * getMessageInternal("notification.template.alignment.processing.singleCell", [], _) >> ""
                    2 * getMessageInternal("notification.template.alignment.processing.roddy", [], _) >> ""
                    1 * getMessageInternal("notification.template.references.alignment.pancan", [], _) >> ""
                    1 * getMessageInternal("notification.template.references.alignment.cellRanger", [], _) >> ""
                    1 * getMessageInternal("notification.template.annotation.cellRanger.selfservice", [], _) >> ""
                    0 * _
            }
        )

        when:
        createNotificationTextService.alignmentNotification(processingStatus)

        then:
        true
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

    @Unroll("instanceNotification for #dataList.type and multi projects=#dataList.multipleProjects and status=#dataList.processingStatus, return message")
    void "instanceNotification, return message"() {
        given:
        DomainFactory.createRoddyAlignableSeqTypes()
        DomainFactory.createProcessingOptionForEmailSenderSalutation()

        Map data1 = createData([
                (dataList.customProcessingStatus): ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
                seqType                          : SeqTypeService."${dataList.type}PipelineSeqTypes".first(),
        ])

        Map data2 = createData(
                project: dataList.multipleProjects ? DomainFactory.createProject() : data1.seqTrack.project,
                (dataList.customProcessingStatus): dataList.processingStatus,
                seqType: SeqTypeService."${dataList.type}PipelineSeqTypes".first(),
        )

        Map data3 = createData(
                project: dataList.multipleProjects ? DomainFactory.createProject() : data1.seqTrack.project,
                (dataList.customProcessingStatus): dataList.processingStatus,
        )

        ProcessingStatus processingStatus = new ProcessingStatus([
                data1.seqTrackProcessingStatus,
                data2.seqTrackProcessingStatus,
                data3.seqTrackProcessingStatus,
        ])

        OtrsTicket.ProcessingStep processingStep = (OtrsTicket.ProcessingStep) dataList.processingStep
        boolean expectsLinks = processingStep.controllerName && processingStep.actionName

        int projectCount = 0
        if (expectsLinks) {
            projectCount = dataList.multipleProjects && dataList.processingStatus == ProcessingStatus.WorkflowProcessingStatus.ALL_DONE ? 2 : 1
        }

        CreateNotificationTextService createNotificationTextService = new CreateNotificationTextService(
                linkGenerator: Mock(LinkGenerator) {
                    projectCount * link(_) >> 'link'
                },
                messageSourceService: getMessageSourceServiceWithMockedMessageSource(),
        )
        createNotificationTextService.processingOptionService = new ProcessingOptionService()

        List<SamplePair> samplePairWithAnalysis = [data1.samplePair]
        List<SamplePair> samplePairWithoutAnalysis = []

        switch (dataList.processingStatus) {
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
        String expectedLinks = expectsLinks ? samplePairWithAnalysis*.project.unique().collect {
            'link'
        }.join('\n') : ""

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

        expected += "\n${dataList.type} analysis infos\n"

        when:
        String message = createNotificationTextService."${dataList.notification}"(processingStatus)

        then:
        expected == message

        where:
        dataList << pairAnalysisContentsPermutation
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
        boolean singeCell = properties.singleCell
        SeqType seqType = properties.seqType ?: DomainFactory.createSeqTypePaired([singleCell: singeCell])
        ProcessingStatus.WorkflowProcessingStatus installationProcessingStatus = properties.installationProcessingStatus ?:
                ProcessingStatus.WorkflowProcessingStatus.ALL_DONE
        ProcessingStatus.WorkflowProcessingStatus alignmentProcessingStatus = properties.alignmentProcessingStatus ?:
                ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        ProcessingStatus.WorkflowProcessingStatus snvProcessingStatus = properties.snvProcessingStatus ?:
                ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        ProcessingStatus.WorkflowProcessingStatus indelProcessingStatus = properties.indelProcessingStatus ?:
                ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        ProcessingStatus.WorkflowProcessingStatus sophiaProcessingStatus = properties.sophiaProcessingStatus ?:
                ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        ProcessingStatus.WorkflowProcessingStatus aceseqProcessingStatus = properties.aceseqProcessingStatus ?:
                ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        ProcessingStatus.WorkflowProcessingStatus runYapsaProcessingStatus = properties.runYapsaProcessingStatus ?:
                ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
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

        Map mergingWorkPackageProperties = [
                sample         : seqTrack.sample,
                seqType        : seqType,
                referenceGenome: referenceGenome,
        ]

        AlignmentInfo alignmentInfo
        MergingWorkPackage mergingWorkPackage
        AbstractMergedBamFile abstractMergedBamFile
        if (properties.singleCell == true) {
            alignmentInfo = createSingleCellAlignmentInfo(seqTrack)
            mergingWorkPackage = AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createMergingWorkPackage(mergingWorkPackageProperties)
            abstractMergedBamFile = AlignmentPipelineFactory.CellRangerFactoryInstance.INSTANCE.createBamFile([workPackage: mergingWorkPackage])
        } else {
            alignmentInfo = createRoddyAlignmentInfo(seqTrack)
            mergingWorkPackage = DomainFactory.createMergingWorkPackage(mergingWorkPackageProperties + [
                    statSizeFileName: "statSizeFileName_${DomainFactory.counter++}.tab",
                    pipeline        : DomainFactory.createPanCanPipeline(),
            ])
            abstractMergedBamFile = DomainFactory.createRoddyBamFile([workPackage: mergingWorkPackage])
        }

        SamplePair samplePair = DomainFactory.createDisease(mergingWorkPackage)

        SeqTrackProcessingStatus seqTrackProcessingStatus = new SeqTrackProcessingStatus(
                seqTrack,
                installationProcessingStatus,
                ProcessingStatus.WorkflowProcessingStatus.ALL_DONE,
                [
                        new MergingWorkPackageProcessingStatus(
                                mergingWorkPackage,
                                alignmentProcessingStatus,
                                abstractMergedBamFile,
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
                                        ),
                                ]
                        ),
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

    MessageSourceService getMessageSourceServiceWithMockedMessageSource() {
        return new MessageSourceService(
            messageSource: getMessageSource()
        )
    }

    @SuppressWarnings("GStringExpressionWithinString")
    @Override
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
            _ * getMessageInternal("notification.template.installation.furtherProcessing.cellRanger", [], _) >> '''further processing cell ranger'''
            _ * getMessageInternal("notification.template.installation.furtherProcessing.cellRanger.faq", [], _) >> ''' faq'''
            _ * getMessageInternal("notification.template.installation.furtherProcessing.furtherNotification", [], _) >> ''' further notification'''
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

            _ * getMessageInternal("notification.template.annotation.cellRanger.selfservice", [], _) >> '''finalRunSelectionLink: ${finalRunSelectionLink}'''
            _ * getMessageInternal("notification.template.annotation.cellRanger.selfservice.alreadyFinal", [], _) >> '''serviceMail: ${serviceMail}'''

            _ * getMessageInternal("notification.template.references.alignment.pancan", [], _) >> '''pancan alignment infos\n'''
            _ * getMessageInternal("notification.template.references.snv", [], _) >> '''snv analysis infos\n'''
            _ * getMessageInternal("notification.template.references.indel", [], _) >> '''indel analysis infos\n'''
            _ * getMessageInternal("notification.template.references.sophia", [], _) >> '''sophia analysis infos\n'''
            _ * getMessageInternal("notification.template.references.aceseq", [], _) >> '''aceseq analysis infos\n'''
            _ * getMessageInternal("notification.template.references.runyapsa", [], _) >> '''runYapsa analysis infos\n'''

            0 * _
        }
    }
}
