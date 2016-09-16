package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.beans.factory.annotation.*

import static de.dkfz.tbi.otp.ngsdata.LsdfFilesService.*


abstract class AbstractExecutePanCanJob extends AbstractRoddyJob {

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ExecuteRoddyCommandService executeRoddyCommandService

    @Autowired
    BedFileService bedFileService


    @Override
    protected String prepareAndReturnWorkflowSpecificCommand(RoddyResult roddyResult, Realm realm) throws Throwable {
        assert roddyResult : "roddyResult must not be null"
        assert realm : "realm must not be null"

        RoddyBamFile roddyBamFile = roddyResult as RoddyBamFile

        String analysisIDinConfigFile = executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
        String nameInConfigFile = roddyBamFile.config.getNameUsedInConfig()

        LsdfFilesService.ensureFileIsReadableAndNotEmpty(new File(roddyBamFile.config.configFilePath))

        return executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, nameInConfigFile, analysisIDinConfigFile, realm) +
                prepareAndReturnWorkflowSpecificParameter(roddyBamFile) +
                prepareAndReturnCValues(roddyBamFile)
    }


    public String prepareAndReturnCValues(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "roddyBamFile must not be null"

        File referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(roddyBamFile.project, roddyBamFile.referenceGenome) as File
        assert referenceGenomeFastaFile : "Path to the reference genome file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(referenceGenomeFastaFile)

        File chromosomeStatSizeFile = referenceGenomeService.chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage)
        assert chromosomeStatSizeFile : "Path to the chromosome stat size file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(chromosomeStatSizeFile)

        return "--cvalues=\"INDEX_PREFIX:${referenceGenomeFastaFile}" +
                ",CHROM_SIZES_FILE:${chromosomeStatSizeFile}" +
                prepareAndReturnWorkflowSpecificCValues(roddyBamFile) +
                "${(roddyBamFile.processingPriority >= ProcessingPriority.FAST_TRACK_PRIORITY) ? ",PBS_AccountName:FASTTRACK" : ""}" +
                ",possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}" +
                ",possibleTumorSampleNamePrefixes:\""
    }


    protected abstract String prepareAndReturnWorkflowSpecificCValues(RoddyBamFile roddyBamFile)

    protected abstract String prepareAndReturnWorkflowSpecificParameter(RoddyBamFile roddyBamFile)

    protected abstract void workflowSpecificValidation(RoddyBamFile roddyBamFile)


    @Override
    protected void validate(RoddyResult roddyResult) throws Throwable {
        assert roddyResult : "Input roddyResultObject must not be null"

        RoddyBamFile roddyBamFile = roddyResult as RoddyBamFile

        executeRoddyCommandService.correctPermissions(roddyBamFile, configService.getRealmDataProcessing(roddyBamFile.project))

        try {
            ensureCorrectBaseBamFileIsOnFileSystem(roddyBamFile.baseBamFile)
        } catch (AssertionError e) {
            throw new RuntimeException('The input BAM file seems to have changed on the file system while this job was processing it.', e)
        }

        ['Bam', 'Bai', 'Md5sum'].each {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(roddyBamFile."work${it}File")
        }

        ['MergedQA', 'ExecutionStore'].each {
            LsdfFilesService.ensureDirIsReadableAndNotEmpty(roddyBamFile."work${it}Directory")
        }

        workflowSpecificValidation(roddyBamFile)

        ensureFileIsReadableAndNotEmpty(roddyBamFile.workMergedQAJsonFile)

        roddyBamFile.workSingleLaneQAJsonFiles.values().each {
            ensureFileIsReadableAndNotEmpty(it)
        }

        assert [AbstractMergedBamFile.FileOperationStatus.DECLARED, AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING].contains(roddyBamFile.fileOperationStatus)
        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING
        assert roddyBamFile.save(flush: true)
    }


    void ensureCorrectBaseBamFileIsOnFileSystem(RoddyBamFile baseBamFile) {
        if (baseBamFile) {
            File bamFilePath = baseBamFile.getPathForFurtherProcessing()
            assert bamFilePath.exists()
            assert baseBamFile.fileSize == bamFilePath.length()
        }
    }

}
