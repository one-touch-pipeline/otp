/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata.referencegenome

import grails.gorm.transactions.Transactional
import groovy.transform.TupleConstructor
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import de.dkfz.tbi.otp.ngsdata.StatSizeFileName
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesCommonName
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator

import java.nio.file.FileSystem
import java.nio.file.Path

import static org.springframework.util.Assert.notNull

@Transactional
class ReferenceGenomeService {

    ConfigService configService
    FileService fileService
    FileSystemService fileSystemService
    ProcessingOptionService processingOptionService

    final static String CHROMOSOME_SIZE_FILES_PREFIX = "stats"
    final static String FINGER_PRINTING_FILE_FOLDER_NAME = "fingerPrinting"

    /**
     * load the {@link ReferenceGenome} with the given id from the database and returns it.
     *
     * @param id the id of the {@link ReferenceGenome} to load
     * @return the loaded {@link ReferenceGenome} or <code>null</code>, if not founded
     */
    ReferenceGenome referenceGenome(long id) {
        return ReferenceGenome.get(id)
    }

    ReferenceGenome findByName(String name) {
        return CollectionUtils.atMostOneElement(ReferenceGenome.findAllByName(name))
    }

    List<ReferenceGenome> list() {
        return ReferenceGenome.list(sort: "name", order: "asc")
    }

    /**
     * @param referenceGenome the reference genome for which the directory path is created
     * @return path to a directory storing files for the given reference genome
     */
    File referenceGenomeDirectory(ReferenceGenome referenceGenome, boolean checkExistence = true) {
        notNull referenceGenome, "The reference genome is not specified"
        String path = processingOptionService.findOptionAsString(OptionName.BASE_PATH_REFERENCE_GENOME)
        assert OtpPathValidator.isValidAbsolutePath(path)
        return checkFileExistence(new File(path, referenceGenome.path), checkExistence)
    }

    File fingerPrintingFile(ReferenceGenome referenceGenome, boolean checkExistence = true) {
        File referenceGenomeBasePath = referenceGenomeDirectory(referenceGenome, checkExistence)
        File fingerPrintingFile = new File(referenceGenomeBasePath, "${FINGER_PRINTING_FILE_FOLDER_NAME}/${referenceGenome.fingerPrintingFileName}")
        return checkFileExistence(fingerPrintingFile, checkExistence)
    }

    /**
     * returns the path to the fasta file for the given reference genome
     * @param the reference genome for which the file path is created
     */
    File fastaFilePath(ReferenceGenome referenceGenome, boolean checkExistence = true) {
        return checkFileExistence(new File(referenceGenomeDirectory(referenceGenome, checkExistence),
                "${referenceGenome.fileNamePrefix}.fa"), checkExistence)
    }

    /**
     * returns the entries in the reference genome, which belong to a chromosome
     * @param referenceGenome , the reference genome, for which the chromosomes shall be returned
     */
    List<ReferenceGenomeEntry> chromosomesInReferenceGenome(ReferenceGenome referenceGenome) {
        notNull(referenceGenome, "the referenceGenome in method chromosomesInReferenceGenome is null")
        Classification classification = Classification.CHROMOSOME
        return ReferenceGenomeEntry.findAllByReferenceGenomeAndClassification(referenceGenome, classification)
    }

    /**
     * returns the path to the cytosine position index file for the given reference genome depending on project
     * @param the reference genome for which the file path is created and the belonging project
     */
    File cytosinePositionIndexFilePath(ReferenceGenome referenceGenome) {
        assert referenceGenome.cytosinePositionsIndex: "cytosinePositionsIndex is not set"
        File file = new File(referenceGenomeDirectory(referenceGenome), referenceGenome.cytosinePositionsIndex)
        return checkFileExistence(file, true)
    }

    /**
     * returns the path to the file containing the reference genome meta information (names, length values)
     */
    File referenceGenomeMetaInformationPath(ReferenceGenome referenceGenome) {
        notNull(referenceGenome, "The input referenceGenome of the method referenceGenomeMetaInformationPath is null")
        return new File(referenceGenomeDirectory(referenceGenome), "metaInformation.txt")
    }

    File pathToChromosomeSizeFilesPerReference(ReferenceGenome referenceGenome, boolean checkExistence = true) {
        notNull(referenceGenome, "The reference genome is not specified")
        File file = new File(referenceGenomeDirectory(referenceGenome, checkExistence), CHROMOSOME_SIZE_FILES_PREFIX)
        return checkFileExistence(file, checkExistence)
    }

    File chromosomeStatSizeFile(MergingWorkPackage mergingWorkPackage, boolean checkExistence = true) {
        assert mergingWorkPackage, "The mergingWorkPackage is not specified"
        assert mergingWorkPackage.statSizeFileName: "No stat file size name is defined for ${mergingWorkPackage}"
        File file = new File(pathToChromosomeSizeFilesPerReference(mergingWorkPackage.referenceGenome, checkExistence), mergingWorkPackage.statSizeFileName)
        return checkFileExistence(file, checkExistence)
    }

    File chromosomeLengthFile(AbstractMergingWorkPackage mergingWorkPackage, boolean checkExistence = true) {
        assert mergingWorkPackage, "The mergingWorkPackage is not specified"
        assert mergingWorkPackage.referenceGenome.chromosomeLengthFilePath: "No chromosome length file path is defined for ${mergingWorkPackage}"
        File file = new File(pathToChromosomeSizeFilesPerReference(mergingWorkPackage.referenceGenome, checkExistence),
                mergingWorkPackage.referenceGenome.chromosomeLengthFilePath)
        return checkFileExistence(file, checkExistence)
    }

    File gcContentFile(AbstractMergingWorkPackage mergingWorkPackage, boolean checkExistence = true) {
        assert mergingWorkPackage, "The mergingWorkPackage is not specified"
        assert mergingWorkPackage.referenceGenome.gcContentFile: "No gc content file path is defined for ${mergingWorkPackage}"
        File file = new File(pathToChromosomeSizeFilesPerReference(mergingWorkPackage.referenceGenome, checkExistence),
                mergingWorkPackage.referenceGenome.gcContentFile)
        return checkFileExistence(file, checkExistence)
    }

    private File checkFileExistence(File file, boolean checkExistence) {
        if (!checkExistence || file.canRead()) {
            return file
        }
        throw new RuntimeException("${file} can not be read")
    }

    /**
     * This method is used to import new {@link ReferenceGenome}s and the corresponding {@link ReferenceGenomeEntry}s
     * and {@link StatSizeFileName}s.
     *
     * @param fastaEntries use 'scripts/ReferenceGenome/getReferenceGenomeInfo.py' to create the values for this parameter
     * @param cytosinePositionsIndex only for methylCtools processed reference genomes
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void loadReferenceGenome(String name, Set<SpeciesCommonName> species, String path, String fileNamePrefix, String cytosinePositionsIndex,
                             String chromosomePrefix, String chromosomeSuffix,
                             List<FastaEntry> fastaEntries, String fingerPrintingFileName, List<String> statSizeFileNames) {
        // get list of all standard chromosomes (1...22, X, Y)
        List<String> standardChromosomes = Chromosomes.allLabels()
        standardChromosomes.remove("M")
        assert standardChromosomes.size() == 24

        // calculate length values
        long length = 0
        long lengthWithoutN = 0
        long lengthRefChromosomes = 0
        long lengthRefChromosomesWithoutN = 0
        fastaEntries.each { entry ->
            // overall count
            length += entry.length
            lengthWithoutN += entry.lengthWithoutN
            // count if entry is a standard chromosome
            if (standardChromosomes.contains(entry.alias)) {
                lengthRefChromosomes += entry.length
                lengthRefChromosomesWithoutN += entry.lengthWithoutN
            }
        }

        ReferenceGenome referenceGenome = new ReferenceGenome(
                name: name,
                species: species,
                path: path,
                fileNamePrefix: fileNamePrefix,
                cytosinePositionsIndex: cytosinePositionsIndex,
                length: length,
                lengthWithoutN: lengthWithoutN,
                lengthRefChromosomes: lengthRefChromosomes,
                lengthRefChromosomesWithoutN: lengthRefChromosomesWithoutN,
        )
        referenceGenome.chromosomePrefix = chromosomePrefix
        referenceGenome.chromosomeSuffix = chromosomeSuffix
        referenceGenome.fingerPrintingFileName = fingerPrintingFileName
        referenceGenome.save(flush: true)

        fastaEntries.each { entry ->
            new ReferenceGenomeEntry(
                    name: entry.name,
                    alias: entry.alias,
                    length: entry.length,
                    lengthWithoutN: entry.lengthWithoutN,
                    classification: entry.classification,
                    referenceGenome: referenceGenome,
            ).save(flush: true)
        }

        statSizeFileNames.each { String fileName ->
            new StatSizeFileName(
                    name: fileName,
                    referenceGenome: referenceGenome
            ).save(flush: true)
        }

        createReferenceGenomeMetafile(referenceGenome)
    }

    /**
     * Method to create a tsv file including the reference genome meta information:
     * reference genome entry names
     * length of reference genome entry
     * lengthWithoutN of reference genome entry
     *
     * The create tsv file is required by qa.jar
     */
    void createReferenceGenomeMetafile(ReferenceGenome referenceGenome) {
        FileSystem fileSystem = fileSystemService.remoteFileSystemOnDefaultRealm
        Path path = fileSystem.getPath(referenceGenomeMetaInformationPath(referenceGenome).absolutePath)

        String content = ReferenceGenomeEntry.findAllByReferenceGenome(referenceGenome).collect { ReferenceGenomeEntry referenceGenomeEntry ->
            return [
                    referenceGenomeEntry.name,
                    referenceGenomeEntry.length,
                    referenceGenomeEntry.lengthWithoutN,
            ].join("\t")
        }.join("\n")

        fileService.createFileWithContentOnDefaultRealm(path, content)
    }

    void checkReferenceGenomeFilesAvailability(AbstractMergingWorkPackage mergingWorkPackage) {
        [
                new File(mergingWorkPackage.referenceGenome.mappabilityFile),
                new File(mergingWorkPackage.referenceGenome.replicationTimeFile),
                gcContentFile(mergingWorkPackage),
                new File(mergingWorkPackage.referenceGenome.geneticMapFileX),
                new File(mergingWorkPackage.referenceGenome.knownHaplotypesFileX),
                new File(mergingWorkPackage.referenceGenome.knownHaplotypesLegendFileX),
        ].each {
            LsdfFilesService.ensureFileIsReadableAndNotEmpty(it)
        }

        [
                new File(mergingWorkPackage.referenceGenome.geneticMapFile).parentFile,
                new File(mergingWorkPackage.referenceGenome.knownHaplotypesFile).parentFile,
                new File(mergingWorkPackage.referenceGenome.knownHaplotypesLegendFile).parentFile,
        ].each {
            LsdfFilesService.ensureDirIsReadableAndNotEmpty(it)
        }
    }

    static List<StatSizeFileName> getStatSizeFileNames(ReferenceGenome referenceGenome) {
        return StatSizeFileName.findAllByReferenceGenome(referenceGenome, [sort: "name", order: "asc"])
    }
}

@TupleConstructor
class FastaEntry {
    String name
    String alias
    long length
    long lengthWithoutN
    Classification classification
}
