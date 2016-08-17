package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.LsdfFilesService.*

@Component
@Scope("prototype")
@UseJobLog
class ExecutePanCanJob extends AbstractRoddyAlignmentJob implements AutoRestartableJob {

    @Override
    protected List<String> prepareAndReturnWorkflowSpecificCValues(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "roddyBamFile must not be null"

        List<String> filesToMerge = getFilesToMerge(roddyBamFile)

        RoddyBamFile baseBamFile = roddyBamFile.baseBamFile
        ensureCorrectBaseBamFileIsOnFileSystem(baseBamFile)

        List<String> cValues = prepareAndReturnAlignmentCValues(roddyBamFile)

        if (roddyBamFile.seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            BedFile bedFile = roddyBamFile.bedFile
            File bedFilePath = bedFileService.filePath(bedFile) as File
            cValues.add("TARGET_REGIONS_FILE:${bedFilePath}")
            cValues.add("TARGETSIZE:${bedFile.targetSize}")
        }
        cValues.add("fastq_list:${filesToMerge.join(";")}")
        if (baseBamFile) {
            cValues.add("bam:${baseBamFile.getPathForFurtherProcessing()}")
        }

        return cValues
    }


    @Override
    protected String prepareAndReturnWorkflowSpecificParameter(RoddyBamFile roddyBamFile) {
        return ""
    }


    protected List<File> getFilesToMerge(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "roddyBamFile must not be null"
        List<File> vbpDataFiles = []

        roddyBamFile.seqTracks.each { SeqTrack seqTrack ->
            List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
            assert LibraryLayout.valueOf(seqTrack.seqType.libraryLayout).mateCount == dataFiles.size()
            dataFiles.sort {it.mateNumber}.each { DataFile dataFile ->
                File file = new File(lsdfFilesService.getFileViewByPidPath(dataFile))
                LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
                assert dataFile.fileSize == file.length()
                vbpDataFiles.add(file)
            }
        }

        if (LibraryLayout.valueOf(roddyBamFile.seqType.libraryLayout).mateCount == 2) {
            vbpDataFiles.collate(2).each {
                MetaDataService.ensurePairedSequenceFileNameConsistency(it.first().path, it.last().path)
            }
        }

        return vbpDataFiles
    }


    @Override
    protected void workflowSpecificValidation(RoddyBamFile roddyBamFile) {
        if (roddyBamFile.seqType.seqTypeName == SeqTypeNames.EXOME) {
            ensureFileIsReadableAndNotEmpty(roddyBamFile.workMergedQATargetExtractJsonFile)
        }
        if (roddyBamFile.seqType.isRna()) {
            RnaRoddyBamFile rnaRoddyBamFile = roddyBamFile as RnaRoddyBamFile
            ensureFileIsReadableAndNotEmpty(rnaRoddyBamFile.correspondingWorkChimericBamFile)
        }
    }
}
