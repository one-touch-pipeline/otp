package de.dkfz.tbi.otp.ngsdata

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification

class ReferenceGenomeService {

    ConfigService configService

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
     * returns the reference genome depending on the project and the sequencing type
     * @param project the project, to which the reference genome belongs to
     * @param seqType the sequencing type, to which the reference genome belongs to
     * @deprecated The {@link ReferenceGenome} will not depend on {@link Project} and {@link SeqType} alone in the
     * future (see OTP-905 for example). Use {@link SeqTrack#getConfiguredReferenceGenome()} instead.
     */
    @Deprecated
    public ReferenceGenome referenceGenome(Project project, SeqType seqType) {
        notNull(project, "the project of the method referenceGenome is null")
        notNull(seqType, "the seqType of the method referenceGenome is null")
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.
                findByProjectAndSeqTypeAndDeprecatedDateIsNull(project, seqType)
        notNull(referenceGenomeProjectSeqType,
                "There is no reference genome defined for the combination of project ${project} and seqType ${seqType}")
        return referenceGenomeProjectSeqType.referenceGenome
    }

    /**
     * returns path to the directory storing files for the given reference genome depending on project
     * @param reference genome the reference genome for which the directory path is created
     * @param project the project, which belongs to the reference genome
     */
    public String filePathToDirectory(Project project, ReferenceGenome referenceGenome) {
        notNull(project, "The project is not specified")
        notNull(referenceGenome, "The reference genome is not specified")
        Realm realm = configService.getRealmDataProcessing(project)
        filePathToDirectory(realm, referenceGenome)
    }

    /**
     * @param realm - directory path is created relatively to this {@link Realm.OperationType.DATA_PROCESSING} realm
     * @param referenceGenome - the reference genome for which the directory path is created
     * @return path to a directory storing files for the given reference genome on the given realm
     */
    public String filePathToDirectory(Realm realm, ReferenceGenome referenceGenome) {
        notNull(realm, "realm is not specified")
        notNull(referenceGenome, "The reference genome is not specified")
        isTrue(realm.operationType == Realm.OperationType.DATA_PROCESSING)
        String realmSpecificPath = realm.processingRootPath
        final String allReferenceGenomes = "reference_genomes"
        String referenceGenomeSpecificPath = referenceGenome.path
        String directoryPath = "/${realmSpecificPath}/${allReferenceGenomes}/${referenceGenomeSpecificPath}/"
        File file = new File(directoryPath)
        if (file.canRead()) {
            return directoryPath
        } else {
            throw new RuntimeException(
            "The directory ${directoryPath} storing the information of the reference genome can not be read")
        }
    }

    /**
     * returns path to the directory storing given reference genome files followed by the common file name prefix depending on the project
     * @param reference genome the reference genome for which the path and the prefix are created
     * @param project the project, which belongs to the reference genome
     */
    public String prefixOnlyFilePath(Project project, ReferenceGenome referenceGenome) {
        notNull(project, "The project is not specified")
        notNull(referenceGenome, "The reference genome is not specified")
        String refGenomeFileNamePrefix = referenceGenome.fileNamePrefix
        return filePathToDirectory(project, referenceGenome) + "${refGenomeFileNamePrefix}"
    }

    /**
     * returns the path to the fasta file for the given reference genome depending on project
     * @param the reference genome for which the file path is created and the belonging project
     */
    public String fastaFilePath(Project project, ReferenceGenome referenceGenome) {
        String referenceGenomeFastaFilePath = prefixOnlyFilePath(project, referenceGenome) + ".fa"
        File file = new File(referenceGenomeFastaFilePath)
        if (file.canRead()) {
            return referenceGenomeFastaFilePath
        } else {
            throw new RuntimeException(
            "The fasta file ${referenceGenomeFastaFilePath} storing the information of the reference genome can not be read")
        }
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
     * returns the path to the file containing the reference genome meta information (names, length values)
     */
    public String referenceGenomeMetaInformationPath(Realm realm, ReferenceGenome referenceGenome) {
        notNull(realm, "The input realm of the method referenceGenomeMetaInformationPath is null")
        notNull(referenceGenome, "The input referenceGenome of the method referenceGenomeMetaInformationPath is null")
        return filePathToDirectory(realm, referenceGenome) + "metaInformation.txt"
    }
}
