package de.dkfz.tbi.otp.dataprocessing

import org.springframework.util.Assert
import de.dkfz.tbi.otp.ngsdata.*

class ReferenceGenomeService {

    ConfigService configService

    /**
     * returns the reference genome depending on the project and the sequencing type
     * @param project the project, to which the reference genome belongs to
     * @param seqType the sequencing type, to which the reference genome belongs to
     */
    public ReferenceGenome getReferenceGenome(Project project, SeqType seqType) {
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = ReferenceGenomeProjectSeqType.
                        findByProjectAndSeqTypeAndDeprecatedDateIsNull(project, seqType)
        Assert.notNull(referenceGenomeProjectSeqType,
            "There is no reference genome defined for the combination of project ${project} and seqType ${seqType}")
        return referenceGenomeProjectSeqType.referenceGenome
    }

    /**
     * returns path to the directory storing files for the given reference genome depending on project
     * @param reference genome the reference genome for which the directory path is created
     * @param project the project, which belongs to the reference genome
     */
    public String filePathToDirectory(Project project, ReferenceGenome referenceGenome) {
        Assert.notNull(project, "The project is not specified")
        Assert.notNull(referenceGenome, "The reference genome is not specified")
        Realm realm = configService.getRealmDataProcessing(project)
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
     * @param reference genome the reference genome for which the path and the suffix are created
     * @param project the project, which belongs to the reference genome
     */
    public String filePathOnlySuffix(Project project, ReferenceGenome referenceGenome) {
        Assert.notNull(project, "The project is not specified")
        Assert.notNull(referenceGenome, "The reference genome is not specified")
        String refGenomeFileNamePrefix = referenceGenome.fileNamePrefix
        return filePathToDirectory(project, referenceGenome) + "${refGenomeFileNamePrefix}"
    }

    /**
     * returns the path to the fasta file for the given reference genome depending on project
     * @param the reference genome for which the file path is created and the belonging project
     */
    public String fastaFilePath(Project project, ReferenceGenome referenceGenome) {
        String referenceGenomeFastaFilePath = filePathOnlySuffix(project, referenceGenome) + ".fa"
        File file = new File(referenceGenomeFastaFilePath)
        if (file.canRead()) {
            return referenceGenomeFastaFilePath
        } else {
            throw new RuntimeException(
                "The fasta file ${referenceGenomeFastaFilePath} storing the information of the reference genome can not be read")
        }
    }
}