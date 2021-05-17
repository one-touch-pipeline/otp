/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator

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
     * This directory is in {@link de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName#BASE_PATH_REFERENCE_GENOME}.
     *
     * suppressing because changing this would involve refactoring the code as well as the database columns
     */
    @SuppressWarnings("GrailsDomainReservedSqlKeywordName")
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
     * The following two files belong to ENCODE (https://genome.ucsc.edu/ENCODE/)
     * and will therefore be stored under the ENCODE folder in each reference genome
     */
    String mappabilityFile

    String replicationTimeFile

    /*
     * The following six files belong to IMPUTE (https://mathgen.stats.ox.ac.uk/impute/impute_v2.html)
     * and will therefore be stored under the IMPUTE folder in each reference genome
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
        path(unique: true, blank: false, shared: "relativePath")
        fileNamePrefix(blank: false, shared: "pathComponent")
        length shared: 'greaterThanZero'
        lengthWithoutN shared: 'greaterThanZero'
        lengthRefChromosomes shared: 'greaterThanZero'
        lengthRefChromosomesWithoutN shared: 'greaterThanZero'
        cytosinePositionsIndex nullable: true, blank: false, shared: "pathComponent"
        chromosomePrefix (blank: true)
        chromosomeSuffix (blank: true)
        chromosomeLengthFilePath(nullable: true, blank: false, shared: "pathComponent")
        fingerPrintingFileName nullable: true
        gcContentFile (nullable: true, blank: false, shared: "pathComponent")
        mappabilityFile (nullable: true, maxSize: 500, blank: false, shared: "absolutePath")
        replicationTimeFile (nullable: true, maxSize: 500, blank: false, shared: "absolutePath")
        geneticMapFile (nullable: true, maxSize: 500, validator: {
            it == null || OtpPathValidator.isValidAbsolutePathContainingVariable(it)
        })
        knownHaplotypesFile (nullable: true, maxSize: 500, validator: {
            it == null || OtpPathValidator.isValidAbsolutePathContainingVariable(it)
        })
        knownHaplotypesLegendFile (nullable: true, maxSize: 500, validator: {
            it == null || OtpPathValidator.isValidAbsolutePathContainingVariable(it)
        })
        geneticMapFileX (nullable: true, maxSize: 500, blank: false, shared: "absolutePath")
        knownHaplotypesFileX (nullable: true, maxSize: 500, blank: false, shared: "absolutePath")
        knownHaplotypesLegendFileX (nullable: true, maxSize: 500, blank: false, shared: "absolutePath")
    }

    static hasMany = [
        referenceGenomeIndexes: ReferenceGenomeIndex,
    ]

    @Override
    String toString() {
        name
    }
}
