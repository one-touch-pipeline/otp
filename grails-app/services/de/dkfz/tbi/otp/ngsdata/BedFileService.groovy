package de.dkfz.tbi.otp.ngsdata

import static org.springframework.util.Assert.*

class BedFileService {

    ReferenceGenomeService referenceGenomeService
    private final static String TARGET_REGIONS_DIR = "targetRegions"

    /**
     * @return absolute path to the given {@link BedFile}
     * at given {@link Realm}
     */
    String filePath(BedFile bedFile, boolean checkExistence = true) {
        notNull(bedFile, "bedFile must not be null")
        String refGenomePath = referenceGenomeService.referenceGenomeDirectory(bedFile.referenceGenome, checkExistence).path
        String bedFilePath = "${refGenomePath}/${TARGET_REGIONS_DIR}/${bedFile.fileName}"
        File file = new File(bedFilePath)
        if (!checkExistence || file.canRead()) {
            return bedFilePath
        } else {
            throw new RuntimeException(
            "the bedFile can not be read: ${bedFilePath}")
        }
    }
}
