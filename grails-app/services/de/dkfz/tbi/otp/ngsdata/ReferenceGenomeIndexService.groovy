package de.dkfz.tbi.otp.ngsdata


class ReferenceGenomeIndexService {
    ReferenceGenomeService referenceGenomeService

    static final String REFERENCE_GENOME_INDEX_PATH_COMPONENT = "indexes"

    File getFile(ReferenceGenomeIndex referenceGenomeIndex) {
        assert referenceGenomeIndex : "referenceGenomeIndex is null"
        new File(getBasePath(referenceGenomeIndex), referenceGenomeIndex.path)
    }


    private File getBasePath(ReferenceGenomeIndex referenceGenomeIndex) {
        new File(new File(referenceGenomeService.referenceGenomeDirectory(referenceGenomeIndex.referenceGenome, false),
                REFERENCE_GENOME_INDEX_PATH_COMPONENT), referenceGenomeIndex.toolName.path)
    }
}
