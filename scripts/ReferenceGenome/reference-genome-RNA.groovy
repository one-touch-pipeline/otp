/*
 * Copyright 2011-2020 The OTP authors
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
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeIndexService
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CollectionUtils

GeneModelService geneModelService = ctx.geneModelService
ReferenceGenomeIndexService referenceGenomeIndexService = ctx.referenceGenomeIndexService

def saveAndCheckGeneModel = { GeneModel model ->
    model.save(flush: true)
    assert geneModelService.getFile(model).exists()
    assert geneModelService.getExcludeFile(model).exists()
    if (model.dexSeqFileName) {
        assert geneModelService.getDexSeqFile(model).exists()
    }
    if (model.gcFileName) {
        assert geneModelService.getGcFile(model).exists()
    }
}

def saveAndCheckReferenceGenomeIndex = { ReferenceGenomeIndex index ->
    index.save(flush: true)
    assert referenceGenomeIndexService.getFile(index).exists()
}

ToolName arribaBlacklist     = CollectionUtils.atMostOneElement(ToolName.findAllByName(ProjectService.ARRIBA_BLACKLIST))
ToolName arribaKnownFusions  = CollectionUtils.atMostOneElement(ToolName.findAllByName(ProjectService.ARRIBA_KNOWN_FUSIONS))
ToolName genomeGatk          = CollectionUtils.atMostOneElement(ToolName.findAllByName(ProjectService.GENOME_GATK_INDEX))
ToolName genomeKallistoIndex = CollectionUtils.atMostOneElement(ToolName.findAllByName(ProjectService.GENOME_KALLISTO_INDEX))
ToolName genomeStarIndex_100 = CollectionUtils.atMostOneElement(ToolName.findAllByName("${ProjectService.GENOME_STAR_INDEX}_100"))
ToolName genomeStarIndex_200 = CollectionUtils.atMostOneElement(ToolName.findAllByName("${ProjectService.GENOME_STAR_INDEX}_200"))
ToolName genomeStarIndex_50  = CollectionUtils.atMostOneElement(ToolName.findAllByName("${ProjectService.GENOME_STAR_INDEX}_50"))

ReferenceGenome.withTransaction {
    // #################################################################################
    // ###############  human reference genome hs37d5 + PhiX            ################
    // #################################################################################
    ReferenceGenome referenceGenome_1KGRef_PhiX = CollectionUtils.atMostOneElement(ReferenceGenome.findAllByName("1KGRef_PhiX"))

    saveAndCheckGeneModel(new GeneModel(
            referenceGenome: referenceGenome_1KGRef_PhiX,
            path: "gencode19",
            fileName: "gencode.v19.annotation_plain.gtf",
            excludeFileName: "gencode.v19.annotation_plain.chrXYMT.rRNA.tRNA.gtf",
            dexSeqFileName: "gencode.v19.annotation_plain.dexseq.gff",
            gcFileName: "gencode.v19.annotation_plain.transcripts.autosomal_transcriptTypeProteinCoding_nonPseudo.1KGRef.gc",
    ))

    saveAndCheckReferenceGenomeIndex(new ReferenceGenomeIndex(
            toolName: arribaBlacklist,
            referenceGenome: referenceGenome_1KGRef_PhiX,
            indexToolVersion: "2017-01-16",
            path: "blacklist_hs37d5_gencode19_2017-01-09.tsv.gz"
    ))

    saveAndCheckReferenceGenomeIndex(new ReferenceGenomeIndex(
            toolName: arribaKnownFusions,
            referenceGenome: referenceGenome_1KGRef_PhiX,
            indexToolVersion: "2017-01-16",
            path: "known_fusions_CancerGeneCensus_gencode19_2017-01-16.tsv.gz"
    ))

    saveAndCheckReferenceGenomeIndex(new ReferenceGenomeIndex(
            toolName: genomeGatk,
            referenceGenome: referenceGenome_1KGRef_PhiX,
            indexToolVersion: "06",
            path: "hs37d5_PhiX.fa"
    ))

    saveAndCheckReferenceGenomeIndex(new ReferenceGenomeIndex(
            toolName: genomeKallistoIndex,
            referenceGenome: referenceGenome_1KGRef_PhiX,
            indexToolVersion: "0.43.0",
            path: "kallisto-0.43.0_1KGRef_Gencode19_k31/kallisto-0.43.0_1KGRef_Gencode19_k31.noGenes.index"
    ))

    saveAndCheckReferenceGenomeIndex(new ReferenceGenomeIndex(
            toolName: genomeStarIndex_100,
            referenceGenome: referenceGenome_1KGRef_PhiX,
            indexToolVersion: "2.5.2b",
            path: "STAR_2.5.2b_1KGRef_PhiX_Gencode19_100bp"
    ))

    saveAndCheckReferenceGenomeIndex(new ReferenceGenomeIndex(
            toolName: genomeStarIndex_200,
            referenceGenome: referenceGenome_1KGRef_PhiX,
            indexToolVersion: "2.5.2b",
            path: "STAR_2.5.2b_1KGRef_PhiX_Gencode19_200bp"
    ))

    saveAndCheckReferenceGenomeIndex(new ReferenceGenomeIndex(
            toolName: genomeStarIndex_50,
            referenceGenome: referenceGenome_1KGRef_PhiX,
            indexToolVersion: "2.5.2b",
            path: "STAR_2.5.2b_1KGRef_PhiX_Gencode19_50bp"
    ))

    // #################################################################################
    // ###############  xenograft reference genome  mm10 + PhiX         ################
    // #################################################################################
    ReferenceGenome referenceGenome_GRCm38mm10_PhiX = CollectionUtils.atMostOneElement(ReferenceGenome.findAllByName("GRCm38mm10_PhiX"))

    saveAndCheckGeneModel(new GeneModel(
            referenceGenome: referenceGenome_GRCm38mm10_PhiX,
            path: "gencodeM12",
            fileName: "gencode.vM12.annotation_plain.w_GeneTranscriptID_MT.gtf",
            excludeFileName: "gencode.vM12.annotation_plain.chrXYMT.rRNA.tRNA.gtf",
            dexSeqFileName: null,
            gcFileName: null
    ))

    saveAndCheckReferenceGenomeIndex(new ReferenceGenomeIndex(
            toolName: genomeGatk,
            referenceGenome: referenceGenome_GRCm38mm10_PhiX,
            indexToolVersion: "06",
            path: "GRCm38mm10_PhiX.fa"
    ))

    saveAndCheckReferenceGenomeIndex(new ReferenceGenomeIndex(
            toolName: genomeKallistoIndex,
            referenceGenome: referenceGenome_GRCm38mm10_PhiX,
            indexToolVersion: "0.43.0",
            path: "kallisto-0.43.0_GRCm38mm10_gencodevM12_k31/kallisto-0.43.0_GRCm38mm10_GencodevM12_k31.index"
    ))

    saveAndCheckReferenceGenomeIndex(new ReferenceGenomeIndex(
            toolName: genomeStarIndex_200,
            referenceGenome: referenceGenome_GRCm38mm10_PhiX,
            indexToolVersion: "2.5.2b",
            path: "STAR_2.5.2b_GRCm38mm10_gencodevM12_PhiX_200bp"
    ))

    // #################################################################################
    // ###############  xenograft reference genome hs37d5 + PhiX + mm10 ################
    // #################################################################################
    ReferenceGenome referenceGenome_hs37d5_GRCm38mm_PhiX = CollectionUtils.atMostOneElement(ReferenceGenome.findAllByName("hs37d5_GRCm38mm_PhiX"))

    saveAndCheckGeneModel(new GeneModel(
            referenceGenome: referenceGenome_hs37d5_GRCm38mm_PhiX,
            path: "gencode19",
            fileName: "gencode.v19.annotation_plain.gtf",
            excludeFileName: "gencode.v19.annotation_plain.chrXYMT.rRNA.tRNA.gtf",
            dexSeqFileName: "gencode.v19.annotation_plain.dexseq.gff",
            gcFileName: null
    ))

    saveAndCheckReferenceGenomeIndex(new ReferenceGenomeIndex(
            toolName: arribaBlacklist,
            referenceGenome: referenceGenome_hs37d5_GRCm38mm_PhiX,
            indexToolVersion: "2017-01-16",
            path: "blacklist_hs37d5_gencode19_2017-01-09.tsv.gz"
    ))

    saveAndCheckReferenceGenomeIndex(new ReferenceGenomeIndex(
            toolName: arribaKnownFusions,
            referenceGenome: referenceGenome_hs37d5_GRCm38mm_PhiX,
            indexToolVersion: "2017-01-16",
            path: "known_fusions_CancerGeneCensus_gencode19_2017-01-16.tsv.gz"
    ))

    saveAndCheckReferenceGenomeIndex(new ReferenceGenomeIndex(
            toolName: genomeGatk,
            referenceGenome: referenceGenome_hs37d5_GRCm38mm_PhiX,
            indexToolVersion: "06",
            path: "hs37d5_GRCm38mm10_PhiX.fa"
    ))

    saveAndCheckReferenceGenomeIndex(new ReferenceGenomeIndex(
            toolName: genomeKallistoIndex,
            referenceGenome: referenceGenome_hs37d5_GRCm38mm_PhiX,
            indexToolVersion: "0.43.0",
            path: "kallisto-0.43.0_hs37d5_gencodev19_GRCm38mm_gencodevM12_k31/kallisto-0.43.0_hs37d5_gencodev19_GRCm38mm_gencodevM12_k31.index"
    ))

    saveAndCheckReferenceGenomeIndex(new ReferenceGenomeIndex(
            toolName: genomeStarIndex_200,
            referenceGenome: referenceGenome_hs37d5_GRCm38mm_PhiX,
            indexToolVersion: "2.5.2b",
            path: "STAR_2.5.2b_hs37d5_gencodev19_GRCm38mm_gencodevM12_PhiX_200bp"
    ))
}
