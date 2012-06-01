package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*

import org.springframework.beans.factory.annotation.Autowired

class DataProcessingFilesService {

    def grailsApplication

    public enum FileSetEntry {
        OutputBaseDir,
        RefIndex,
        ScriptDir
    }

    public enum OutputDirectories {
        fastx,
        alignment,
        structural_variation,
        coverage,
        base
    }

    public enum ScriptFileID {
        Fastx,
        Alignment,
        SampeSort
    }

    private static final HashMap<String, String> filenameEnds = [
        "fastxStatsText": "stats.txt",
        "fastxStatsQImg": "quality.png",
        "fastxStatsNImg": "nucleotideDistr.png",
        "fastxStatsFstQ": "fastq",
        "alignmentSAI": "sequence.sai",
    ]

    private static final HashMap<ScriptFileID, String> scriptFiles = [
        (ScriptFileID.Fastx) : "fastx_qc_new.sh",
        (ScriptFileID.Alignment) : "align_bwa_new.sh",
        (ScriptFileID.SampeSort) : "sampeSort.sh"
    ]

    String getScriptFile(ScriptFileID index) {
        String scriptDir = grailsApplication.config.otp.dataprocessing.scriptdir
        if (scriptFiles.containsKey(index)) {
            String file = scriptDir + "/" + scriptFiles[index]
            if(!(new File(file)).exists())
            throw new Exception("Script ${index} cannot be found")
            return file
        } else
        throw new Exception("Script ${index} is not specified")
    }

    String getOutputDirectory(Individual individual) {
        String outputBaseDir = grailsApplication.config.otp.dataprocessing.outputbasedir
        return "${outputBaseDir}results_by_pid/${individual.pid}/"
    }

    String getOutputDirectory(Individual individual, String dir) {
        if (dir) {
            return getOutputDirectory(individual, dir as OutputDirectories)
        }

        return getOutputDirectory(individual)
    }

    String getOutputDirectory(Individual individual, OutputDirectories dir) {
        String outputBaseDir = grailsApplication.config.otp.dataprocessing.outputbasedir
        return "${outputBaseDir}results_by_pid/${individual.pid}/${dir}/"
    }

    public String getPathOfFastxStatsFile(DataFile df) {
        return calculateFilenameWithSampleRunLaneAndIndex(df, "fastxStatsText")
    }

    public String getPathOfFastQualityImage(DataFile df) {
        return calculateFilenameWithSampleRunLaneAndIndex(df, "fastxStatsQImg")
    }

    public String getPathOfFastxNucleotideDistImage(DataFile df) {
        return calculateFilenameWithSampleRunLaneAndIndex(df, "fastxStatsNImg")
    }

    public String getPathOfFastxFastqFile(DataFile df) {
        return calculateFilenameWithSampleRunLaneAndIndex(df, "fastxStatsFstQ")
    }

    public String getPathOfBamFile(DataFile df) {
        return calculateFilenameWithSampleRunLaneAndIndex(df, "alignmentSAI")
    }

    public String calculateFilenameWithSampleRunLaneAndIndex(DataFile df, String postfix) {
        // HINT: Could burn as the access could mess up
        SeqTrack seq = df.seqTrack
        Run run = seq.run
        Sample sample = seq.sample
        Individual individual = sample.individual
        String dir = getOutputDirectory(individual, OutputDirectories.fastx)
        String pid = individual.pid
        String filename = ""

        //Get the lane index. As this is not very easy I try a different approach:
        // - Get all datafiles for the seqtrack
        // - Sort them by the filename
        // - Find the index of the current datafile within the sorted array
        // - TODO Does not handle NOINDEX or if no index exists

        List<DataFile> allFiles = DataFile.findAllBySeqTrack(seq, [sort:'fileName'])
        int index = allFiles.indexOf(df)

        filename = "${dir}${sample.type}_${run.name}_${seq.laneId}_${index + 1}.${filenameEnds[postfix]}"

        return filename
    }

    public String calculateFilenameWithSampleRunAndLane(DataFile df, String postfix) {
        SeqTrack seq = df.seqTrack
        Run run = seq.run
        Sample sample = seq.sample
        Individual individual = sample.individual
        String dir = getOutputDirectory(individual, OutputDirectories.fastx)

        String filename = "${dir}${sample.type}_${run.name}_${seq.laneId}.${filenameEnds[postfix]}"

        return filename
    }

    public String calculateFilenameWithSampleAndRun(DataFile df, String postfix) {
        SeqTrack seq = df.seqTrack
        Run run = seq.run
        Sample sample = seq.sample
        Individual individual = sample.individual
        String dir = getOutputDirectory(individual, OutputDirectories.fastx)

        String filename = "${dir}${sample.type}_${run.name}.${filenameEnds[postfix]}"

        return filename
    }

    public String calculateFilenameWithSample(DataFile df, String postfix) {
        SeqTrack seq = df.seqTrack
        Sample sample = seq.sample
        Individual individual = sample.individual
        String dir = getOutputDirectory(individual, OutputDirectories.fastx)

        String filename = "${dir}${sample.type}.${filenameEnds[postfix]}"

        return filename
    }

    public String calculateFilenameWithPID(DataFile df, String postfix) {
        SeqTrack seq = df.seqTrack
        Sample sample = seq.sample
        Individual individual = sample.individual
        String pid = individual.pid
        String dir = getOutputDirectory(individual, OutputDirectories.fastx)

        String filename = "${dir}${pid}.${filenameEnds[postfix]}"

        return filename
    }
}
