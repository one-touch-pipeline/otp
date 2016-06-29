package de.dkfz.tbi.otp.job.jobs.roddyAlignment


import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.jobs.AutoRestartable
import de.dkfz.tbi.otp.ngsdata.BedFile
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.MetaDataService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames

import static de.dkfz.tbi.otp.ngsdata.LsdfFilesService.ensureFileIsReadableAndNotEmpty


class ExecutePanCanJob extends AbstractExecutePanCanJob implements AutoRestartable{

    @Override
    protected String prepareAndReturnWorkflowSpecificCValues(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "roddyBamFile must not be null"

        List<String> filesToMerge = getFilesToMerge(roddyBamFile)

        RoddyBamFile baseBamFile = roddyBamFile.baseBamFile
        ensureCorrectBaseBamFileIsOnFileSystem(baseBamFile)

        String additionalCValues = ''
        if (roddyBamFile.seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            BedFile bedFile = roddyBamFile.bedFile
            Realm dataProcessingRealm = configService.getRealmDataProcessing(roddyBamFile.project)
            File bedFilePath = bedFileService.filePath(dataProcessingRealm, bedFile) as File
            additionalCValues += "TARGET_REGIONS_FILE:${bedFilePath},"
            additionalCValues += "TARGETSIZE:${bedFile.targetSize},"
        }

        return ",fastq_list:${filesToMerge.join(";")}," +
                "${baseBamFile ? "bam:${baseBamFile.getPathForFurtherProcessing()}," : ""}" +
                additionalCValues +
                "possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}"
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
            assert 2 == dataFiles.size()
            dataFiles.sort {it.mateNumber}.each { DataFile dataFile ->
                File file = new File(lsdfFilesService.getFileViewByPidPath(dataFile))
                LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
                assert dataFile.fileSize == file.length()
                vbpDataFiles.add(file)
            }
        }

        vbpDataFiles.collate(2).each {
            MetaDataService.ensurePairedSequenceFileNameConsistency(it.first().path, it.last().path)
        }

        return vbpDataFiles
    }


    @Override
    protected void workflowSpecificValidation(RoddyBamFile roddyBamFile) {
        if (roddyBamFile.seqType.seqTypeName == SeqTypeNames.EXOME) {
            ensureFileIsReadableAndNotEmpty(roddyBamFile.workMergedQATargetExtractJsonFile)
        }
    }
}
