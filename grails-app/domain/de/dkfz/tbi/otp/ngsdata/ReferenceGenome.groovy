package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.utils.Entity

import java.util.regex.Pattern

class ReferenceGenome implements Entity {

    /**
     * Name of a reference genome.
     * One human genome can have many versions (e.g. thousandGenomes is a version of hg_*).
     * Information about genome and version is not important for processing,
     * each new reference-genome-file in our case defines a new genome.
     * If needed we can add information about version later on.
     * Normally the name reflects the information about the genome and the version.
     */
    String name

    /**
     * Reference genome specific directory
     *
     * The complete path to the files containing reference genome information consists of 4 parts:
     * ${realmSpecificPath}: depends on the realm and must be generated with some logic.
     * ${allReferenceGenomes}: directory containing all reference genomes at this ${realmSpecificPath}.
     * ${referenceGenomeSpecificPath}: directory, which is specific for the reference genome (is stored in this property).
     * ${refGenomeFileNamePrefix}.*: prefix name of all files, which belong to one reference genome.
     */
    String path

    /**
     * Reference genome information is represented with several files, having the same file name prefix but different extensions.
     * This property stores such a common file name prefix.
     * It is the last part of the complete path to the reference genome files (${refGenomeFileNamePrefix})
     */
    String fileNamePrefix

    /**
     * Total genome size in base pairs.
     * Is calculated as the sum of the lengths of all DNA entries.
     */
    long length

    /**
     * Total genome size in base pairs calculated as
     * the sum of the lengthWithoutN of all DNA entries.
     */
    long lengthWithoutN

    /**
     * Number of base pairs of all chromosomes.
     * Is calculated as the sum of all chromosome lengths.
     */
    long lengthRefChromosomes

    /**
     * Number of base pairs of all chromosomes calculated as
     * the sum of all chromosomes lengthWithoutN.
     */
    long lengthRefChromosomesWithoutN

    /**
     * Path of the file which contains information about the length of chromosomes
     */
    String chromosomeLengthFilePath

    /**
     * File which contains the GC content. Is also located in the stats directory per reference genome
     */
    String gcContentFile

    /**
     * File name of cytosine positions index
     */
    String cytosinePositionsIndex

    String chromosomePrefix

    String chromosomeSuffix

    String fingerPrintingFileName


    /*
     * The following two files belong to ENCODE (https://genome.ucsc.edu/ENCODE/) and will therefore be stored under the ENCODE folder in each reference genome
     */
    String mappabilityFile

    String replicationTimeFile

    /*
     * The following six files belong to IMPUTE (https://mathgen.stats.ox.ac.uk/impute/impute_v2.html) and will therefore be stored under the IMPUTE folder in each reference genome
     */

    String geneticMapFile

    String knownHaplotypesFile

    String knownHaplotypesLegendFile

    String geneticMapFileX

    String knownHaplotypesFileX

    String knownHaplotypesLegendFileX


    /**
     * It has to be ensured that there is only one reference genome stored per directory -> unique path
     */
    static constraints = {
        name(unique: true, blank: false)
        path(unique: true, blank: false, validator: { OtpPath.isValidRelativePath(it) })
        fileNamePrefix(blank: false, validator: { OtpPath.isValidPathComponent(it) })
        length shared: 'greaterThanZero'
        lengthWithoutN shared: 'greaterThanZero'
        lengthRefChromosomes shared: 'greaterThanZero'
        lengthRefChromosomesWithoutN shared: 'greaterThanZero'
        cytosinePositionsIndex nullable: true, blank: false,
                validator: { !it || OtpPath.isValidPathComponent(it) }
        chromosomePrefix (blank: true)
        chromosomeSuffix (blank: true)
        chromosomeLengthFilePath(nullable: true, blank: false, validator: { it == null || OtpPath.isValidPathComponent(it) })
        fingerPrintingFileName nullable: true
        gcContentFile (nullable: true, validator: { it == null || OtpPath.isValidPathComponent(it) })
        mappabilityFile (nullable: true, maxSize: 500, validator: { it == null || OtpPath.isValidAbsolutePath(it) })
        replicationTimeFile (nullable: true, maxSize: 500, validator: { it == null || OtpPath.isValidAbsolutePath(it) })
        geneticMapFile (nullable: true, maxSize: 500, validator: { it == null || isValidAbsolutePathContainingVariable(it) })
        knownHaplotypesFile (nullable: true, maxSize: 500, validator: { it == null || isValidAbsolutePathContainingVariable(it) })
        knownHaplotypesLegendFile (nullable: true, maxSize: 500, validator: { it == null || isValidAbsolutePathContainingVariable(it) })
        geneticMapFileX (nullable: true, maxSize: 500, validator: { it == null || OtpPath.isValidAbsolutePath(it) })
        knownHaplotypesFileX (nullable: true, maxSize: 500, validator: { it == null || OtpPath.isValidAbsolutePath(it) })
        knownHaplotypesLegendFileX (nullable: true, maxSize: 500, validator: { it == null || OtpPath.isValidAbsolutePath(it) })
    }

    String toString() {
        name
    }

    List<StatSizeFileName> getStatSizeFileNames() {
        return StatSizeFileName.findAllByReferenceGenome(this, [sort: "name", order: "asc"])
    }

    static boolean isValidAbsolutePathContainingVariable(String string) {
        String pathComponentRegex = /[a-zA-Z0-9_\-\+\.\$\{\}]+/
        return Pattern.compile(/^(?:\/${pathComponentRegex})+$/).matcher(string).matches() && !OtpPath.ILLEGAL_IN_NORMALIZED_PATH.matcher(string).find()
    }
}
