package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.beans.factory.annotation.*

import java.util.regex.*

import static de.dkfz.tbi.otp.ngsdata.LsdfFilesService.*

abstract class AbstractExecutePanCanJob extends AbstractRoddyJob {

    public static final Pattern READ_GROUP_PATTERN = Pattern.compile(/^@RG\s+ID:([^\s]+)\s/)

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ExecuteRoddyCommandService executeRoddyCommandService

    @Autowired
    BedFileService bedFileService

    @Autowired
    ExecutionService executionService


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

        validateReadGroups(roddyBamFile)

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

    void validateReadGroups(RoddyBamFile bamFile) {

        File bamFilePath = bamFile.workBamFile
        Realm realm = configService.getRealmDataProcessing(bamFile.project)
        List<String> readGroupsInBam = executionService.executeCommandReturnProcessOutput(
                realm, "set -o pipefail; samtools view -H ${bamFilePath} | grep ^@RG\\\\s", realm.roddyUser
        ).assertExitCodeZeroAndStderrEmpty().stdout.split('\n').collect {
            Matcher matcher = READ_GROUP_PATTERN.matcher(it)
            if (!matcher.find()) {
                throw new RuntimeException("Line does not match expected @RG pattern: ${it}")
            }
            return matcher.group(1)
        }.sort()

        List<String> expectedReadGroups = bamFile.containedSeqTracks.collect { RoddyBamFile.getReadGroupName(it) }.sort()

        if (readGroupsInBam != expectedReadGroups) {
            throw new RuntimeException(
"""Read groups in BAM file are not as expected.
Read groups in ${bamFilePath}:
${readGroupsInBam.join('\n')}
Expected read groups:
${expectedReadGroups.join('\n')}""")
        }
    }

    void ensureCorrectBaseBamFileIsOnFileSystem(RoddyBamFile baseBamFile) {
        if (baseBamFile) {
            File bamFilePath = baseBamFile.getPathForFurtherProcessing()
            assert bamFilePath.exists()
            assert baseBamFile.fileSize == bamFilePath.length()
        }
    }

}
