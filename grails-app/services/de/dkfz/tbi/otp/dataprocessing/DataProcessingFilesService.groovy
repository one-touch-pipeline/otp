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
