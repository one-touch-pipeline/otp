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
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesCommonName
import de.dkfz.tbi.otp.utils.CollectionUtils

String referenceGenomeSpecies = """\
1KGRef_PhiX | Human
GRCh38_decoy_ebv_phiX_alt_hla_chr | Mouse
GRCm38mm10 | Mouse
GRCm38mm10_PhiX | Mouse
GRCm38mm10_PhiX_hD3A | Mouse
hg19 | Human
hg38 | Human
hg38_CGA_000001405.15-no_alt_analysis_set | Human
hg38_PhiX | Human
hg_GRCh38 | Human
hg_GRCh38-2020-A | Human
hg_GRCh38-2020-A_premrna | Human
hg_GRCm38 | Mouse
hg_GRCm38-2020-A | Mouse
hs37d5 | Human
hs37d5+mouse | Human + Mouse
hs37d5_Bovine_Phix | Human + Bovine
hs37d5_GRCm38mm | Human + Mouse
hs37d5_GRCm38mm_PhiX | Human + Mouse
methylCtools_GRCm38mm10_PhiX_Lambda | Mouse
methylCtools_GRCm38mm10_PhiX_Lambda_hD3A | Mouse
methylCtools_hg38_PhiX_Lambda | Human
methylCtools_hg38_PhiX_Lambda_Benchmark | Human
methylCtools_hs37d5_GRCm38mm10_PhiX_Lambda | Human + Mouse
methylCtools_hs37d5_PhiX_Lambda | Human
methylCtools_mm10_UCSC_PhiX_Lambda | Mouse
methylCtools_mm9_PhiX_Lambda | Mouse
mm9_phiX | Mouse
hg19_pathogens | Human
methylCtools_hg38p13_lambda_phix_herpes | Human
refdata-gex-GRCh38 | Human
"""

List<List<String>> rgs = referenceGenomeSpecies.split("\n").collect { it.split("\\|")*.trim() }
rgs.each {
    ReferenceGenome referenceGenome = CollectionUtils.atMostOneElement(ReferenceGenome.findAllByName(it[0].trim()))
    if (referenceGenome) {
        List<SpeciesCommonName> species = it[1].split(" \\+ ").collect { speciesName ->
            CollectionUtils.exactlyOneElement(SpeciesCommonName.findAllByName(speciesName.trim()))
        }
        referenceGenome.species = species as Set
        referenceGenome.save(flush: true)
    }
}
[]
