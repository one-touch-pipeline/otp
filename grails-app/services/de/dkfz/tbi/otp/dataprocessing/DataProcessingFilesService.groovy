package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*


class DataProcessingFilesService {

    def configService

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

    private String getOutputRoot(Individual individual) {
        if (!individual) {
            throw new IllegalArgumentException("individual must not be null")
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
        String outputBaseDir = getOutputRoot(individual)
        String postfix = (!dir || dir == OutputDirectories.BASE) ? "" : "${dir.toString().toLowerCase()}/"
        return "${outputBaseDir}/results_per_pid/${individual.pid}/${postfix}"
    }
}
