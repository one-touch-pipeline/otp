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

    private String getOutputStem(Individual individual) {
        if (!individual) {
            throw new RuntimeException("individual must not be null")
        }
        Project project = individual.project
        Realm realm = configService.getRealmDataProcessing(project)
        String rpPath = realm.processingRootPath
        String pdName = project.dirName
        return "${rpPath}/${pdName}"
    }

    public String getOutputDirectory(Individual individual) {
        return getOutputDirectory(individual, OutputDirectories.BASE)
    }

    public String getOutputDirectory(Individual individual, String dir) {
        return dir ? getOutputDirectory(individual, dir.toUpperCase() as OutputDirectories) : getOutputDirectory(individual)
    }

    public String getOutputDirectory(Individual individual, OutputDirectories dir) {
        String outputBaseDir = getOutputStem(individual)
        String postfix = (!dir || dir == OutputDirectories.BASE) ? "" : "${dir.toString().toLowerCase()}/"
        String outputDirectory = "${outputBaseDir}/results_per_pid/${individual.pid}/${postfix}"
        return outputDirectory
    }
}
