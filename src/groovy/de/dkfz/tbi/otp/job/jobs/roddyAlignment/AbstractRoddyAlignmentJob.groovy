package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*

import java.util.regex.*

import static de.dkfz.tbi.otp.ngsdata.LsdfFilesService.*

abstract class AbstractRoddyAlignmentJob extends AbstractExecutePanCanJob<RoddyBamFile> {

    public List<String> prepareAndReturnAlignmentCValues(RoddyBamFile roddyBamFile) {
        assert roddyBamFile

        File referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(roddyBamFile.project, roddyBamFile.referenceGenome) as File
        assert referenceGenomeFastaFile: "Path to the reference genome file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(referenceGenomeFastaFile)

        File chromosomeStatSizeFile = referenceGenomeService.chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage)
        assert chromosomeStatSizeFile: "Path to the chromosome stat size file is null"
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(chromosomeStatSizeFile)

        return ["INDEX_PREFIX:${referenceGenomeFastaFile}",
                "CHROM_SIZES_FILE:${chromosomeStatSizeFile}",
                "possibleControlSampleNamePrefixes:${roddyBamFile.getSampleType().dirName}",
                "possibleTumorSampleNamePrefixes:"]
    }


    @Override
    protected void validate(RoddyBamFile roddyBamFile) throws Throwable {
        assert roddyBamFile: "Input roddyBamFile must not be null"

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

        List<String> expectedReadGroups = bamFile.containedSeqTracks.collect {
            RoddyBamFile.getReadGroupName(it)
        }.sort()

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

    protected abstract void workflowSpecificValidation(RoddyBamFile bamFile)
}
