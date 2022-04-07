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

package migration

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.utils.CollectionUtils

String referenceGenomeSpecies = """\
1KGRef_PhiX | Homo sapiens
GRCh38_decoy_ebv_phiX_alt_hla_chr | Mus musculus
GRCm38mm10 | Mus musculus
GRCm38mm10_PhiX | Mus musculus
GRCm38mm10_PhiX_hD3A | Mus musculus
hg19 | Homo sapiens
hg38 | Homo sapiens
hg38_CGA_000001405.15-no_alt_analysis_set | Homo sapiens
hg38_PhiX | Homo sapiens
hg_GRCh38 | Homo sapiens
hg_GRCh38-2020-A | Homo sapiens
hg_GRCh38-2020-A_premrna | Homo sapiens
hg_GRCm38 | Mus musculus
hg_GRCm38-2020-A | Mus musculus
hs37d5 | Homo sapiens
hs37d5+mouse | Homo sapiens + Mus musculus
hs37d5_Bovine_Phix | Homo sapiens + Bos taurus
hs37d5_GRCm38mm | Homo sapiens + Mus musculus
hs37d5_GRCm38mm_PhiX | Homo sapiens + Mus musculus
methylCtools_GRCm38mm10_PhiX_Lambda | Mus musculus
methylCtools_GRCm38mm10_PhiX_Lambda_hD3A | Mus musculus
methylCtools_hg38_PhiX_Lambda | Homo sapiens
methylCtools_hg38_PhiX_Lambda_Benchmark | Homo sapiens
methylCtools_hs37d5_GRCm38mm10_PhiX_Lambda | Homo sapiens + Mus musculus
methylCtools_hs37d5_PhiX_Lambda | Homo sapiens
methylCtools_mm10_UCSC_PhiX_Lambda | Mus musculus
methylCtools_mm9_PhiX_Lambda | Mus musculus
mm9_phiX | Mus musculus
hg19_pathogens | Homo sapiens
methylCtools_hg38p13_lambda_phix_herpes | Homo sapiens
refdata-gex-GRCh38 | Homo sapiens
"""

Map speciesMap = [
        "Homo sapiens": CollectionUtils.exactlyOneElement(SpeciesWithStrain.where { species.scientificName == "Homo sapiens" && strain.name == "No strain available" }.list()),
        "Mus musculus": CollectionUtils.exactlyOneElement(SpeciesWithStrain.where { species.scientificName == "Mus musculus" && strain.name == "Unknown" }.list()),
]

List<List<String>> rgs = referenceGenomeSpecies.split("\n").collect { it.split("\\|")*.trim() }
rgs.each {
    ReferenceGenome referenceGenome = CollectionUtils.atMostOneElement(ReferenceGenome.findAllByName(it[0].trim()))
    if (referenceGenome) {
        Set<SpeciesWithStrain> species = it[1].split(" \\+ ").collect { String speciesName -> speciesMap[speciesName] }
        referenceGenome.species = species as Set
        referenceGenome.save(flush: true)
    }
}
[]
