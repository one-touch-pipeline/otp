package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import groovy.transform.*
import org.springframework.security.access.prepost.*

import static org.springframework.util.Assert.*


class ReferenceGenomeService {

    ConfigService configService
    ProcessingOptionService processingOptionService

    public final static String CHROMOSOME_SIZE_FILES_PREFIX = "stats"
    public final static String FINGER_PRINTING_FILE_FOLDER_NAME = "fingerPrinting"

    /**
     * load the {@link ReferenceGenome} with the given id from the database and returns it.
     *
     * @param id the id of the {@link ReferenceGenome} to load
     * @return the loaded {@link ReferenceGenome} or <code>null</code>, if not founded
     */
    public ReferenceGenome referenceGenome(long id) {
        return ReferenceGenome.get(id)
    }

    /**
     * @param referenceGenome the reference genome for which the directory path is created
     * @return path to a directory storing files for the given reference genome
     */
    public File referenceGenomeDirectory(ReferenceGenome referenceGenome, boolean checkExistence = true) {
        notNull referenceGenome, "The reference genome is not specified"
        String path = processingOptionService.getValueOfProcessingOption(OptionName.BASE_PATH_REFERENCE_GENOME)
        assert OtpPath.isValidAbsolutePath(path)
        return checkFileExistence(new File(path, referenceGenome.path), checkExistence)
    }

    public File fingerPrintingFile(ReferenceGenome referenceGenome, boolean checkExistence = true) {
        File referenceGenomeBasePath = referenceGenomeDirectory(referenceGenome, checkExistence)
        File fingerPrintingFile = new File(referenceGenomeBasePath, "${FINGER_PRINTING_FILE_FOLDER_NAME}/${referenceGenome.fingerPrintingFileName}")
        return checkFileExistence(fingerPrintingFile, checkExistence)
    }

    /**
     * returns the path to the fasta file for the given reference genome
     * @param the reference genome for which the file path is created
     */
    public File fastaFilePath(ReferenceGenome referenceGenome, boolean checkExistence = true) {
        return checkFileExistence(new File(referenceGenomeDirectory(referenceGenome, checkExistence),
                "${referenceGenome.fileNamePrefix}.fa"), checkExistence)
    }

    /**
     * returns the entries in the reference genome, which belong to a chromosome
     * @param referenceGenome, the reference genome, for which the chromosomes shall be returned
     */
    public List<ReferenceGenomeEntry> chromosomesInReferenceGenome(ReferenceGenome referenceGenome) {
        notNull(referenceGenome, "the referenceGenome in method chromosomesInReferenceGenome is null")
        Classification classification = Classification.CHROMOSOME
        return ReferenceGenomeEntry.findAllByReferenceGenomeAndClassification(referenceGenome, classification)
    }

    /**
     * returns the path to the cytosine position index file for the given reference genome depending on project
     * @param the reference genome for which the file path is created and the belonging project
     */
    public File cytosinePositionIndexFilePath(ReferenceGenome referenceGenome) {
        assert referenceGenome.cytosinePositionsIndex : "cytosinePositionsIndex is not set"
        File file = new File(referenceGenomeDirectory(referenceGenome), referenceGenome.cytosinePositionsIndex)
        return checkFileExistence(file, true)
    }

    /**
     * returns the path to the file containing the reference genome meta information (names, length values)
     */
    public File referenceGenomeMetaInformationPath(ReferenceGenome referenceGenome) {
        notNull(referenceGenome, "The input referenceGenome of the method referenceGenomeMetaInformationPath is null")
        return new File(referenceGenomeDirectory(referenceGenome), "metaInformation.txt")
    }

    public File pathToChromosomeSizeFilesPerReference(ReferenceGenome referenceGenome, boolean checkExistence = true) {
        notNull(referenceGenome, "The reference genome is not specified")
        File file = new File(referenceGenomeDirectory(referenceGenome, checkExistence), CHROMOSOME_SIZE_FILES_PREFIX)
        return checkFileExistence(file, checkExistence)
    }

    public File chromosomeStatSizeFile(MergingWorkPackage mergingWorkPackage, boolean checkExistence = true) {
        assert mergingWorkPackage, "The mergingWorkPackage is not specified"
        assert mergingWorkPackage.statSizeFileName : "No stat file size name is defined for ${mergingWorkPackage}"
        File file = new File(pathToChromosomeSizeFilesPerReference(mergingWorkPackage.referenceGenome, checkExistence), mergingWorkPackage.statSizeFileName)
        return checkFileExistence(file, checkExistence)
    }

    public File chromosomeLengthFile(MergingWorkPackage mergingWorkPackage, boolean checkExistence = true) {
        assert mergingWorkPackage, "The mergingWorkPackage is not specified"
        assert mergingWorkPackage.referenceGenome.chromosomeLengthFilePath : "No chromosome length file path is defined for ${mergingWorkPackage}"
        File file = new File(pathToChromosomeSizeFilesPerReference(mergingWorkPackage.referenceGenome, checkExistence), mergingWorkPackage.referenceGenome.chromosomeLengthFilePath)
        return checkFileExistence(file, checkExistence)
    }

    public File gcContentFile(MergingWorkPackage mergingWorkPackage, boolean checkExistence = true) {
        assert mergingWorkPackage, "The mergingWorkPackage is not specified"
        assert mergingWorkPackage.referenceGenome.gcContentFile : "No gc content file path is defined for ${mergingWorkPackage}"
        File file = new File(pathToChromosomeSizeFilesPerReference(mergingWorkPackage.referenceGenome, checkExistence), mergingWorkPackage.referenceGenome.gcContentFile)
        return checkFileExistence(file, checkExistence)
    }


    private File checkFileExistence(File file, boolean checkExistence) {
        if (!checkExistence || file.canRead()) {
            return file
        } else {
            throw new RuntimeException("${file} can not be read")
        }
    }

    /**
     * This method is used to import new {@link ReferenceGenome}s and the corresponding {@link ReferenceGenomeEntry}s
     * and {@link StatSizeFileName}s.
     *
     * @param fastaEntries use 'scripts/ReferenceGenome/getReferenceGenomeInfo.py' to create the values for this parameter
     * @param cytosinePositionsIndex only for methylCtools processed reference genomes
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public void loadReferenceGenome(String name, String path, String fileNamePrefix, String cytosinePositionsIndex, String chromosomePrefix, String chromosomeSuffix,
                                    List<FastaEntry> fastaEntries, List<String> statSizeFileNames) {
        // get list of all standard chromosomes (1…22, X, Y)
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
        referenceGenome.save(flush: true, failOnError: true)


        fastaEntries.each { entry ->
            new ReferenceGenomeEntry(
                    name: entry.name,
                    alias: entry.alias,
                    length: entry.length,
                    lengthWithoutN: entry.lengthWithoutN,
                    classification: entry.classification,
                    referenceGenome: referenceGenome,
            ).save(flush: true, failOnError: true)
        }

        statSizeFileNames.each { String fileName ->
            new StatSizeFileName(
                    name: fileName,
                    referenceGenome: referenceGenome
            ).save(flush: true, failOnError: true)
        }
    }

    void checkReferenceGenomeFilesAvailability(MergingWorkPackage mergingWorkPackage) {
        [
                new File(mergingWorkPackage.referenceGenome.mappabilityFile),
                new File(mergingWorkPackage.referenceGenome.replicationTimeFile),
                gcContentFile(mergingWorkPackage),
                new File(mergingWorkPackage.referenceGenome.geneticMapFileX),
                new File(mergingWorkPackage.referenceGenome.knownHaplotypesFileX),
                new File(mergingWorkPackage.referenceGenome.knownHaplotypesLegendFileX)

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
}

@TupleConstructor
class FastaEntry {
    String name
    String alias
    long length
    long lengthWithoutN
    Classification classification
}
