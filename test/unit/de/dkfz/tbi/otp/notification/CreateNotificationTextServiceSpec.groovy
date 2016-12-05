package de.dkfz.tbi.otp.notification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.codehaus.groovy.grails.web.mapping.*
import spock.lang.*

import static de.dkfz.tbi.otp.tracking.OtrsTicket.ProcessingStep.*

@Mock([
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
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
@TestMixin(GrailsUnitTestMixin)
class CreateNotificationTextServiceSpec extends Specification {


    void "createMessage, when template is null, throw assert"() {
        when:
        new CreateNotificationTextService().createMessage(null, [:])

        then:
        AssertionError e = thrown()
        e.message.contains('assert templateName')
    }


    void "createMessage, when template is not found, throw ProcessingException"() {
        when:
        new CreateNotificationTextService().createMessage('unknownTemplate', [:])

        then:
        ProcessingException e = thrown()
        e.message.contains('no option has been found with name')
    }


    void "createMessage, when template exist, return notification text"() {
        given:
        String templateName = 'SomeTemplateName'
        String templateText = 'Some text ${placeholder} some text'
        String placeHolder = 'information'

        DomainFactory.createProcessingOption([
                name   : templateName,
                type   : null,
                project: null,
                value  : templateText,
        ])

        when:
        String message = new CreateNotificationTextService().createMessage(templateName, [placeholder: placeHolder])

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


    void "getSeqTypeDirectories, when paths do not start with icgc, then the paths should not be changed"() {
        given:
        SeqTrack seqTrack1 = DomainFactory.createSeqTrackWithTwoDataFiles()
        SeqTrack seqTrack2 = DomainFactory.createSeqTrackWithTwoDataFiles()
        Realm realm1 = DomainFactory.createRealmDataManagement(name: seqTrack1.project.realmName)
        Realm realm2 = DomainFactory.createRealmDataManagement(name: seqTrack2.project.realmName)

        when:
        String fileNameString = new CreateNotificationTextService(
                configService: new ConfigService(),
                lsdfFilesService: new LsdfFilesService(),
        ).getSeqTypeDirectories([seqTrack1, seqTrack2])
        String expected = [
                new File("${realm1.rootPath}/${seqTrack1.project.dirName}/sequencing/${seqTrack1.seqType.dirName}"),
                new File("${realm2.rootPath}/${seqTrack2.project.dirName}/sequencing/${seqTrack2.seqType.dirName}"),
        ].sort().join('\n')

        then:
        expected == fileNameString
    }


    void "getSeqTypeDirectories, when path starts with icgc, then the path should be changed to start with lsdf"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles()
        DomainFactory.createRealmDataManagement([
                name    : seqTrack.project.realmName,
                rootPath: LsdfFilesService.MOUNTPOINT_WITH_ICGC
        ])

        when:
        String fileNameString = new CreateNotificationTextService(
                configService: new ConfigService(),
                lsdfFilesService: new LsdfFilesService(),
        ).getSeqTypeDirectories([seqTrack])
        String expected = [
                new File("${LsdfFilesService.MOUNTPOINT_WITH_LSDF}/${seqTrack.project.dirName}/sequencing/${seqTrack.seqType.dirName}"),
        ].join('\n')

        then:
        expected == fileNameString
    }


    void "getMergingDirectories, when seqTracks is null, throw assert"() {
        when:
        new CreateNotificationTextService().getMergingDirectories(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert seqTracks')
    }


    void "getMergingDirectories, when paths do not start with icgc, then the paths should not be changed"() {
        given:
        SeqTrack seqTrack1 = DomainFactory.createSeqTrackWithTwoDataFiles()
        SeqTrack seqTrack2 = DomainFactory.createSeqTrackWithTwoDataFiles()
        // seqTrack3 has the same path with placeholders as seqTrack1
        SeqTrack seqTrack3 = DomainFactory.createSeqTrackWithTwoDataFiles(
                sample: DomainFactory.createSample(
                        individual: DomainFactory.createIndividual(
                                project: seqTrack1.project,
                        ),
                ),
                seqType: seqTrack1.seqType,
        )
        Realm realm1 = DomainFactory.createRealmDataManagement(name: seqTrack1.project.realmName)
        Realm realm2 = DomainFactory.createRealmDataManagement(name: seqTrack2.project.realmName)

        when:
        String fileNameString = new CreateNotificationTextService(
                mergedAlignmentDataFileService: new MergedAlignmentDataFileService(),
        ).getMergingDirectories([seqTrack1, seqTrack2, seqTrack3])
        String expected = [
                new File("${realm1.rootPath}/${seqTrack1.project.dirName}/sequencing/${seqTrack1.seqType.dirName}/view-by-pid/\${PID}/\${SAMPLE_TYPE}/${seqTrack1.seqType.libraryLayoutDirName}/merged-alignment"),
                new File("${realm2.rootPath}/${seqTrack2.project.dirName}/sequencing/${seqTrack2.seqType.dirName}/view-by-pid/\${PID}/\${SAMPLE_TYPE}/${seqTrack2.seqType.libraryLayoutDirName}/merged-alignment"),
        ].sort().join('\n')

        then:
        expected == fileNameString
    }


    void "getMergingDirectories, when path starts with icgc, then the path should be changed to start with lsdf"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles()
        DomainFactory.createRealmDataManagement([
                name    : seqTrack.project.realmName,
                rootPath: LsdfFilesService.MOUNTPOINT_WITH_ICGC
        ])

        when:
        String fileNameString = new CreateNotificationTextService(
                mergedAlignmentDataFileService: new MergedAlignmentDataFileService(),
        ).getMergingDirectories([seqTrack])
        String expected = new File("${LsdfFilesService.MOUNTPOINT_WITH_LSDF}/${seqTrack.project.dirName}/sequencing/${seqTrack.seqType.dirName}/view-by-pid/\${PID}/\${SAMPLE_TYPE}/${seqTrack.seqType.libraryLayoutDirName}/merged-alignment").path

        then:
        expected == fileNameString
    }


    void "variantCallingDirectories, when samplePairsFinished is null, return empty string"() {
        expect:
        '' == new CreateNotificationTextService().variantCallingDirectories(null, SNV)
    }


    void "variantCallingDirectories, when paths do not start with icgc, then the paths should not be changed"() {
        given:
        SamplePair samplePair1 = DomainFactory.createSamplePair()
        SamplePair samplePair2 = DomainFactory.createSamplePair()

        Realm realm1 = DomainFactory.createRealmDataManagement(name: samplePair1.project.realmName)
        Realm realm2 = DomainFactory.createRealmDataManagement(name: samplePair2.project.realmName)

        when:
        String fileNameString = new CreateNotificationTextService().variantCallingDirectories([samplePair1, samplePair2], analysis)
        String expected = [
                new File("${realm1.rootPath}/${samplePair1.project.dirName}/sequencing/${samplePair1.seqType.dirName}/view-by-pid/${samplePair1.individual.pid}/${pathSegment}/${samplePair1.seqType.libraryLayoutDirName}/${samplePair1.sampleType1.dirName}_${samplePair1.sampleType2.dirName}"),
                new File("${realm2.rootPath}/${samplePair2.project.dirName}/sequencing/${samplePair2.seqType.dirName}/view-by-pid/${samplePair2.individual.pid}/${pathSegment}/${samplePair2.seqType.libraryLayoutDirName}/${samplePair2.sampleType1.dirName}_${samplePair2.sampleType2.dirName}"),
        ].sort().join('\n')

        then:
        expected == fileNameString

        where:
        analysis || pathSegment
        SNV      || "snv_results"
        INDEL    || "indel_results"
    }


    void "variantCallingDirectories, when path starts with icgc, then the path should be changed to start with lsdf"() {
        given:
        SamplePair samplePair = DomainFactory.createSamplePair()
        DomainFactory.createRealmDataManagement([
                name    : samplePair.project.realmName,
                rootPath: LsdfFilesService.MOUNTPOINT_WITH_ICGC
        ])

        when:
        String fileNameString = new CreateNotificationTextService().variantCallingDirectories([samplePair], analysis)
        String expected = new File("${LsdfFilesService.MOUNTPOINT_WITH_LSDF}/${samplePair.project.dirName}/sequencing/${samplePair.seqType.dirName}/view-by-pid/${samplePair.individual.pid}/${pathSegment}/${samplePair.seqType.libraryLayoutDirName}/${samplePair.sampleType1.dirName}_${samplePair.sampleType2.dirName}").path

        then:
        expected == fileNameString

        where:
        analysis || pathSegment
        SNV      || "snv_results"
        INDEL    || "indel_results"
   }


    void "getSamplePairRepresentation, when empty sample pair list, should return empty string"() {
        when:
        String samplePairs = new CreateNotificationTextService(
                mergedAlignmentDataFileService: new MergedAlignmentDataFileService(),
        ).getSamplePairRepresentation([])

        then:
        '' == samplePairs
    }


    void "getSamplePairRepresentation, when sample pair list is not empty, should return sample pair representations"() {
        given:
        SamplePair samplePair1 = DomainFactory.createSamplePair()
        SamplePair samplePair2 = DomainFactory.createSamplePair()

        when:
        String samplePairs = new CreateNotificationTextService(
                mergedAlignmentDataFileService: new MergedAlignmentDataFileService(),
        ).getSamplePairRepresentation([samplePair1, samplePair2])
        String expectedSamplePair = [
                "${samplePair1.individual.displayName} ${samplePair1.sampleType1.displayName} ${samplePair1.sampleType2.displayName} ${samplePair1.seqType.displayName}",
                "${samplePair2.individual.displayName} ${samplePair2.sampleType1.displayName} ${samplePair2.sampleType2.displayName} ${samplePair2.seqType.displayName}",
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
        DomainFactory.createPanCanAlignableSeqTypes()
        DomainFactory.createNotificationProcessingOptions()

        Map data1 = createData([sampleId1: 'sampleId1'])
        Map data2 = createData(
                sampleId1: 'sampleId2a',
                sampleId2: 'sampleId2b',
                project: multipleProjects ? DomainFactory.createProjectWithRealms() : data1.seqTrack.project,
                seqType: multipleSeqTypes ? DomainFactory.createSeqTypePaired() : data1.seqTrack.seqType,
                run: multipleRuns ? DomainFactory.createRun() : data1.seqTrack.run,
                installationProcessingStatus: installationProcessingStatus,
                alignmentProcessingStatus: align ? ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_MIGHT_DO : ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
        )

        ProcessingStatus processingStatus = new ProcessingStatus([
                data1.seqTrackProcessingStatus,
                data2.seqTrackProcessingStatus,
        ])

        int projectCount = multipleProjects && installationProcessingStatus == ProcessingStatus.WorkflowProcessingStatus.ALL_DONE ? 2 : 1

        CreateNotificationTextService createNotificationTextService = new CreateNotificationTextService(
                linkGenerator: Mock(LinkGenerator) {
                    projectCount * link(_) >> 'link'
                },
                configService: new ConfigService(),
                lsdfFilesService: new LsdfFilesService(),
                mergedAlignmentDataFileService: new MergedAlignmentDataFileService(),
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
        DomainFactory.createPanCanAlignableSeqTypes()
        DomainFactory.createNotificationProcessingOptions()

        Map data1 = createData([
                sampleId1                : 'sampleId1',
                alignmentProcessingStatus: ProcessingStatus.WorkflowProcessingStatus.ALL_DONE
        ])
        Map data2 = createData(
                sampleId1: 'sampleId2a',
                sampleId2: 'sampleId2b',
                project: multipleProjects ? DomainFactory.createProjectWithRealms() : data1.seqTrack.project,
                seqType: multipleSeqTypes ? DomainFactory.createSeqTypePaired() : data1.seqTrack.seqType,
                run: data1.seqTrack.run,
                alignmentProcessingStatus: secondSampleAligned ? ProcessingStatus.WorkflowProcessingStatus.ALL_DONE : ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
                snvProcessingStatus: snv ? ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_MIGHT_DO : ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO,
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
                mergedAlignmentDataFileService: new MergedAlignmentDataFileService(),
        )

        List<SeqTrack> seqTracks = [data1.seqTrack]
        List<SamplePair> samplePairWithoutSnv = [data1.samplePair]
        List<SamplePair> samplePairWithSnv = []
        String expectedSamples = "${createNotificationTextService.getSampleName(data1.seqTrack)} (${data1.sampleId1})"
        if (secondSampleAligned) {
            seqTracks.add(data2.seqTrack)
            expectedSamples += "\n${createNotificationTextService.getSampleName(data2.seqTrack)} (${data2.sampleId1}, ${data2.sampleId2})"
            if (snv) {
                samplePairWithSnv.add(data2.samplePair)
            } else {
                samplePairWithoutSnv.add(data2.samplePair)
            }
        }


        String expectedLinks = seqTracks*.project.unique().collect { 'link' }.join('\n')
        List<String> alignments = []
        if (projectCount == 2) {
            alignments << "***********************"
            alignments << data1.seqTrack.project.name
        }
        alignments << createAlignmentInfoString(data1)
        if (alignmentCount == 2) {
            alignments << ''
            if (projectCount == 2) {
                alignments << "***********************"
                alignments << data2.seqTrack.project.name
            }
            alignments << createAlignmentInfoString(data2)
        }
        String expectedPaths = createNotificationTextService.getMergingDirectories(seqTracks)
        String expectedAlignment = alignments.join('\n').trim()
        String expectedSnvRunning = samplePairWithSnv ? """\n
run snv
samplePairsWillProcess: ${createNotificationTextService.getSamplePairRepresentation(samplePairWithSnv)}
""" : ''

        String expectedSnvNotRunning = """\n
no snv
samplePairsWontProcess: ${createNotificationTextService.getSamplePairRepresentation(samplePairWithoutSnv)}
"""

        String expected = """
alignment finished
samples: ${expectedSamples}
links: ${expectedLinks}
processingValues: ${expectedAlignment}
paths: ${expectedPaths}
${expectedSnvRunning}${expectedSnvNotRunning}"""

        when:
        String message = createNotificationTextService.alignmentNotification(processingStatus)

        then:
        expected == message

        where:
        multipleSeqTypes | multipleProjects | secondSampleAligned | snv
        false            | false            | true                | false
        true             | false            | true                | false
        false            | true             | true                | false
        false            | false            | false               | false
        false            | false            | true                | true
    }


    void "snvNotification, when ProcessingStatus is null, throw assert"() {
        when:
        new CreateNotificationTextService().snvNotification(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert status')
    }

    @Unroll
    void "snvNotification, return message"() {
        given:
        DomainFactory.createPanCanAlignableSeqTypes()
        DomainFactory.createNotificationProcessingOptions()

        Map data1 = createData([
                snvProcessingStatus: ProcessingStatus.WorkflowProcessingStatus.ALL_DONE
        ])
        Map data2 = createData(
                project: multipleProjects ? DomainFactory.createProjectWithRealms() : data1.seqTrack.project,
                snvProcessingStatus: snvProcessingStatus,
        )

        ProcessingStatus processingStatus = new ProcessingStatus([
                data1.seqTrackProcessingStatus,
                data2.seqTrackProcessingStatus,
        ])

        int projectCount = multipleProjects && snvProcessingStatus == ProcessingStatus.WorkflowProcessingStatus.ALL_DONE ? 2 : 1

        CreateNotificationTextService createNotificationTextService = new CreateNotificationTextService(
                linkGenerator: Mock(LinkGenerator) {
                    projectCount * link(_) >> 'link'
                },
                mergedAlignmentDataFileService: new MergedAlignmentDataFileService(),
        )

        List<SamplePair> samplePairWithSnv = [data1.samplePair]
        List<SamplePair> samplePairWithoutSnv = []
        switch (snvProcessingStatus) {
            case ProcessingStatus.WorkflowProcessingStatus.ALL_DONE:
                samplePairWithSnv.add(data2.samplePair)
                break
            case ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO:
                samplePairWithoutSnv.add(data2.samplePair)
                break
            default:
                //ignore samplepair
                break
        }

        String expectedSamplePairsFinished = createNotificationTextService.getSamplePairRepresentation(samplePairWithSnv)
        String expectedSamplePairsNotProcessed = createNotificationTextService.getSamplePairRepresentation(samplePairWithoutSnv)
        String expectedDirectories = createNotificationTextService.variantCallingDirectories(samplePairWithSnv, SNV)
        String expectedLinks = samplePairWithSnv*.project.unique().collect { 'link' }.join('\n')

        String expected = """
snv finished
samplePairsFinished: ${expectedSamplePairsFinished}
otpLinks: ${expectedLinks}
directories: ${expectedDirectories}
"""
        if (expectedSamplePairsNotProcessed) {
            expected += """\n
snv not processed
samplePairsNotProcessed: ${expectedSamplePairsNotProcessed}
"""
        }

        when:
        String message = createNotificationTextService.snvNotification(processingStatus)

        then:
        expected == message

        where:
        multipleProjects | snvProcessingStatus
        false            | ProcessingStatus.WorkflowProcessingStatus.ALL_DONE
        true             | ProcessingStatus.WorkflowProcessingStatus.ALL_DONE
        false            | ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        false            | ProcessingStatus.WorkflowProcessingStatus.PARTLY_DONE_MIGHT_DO_MORE
    }

    void "indelNotification, when ProcessingStatus is null, throw assert"() {
        when:
        new CreateNotificationTextService().indelNotification(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert status')
    }

    @Unroll
    void "indelNotification, return message"() {
        given:
        DomainFactory.createPanCanAlignableSeqTypes()
        DomainFactory.createNotificationProcessingOptions()

        Map data1 = createData([
                indelProcessingStatus: ProcessingStatus.WorkflowProcessingStatus.ALL_DONE
        ])
        Map data2 = createData(
                project: multipleProjects ? DomainFactory.createProjectWithRealms() : data1.seqTrack.project,
                indelProcessingStatus: indelProcessingStatus,
        )

        ProcessingStatus processingStatus = new ProcessingStatus([
                data1.seqTrackProcessingStatus,
                data2.seqTrackProcessingStatus,
        ])

        int projectCount = multipleProjects && indelProcessingStatus == ProcessingStatus.WorkflowProcessingStatus.ALL_DONE ? 2 : 1

        CreateNotificationTextService createNotificationTextService = new CreateNotificationTextService(
                mergedAlignmentDataFileService: new MergedAlignmentDataFileService(),
        )

        List<SamplePair> samplePairWithIndel = [data1.samplePair]
        List<SamplePair> samplePairWithoutIndel = []
        switch (indelProcessingStatus) {
            case ProcessingStatus.WorkflowProcessingStatus.ALL_DONE:
                samplePairWithIndel.add(data2.samplePair)
                break
            case ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO:
                samplePairWithoutIndel.add(data2.samplePair)
                break
            default:
                //ignore samplepair
                break
        }

        String expectedSamplePairsFinished = createNotificationTextService.getSamplePairRepresentation(samplePairWithIndel)
        String expectedSamplePairsNotProcessed = createNotificationTextService.getSamplePairRepresentation(samplePairWithoutIndel)
        String expectedDirectories = createNotificationTextService.variantCallingDirectories(samplePairWithIndel, INDEL)

        String expected = """
indel finished
samplePairsFinished: ${expectedSamplePairsFinished}
directories: ${expectedDirectories}
"""
        if (expectedSamplePairsNotProcessed) {
            expected += """\n
indel not processed
samplePairsNotProcessed: ${expectedSamplePairsNotProcessed}
"""
        }

        when:
        String message = createNotificationTextService.indelNotification(processingStatus)

        then:
        expected == message

        where:
        multipleProjects | indelProcessingStatus
        false            | ProcessingStatus.WorkflowProcessingStatus.ALL_DONE
        true             | ProcessingStatus.WorkflowProcessingStatus.ALL_DONE
        false            | ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        false            | ProcessingStatus.WorkflowProcessingStatus.PARTLY_DONE_MIGHT_DO_MORE
    }


    void "notification, when an argument is null, throw assert"() {
        when:
        new CreateNotificationTextService().notification(ticket, status, processingStep)

        then:
        AssertionError e = thrown()
        e.message.contains("assert ${text}")

        where:
        ticket           | status                 | processingStep || text
        new OtrsTicket() | new ProcessingStatus() | null           || 'processingStep'
        new OtrsTicket() | null                   | SNV            || 'status'
        null             | new ProcessingStatus() | SNV            || 'otrsTicket'
    }


    @Unroll
    void "notification, return message"() {
        given:
        DomainFactory.createNotificationProcessingOptions()
        OtrsTicket ticket = DomainFactory.createOtrsTicket(
                seqCenterComment: seqCenterComment,
        )
        ProcessingStatus processingStatus = new ProcessingStatus()

        CreateNotificationTextService createNotificationTextService = Spy(CreateNotificationTextService) {
            (processingStep == INSTALLATION ? 1 : 0) *
                    installationNotification(processingStatus) >> INSTALLATION.toString()
            (processingStep == ALIGNMENT ? 1 : 0) *
                    alignmentNotification(processingStatus) >> ALIGNMENT.toString()
            (processingStep == SNV ? 1 : 0) *
                    snvNotification(processingStatus) >> SNV.toString()
            (processingStep == INDEL ? 1 : 0) *
                    indelNotification(processingStatus) >> INDEL.toString()
        }

        String expectedSeqCenterComment = seqCenterComment ? """

******************************
Note from sequencing center:
${seqCenterComment}
******************************""" : ''

        String expected = """
base notification
stepInformation: ${processingStep.toString()}
seqCenterComment: ${expectedSeqCenterComment}
"""

        when:
        String message = createNotificationTextService.notification(ticket, processingStatus, processingStep)

        then:
        expected == message

        where:
        processingStep | seqCenterComment
        INSTALLATION   | null
        ALIGNMENT      | null
        SNV            | null
        INDEL          | null
        INSTALLATION   | 'Some comment'
        ALIGNMENT      | 'Some comment'
        SNV            | 'Some comment'
        INDEL          | 'Some comment'
    }


    void "notification, when call for ProcessingStep FASTQC, throw an exception"() {
        when:
        new CreateNotificationTextService().notification(DomainFactory.createOtrsTicket(), new ProcessingStatus(), FASTQC)

        then:
        thrown(MissingMethodException)
    }


    private static Map createData(Map properties = [:]) {
        Project project = properties.project ?: DomainFactory.createProjectWithRealms()
        SeqType seqType = properties.seqType ?: DomainFactory.createSeqTypePaired()
        ProcessingStatus.WorkflowProcessingStatus installationProcessingStatus = properties.installationProcessingStatus ?: ProcessingStatus.WorkflowProcessingStatus.ALL_DONE
        ProcessingStatus.WorkflowProcessingStatus alignmentProcessingStatus = properties.alignmentProcessingStatus ?: ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        ProcessingStatus.WorkflowProcessingStatus snvProcessingStatus = properties.snvProcessingStatus ?: ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        ProcessingStatus.WorkflowProcessingStatus indelProcessingStatus = properties.indelProcessingStatus ?: ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        Run run = properties.run ?: DomainFactory.createRun()
        String sampleId1 = properties.sampleId1 ?: "sampleId_${DomainFactory.counter++}"
        String sampleId2 = properties.sampleId2

        SeqTrack seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles([
                seqType: seqType,
                sample : DomainFactory.createSample([
                        individual: DomainFactory.createIndividual([
                                project: project
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
                pipeline        : DomainFactory.createPanCanPipeline()

        ])

        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([workPackage: mergingWorkPackage])

        SamplePair samplePair = DomainFactory.createDisease(mergingWorkPackage)

        ProjectOverviewService.AlignmentInfo alignmentInfo = createAlignmentInfo(seqTrack)

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

    private static ProjectOverviewService.AlignmentInfo createAlignmentInfo(SeqTrack seqTrack) {
        String prefix = "${seqTrack.project.name}_${seqTrack.seqType.displayName}"
        return new ProjectOverviewService.AlignmentInfo(
                bwaCommand: "${prefix}_bwaCommand",
                bwaOptions: "${prefix}_bwaOptions",
                samToolsCommand: "${prefix}_samTools",
                mergeCommand: "${prefix}_mergeCommand",
                mergeOptions: "${prefix}_mergeOptions",
        )
    }

    private static String createAlignmentInfoString(Map data) {
        return """\
alignment information
seqType: ${data.seqTrack.seqType.displayName}
referenceGenome: ${data.referenceGenome}
alignmentProgram: ${data.alignmentInfo.bwaCommand}
alignmentParameter: ${data.alignmentInfo.bwaOptions}
mergingProgram: ${data.alignmentInfo.mergeCommand}
mergingParameter: ${data.alignmentInfo.mergeOptions}
samtoolsProgram: ${data.alignmentInfo.samToolsCommand}"""
    }

}
