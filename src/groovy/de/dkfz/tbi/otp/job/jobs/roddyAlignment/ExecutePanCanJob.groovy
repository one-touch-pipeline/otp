package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.MetaDataService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import org.springframework.beans.factory.annotation.Autowired


class ExecutePanCanJob extends AbstractRoddyJob {

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    LsdfFilesService lsdfFilesService


    @Override
    protected String prepareAndReturnWorkflowSpecificCommand(RoddyResult roddyResult, Realm realm) throws Throwable {
        assert roddyResult : "roddyResult must not be null"
        assert realm : "realm must not be null"

        RoddyBamFile roddyBamFile = roddyResult as RoddyBamFile
        RoddyBamFile baseBamFile = roddyBamFile.baseBamFile

        List<File> vbpDataFiles = []

        roddyBamFile.seqTracks.each { SeqTrack seqTrack ->
            List<DataFile> dataFiles = DataFile.findAllBySeqTrack(seqTrack)
            assert 2 == dataFiles.size()
            dataFiles.sort {it.readNumber}.each { DataFile dataFile ->
                File file = new File(lsdfFilesService.getFileViewByPidPath(dataFile))
                LsdfFilesService.ensureFileIsReadableAndNotEmpty(file)
                assert dataFile.fileSize == file.length()
                vbpDataFiles.add(file)
            }
        }

        vbpDataFiles.collate(2).each {
            MetaDataService.ensurePairedSequenceFileNameConsistency(it.first().path, it.last().path)
        }

        String seqTracksToMerge = vbpDataFiles.join(";")

        File referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(roddyBamFile.project, roddyBamFile.referenceGenome) as File
        assert referenceGenomeFastaFile : "Path to the reference genome file is null"

        File chromosomeSizeFiles = referenceGenomeService.pathToChromosomeSizeFilesPerReference(roddyBamFile.project, roddyBamFile.referenceGenome) as File
        assert chromosomeSizeFiles : "Path to the chromosome size files is null"

        String analysisIDinConfigFile = executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
        String nameInConfigFile = "${roddyBamFile.workflow.name}_${roddyBamFile.config.externalScriptVersion}"

        ensureCorrectBaseBamFileIsOnFileSystem(baseBamFile)
        LsdfFilesService.ensureFileIsReadableAndNotEmpty(new File(roddyBamFile.config.configFilePath))
        LsdfFilesService.ensureDirIsReadableAndNotEmpty(chromosomeSizeFiles)


        return executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, nameInConfigFile, analysisIDinConfigFile, realm) +
                    "--cvalues=\"fastq_list:${seqTracksToMerge},"  +
                    "${baseBamFile ? "bam:${baseBamFile.finalBamFile}," : ""}" +
                    "REFERENCE_GENOME:${referenceGenomeFastaFile}," +
                    "INDEX_PREFIX:${referenceGenomeFastaFile}," +
                    "CHROM_SIZES_FILE:${chromosomeSizeFiles}," +
                    "possibleControlSampleNamePrefixes:${roddyBamFile.sampleType.dirName}\""
    }


    @Override
    protected void validate(RoddyResult roddyResult) throws Throwable {
        assert roddyResult : "Input roddyResultObject must not be null"

        RoddyBamFile roddyBamFile = roddyResult as RoddyBamFile

        try {
            ensureCorrectBaseBamFileIsOnFileSystem(roddyBamFile.baseBamFile)
        } catch (AssertionError e) {
            throw new RuntimeException('The input BAM file seems to have changed on the file system while this job was processing it.', e)
        }

        ['Bam', 'Bai', 'Md5sum'].each {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(roddyBamFile."tmpRoddy${it}File")
        }

        ['MergedQA', 'ExecutionStore'].each {
            LsdfFilesService.ensureDirIsReadableAndNotEmpty(roddyBamFile."tmpRoddy${it}Directory")
        }

        roddyBamFile.tmpRoddySingleLaneQADirectories.each { seqTrack, singleLaneQcDir ->
            LsdfFilesService.ensureDirIsReadableAndNotEmpty(singleLaneQcDir)
        }

        assert [FileOperationStatus.DECLARED, FileOperationStatus.NEEDS_PROCESSING].contains(roddyBamFile.fileOperationStatus)
        roddyBamFile.fileOperationStatus = FileOperationStatus.NEEDS_PROCESSING
        assert roddyBamFile.save(flush: true)
    }

    void ensureCorrectBaseBamFileIsOnFileSystem(RoddyBamFile baseBamFile) {
        if (baseBamFile) {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(baseBamFile.finalBamFile)
            assert baseBamFile.fileSize == baseBamFile.finalBamFile.length()
        }
    }
}
