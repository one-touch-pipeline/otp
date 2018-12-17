package de.dkfz.tbi.otp.ngsdata

class GeneModelService {
    ReferenceGenomeService referenceGenomeService

    static final String GENE_MODEL_PATH_COMPONENT = "gencode"

    File getFile(GeneModel geneModel) {
        new File(getBasePath(geneModel), geneModel.fileName)
    }

    File getExcludeFile(GeneModel geneModel) {
        new File(getBasePath(geneModel), geneModel.excludeFileName)
    }

    File getDexSeqFile(GeneModel geneModel) {
        new File(getBasePath(geneModel), geneModel.dexSeqFileName)
    }

    File getGcFile(GeneModel geneModel) {
        new File(getBasePath(geneModel), geneModel.gcFileName)
    }


    private File getBasePath(GeneModel geneModel) {
        assert geneModel : "geneModel is null"
        new File(new File(referenceGenomeService.referenceGenomeDirectory(geneModel.referenceGenome, false),
                GENE_MODEL_PATH_COMPONENT), geneModel.path)
    }
}
