package workflows

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.alignment.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.plugin.springsecurity.*
import org.joda.time.*
import org.junit.*

import static de.dkfz.tbi.otp.job.processing.PbsOptionMergingService.*
import static de.dkfz.tbi.otp.ngsdata.SeqTypeNames.*

@Ignore
class ConveyBwaAlignmentWorkflowTests extends WorkflowTestCase {
    ProcessingOptionService processingOptionService
    LsdfFilesService lsdfFilesService
    ProcessedBamFileService processedBamFileService
    ProcessedSaiFileService processedSaiFileService


    DataFile dataFile
    DataFile dataFile2
    AlignmentPass alignmentPass
    SeqTrack seqTrack

    final static String REF_GEN_FILE_NAME_PREFIX = 'hs37d5'

    @Before
    void setUp() {
        File sourceFastqR1 = new File(testDataDir, "35-3B_NoIndex_L007_R1_complete_filtered.fastq.gz")
        File sourceFastqR2 = new File(testDataDir, "35-3B_NoIndex_L007_R2_complete_filtered.fastq.gz")
        File sourceRefGenDir = new File("${testDataDir}/reference_genomes/bwa06_1KGRef")

        SeqPlatform seqPlatform = SeqPlatform.build(name: "Illumina SeqDingsi")
        Run run = DomainFactory.createRun(seqPlatform: seqPlatform)
        SoftwareTool softwareTool = SoftwareTool.build()


        Project project = Project.build(
                realmName: realm.name,
        )
        assert(project.save(flush: true))

        Individual.build(
                project: project,
                type: Individual.Type.REAL,
        )

        MergingWorkPackage workPackage = createWorkPackage(seqPlatform.seqPlatformGroup)

        new ReferenceGenomeProjectSeqType(
                project: project,
                seqType: findSeqType(),
                referenceGenome: workPackage.referenceGenome,
        ).save(flush:true)

        seqTrack = new SeqTrack(
                sample: workPackage.sample,
                fastqcState: SeqTrack.DataProcessingState.FINISHED,
                seqType: findSeqType(),
                laneId: "0",
                run: run,
                libraryPreparationKit: workPackage.libraryPreparationKit,
                kitInfoReliability: InformationReliability.KNOWN,
                pipelineVersion: softwareTool,
        )
        assert seqTrack.save(flush: true, failOnError: true)

        dataFile = DomainFactory.createSequenceDataFile(
                mateNumber: 1,
                fileExists: true,
                fileSize: 1,
                fileWithdrawn: false,
                fileName: 'asdf_R1.fastq.gz',
                vbpFileName: 'asdf_R1.fastq.gz',
                seqTrack: seqTrack,
                nReads: 10000,
        )
        assert dataFile.save(flush: true, failOnError: true)

        dataFile2 = DomainFactory.createSequenceDataFile(
                mateNumber: 2,
                fileExists: true,
                fileSize: 1,
                fileWithdrawn: false,
                fileName: 'asdf_R2.fastq.gz',
                vbpFileName: 'asdf_R2.fastq.gz',
                seqTrack: seqTrack,
                nReads: 10000,
        )
        assert dataFile.save(flush: true, failOnError: true)

        FastqcProcessedFile fastqcProcessedFile = new FastqcProcessedFile(
                contentUploaded: true,
                dataFile: dataFile,
        ).save(flush: true, failOnError: true)

        alignmentPass = DomainFactory.createAlignmentPass(
                seqTrack: seqTrack,
                identifier: AlignmentPass.nextIdentifier(seqTrack),
                alignmentState: AlignmentPass.AlignmentState.NOT_STARTED,
                referenceGenome: workPackage.referenceGenome,
        )


        linkFileUtils.createAndValidateLinks([(sourceFastqR1): new File(lsdfFilesService.getFileFinalPath(dataFile))], realm)
        linkFileUtils.createAndValidateLinks([(sourceFastqR2): new File(lsdfFilesService.getFileFinalPath(dataFile2))], realm)

        linkFileUtils.createAndValidateLinks([(new File(lsdfFilesService.getFileFinalPath(dataFile))): new File(lsdfFilesService.getFileViewByPidPath(dataFile))], realm)
        linkFileUtils.createAndValidateLinks([(new File(lsdfFilesService.getFileFinalPath(dataFile2))): new File(lsdfFilesService.getFileViewByPidPath(dataFile2))], realm)

        linkFileUtils.createAndValidateLinks([(sourceRefGenDir): referenceGenomeService.referenceGenomeDirectory(workPackage.referenceGenome, false)], realm)


        SpringSecurityUtils.doWithAuth("admin") {
            processingOptionService.createOrUpdate('basesPerBytesFastQ', null, null, "1", "description")

            processingOptionService.createOrUpdate(
                    "${PBS_PREFIX}${ConveyBwaAlignmentJob.simpleName}",
                    'DKFZ',
                    null,
                    '''{
                        "-l": {
                            "walltime": "20:00",
                            "nodes": "1:ppn=12",
                            "mem": "126g"
                            },
                        "-q": "convey",
                        "-m": "a",
                        "-S": "/bin/bash",
                        "-j": "oe"
                        }''',
                    'Convey option for cluster'
            )
        }
    }

    static SeqType findSeqType() {
        return CollectionUtils.exactlyOneElement(SeqType.findAllWhere(
                name: WHOLE_GENOME.seqTypeName,
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED,
        ))
    }

    MergingWorkPackage createWorkPackage(SeqPlatformGroup seqPlatformGroup) {
        SeqType seqType = findSeqType()

        Pipeline pipeline = Pipeline.build(
                name: Pipeline.Name.DEFAULT_OTP,
                type: Pipeline.Type.ALIGNMENT,
        )

        ReferenceGenome referenceGenome = ReferenceGenome.build(
                fileNamePrefix: REF_GEN_FILE_NAME_PREFIX,
        )

        LibraryPreparationKit kit = new LibraryPreparationKit(
                name: "~* xX liBrArYprEPaRaTioNkiT Xx *~",
                shortDisplayName: "~* lPk *~",
        )
        assert kit.save(flush: true, failOnError: true)

        MergingWorkPackage workPackage = MergingWorkPackage.build(
                pipeline: pipeline,
                seqType: seqType,
                referenceGenome: referenceGenome,
                needsProcessing: false,
                libraryPreparationKit: kit,
                seqPlatformGroup: seqPlatformGroup,
        )

        workPackage.sampleType.name = "CONTROL"
        workPackage.sampleType.save(flush: true)

        workPackage.project.realmName = realm.name
        workPackage.project.save(flush: true)

        return workPackage
    }

    @Test
    void testWorkflow() {
        execute()


        assert alignmentPass.refresh().alignmentState == AlignmentPass.AlignmentState.FINISHED
        assert seqTrack.refresh().qualityEncoding == SeqTrack.QualityEncoding.PHRED

        ProcessedBamFile processedBamFile = CollectionUtils.exactlyOneElement(ProcessedBamFile.all)
        assert processedBamFile.status == AbstractBamFile.State.NEEDS_PROCESSING
        File bamFilePath = new File(processedBamFileService.getFilePath(processedBamFile))
        assert bamFilePath.exists()
        assert new File(processedBamFileService.bwaSampeErrorLogFilePath(processedBamFile)).exists()

        LogThreadLocal.withThreadLog(System.out) {
            ProcessHelperService.executeAndWait("zcat ${bamFilePath} 1> /dev/null").assertExitCodeZeroAndStderrEmpty()
            assert bamFilePath.length() == processedBamFile.fileSize
        }


        assert ProcessedSaiFile.all == ProcessedSaiFile.findAllByAlignmentPassAndDataFileInList(alignmentPass, [dataFile, dataFile2])

        ProcessedSaiFile.all.each { ProcessedSaiFile saiFile ->
            assert new File(processedSaiFileService.getFilePath(saiFile)).exists()
            assert new File(processedSaiFileService.bwaAlnErrorLogFilePath(saiFile)).exists()
        }
    }

    @Override
    List<String> getWorkflowScripts() {
        ["scripts/workflows/ConveyBwaAlignmentWorkflow.groovy"]
    }

    @Override
    Duration getTimeout() {
        Duration.standardHours(1)
    }
}
