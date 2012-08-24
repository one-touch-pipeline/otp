package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.common.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*

import sun.reflect.generics.reflectiveObjects.NotImplementedException
import org.codehaus.groovy.grails.commons.GrailsApplication
import grails.util.GrailsUtil

class DataProcessingFilesService {

    ConfigService configService

    def grailsApplication

    public enum OutputDirectories {
        BASE,
        ALIGNMENT,
        COVERAGE,
        FASTX_QC,
        FLAGSTATS,
        INSERTSIZE_DISTRIBUTION,
        SNPCOMP,
        STRUCTURAL_VARIATION
    }

    public enum ScriptFileID {
        Alignment,
        AlignmentSummary,
        ChromosomeDiff,
        Fastqc,
        Fastx,
        GenomeCoverage,
        InsertSizes,
        MergeBamFiles,
        PairedEndReadAberrations,
        QualityDetermination,
        SampeSort,
        SamtoolsIndex,
        SamtoolsFlagstat
    }

    static final HashMap<ProcessedFile.Type, String> filenameEnds = [
        (ProcessedFile.Type.ALIGNED_SEQUENCE): ".sequence.sai",
        (ProcessedFile.Type.ALIGNED_LANE_QUALITY): ".laneQuality.txt",
        (ProcessedFile.Type.ALIGNMENT_SUMMARY_FILE) : "_wroteQcSummary.txt",
        (ProcessedFile.Type.BAM) : ".bam",
        (ProcessedFile.Type.BAM_INDEX): ".bai", //Additional, depends on bam file
        (ProcessedFile.Type.FASTQC_ARCHIVE): "_fastqc.zip", //Replacing something...
        (ProcessedFile.Type.FLAGSTATS): "_flagstats.txt", //Additional, depends on bam file
        (ProcessedFile.Type.GENOME_COVERAGE_FILE): ".DepthOfCoverage.txt",
        (ProcessedFile.Type.INSERTSIZE_DISTRIBUTION_FILE): "_insertsizes.txt",
        (ProcessedFile.Type.INSERTSIZE_DISTRIBUTION_PLOT): "_insertsize_plot.png",
        (ProcessedFile.Type.STRUCTURAL_VARIATION_FILE): "_DiffChroms.txt",
        (ProcessedFile.Type.STRUCTURAL_VARIATION_PLOT): "_DiffChroms.png",
        (ProcessedFile.Type.PAIRED_BAM): "_paired.bam.sorted.bam" //Setting
    ]

    // TODO: Should be put in another place!
    public static final HashMap<ScriptFileID, String> scriptFiles = [
        (ScriptFileID.Alignment) : "align_bwa_new.sh",
        (ScriptFileID.AlignmentSummary) : "writeQCsummary_newFlagstat.py",
        (ScriptFileID.ChromosomeDiff) : "postalignDiffChrom.sh",
        (ScriptFileID.Fastqc) : "fastqc_check.sh",
        (ScriptFileID.Fastx) : "fastx_qc_new.sh",
        (ScriptFileID.GenomeCoverage) : "genome_coverage.sh",
        (ScriptFileID.InsertSizes) : "postalignInsertSize.sh",
        (ScriptFileID.MergeBamFiles) : "merge_new.sh",
        (ScriptFileID.PairedEndReadAberrations) : "pairedEndReadAberrations.py",
        (ScriptFileID.QualityDetermination) : "PhredOrIllumina.pl",
        (ScriptFileID.SampeSort) : "postalignSampeSort.sh",
        (ScriptFileID.SamtoolsIndex) : "postalignSamtoolsIndex.sh",
        (ScriptFileID.SamtoolsFlagstat) : "postalignSamtoolsFlagstat.sh"
    ]

    String getScriptFile(ScriptFileID index, Project project) {
        String scriptDir = getScriptDir(index)
        if (scriptFiles.containsKey(index)) {
            String file = "${scriptDir}/" + scriptFiles[index]
            if ( !(new File(file)).exists() ) {
                throw new IOException("Script ${index} cannot be found at ${file}, scriptDir is ${scriptDir}")
            }
            return file
        }
        throw new IOException("Script ${index} is not specified")
    }

    String getScriptDir(ScriptFileID index, Project project) {
        Realm realm = configService.getRealm(project, Realm.OperationType.DATA_PROCESSING)
        String scriptDir = realm.programsRootPath + "/analysis_tools"
    }

    public String getOutputStem(Individual individual) {
        if (individual == null) {
            throw new RuntimeException("individual must not be null")
        }
        Project project = individual.project
        Realm realm = configService.getRealm(project, Realm.OperationType.DATA_PROCESSING)
        String pdName = project.dirName
        String rpPath = realm.processingRootPath
        String outputBaseDir = rpPath + pdName
        String outputDirectory = "${outputBaseDir}"
        return outputDirectory
    }

    public String getOutputDirectory(Individual individual) {
        return getOutputDirectory(individual, OutputDirectories.BASE)
    }

    public String getOutputDirectory(Individual individual, String dir) {
        return dir ? getOutputDirectory(individual, dir.toUpperCase() as OutputDirectories) : getOutputDirectory(individual)
    }

    public String getOutputDirectory(Individual individual, OutputDirectories dir) {
        String outputBaseDir = getOutputStem(individual)
        String postfix = (dir == OutputDirectories.BASE || dir == null || dir == "" ? "" : "${dir.toString().toLowerCase()}/")
        String outputDirectory = "${outputBaseDir}/results_per_pid/${individual.pid}/${postfix}"
        return outputDirectory
    }

    /**
     * Returns something like [sampleid]_[runid]_[laneid]
     */
    public String getOutfilePrefix(ProcessedFile pf) {
        if (pf.seqTrack != null) {
            RuntimeInfoCollection ri =  new RuntimeInfoCollection(pf.seqTrack)
            return "${ri.sample.type}_${ri.run.name}_${ri.seqTrack.laneId}"
        } else {
            throw new RuntimeException("getOutfilePrefix does not support file type.")
        }
    }

    public String calculateFilenameWithSampleRunLaneAndIndex(DataFile df, ProcessedFile.Type postfix, OutputDirectories subDir) {
        RuntimeInfoCollection ri =  new RuntimeInfoCollection(df)
        String dir = getOutputDirectory(ri.individual, subDir)

        //Get the lane index. As this is not very easy I try a different approach:
        // - Get all datafiles for the seqtrack
        // - Sort them by the filename
        // - Find the index of the current datafile within the sorted array
        // TODO: Does not handle NOINDEX or if no index exists and returns 1 as the index
        // TODO: Also think of sample names!
        List<DataFile> allFiles = DataFile.findAllBySeqTrack(ri.seqTrack, [sort:'fileName'])
        if (allFiles.size() < 1) {
            throw new RuntimeException("A file index can only be calculated if at least one DataFile is avaible (DataFile: ${df.id})")
        }
        int index = 0
        for (DataFile dff in allFiles) {
            index++
            if (dff.id == df.id) {
                break;
            }
        }

        return "${dir}${ri.sample.type}_${ri.run.name}_${ri.seqTrack.laneId}_${index}${filenameEnds[postfix]}"
    }

    public String calculateFilenameWithSampleRunAndLane(DataFile df, ProcessedFile.Type postfix, OutputDirectories subDir) {
        RuntimeInfoCollection ri =  new RuntimeInfoCollection(df)
        String dir = getOutputDirectory(ri.individual, subDir)
        return "${dir}${ri.sample.type}_${ri.run.name}_${ri.seqTrack.laneId}${filenameEnds[postfix]}"
    }

    public String calculateFilenameWithSampleRunAndLane(SeqTrack sq, ProcessedFile.Type postfix, OutputDirectories subDir) {
        RuntimeInfoCollection ri =  new RuntimeInfoCollection(sq)
        String dir = getOutputDirectory(ri.individual, subDir)
        return "${dir}${ri.sample.type}_${ri.run.name}_${ri.seqTrack.laneId}${filenameEnds[postfix]}"
    }

    public String calculateFilenameWithSampleAndRun(DataFile df, ProcessedFile.Type postfix, OutputDirectories subDir) {
        RuntimeInfoCollection ri = new RuntimeInfoCollection(df)
        String dir = getOutputDirectory(ri.individual, subDir)
        return "${dir}${ri.sample.type}_${ri.run.name}${filenameEnds[postfix]}"
    }

    public String calculateFilenameWithSample(DataFile df, ProcessedFile.Type postfix, OutputDirectories subDir) {
        RuntimeInfoCollection ri = new RuntimeInfoCollection(df)
        String dir = getOutputDirectory(ri.individual, subDir)
        return "${dir}${ri.sample.type}.${filenameEnds[postfix]}"
    }

    public String calculateFilenameWithPID(DataFile df, ProcessedFile.Type postfix, OutputDirectories subDir) {
        RuntimeInfoCollection ri = new RuntimeInfoCollection(df)
        String dir = getOutputDirectory(ri.individual, subDir)
        return "${dir}${ri.individual.pid}.${filenameEnds[postfix]}"
    }

    // TODO: Make this for more than .sequence. files.
    public String getPathOfFastqcFile(DataFile df) {
        RuntimeInfoCollection ri =  new RuntimeInfoCollection(df)
        String dir = getOutputDirectory(ri.individual, OutputDirectories.FASTX_QC)
        int ind = df.fileName.indexOf(".")
        String fn = dir + df.fileName[0 .. ind - 1] + filenameEnds[ProcessedFile.Type.FASTQC_ARCHIVE]
        println fn
        int idx = df.fileName.indexOf(".")
        fn = "${dir}/${df.fileName.substring(0, idx)}${filenameEnds[ProcessedFile.Type.FASTQC_ARCHIVE]}"
        println fn
        return fn
    }

    public String getPathOfAlignmentSummaryFile(ProcessedFile pf) {
        if (pf.seqTrack != null) {
            return calculateFilenameWithSampleRunAndLane(pf.seqTrack, ProcessedFile.Type.ALIGNMENT_SUMMARY_FILE, OutputDirectories.BASE)
        }
        throw new RuntimeException("Not supported yet!")
    }

    public String getPathOfAlignedSequenceFile(DataFile df) {
        if (df == null) {
            throw new NullPointerException("File must be specified for ${this.class.name}.getPathOfAlignedSequenceFile")
        }
        return calculateFilenameWithSampleRunLaneAndIndex(df, ProcessedFile.Type.ALIGNED_SEQUENCE, OutputDirectories.ALIGNMENT)
    }

    public String getPathOfLaneQualityFile(DataFile df) {
        if (df == null) {
            throw new NullPointerException("File must be specified for ${this.class.name}.getPathOfLaneQualityFile")
        }
        return calculateFilenameWithSampleRunAndLane(df, ProcessedFile.Type.ALIGNED_LANE_QUALITY, OutputDirectories.ALIGNMENT)
    }

    public String getPathOfBamFile(ProcessedFile bamFile) {
        if (bamFile.seqTrack != null) { //bamFile was built with seqTrack
            return getPathOfPairedBamFile(bamFile.seqTrack)
        }
        // TODO: Fill in
        throw new RuntimeException("Not supported yet!")
    }

    public String getPathOfPairedBamFile(SeqTrack st) {
        return calculateFilenameWithSampleRunAndLane(st, ProcessedFile.Type.PAIRED_BAM, OutputDirectories.ALIGNMENT)
    }

    public String getPathOfPairedBamIndexFile(ProcessedFile pf) {
        SeqTrack st = pf.seqTrack
        if ( st != null) {
            return calculateFilenameWithSampleRunAndLane(st, ProcessedFile.Type.PAIRED_BAM, OutputDirectories.ALIGNMENT) + filenameEnds[ProcessedFile.Type.BAM_INDEX]
        }
        // TODO: Add other cases like merged bam...
        throw new IOException("Not all cases in here... ")
    }

    public String getPathOfFlagstatFile(ProcessedFile pf) {
        SeqTrack st = pf.seqTrack
        if ( st != null) {
            return calculateFilenameWithSampleRunAndLane(st, ProcessedFile.Type.PAIRED_BAM, OutputDirectories.FLAGSTATS) + filenameEnds[ProcessedFile.Type.FLAGSTATS]
        }
        // TODO: Add other cases like merged bam...
        throw new IOException("Not all cases in here... ")
    }

    public String getPathOfStructuralVariationFile(ProcessedFile pf) {
        if (pf.seqTrack != null) {
            String temp = calculateFilenameWithSampleRunAndLane(pf.seqTrack, ProcessedFile.Type.PAIRED_BAM, OutputDirectories.STRUCTURAL_VARIATION)
            return temp + filenameEnds[ProcessedFile.Type.STRUCTURAL_VARIATION_FILE]
        }
        // TODO: Add other cases like merged bam...
        throw new IOException("Not all cases in here... ")
    }

    public String getPathOfStructuralVariationPlot(ProcessedFile pf) {
        if (pf.seqTrack != null) {
            String temp = calculateFilenameWithSampleRunAndLane(pf.seqTrack, ProcessedFile.Type.PAIRED_BAM, OutputDirectories.STRUCTURAL_VARIATION)
            return temp + filenameEnds[ProcessedFile.Type.STRUCTURAL_VARIATION_PLOT]
        }
        // TODO: Add other cases like merged bam...
        throw new IOException("Not all cases in here... ")
    }

    private String getPathOfInsertSizeDistributionPrefix(ProcessedFile pf) {
        if (pf.seqTrack != null) {
            String bamPath = calculateFilenameWithSampleRunAndLane(pf.seqTrack, ProcessedFile.Type.PAIRED_BAM, OutputDirectories.INSERTSIZE_DISTRIBUTION)
            int index = bamPath.indexOf(".")
            return bamPath[0 .. index - 1]
        }
        // TODO: Add other cases like merged bam...
        throw new IOException("Not all cases in here... ")
    }

    public String getPathOfInsertSizeDistributionFile(ProcessedFile pf) {
        return getPathOfInsertSizeDistributionPrefix(pf) + filenameEnds[ProcessedFile.Type.INSERTSIZE_DISTRIBUTION_FILE]
    }

    public String getPathOfInsertSizeDistributionPlot(ProcessedFile pf) {
        return getPathOfInsertSizeDistributionPrefix(pf) + filenameEnds[ProcessedFile.Type.INSERTSIZE_DISTRIBUTION_PLOT]
    }

    public String getPathOfGenomeCoverageFile(ProcessedFile pf) {
        if (pf.seqTrack != null) {
            String covPath = calculateFilenameWithSampleRunAndLane(pf.seqTrack, ProcessedFile.Type.GENOME_COVERAGE_FILE, OutputDirectories.COVERAGE)
            return covPath
        }
        // TODO: Add other cases like merged bam...
        throw new IOException("Not all cases in here... ")
    }

    // TODO: Think of moving the processed file related methods to a custom service
    public ProcessedFile createAndSaveProcessedFile(ProcessedFile.Type pfType, def srcObject, Object creatingObject, Individual individual, MergingLog.Execution executedBy, String outputParameterName = null) {
        ProcessedFile pf = new ProcessedFile(type: pfType, createdByJobClass: creatingObject.class.name, individual: individual, executedBy: executedBy)
        if (outputParameterName != null) {
            pf.jobClassParameterName = outputParameterName
        }
        switch (srcObject.class) {
            case ProcessedFile.class:
                pf.processedFile = (ProcessedFile) srcObject
                break;
            case SeqTrack.class:
                pf.seqTrack = (SeqTrack) srcObject
                break;
            case DataFile.class:
                pf.dataFile = (DataFile) srcObject
                break;
            case Sample.class:
                pf.sample = (Sample) srcObject
                break;
            case Run.class:
                pf.run = (Run) srcObject
                break;
            default:
                throw new RuntimeException("File type ${file.class.name} is not supported within ProcessedFile")
        }
        return pf.save()
    }

    /**
     * Checks if a processed file exists and stores relevant data for this file
     * Returns true if the file exists.
     *
     */
    public boolean checkAndStoreFileExistance(long pfID, String filePath) {
        File f = new File(filePath)
        boolean outFileExists = f.canRead()

        ProcessedFile pf = ProcessedFile.findById(pfID)
        if (pf == null) {
            throw new RuntimeException("ProcessedFile '${filePath}' could not be found")
        }
        pf.fileExists = outFileExists ? ProcessedFile.State.TRUE :  ProcessedFile.State.FALSE
        pf.save(flush: true)

        return outFileExists
    }
}
