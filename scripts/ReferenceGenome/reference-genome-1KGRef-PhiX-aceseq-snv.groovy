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

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService

ReferenceGenomeService referenceGenomeService = ctx.referenceGenomeService

ReferenceGenome referenceGenome = ReferenceGenome.findByName('1KGRef_PhiX')
println  referenceGenome.knownHaplotypesLegendFileX
println  referenceGenome.knownHaplotypesLegendFile
println  referenceGenome.knownHaplotypesFileX
println  referenceGenome.knownHaplotypesFile
println  referenceGenome.geneticMapFileX
println  referenceGenome.geneticMapFile
println  referenceGenome.gcContentFile
println  referenceGenome.mappabilityFile
println  referenceGenome.replicationTimeFile

String basePath = referenceGenomeService.referenceGenomeDirectory(referenceGenome, false).absolutePath

// for ACEseq
referenceGenome.knownHaplotypesLegendFileX = "${basePath}/databases/IMPUTE/ALL_1000G_phase1integrated_v3_impute/ALL_1000G_phase1integrated_v3_chrX_nonPAR_impute.legend.gz"
referenceGenome.knownHaplotypesLegendFile = "${basePath}/databases/IMPUTE/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono/ALL.chr\${CHR_NAME}.integrated_phase1_v3.20101123.snps_indels_svs.genotypes.nomono.legend.gz"
referenceGenome.knownHaplotypesFileX = "${basePath}/databases/IMPUTE/ALL_1000G_phase1integrated_v3_impute/ALL_1000G_phase1integrated_v3_chrX_nonPAR_impute.hap.gz"
referenceGenome.knownHaplotypesFile = "${basePath}/databases/IMPUTE/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono/ALL.chr\${CHR_NAME}.integrated_phase1_v3.20101123.snps_indels_svs.genotypes.nomono.haplotypes.gz"
referenceGenome.geneticMapFileX = "${basePath}/databases/IMPUTE/ALL_1000G_phase1integrated_v3_impute/genetic_map_chrX_nonPAR_combined_b37.txt"
referenceGenome.geneticMapFile = "${basePath}/databases/IMPUTE/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono/genetic_map_chr\${CHR_NAME}_combined_b37.txt"
referenceGenome.gcContentFile = "hg19_GRch37_100genomes_gc_content_10kb.txt"
referenceGenome.mappabilityFile = "${basePath}/databases/UCSC/wgEncodeCrgMapabilityAlign100mer_chr.bedGraph.gz"
referenceGenome.replicationTimeFile = "${basePath}/databases/ENCODE/ReplicationTime_10cellines_mean_10KB.Rda"

//for ACEseq and SNV
referenceGenome.chromosomeLengthFilePath = "hg19_chrTotalLength.tsv"

referenceGenome.save(flush: true)
