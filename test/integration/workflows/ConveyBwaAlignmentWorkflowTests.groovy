package workflows

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.alignment.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.plugin.springsecurity.*
import org.joda.time.*
import org.junit.*

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

        SeqPlatform seqPlatform = DomainFactory.createSeqPlatform([
                name: "Illumina SeqDingsi"
        ])

        MergingWorkPackage workPackage = DomainFactory.createMergingWorkPackage([
                pipeline        : DomainFactory.createDefaultOtpPipeline(),
                seqType         : DomainFactory.createWholeGenomeSeqType(),
                seqPlatformGroup: seqPlatform.seqPlatformGroup,
                referenceGenome : DomainFactory.createReferenceGenome([
                        fileNamePrefix: REF_GEN_FILE_NAME_PREFIX,
                ]),
                needsProcessing : false,
        ])

        workPackage.project.realmName = realm.name
        workPackage.project.save(flush: true)

        seqTrack = DomainFactory.createSeqTrackWithTwoDataFiles(workPackage, [
                fastqcState: SeqTrack.DataProcessingState.FINISHED,
                run: DomainFactory.createRun([
                    seqPlatform: seqPlatform
                ]),
        ], [
                fileExists   : true,
                fileSize     : 1,
                fileWithdrawn: false,
                nReads       : 10000,
        ], [
                fileExists   : true,
                fileSize     : 1,
                fileWithdrawn: false,
                nReads       : 10000,
        ])

        seqTrack.dataFiles.each {
            DomainFactory.createFastqcProcessedFile(
                    contentUploaded: true,
                    dataFile: it,
            )
        }
        (dataFile, dataFile2) = seqTrack.dataFiles.sort {
            it.mateNumber
        }

        alignmentPass = DomainFactory.createAlignmentPass(
                seqTrack: seqTrack,
                alignmentState: AlignmentPass.AlignmentState.NOT_STARTED,
                referenceGenome: workPackage.referenceGenome,
        )

        DomainFactory.createReferenceGenomeProjectSeqType(
                project: workPackage.project,
                seqType: workPackage.seqType,
                referenceGenome: workPackage.referenceGenome,
        )

        linkFileUtils.createAndValidateLinks([
                (sourceFastqR1)                                         : new File(lsdfFilesService.getFileFinalPath(dataFile)),
                (sourceFastqR2)                                         : new File(lsdfFilesService.getFileFinalPath(dataFile2)),
                (new File(lsdfFilesService.getFileFinalPath(dataFile))) : new File(lsdfFilesService.getFileViewByPidPath(dataFile)),
                (new File(lsdfFilesService.getFileFinalPath(dataFile2))): new File(lsdfFilesService.getFileViewByPidPath(dataFile2)),
                (sourceRefGenDir)                                       : referenceGenomeService.referenceGenomeDirectory(workPackage.referenceGenome, false),
        ], realm)


        SpringSecurityUtils.doWithAuth("admin") {
            processingOptionService.createOrUpdate(ProcessingOption.OptionName.STATISTICS_BASES_PER_BYTES_FASTQ, null, null, "1")

            processingOptionService.createOrUpdate(
                    ProcessingOption.OptionName.CLUSTER_SUBMISSIONS_OPTION,
                    "${ConveyBwaAlignmentJob.simpleName}",
                    null,
                    '{"QUEUE":"convey","WALLTIME":"PT20M","MEMORY":"126g","CORES":"12"}',
            )
        }
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
