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
 *  which lies in ${OtpProperty#PATH_PROCESSING_ROOT}/reference_genomes/${path}/
 *  A python helper script 'scripts/ReferenceGenome/getReferenceGenomeInfo.py' which extracts
 *  information from the fasta file was used to generate the fastaEntries content.
*/

String name = "mm9_phiX"
Set<SpeciesWithStrain> species = [
        CollectionUtils.exactlyOneElement(SpeciesWithStrain.where { species.scientificName == "Mus musculus" && strain.name == "No strain available" }.list()),
] as Set
@Field
String path = "bwa07_mm9_PhiX"
String fileNamePrefix = "mm9_phiX"
String cytosinePositionsIndex = null
String chromosomePrefix = ""
String chromosomeSuffix = ""
String fingerPrintingFileName = null
@Field
List<String> statSizeFileNames = [
        "mm9_phiX.fa.chrLenOnlyACGT_realChromosomes.tab",
        "mm9_phiX.fa.chrLenOnlyACGT.tab",
]

List<FastaEntry> fastaEntries = [
        new FastaEntry("chr1", "1", 197195432, 191477555, Classification.CHROMOSOME),
        new FastaEntry("chr10", "10", 129993255, 126848050, Classification.CHROMOSOME),
        new FastaEntry("chr11", "11", 121843856, 118743556, Classification.CHROMOSOME),
        new FastaEntry("chr12", "12", 121257530, 117460921, Classification.CHROMOSOME),
        new FastaEntry("chr13", "13", 120284312, 116371180, Classification.CHROMOSOME),
        new FastaEntry("chr14", "14", 125194864, 121635411, Classification.CHROMOSOME),
        new FastaEntry("chr15", "15", 103494974, 100439974, Classification.CHROMOSOME),
        new FastaEntry("chr16", "16", 98319150, 95005133, Classification.CHROMOSOME),
        new FastaEntry("chr17", "17", 95272651, 91898517, Classification.CHROMOSOME),
        new FastaEntry("chr18", "18", 90772031, 87600101, Classification.CHROMOSOME),
        new FastaEntry("chr19", "19", 61342430, 58142230, Classification.CHROMOSOME),
        new FastaEntry("chr2", "2", 181748087, 178392724, Classification.CHROMOSOME),
        new FastaEntry("chr3", "3", 159599783, 156393914, Classification.CHROMOSOME),
        new FastaEntry("chr4", "4", 155630120, 151886944, Classification.CHROMOSOME),
        new FastaEntry("chr5", "5", 152537259, 147721185, Classification.CHROMOSOME),
        new FastaEntry("chr6", "6", 149517037, 146317036, Classification.CHROMOSOME),
        new FastaEntry("chr7", "7", 152524553, 141878519, Classification.CHROMOSOME),
        new FastaEntry("chr8", "8", 131738871, 124796771, Classification.CHROMOSOME),
        new FastaEntry("chr9", "9", 124076172, 120720957, Classification.CHROMOSOME),
        new FastaEntry("chrM", "M", 16299, 16299, Classification.MITOCHONDRIAL),
        new FastaEntry("chrX", "X", 166650296, 162081539, Classification.CHROMOSOME),
        new FastaEntry("chrY", "Y", 15902555, 2702555, Classification.CHROMOSOME),
        new FastaEntry("phix", "phix", 5386, 5386, Classification.UNDEFINED),
]

ReferenceGenomeService referenceGenomeService = ctx.referenceGenomeService
referenceGenomeService.loadReferenceGenome(name, species, path, fileNamePrefix, cytosinePositionsIndex, chromosomePrefix, chromosomeSuffix,
        fastaEntries, fingerPrintingFileName, statSizeFileNames)
