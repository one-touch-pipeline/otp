package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import groovy.transform.*
import org.springframework.security.access.prepost.*

import static org.springframework.util.Assert.*


class ReferenceGenomeService {

    ConfigService configService

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
     * returns path to the directory storing files for the given reference genome depending on project
     * @param reference genome the reference genome for which the directory path is created
     * @param project the project, which belongs to the reference genome
     */
    public String filePathToDirectory(Project project, ReferenceGenome referenceGenome, boolean checkExistence = true) {
        notNull(project, "The project is not specified")
        notNull(referenceGenome, "The reference genome is not specified")
        Realm realm = configService.getRealmDataProcessing(project)
        assert realm : "Realm not found for project ${project}"
        filePathToDirectory(realm, referenceGenome, checkExistence)
    }

    /**
     * @param realm - directory path is created relatively to this {@link Realm.OperationType#DATA_PROCESSING} realm
     * @param referenceGenome - the reference genome for which the directory path is created
     * @return path to a directory storing files for the given reference genome on the given realm
     */
    public String filePathToDirectory(Realm realm, ReferenceGenome referenceGenome, boolean checkExistence = true) {
        notNull(realm, "realm is not specified")
        notNull(referenceGenome, "The reference genome is not specified")
        isTrue(realm.operationType == Realm.OperationType.DATA_PROCESSING)
        String realmSpecificPath = realm.processingRootPath
        final String allReferenceGenomes = "reference_genomes"
        String referenceGenomeSpecificPath = referenceGenome.path
        String directoryPath = "${realmSpecificPath}/${allReferenceGenomes}/${referenceGenomeSpecificPath}/"
        File file = new File(directoryPath)
        return "${checkFileExistence(file, checkExistence)}/"
    }

    /**
     * returns path to the directory storing given reference genome files followed by the common file name prefix depending on the project
     * @param reference genome the reference genome for which the path and the prefix are created
     * @param project the project, which belongs to the reference genome
     */
    public String prefixOnlyFilePath(Project project, ReferenceGenome referenceGenome, boolean checkExistence = true) {
        notNull(project, "The project is not specified")
        notNull(referenceGenome, "The reference genome is not specified")
        String refGenomeFileNamePrefix = referenceGenome.fileNamePrefix
        return filePathToDirectory(project, referenceGenome, checkExistence) + "${refGenomeFileNamePrefix}"
    }

    public File fingerPrintingFile(Project project, ReferenceGenome referenceGenome, boolean checkExistence = true) {
        File referenceGenomeBasePath = new File(prefixOnlyFilePath(project, referenceGenome, checkExistence))
        File fingerPrintingFile = new File(referenceGenomeBasePath, "${FINGER_PRINTING_FILE_FOLDER_NAME}/${referenceGenome.fingerPrintingFileName}")
        return checkFileExistence(fingerPrintingFile, checkExistence)
    }

    /**
     * returns the path to the fasta file for the given reference genome depending on project
     * @param the reference genome for which the file path is created and the belonging project
     */
    public String fastaFilePath(Project project, ReferenceGenome referenceGenome, boolean checkExistence = true) {
        String referenceGenomeFastaFilePath = prefixOnlyFilePath(project, referenceGenome, checkExistence) + ".fa"
        File file = new File(referenceGenomeFastaFilePath)
        return checkFileExistence(file, checkExistence)
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
    public File cytosinePositionIndexFilePath(Project project, ReferenceGenome referenceGenome) {
        assert referenceGenome.cytosinePositionsIndex : "cytosinePositionsIndex is not set"
        File file = new File(filePathToDirectory(project, referenceGenome), referenceGenome.cytosinePositionsIndex)
        return checkFileExistence(file, true)
    }

    /**
     * returns the path to the file containing the reference genome meta information (names, length values)
     */
    public String referenceGenomeMetaInformationPath(Realm realm, ReferenceGenome referenceGenome) {
        notNull(realm, "The input realm of the method referenceGenomeMetaInformationPath is null")
        notNull(referenceGenome, "The input referenceGenome of the method referenceGenomeMetaInformationPath is null")
        return filePathToDirectory(realm, referenceGenome) + "metaInformation.txt"
    }

    public File pathToChromosomeSizeFilesPerReference(Project project, ReferenceGenome referenceGenome, boolean checkExistence = true) {
        notNull(project, "The project is not specified")
        notNull(referenceGenome, "The reference genome is not specified")
        File file = new File(filePathToDirectory(project, referenceGenome, checkExistence), CHROMOSOME_SIZE_FILES_PREFIX)
        return checkFileExistence(file, checkExistence)
    }

    public File chromosomeStatSizeFile(MergingWorkPackage mergingWorkPackage, boolean checkExistence = true) {
        assert mergingWorkPackage, "The mergingWorkPackage is not specified"
        assert mergingWorkPackage.statSizeFileName : "No stat file size name is defined for ${mergingWorkPackage}"
        File file = new File(pathToChromosomeSizeFilesPerReference(mergingWorkPackage.project, mergingWorkPackage.referenceGenome, checkExistence), mergingWorkPackage.statSizeFileName)
        return checkFileExistence(file, checkExistence)
    }

    public File chromosomeLengthFile(MergingWorkPackage mergingWorkPackage, boolean checkExistence = true) {
        assert mergingWorkPackage, "The mergingWorkPackage is not specified"
        assert mergingWorkPackage.referenceGenome.chromosomeLengthFilePath : "No chromosome length file path is defined for ${mergingWorkPackage}"
        File file = new File(pathToChromosomeSizeFilesPerReference(mergingWorkPackage.project, mergingWorkPackage.referenceGenome, checkExistence), mergingWorkPackage.referenceGenome.chromosomeLengthFilePath)
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
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public void loadReferenceGenome(String name, String path, String fileNamePrefix, String cytosinePositionsIndex,
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
        ).save(flush: true, failOnError: true)


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
}

@TupleConstructor
class FastaEntry {
    String name
    String alias
    long length
    long lengthWithoutN
    Classification classification
}
