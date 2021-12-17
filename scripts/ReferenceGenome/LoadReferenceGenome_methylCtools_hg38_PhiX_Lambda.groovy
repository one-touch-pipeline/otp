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

String name = "methylCtools_hg38_PhiX_Lambda"
Set<SpeciesWithStrain> species = [
        CollectionUtils.exactlyOneElement(SpeciesWithStrain.where { species.scientificName == "Homo sapiens" && strain.name == "No strain available" }.list()),
] as Set
@Field
String path = "bwa06_methylCtools_hg38_PhiX_Lambda"
String fileNamePrefix = "GCA_000001405.15_GRCh38_no_alt_analysis_set_adapted_naming_phix_L.conv"
String cytosinePositionsIndex = "GCA_000001405.15_GRCh38_no_alt_analysis_set_adapted_naming_phix_L.pos.gz"
String chromosomePrefix = "chr"
String chromosomeSuffix = "chr"
String fingerPrintingFileName = null
@Field
List<String> statSizeFileNames = [
        "GCA_000001405.15_GRCh38_no_alt_analysis_set_adapted_naming_phix_L.chrLenOnlyACGT.tab",
]

List<FastaEntry> fastaEntries = [
        new FastaEntry("chr1", "1", 248956422, 230481014, Classification.CHROMOSOME),
        new FastaEntry("chr2", "2", 242193529, 240548237, Classification.CHROMOSOME),
        new FastaEntry("chr3", "3", 198295559, 198100142, Classification.CHROMOSOME),
        new FastaEntry("chr4", "4", 190214555, 189752667, Classification.CHROMOSOME),
        new FastaEntry("chr5", "5", 181538259, 178983193, Classification.CHROMOSOME),
        new FastaEntry("chr6", "6", 170805979, 170078523, Classification.CHROMOSOME),
        new FastaEntry("chr7", "7", 159345973, 158970135, Classification.CHROMOSOME),
        new FastaEntry("chr8", "8", 145138636, 144768136, Classification.CHROMOSOME),
        new FastaEntry("chr9", "9", 138394717, 121790553, Classification.CHROMOSOME),
        new FastaEntry("chr10", "10", 133797422, 133262998, Classification.CHROMOSOME),
        new FastaEntry("chr11", "11", 135086622, 134533742, Classification.CHROMOSOME),
        new FastaEntry("chr12", "12", 133275309, 133137819, Classification.CHROMOSOME),
        new FastaEntry("chr13", "13", 114364328, 97983128, Classification.CHROMOSOME),
        new FastaEntry("chr14", "14", 107043718, 88518101, Classification.CHROMOSOME),
        new FastaEntry("chr15", "15", 101991189, 84641325, Classification.CHROMOSOME),
        new FastaEntry("chr16", "16", 90338345, 81805944, Classification.CHROMOSOME),
        new FastaEntry("chr17", "17", 83257441, 82920216, Classification.CHROMOSOME),
        new FastaEntry("chr18", "18", 80373285, 80089605, Classification.CHROMOSOME),
        new FastaEntry("chr19", "19", 58617616, 55766397, Classification.CHROMOSOME),
        new FastaEntry("chr20", "20", 64444167, 63944257, Classification.CHROMOSOME),
        new FastaEntry("chr21", "21", 46709983, 38038574, Classification.CHROMOSOME),
        new FastaEntry("chr22", "22", 50818468, 37071985, Classification.CHROMOSOME),
        new FastaEntry("chrX", "X", 156040895, 154893034, Classification.CHROMOSOME),
        new FastaEntry("chrY", "Y", 57227415, 23636355, Classification.CHROMOSOME),
        new FastaEntry("chrM", "M", 16569, 16568, Classification.MITOCHONDRIAL),
        new FastaEntry("chr1_KI270706v1_random", "chr1_KI270706v1_random", 175055, 175055, Classification.CONTIG),
        new FastaEntry("chr1_KI270707v1_random", "chr1_KI270707v1_random", 32032, 32032, Classification.CONTIG),
        new FastaEntry("chr1_KI270708v1_random", "chr1_KI270708v1_random", 127682, 127682, Classification.CONTIG),
        new FastaEntry("chr1_KI270709v1_random", "chr1_KI270709v1_random", 66860, 66860, Classification.CONTIG),
        new FastaEntry("chr1_KI270710v1_random", "chr1_KI270710v1_random", 40176, 40176, Classification.CONTIG),
        new FastaEntry("chr1_KI270711v1_random", "chr1_KI270711v1_random", 42210, 42210, Classification.CONTIG),
        new FastaEntry("chr1_KI270712v1_random", "chr1_KI270712v1_random", 176043, 176043, Classification.CONTIG),
        new FastaEntry("chr1_KI270713v1_random", "chr1_KI270713v1_random", 40745, 40745, Classification.CONTIG),
        new FastaEntry("chr1_KI270714v1_random", "chr1_KI270714v1_random", 41717, 41717, Classification.CONTIG),
        new FastaEntry("chr2_KI270715v1_random", "chr2_KI270715v1_random", 161471, 161471, Classification.CONTIG),
        new FastaEntry("chr2_KI270716v1_random", "chr2_KI270716v1_random", 153799, 153799, Classification.CONTIG),
        new FastaEntry("chr3_GL000221v1_random", "chr3_GL000221v1_random", 155397, 155397, Classification.CONTIG),
        new FastaEntry("chr4_GL000008v2_random", "chr4_GL000008v2_random", 209709, 209709, Classification.CONTIG),
        new FastaEntry("chr5_GL000208v1_random", "chr5_GL000208v1_random", 92689, 92689, Classification.CONTIG),
        new FastaEntry("chr9_KI270717v1_random", "chr9_KI270717v1_random", 40062, 40062, Classification.CONTIG),
        new FastaEntry("chr9_KI270718v1_random", "chr9_KI270718v1_random", 38054, 38054, Classification.CONTIG),
        new FastaEntry("chr9_KI270719v1_random", "chr9_KI270719v1_random", 176845, 176845, Classification.CONTIG),
        new FastaEntry("chr9_KI270720v1_random", "chr9_KI270720v1_random", 39050, 39050, Classification.CONTIG),
        new FastaEntry("chr11_KI270721v1_random", "chr11_KI270721v1_random", 100316, 100316, Classification.CONTIG),
        new FastaEntry("chr14_GL000009v2_random", "chr14_GL000009v2_random", 201709, 201709, Classification.CONTIG),
        new FastaEntry("chr14_GL000225v1_random", "chr14_GL000225v1_random", 211173, 211173, Classification.CONTIG),
        new FastaEntry("chr14_KI270722v1_random", "chr14_KI270722v1_random", 194050, 194050, Classification.CONTIG),
        new FastaEntry("chr14_GL000194v1_random", "chr14_GL000194v1_random", 191469, 191469, Classification.CONTIG),
        new FastaEntry("chr14_KI270723v1_random", "chr14_KI270723v1_random", 38115, 38115, Classification.CONTIG),
        new FastaEntry("chr14_KI270724v1_random", "chr14_KI270724v1_random", 39555, 39555, Classification.CONTIG),
        new FastaEntry("chr14_KI270725v1_random", "chr14_KI270725v1_random", 172810, 172810, Classification.CONTIG),
        new FastaEntry("chr14_KI270726v1_random", "chr14_KI270726v1_random", 43739, 43739, Classification.CONTIG),
        new FastaEntry("chr15_KI270727v1_random", "chr15_KI270727v1_random", 448248, 448248, Classification.CONTIG),
        new FastaEntry("chr16_KI270728v1_random", "chr16_KI270728v1_random", 1872759, 1572759, Classification.CONTIG),
        new FastaEntry("chr17_GL000205v2_random", "chr17_GL000205v2_random", 185591, 185591, Classification.CONTIG),
        new FastaEntry("chr17_KI270729v1_random", "chr17_KI270729v1_random", 280839, 269482, Classification.CONTIG),
        new FastaEntry("chr17_KI270730v1_random", "chr17_KI270730v1_random", 112551, 106582, Classification.CONTIG),
        new FastaEntry("chr22_KI270731v1_random", "chr22_KI270731v1_random", 150754, 150754, Classification.CONTIG),
        new FastaEntry("chr22_KI270732v1_random", "chr22_KI270732v1_random", 41543, 41543, Classification.CONTIG),
        new FastaEntry("chr22_KI270733v1_random", "chr22_KI270733v1_random", 179772, 179772, Classification.CONTIG),
        new FastaEntry("chr22_KI270734v1_random", "chr22_KI270734v1_random", 165050, 165050, Classification.CONTIG),
        new FastaEntry("chr22_KI270735v1_random", "chr22_KI270735v1_random", 42811, 42811, Classification.CONTIG),
        new FastaEntry("chr22_KI270736v1_random", "chr22_KI270736v1_random", 181920, 169637, Classification.CONTIG),
        new FastaEntry("chr22_KI270737v1_random", "chr22_KI270737v1_random", 103838, 103427, Classification.CONTIG),
        new FastaEntry("chr22_KI270738v1_random", "chr22_KI270738v1_random", 99375, 95820, Classification.CONTIG),
        new FastaEntry("chr22_KI270739v1_random", "chr22_KI270739v1_random", 73985, 72423, Classification.CONTIG),
        new FastaEntry("chrY_KI270740v1_random", "chrY_KI270740v1_random", 37240, 37240, Classification.CONTIG),
        new FastaEntry("chrUn_KI270302v1", "chrUn_KI270302v1", 2274, 2274, Classification.CONTIG),
        new FastaEntry("chrUn_KI270304v1", "chrUn_KI270304v1", 2165, 2165, Classification.CONTIG),
        new FastaEntry("chrUn_KI270303v1", "chrUn_KI270303v1", 1942, 1942, Classification.CONTIG),
        new FastaEntry("chrUn_KI270305v1", "chrUn_KI270305v1", 1472, 1472, Classification.CONTIG),
        new FastaEntry("chrUn_KI270322v1", "chrUn_KI270322v1", 21476, 6208, Classification.CONTIG),
        new FastaEntry("chrUn_KI270320v1", "chrUn_KI270320v1", 4416, 4416, Classification.CONTIG),
        new FastaEntry("chrUn_KI270310v1", "chrUn_KI270310v1", 1201, 1201, Classification.CONTIG),
        new FastaEntry("chrUn_KI270316v1", "chrUn_KI270316v1", 1444, 1444, Classification.CONTIG),
        new FastaEntry("chrUn_KI270315v1", "chrUn_KI270315v1", 2276, 2276, Classification.CONTIG),
        new FastaEntry("chrUn_KI270312v1", "chrUn_KI270312v1", 998, 998, Classification.CONTIG),
        new FastaEntry("chrUn_KI270311v1", "chrUn_KI270311v1", 12399, 2966, Classification.CONTIG),
        new FastaEntry("chrUn_KI270317v1", "chrUn_KI270317v1", 37690, 3624, Classification.CONTIG),
        new FastaEntry("chrUn_KI270412v1", "chrUn_KI270412v1", 1179, 1179, Classification.CONTIG),
        new FastaEntry("chrUn_KI270411v1", "chrUn_KI270411v1", 2646, 2646, Classification.CONTIG),
        new FastaEntry("chrUn_KI270414v1", "chrUn_KI270414v1", 2489, 2489, Classification.CONTIG),
        new FastaEntry("chrUn_KI270419v1", "chrUn_KI270419v1", 1029, 1029, Classification.CONTIG),
        new FastaEntry("chrUn_KI270418v1", "chrUn_KI270418v1", 2145, 2145, Classification.CONTIG),
        new FastaEntry("chrUn_KI270420v1", "chrUn_KI270420v1", 2321, 2321, Classification.CONTIG),
        new FastaEntry("chrUn_KI270424v1", "chrUn_KI270424v1", 2140, 2140, Classification.CONTIG),
        new FastaEntry("chrUn_KI270417v1", "chrUn_KI270417v1", 2043, 2043, Classification.CONTIG),
        new FastaEntry("chrUn_KI270422v1", "chrUn_KI270422v1", 1445, 1445, Classification.CONTIG),
        new FastaEntry("chrUn_KI270423v1", "chrUn_KI270423v1", 981, 981, Classification.CONTIG),
        new FastaEntry("chrUn_KI270425v1", "chrUn_KI270425v1", 1884, 1884, Classification.CONTIG),
        new FastaEntry("chrUn_KI270429v1", "chrUn_KI270429v1", 1361, 1361, Classification.CONTIG),
        new FastaEntry("chrUn_KI270442v1", "chrUn_KI270442v1", 392061, 371029, Classification.CONTIG),
        new FastaEntry("chrUn_KI270466v1", "chrUn_KI270466v1", 1233, 1233, Classification.CONTIG),
        new FastaEntry("chrUn_KI270465v1", "chrUn_KI270465v1", 1774, 1774, Classification.CONTIG),
        new FastaEntry("chrUn_KI270467v1", "chrUn_KI270467v1", 3920, 3920, Classification.CONTIG),
        new FastaEntry("chrUn_KI270435v1", "chrUn_KI270435v1", 92983, 92902, Classification.CONTIG),
        new FastaEntry("chrUn_KI270438v1", "chrUn_KI270438v1", 112505, 84011, Classification.CONTIG),
        new FastaEntry("chrUn_KI270468v1", "chrUn_KI270468v1", 4055, 4055, Classification.CONTIG),
        new FastaEntry("chrUn_KI270510v1", "chrUn_KI270510v1", 2415, 2415, Classification.CONTIG),
        new FastaEntry("chrUn_KI270509v1", "chrUn_KI270509v1", 2318, 2318, Classification.CONTIG),
        new FastaEntry("chrUn_KI270518v1", "chrUn_KI270518v1", 2186, 2186, Classification.CONTIG),
        new FastaEntry("chrUn_KI270508v1", "chrUn_KI270508v1", 1951, 1951, Classification.CONTIG),
        new FastaEntry("chrUn_KI270516v1", "chrUn_KI270516v1", 1300, 1300, Classification.CONTIG),
        new FastaEntry("chrUn_KI270512v1", "chrUn_KI270512v1", 22689, 22689, Classification.CONTIG),
        new FastaEntry("chrUn_KI270519v1", "chrUn_KI270519v1", 138126, 137668, Classification.CONTIG),
        new FastaEntry("chrUn_KI270522v1", "chrUn_KI270522v1", 5674, 5674, Classification.CONTIG),
        new FastaEntry("chrUn_KI270511v1", "chrUn_KI270511v1", 8127, 8127, Classification.CONTIG),
        new FastaEntry("chrUn_KI270515v1", "chrUn_KI270515v1", 6361, 6361, Classification.CONTIG),
        new FastaEntry("chrUn_KI270507v1", "chrUn_KI270507v1", 5353, 5353, Classification.CONTIG),
        new FastaEntry("chrUn_KI270517v1", "chrUn_KI270517v1", 3253, 3253, Classification.CONTIG),
        new FastaEntry("chrUn_KI270529v1", "chrUn_KI270529v1", 1899, 1899, Classification.CONTIG),
        new FastaEntry("chrUn_KI270528v1", "chrUn_KI270528v1", 2983, 2983, Classification.CONTIG),
        new FastaEntry("chrUn_KI270530v1", "chrUn_KI270530v1", 2168, 2168, Classification.CONTIG),
        new FastaEntry("chrUn_KI270539v1", "chrUn_KI270539v1", 993, 993, Classification.CONTIG),
        new FastaEntry("chrUn_KI270538v1", "chrUn_KI270538v1", 91309, 63627, Classification.CONTIG),
        new FastaEntry("chrUn_KI270544v1", "chrUn_KI270544v1", 1202, 1202, Classification.CONTIG),
        new FastaEntry("chrUn_KI270548v1", "chrUn_KI270548v1", 1599, 1599, Classification.CONTIG),
        new FastaEntry("chrUn_KI270583v1", "chrUn_KI270583v1", 1400, 1400, Classification.CONTIG),
        new FastaEntry("chrUn_KI270587v1", "chrUn_KI270587v1", 2969, 2969, Classification.CONTIG),
        new FastaEntry("chrUn_KI270580v1", "chrUn_KI270580v1", 1553, 1553, Classification.CONTIG),
        new FastaEntry("chrUn_KI270581v1", "chrUn_KI270581v1", 7046, 7046, Classification.CONTIG),
        new FastaEntry("chrUn_KI270579v1", "chrUn_KI270579v1", 31033, 31033, Classification.CONTIG),
        new FastaEntry("chrUn_KI270589v1", "chrUn_KI270589v1", 44474, 35396, Classification.CONTIG),
        new FastaEntry("chrUn_KI270590v1", "chrUn_KI270590v1", 4685, 4685, Classification.CONTIG),
        new FastaEntry("chrUn_KI270584v1", "chrUn_KI270584v1", 4513, 4513, Classification.CONTIG),
        new FastaEntry("chrUn_KI270582v1", "chrUn_KI270582v1", 6504, 6504, Classification.CONTIG),
        new FastaEntry("chrUn_KI270588v1", "chrUn_KI270588v1", 6158, 6158, Classification.CONTIG),
        new FastaEntry("chrUn_KI270593v1", "chrUn_KI270593v1", 3041, 3041, Classification.CONTIG),
        new FastaEntry("chrUn_KI270591v1", "chrUn_KI270591v1", 5796, 5796, Classification.CONTIG),
        new FastaEntry("chrUn_KI270330v1", "chrUn_KI270330v1", 1652, 1652, Classification.CONTIG),
        new FastaEntry("chrUn_KI270329v1", "chrUn_KI270329v1", 1040, 1040, Classification.CONTIG),
        new FastaEntry("chrUn_KI270334v1", "chrUn_KI270334v1", 1368, 1368, Classification.CONTIG),
        new FastaEntry("chrUn_KI270333v1", "chrUn_KI270333v1", 2699, 2699, Classification.CONTIG),
        new FastaEntry("chrUn_KI270335v1", "chrUn_KI270335v1", 1048, 1048, Classification.CONTIG),
        new FastaEntry("chrUn_KI270338v1", "chrUn_KI270338v1", 1428, 1428, Classification.CONTIG),
        new FastaEntry("chrUn_KI270340v1", "chrUn_KI270340v1", 1428, 1428, Classification.CONTIG),
        new FastaEntry("chrUn_KI270336v1", "chrUn_KI270336v1", 1026, 1026, Classification.CONTIG),
        new FastaEntry("chrUn_KI270337v1", "chrUn_KI270337v1", 1121, 1121, Classification.CONTIG),
        new FastaEntry("chrUn_KI270363v1", "chrUn_KI270363v1", 1803, 1803, Classification.CONTIG),
        new FastaEntry("chrUn_KI270364v1", "chrUn_KI270364v1", 2855, 2855, Classification.CONTIG),
        new FastaEntry("chrUn_KI270362v1", "chrUn_KI270362v1", 3530, 3530, Classification.CONTIG),
        new FastaEntry("chrUn_KI270366v1", "chrUn_KI270366v1", 8320, 8320, Classification.CONTIG),
        new FastaEntry("chrUn_KI270378v1", "chrUn_KI270378v1", 1048, 1048, Classification.CONTIG),
        new FastaEntry("chrUn_KI270379v1", "chrUn_KI270379v1", 1045, 1045, Classification.CONTIG),
        new FastaEntry("chrUn_KI270389v1", "chrUn_KI270389v1", 1298, 1298, Classification.CONTIG),
        new FastaEntry("chrUn_KI270390v1", "chrUn_KI270390v1", 2387, 2387, Classification.CONTIG),
        new FastaEntry("chrUn_KI270387v1", "chrUn_KI270387v1", 1537, 1537, Classification.CONTIG),
        new FastaEntry("chrUn_KI270395v1", "chrUn_KI270395v1", 1143, 1143, Classification.CONTIG),
        new FastaEntry("chrUn_KI270396v1", "chrUn_KI270396v1", 1880, 1880, Classification.CONTIG),
        new FastaEntry("chrUn_KI270388v1", "chrUn_KI270388v1", 1216, 1216, Classification.CONTIG),
        new FastaEntry("chrUn_KI270394v1", "chrUn_KI270394v1", 970, 970, Classification.CONTIG),
        new FastaEntry("chrUn_KI270386v1", "chrUn_KI270386v1", 1788, 1788, Classification.CONTIG),
        new FastaEntry("chrUn_KI270391v1", "chrUn_KI270391v1", 1484, 1484, Classification.CONTIG),
        new FastaEntry("chrUn_KI270383v1", "chrUn_KI270383v1", 1750, 1750, Classification.CONTIG),
        new FastaEntry("chrUn_KI270393v1", "chrUn_KI270393v1", 1308, 1308, Classification.CONTIG),
        new FastaEntry("chrUn_KI270384v1", "chrUn_KI270384v1", 1658, 1658, Classification.CONTIG),
        new FastaEntry("chrUn_KI270392v1", "chrUn_KI270392v1", 971, 971, Classification.CONTIG),
        new FastaEntry("chrUn_KI270381v1", "chrUn_KI270381v1", 1930, 1930, Classification.CONTIG),
        new FastaEntry("chrUn_KI270385v1", "chrUn_KI270385v1", 990, 990, Classification.CONTIG),
        new FastaEntry("chrUn_KI270382v1", "chrUn_KI270382v1", 4215, 4215, Classification.CONTIG),
        new FastaEntry("chrUn_KI270376v1", "chrUn_KI270376v1", 1136, 1136, Classification.CONTIG),
        new FastaEntry("chrUn_KI270374v1", "chrUn_KI270374v1", 2656, 2656, Classification.CONTIG),
        new FastaEntry("chrUn_KI270372v1", "chrUn_KI270372v1", 1650, 1650, Classification.CONTIG),
        new FastaEntry("chrUn_KI270373v1", "chrUn_KI270373v1", 1451, 1451, Classification.CONTIG),
        new FastaEntry("chrUn_KI270375v1", "chrUn_KI270375v1", 2378, 2378, Classification.CONTIG),
        new FastaEntry("chrUn_KI270371v1", "chrUn_KI270371v1", 2805, 2805, Classification.CONTIG),
        new FastaEntry("chrUn_KI270448v1", "chrUn_KI270448v1", 7992, 7992, Classification.CONTIG),
        new FastaEntry("chrUn_KI270521v1", "chrUn_KI270521v1", 7642, 7642, Classification.CONTIG),
        new FastaEntry("chrUn_GL000195v1", "chrUn_GL000195v1", 182896, 182896, Classification.CONTIG),
        new FastaEntry("chrUn_GL000219v1", "chrUn_GL000219v1", 179198, 179198, Classification.CONTIG),
        new FastaEntry("chrUn_GL000220v1", "chrUn_GL000220v1", 161802, 161802, Classification.CONTIG),
        new FastaEntry("chrUn_GL000224v1", "chrUn_GL000224v1", 179693, 179693, Classification.CONTIG),
        new FastaEntry("chrUn_KI270741v1", "chrUn_KI270741v1", 157432, 155620, Classification.CONTIG),
        new FastaEntry("chrUn_GL000226v1", "chrUn_GL000226v1", 15008, 15008, Classification.CONTIG),
        new FastaEntry("chrUn_GL000213v1", "chrUn_GL000213v1", 164239, 164239, Classification.CONTIG),
        new FastaEntry("chrUn_KI270743v1", "chrUn_KI270743v1", 210658, 210658, Classification.CONTIG),
        new FastaEntry("chrUn_KI270744v1", "chrUn_KI270744v1", 168472, 168472, Classification.CONTIG),
        new FastaEntry("chrUn_KI270745v1", "chrUn_KI270745v1", 41891, 41891, Classification.CONTIG),
        new FastaEntry("chrUn_KI270746v1", "chrUn_KI270746v1", 66486, 66486, Classification.CONTIG),
        new FastaEntry("chrUn_KI270747v1", "chrUn_KI270747v1", 198735, 198735, Classification.CONTIG),
        new FastaEntry("chrUn_KI270748v1", "chrUn_KI270748v1", 93321, 93321, Classification.CONTIG),
        new FastaEntry("chrUn_KI270749v1", "chrUn_KI270749v1", 158759, 158759, Classification.CONTIG),
        new FastaEntry("chrUn_KI270750v1", "chrUn_KI270750v1", 148850, 148850, Classification.CONTIG),
        new FastaEntry("chrUn_KI270751v1", "chrUn_KI270751v1", 150742, 150742, Classification.CONTIG),
        new FastaEntry("chrUn_KI270752v1", "chrUn_KI270752v1", 27745, 27745, Classification.CONTIG),
        new FastaEntry("chrUn_KI270753v1", "chrUn_KI270753v1", 62944, 62944, Classification.CONTIG),
        new FastaEntry("chrUn_KI270754v1", "chrUn_KI270754v1", 40191, 40191, Classification.CONTIG),
        new FastaEntry("chrUn_KI270755v1", "chrUn_KI270755v1", 36723, 36723, Classification.CONTIG),
        new FastaEntry("chrUn_KI270756v1", "chrUn_KI270756v1", 79590, 79524, Classification.CONTIG),
        new FastaEntry("chrUn_KI270757v1", "chrUn_KI270757v1", 71251, 61851, Classification.CONTIG),
        new FastaEntry("chrUn_GL000214v1", "chrUn_GL000214v1", 137718, 137718, Classification.CONTIG),
        new FastaEntry("chrUn_KI270742v1", "chrUn_KI270742v1", 186739, 186739, Classification.CONTIG),
        new FastaEntry("chrUn_GL000216v2", "chrUn_GL000216v2", 176608, 176372, Classification.CONTIG),
        new FastaEntry("chrUn_GL000218v1", "chrUn_GL000218v1", 161147, 161147, Classification.CONTIG),
        new FastaEntry("chrEBV", "chrEBV", 171823, 171823, Classification.UNDEFINED),
        new FastaEntry("phix", "phix", 5386, 5386, Classification.UNDEFINED),
        new FastaEntry("L", "L", 48502, 48502, Classification.UNDEFINED),
]

ReferenceGenomeService referenceGenomeService = ctx.referenceGenomeService
referenceGenomeService.loadReferenceGenome(name, species, path, fileNamePrefix, cytosinePositionsIndex, chromosomePrefix, chromosomeSuffix,
        fastaEntries, fingerPrintingFileName, statSizeFileNames)
