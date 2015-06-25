
package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
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
    protected String prepareAndReturnWorkflowSpecificCommand(RoddyResult roddyResult, Realm realm ) throws Throwable {
        assert roddyResult : "roddyResult must not be null"
        assert realm : "realm must not be null"

        RoddyBamFile roddyBamFile = roddyResult as RoddyBamFile
        RoddyBamFile baseBamFile = roddyBamFile.baseBamFile
        List<String> dataFiles = []

        roddyBamFile.seqTracks.each { SeqTrack seqTrack ->
            DataFile.findAllBySeqTrack(seqTrack).each { DataFile dataFile ->
                dataFiles.add(lsdfFilesService.getFileFinalPath(dataFile))
            }
        }

        dataFiles.sort().collate(2).each {
            MetaDataService.ensurePairedSequenceFileNameConsistency(it.first(), it.last())
        }

        String seqTracksToMerge = dataFiles.join(";")

        String referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(roddyBamFile.project, roddyBamFile.referenceGenome)
        assert referenceGenomeFastaFile : "Path to the reference genome file is null"
        String chromosomeSizeFiles = referenceGenomeService.pathToChromosomeSizeFilesPerReference(roddyBamFile.project, roddyBamFile.referenceGenome)
        assert chromosomeSizeFiles : "Path to the chromosome size files is null"

        String analysisIDinConfigFile = executeRoddyCommandService.getAnalysisIDinConfigFile(roddyBamFile)
        String nameInConfigFile = "${roddyBamFile.workflow.name}_${roddyBamFile.config.externalScriptVersion}"

        return executeRoddyCommandService.defaultRoddyExecutionCommand(roddyBamFile, nameInConfigFile, analysisIDinConfigFile, realm) +
                    "--cvalues=fastq_list:${seqTracksToMerge},"  +
                    "${baseBamFile ? "bam:${baseBamFile.finalBamFile}," : ""}" +
                    "REFERENCE_GENOME:${referenceGenomeFastaFile}," +
                    "INDEX_PREFIX:${referenceGenomeFastaFile}," +
                    "CHROM_SIZES_FILE:${chromosomeSizeFiles}," +
                    "possibleControlSampleNamePrefixes:(${roddyBamFile.sampleType.dirName})"
    }


    @Override
    protected void validate(RoddyResult roddyResult) throws Throwable {
        assert roddyResult : "Input roddyResultObject must not be null"

        RoddyBamFile roddyBamFile = roddyResult as RoddyBamFile

        ['Bam', 'Bai', 'Md5sum'].each {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(roddyBamFile."tmpRoddy${it}File")
        }

        ['MergedQA', 'ExecutionStore'].each {
            LsdfFilesService.ensureDirIsReadableAndNotEmpty(roddyBamFile."tmpRoddy${it}Directory")
        }

        roddyBamFile.tmpRoddySingleLaneQADirectories.each { seqTrack, singleLaneQcDir ->
            LsdfFilesService.ensureDirIsReadableAndNotEmpty(singleLaneQcDir)
        }

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING
        assert roddyBamFile.save(flush: true)
    }
}
