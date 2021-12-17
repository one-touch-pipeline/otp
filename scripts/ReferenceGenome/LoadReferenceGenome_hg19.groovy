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

import groovy.transform.Field

import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import de.dkfz.tbi.otp.ngsdata.referencegenome.FastaEntry
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.utils.CollectionUtils

/*
 *  This scripts inserts a new ReferenceGenome object
 *  as well as StatSizeFileName and ReferenceGenomeEntry objects obtained from the fasta file
 *  which lies in ${OtpProperty#BASE_PATH_REFERENCE_GENOME}/${path}/
 *  A python helper script 'scripts/ReferenceGenome/getReferenceGenomeInfo.py' which extracts
 *  information from the fasta file was used to generate the fastaEntries content.
*/

String name = "hg19"
Set<SpeciesWithStrain> species = [
        CollectionUtils.exactlyOneElement(SpeciesWithStrain.where { species.scientificName == "Homo sapiens" && strain.name == "No strain available" }.list()),
] as Set
@Field
String path = "bwa06_hg19_chr"
String fileNamePrefix = "hg19_1-22_X_Y_M"
String cytosinePositionsIndex = null
String chromosomePrefix = "chr"
String chromosomeSuffix = ""
String fingerPrintingFileName = "snp138Common.n1000.vh20140318.bed"
@Field
List<String> statSizeFileNames = [
        "hg19_1-22_X_Y_M.fa.chrLenOnlyACGT.tab",
]

List<FastaEntry> fastaEntries = [
        new FastaEntry("chr1", "1", 249250621, 225280621, Classification.CHROMOSOME),
        new FastaEntry("chr10", "10", 135534747, 131314742, Classification.CHROMOSOME),
        new FastaEntry("chr11", "11", 135006516, 131129516, Classification.CHROMOSOME),
        new FastaEntry("chr12", "12", 133851895, 130481394, Classification.CHROMOSOME),
        new FastaEntry("chr13", "13", 115169878, 95589878, Classification.CHROMOSOME),
        new FastaEntry("chr14", "14", 107349540, 88289540, Classification.CHROMOSOME),
        new FastaEntry("chr15", "15", 102531392, 81694769, Classification.CHROMOSOME),
        new FastaEntry("chr16", "16", 90354753, 78884753, Classification.CHROMOSOME),
        new FastaEntry("chr17", "17", 81195210, 77795210, Classification.CHROMOSOME),
        new FastaEntry("chr18", "18", 78077248, 74657233, Classification.CHROMOSOME),
        new FastaEntry("chr19", "19", 59128983, 55808983, Classification.CHROMOSOME),
        new FastaEntry("chr2", "2", 243199373, 238204522, Classification.CHROMOSOME),
        new FastaEntry("chr20", "20", 63025520, 59505520, Classification.CHROMOSOME),
        new FastaEntry("chr21", "21", 48129895, 35106692, Classification.CHROMOSOME),
        new FastaEntry("chr22", "22", 51304566, 34894562, Classification.CHROMOSOME),
        new FastaEntry("chr3", "3", 198022430, 194797136, Classification.CHROMOSOME),
        new FastaEntry("chr4", "4", 191154276, 187661676, Classification.CHROMOSOME),
        new FastaEntry("chr5", "5", 180915260, 177695260, Classification.CHROMOSOME),
        new FastaEntry("chr6", "6", 171115067, 167395067, Classification.CHROMOSOME),
        new FastaEntry("chr7", "7", 159138663, 155353663, Classification.CHROMOSOME),
        new FastaEntry("chr8", "8", 146364022, 142888922, Classification.CHROMOSOME),
        new FastaEntry("chr9", "9", 141213431, 120143431, Classification.CHROMOSOME),
        new FastaEntry("chrM", "M", 16571, 16571, Classification.MITOCHONDRIAL),
        new FastaEntry("chrX", "X", 155270560, 151100560, Classification.CHROMOSOME),
        new FastaEntry("chrY", "Y", 59373566, 25653566, Classification.CHROMOSOME),
]

ReferenceGenomeService referenceGenomeService = ctx.referenceGenomeService
referenceGenomeService.loadReferenceGenome(name, species, path, fileNamePrefix, cytosinePositionsIndex, chromosomePrefix, chromosomeSuffix,
        fastaEntries, fingerPrintingFileName, statSizeFileNames)
