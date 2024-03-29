/*
 * Copyright 2011-2024 The OTP authors
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
import de.dkfz.tbi.otp.ngsdata.taxonomy.Species
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.utils.CollectionUtils

/*
 *  This scripts inserts a new ReferenceGenome object
 *  as well as StatSizeFileName and ReferenceGenomeEntry objects obtained from the fasta file
 *  which lies in ${OtpProperty#PATH_PROCESSING_ROOT}/reference_genomes/${path}/
 *  A python helper script 'scripts/ReferenceGenome/getReferenceGenomeInfo.py' which extracts
 *  information from the fasta file was used to generate the fastaEntries content.
*/

String name = "hg38"
Set<Species> species = [] as Set
Set<SpeciesWithStrain> speciesWithStrain = [
        CollectionUtils.exactlyOneElement(SpeciesWithStrain.where { species.scientificName == "Homo sapiens" && strain.name == "No strain available" }.list()),
] as Set
@Field
String path = "bwa06_hg38"
String fileNamePrefix = "hg_GRCh38"
String cytosinePositionsIndex = null
String chromosomePrefix = ""
String chromosomeSuffix = ""
String fingerPrintingFileName = null
String defaultStatSizeFileName = "hg_GRCh38.fa.chrLenOnlyACGT_realChromosomes.tab"
@Field
List<String> furtherStatSizeFileNames = [
        "hg_GRCh38.fa.chrLenOnlyACGT.tab",
]

List<FastaEntry> fastaEntries = [
        new FastaEntry("1", "1", 248956422, 230954995, Classification.CHROMOSOME),
        new FastaEntry("10", "10", 133797422, 133457400, Classification.CHROMOSOME),
        new FastaEntry("11", "11", 135086622, 134569614, Classification.CHROMOSOME),
        new FastaEntry("12", "12", 133275309, 133275207, Classification.CHROMOSOME),
        new FastaEntry("13", "13", 114364328, 98244326, Classification.CHROMOSOME),
        new FastaEntry("14", "14", 107043718, 90633714, Classification.CHROMOSOME),
        new FastaEntry("15", "15", 101991189, 84991123, Classification.CHROMOSOME),
        new FastaEntry("16", "16", 90338345, 82128307, Classification.CHROMOSOME),
        new FastaEntry("17", "17", 83257441, 83197343, Classification.CHROMOSOME),
        new FastaEntry("18", "18", 80373285, 80222212, Classification.CHROMOSOME),
        new FastaEntry("19", "19", 58617616, 58557616, Classification.CHROMOSOME),
        new FastaEntry("2", "2", 242193529, 240722501, Classification.CHROMOSOME),
        new FastaEntry("20", "20", 64444167, 64274141, Classification.CHROMOSOME),
        new FastaEntry("21", "21", 46709983, 41349982, Classification.CHROMOSOME),
        new FastaEntry("22", "22", 50818468, 40108466, Classification.CHROMOSOME),
        new FastaEntry("3", "3", 198295559, 198234804, Classification.CHROMOSOME),
        new FastaEntry("4", "4", 190214555, 190014553, Classification.CHROMOSOME),
        new FastaEntry("5", "5", 181538259, 181478083, Classification.CHROMOSOME),
        new FastaEntry("6", "6", 170805979, 170135967, Classification.CHROMOSOME),
        new FastaEntry("7", "7", 159345973, 159345966, Classification.CHROMOSOME),
        new FastaEntry("8", "8", 145138636, 145018621, Classification.CHROMOSOME),
        new FastaEntry("9", "9", 138394717, 122634685, Classification.CHROMOSOME),
        new FastaEntry("MT", "M", 16569, 16568, Classification.MITOCHONDRIAL),
        new FastaEntry("X", "X", 156040895, 155301511, Classification.CHROMOSOME),
        new FastaEntry("Y", "Y", 57227415, 24106423, Classification.CHROMOSOME),
        new FastaEntry("KI270728.1", "KI270728.1", 1872759, 1872759, Classification.CONTIG),
        new FastaEntry("KI270727.1", "KI270727.1", 448248, 448248, Classification.CONTIG),
        new FastaEntry("KI270442.1", "KI270442.1", 392061, 392061, Classification.CONTIG),
        new FastaEntry("KI270729.1", "KI270729.1", 280839, 280839, Classification.CONTIG),
        new FastaEntry("GL000225.1", "GL000225.1", 211173, 211173, Classification.CONTIG),
        new FastaEntry("KI270743.1", "KI270743.1", 210658, 210658, Classification.CONTIG),
        new FastaEntry("GL000008.2", "GL000008.2", 209709, 209709, Classification.CONTIG),
        new FastaEntry("GL000009.2", "GL000009.2", 201709, 201709, Classification.CONTIG),
        new FastaEntry("KI270747.1", "KI270747.1", 198735, 198735, Classification.CONTIG),
        new FastaEntry("KI270722.1", "KI270722.1", 194050, 194050, Classification.CONTIG),
        new FastaEntry("GL000194.1", "GL000194.1", 191469, 191469, Classification.CONTIG),
        new FastaEntry("KI270742.1", "KI270742.1", 186739, 186739, Classification.CONTIG),
        new FastaEntry("GL000205.2", "GL000205.2", 185591, 185591, Classification.CONTIG),
        new FastaEntry("GL000195.1", "GL000195.1", 182896, 182896, Classification.CONTIG),
        new FastaEntry("KI270736.1", "KI270736.1", 181920, 181920, Classification.CONTIG),
        new FastaEntry("KI270733.1", "KI270733.1", 179772, 179772, Classification.CONTIG),
        new FastaEntry("GL000224.1", "GL000224.1", 179693, 179693, Classification.CONTIG),
        new FastaEntry("GL000219.1", "GL000219.1", 179198, 179198, Classification.CONTIG),
        new FastaEntry("KI270719.1", "KI270719.1", 176845, 176845, Classification.CONTIG),
        new FastaEntry("GL000216.2", "GL000216.2", 176608, 176608, Classification.CONTIG),
        new FastaEntry("KI270712.1", "KI270712.1", 176043, 176043, Classification.CONTIG),
        new FastaEntry("KI270706.1", "KI270706.1", 175055, 175055, Classification.CONTIG),
        new FastaEntry("KI270725.1", "KI270725.1", 172810, 172810, Classification.CONTIG),
        new FastaEntry("KI270744.1", "KI270744.1", 168472, 168472, Classification.CONTIG),
        new FastaEntry("KI270734.1", "KI270734.1", 165050, 165050, Classification.CONTIG),
        new FastaEntry("GL000213.1", "GL000213.1", 164239, 164239, Classification.CONTIG),
        new FastaEntry("GL000220.1", "GL000220.1", 161802, 161802, Classification.CONTIG),
        new FastaEntry("KI270715.1", "KI270715.1", 161471, 161471, Classification.CONTIG),
        new FastaEntry("GL000218.1", "GL000218.1", 161147, 161147, Classification.CONTIG),
        new FastaEntry("KI270749.1", "KI270749.1", 158759, 158759, Classification.CONTIG),
        new FastaEntry("KI270741.1", "KI270741.1", 157432, 157432, Classification.CONTIG),
        new FastaEntry("GL000221.1", "GL000221.1", 155397, 155397, Classification.CONTIG),
        new FastaEntry("KI270716.1", "KI270716.1", 153799, 153799, Classification.CONTIG),
        new FastaEntry("KI270731.1", "KI270731.1", 150754, 150754, Classification.CONTIG),
        new FastaEntry("KI270751.1", "KI270751.1", 150742, 150742, Classification.CONTIG),
        new FastaEntry("KI270750.1", "KI270750.1", 148850, 148850, Classification.CONTIG),
        new FastaEntry("KI270519.1", "KI270519.1", 138126, 138126, Classification.CONTIG),
        new FastaEntry("GL000214.1", "GL000214.1", 137718, 137718, Classification.CONTIG),
        new FastaEntry("KI270708.1", "KI270708.1", 127682, 127682, Classification.CONTIG),
        new FastaEntry("KI270730.1", "KI270730.1", 112551, 112551, Classification.CONTIG),
        new FastaEntry("KI270438.1", "KI270438.1", 112505, 112505, Classification.CONTIG),
        new FastaEntry("KI270737.1", "KI270737.1", 103838, 103838, Classification.CONTIG),
        new FastaEntry("KI270721.1", "KI270721.1", 100316, 100316, Classification.CONTIG),
        new FastaEntry("KI270738.1", "KI270738.1", 99375, 99375, Classification.CONTIG),
        new FastaEntry("KI270748.1", "KI270748.1", 93321, 93321, Classification.CONTIG),
        new FastaEntry("KI270435.1", "KI270435.1", 92983, 92983, Classification.CONTIG),
        new FastaEntry("GL000208.1", "GL000208.1", 92689, 92689, Classification.CONTIG),
        new FastaEntry("KI270538.1", "KI270538.1", 91309, 91309, Classification.CONTIG),
        new FastaEntry("KI270756.1", "KI270756.1", 79590, 79590, Classification.CONTIG),
        new FastaEntry("KI270739.1", "KI270739.1", 73985, 73985, Classification.CONTIG),
        new FastaEntry("KI270757.1", "KI270757.1", 71251, 71251, Classification.CONTIG),
        new FastaEntry("KI270709.1", "KI270709.1", 66860, 66860, Classification.CONTIG),
        new FastaEntry("KI270746.1", "KI270746.1", 66486, 66486, Classification.CONTIG),
        new FastaEntry("KI270753.1", "KI270753.1", 62944, 62944, Classification.CONTIG),
        new FastaEntry("KI270589.1", "KI270589.1", 44474, 44474, Classification.CONTIG),
        new FastaEntry("KI270726.1", "KI270726.1", 43739, 43739, Classification.CONTIG),
        new FastaEntry("KI270735.1", "KI270735.1", 42811, 42811, Classification.CONTIG),
        new FastaEntry("KI270711.1", "KI270711.1", 42210, 42210, Classification.CONTIG),
        new FastaEntry("KI270745.1", "KI270745.1", 41891, 41891, Classification.CONTIG),
        new FastaEntry("KI270714.1", "KI270714.1", 41717, 41717, Classification.CONTIG),
        new FastaEntry("KI270732.1", "KI270732.1", 41543, 41543, Classification.CONTIG),
        new FastaEntry("KI270713.1", "KI270713.1", 40745, 40745, Classification.CONTIG),
        new FastaEntry("KI270754.1", "KI270754.1", 40191, 40191, Classification.CONTIG),
        new FastaEntry("KI270710.1", "KI270710.1", 40176, 40176, Classification.CONTIG),
        new FastaEntry("KI270717.1", "KI270717.1", 40062, 40062, Classification.CONTIG),
        new FastaEntry("KI270724.1", "KI270724.1", 39555, 39555, Classification.CONTIG),
        new FastaEntry("KI270720.1", "KI270720.1", 39050, 39050, Classification.CONTIG),
        new FastaEntry("KI270723.1", "KI270723.1", 38115, 38115, Classification.CONTIG),
        new FastaEntry("KI270718.1", "KI270718.1", 38054, 38054, Classification.CONTIG),
        new FastaEntry("KI270317.1", "KI270317.1", 37690, 37690, Classification.CONTIG),
        new FastaEntry("KI270740.1", "KI270740.1", 37240, 37240, Classification.CONTIG),
        new FastaEntry("KI270755.1", "KI270755.1", 36723, 36723, Classification.CONTIG),
        new FastaEntry("KI270707.1", "KI270707.1", 32032, 32032, Classification.CONTIG),
        new FastaEntry("KI270579.1", "KI270579.1", 31033, 31033, Classification.CONTIG),
        new FastaEntry("KI270752.1", "KI270752.1", 27745, 27745, Classification.CONTIG),
        new FastaEntry("KI270512.1", "KI270512.1", 22689, 22689, Classification.CONTIG),
        new FastaEntry("KI270322.1", "KI270322.1", 21476, 21476, Classification.CONTIG),
        new FastaEntry("GL000226.1", "GL000226.1", 15008, 15008, Classification.CONTIG),
        new FastaEntry("KI270311.1", "KI270311.1", 12399, 12399, Classification.CONTIG),
        new FastaEntry("KI270366.1", "KI270366.1", 8320, 8320, Classification.CONTIG),
        new FastaEntry("KI270511.1", "KI270511.1", 8127, 8127, Classification.CONTIG),
        new FastaEntry("KI270448.1", "KI270448.1", 7992, 7992, Classification.CONTIG),
        new FastaEntry("KI270521.1", "KI270521.1", 7642, 7642, Classification.CONTIG),
        new FastaEntry("KI270581.1", "KI270581.1", 7046, 7046, Classification.CONTIG),
        new FastaEntry("KI270582.1", "KI270582.1", 6504, 6504, Classification.CONTIG),
        new FastaEntry("KI270515.1", "KI270515.1", 6361, 6361, Classification.CONTIG),
        new FastaEntry("KI270588.1", "KI270588.1", 6158, 6158, Classification.CONTIG),
        new FastaEntry("KI270591.1", "KI270591.1", 5796, 5796, Classification.CONTIG),
        new FastaEntry("KI270522.1", "KI270522.1", 5674, 5674, Classification.CONTIG),
        new FastaEntry("KI270507.1", "KI270507.1", 5353, 5353, Classification.CONTIG),
        new FastaEntry("KI270590.1", "KI270590.1", 4685, 4685, Classification.CONTIG),
        new FastaEntry("KI270584.1", "KI270584.1", 4513, 4513, Classification.CONTIG),
        new FastaEntry("KI270320.1", "KI270320.1", 4416, 4416, Classification.CONTIG),
        new FastaEntry("KI270382.1", "KI270382.1", 4215, 4215, Classification.CONTIG),
        new FastaEntry("KI270468.1", "KI270468.1", 4055, 4055, Classification.CONTIG),
        new FastaEntry("KI270467.1", "KI270467.1", 3920, 3920, Classification.CONTIG),
        new FastaEntry("KI270362.1", "KI270362.1", 3530, 3530, Classification.CONTIG),
        new FastaEntry("KI270517.1", "KI270517.1", 3253, 3253, Classification.CONTIG),
        new FastaEntry("KI270593.1", "KI270593.1", 3041, 3041, Classification.CONTIG),
        new FastaEntry("KI270528.1", "KI270528.1", 2983, 2983, Classification.CONTIG),
        new FastaEntry("KI270587.1", "KI270587.1", 2969, 2969, Classification.CONTIG),
        new FastaEntry("KI270364.1", "KI270364.1", 2855, 2855, Classification.CONTIG),
        new FastaEntry("KI270371.1", "KI270371.1", 2805, 2805, Classification.CONTIG),
        new FastaEntry("KI270333.1", "KI270333.1", 2699, 2699, Classification.CONTIG),
        new FastaEntry("KI270374.1", "KI270374.1", 2656, 2656, Classification.CONTIG),
        new FastaEntry("KI270411.1", "KI270411.1", 2646, 2646, Classification.CONTIG),
        new FastaEntry("KI270414.1", "KI270414.1", 2489, 2489, Classification.CONTIG),
        new FastaEntry("KI270510.1", "KI270510.1", 2415, 2415, Classification.CONTIG),
        new FastaEntry("KI270390.1", "KI270390.1", 2387, 2387, Classification.CONTIG),
        new FastaEntry("KI270375.1", "KI270375.1", 2378, 2378, Classification.CONTIG),
        new FastaEntry("KI270420.1", "KI270420.1", 2321, 2321, Classification.CONTIG),
        new FastaEntry("KI270509.1", "KI270509.1", 2318, 2318, Classification.CONTIG),
        new FastaEntry("KI270315.1", "KI270315.1", 2276, 2276, Classification.CONTIG),
        new FastaEntry("KI270302.1", "KI270302.1", 2274, 2274, Classification.CONTIG),
        new FastaEntry("KI270518.1", "KI270518.1", 2186, 2186, Classification.CONTIG),
        new FastaEntry("KI270530.1", "KI270530.1", 2168, 2168, Classification.CONTIG),
        new FastaEntry("KI270304.1", "KI270304.1", 2165, 2165, Classification.CONTIG),
        new FastaEntry("KI270418.1", "KI270418.1", 2145, 2145, Classification.CONTIG),
        new FastaEntry("KI270424.1", "KI270424.1", 2140, 2140, Classification.CONTIG),
        new FastaEntry("KI270417.1", "KI270417.1", 2043, 2043, Classification.CONTIG),
        new FastaEntry("KI270508.1", "KI270508.1", 1951, 1951, Classification.CONTIG),
        new FastaEntry("KI270303.1", "KI270303.1", 1942, 1942, Classification.CONTIG),
        new FastaEntry("KI270381.1", "KI270381.1", 1930, 1930, Classification.CONTIG),
        new FastaEntry("KI270529.1", "KI270529.1", 1899, 1899, Classification.CONTIG),
        new FastaEntry("KI270425.1", "KI270425.1", 1884, 1884, Classification.CONTIG),
        new FastaEntry("KI270396.1", "KI270396.1", 1880, 1880, Classification.CONTIG),
        new FastaEntry("KI270363.1", "KI270363.1", 1803, 1803, Classification.CONTIG),
        new FastaEntry("KI270386.1", "KI270386.1", 1788, 1788, Classification.CONTIG),
        new FastaEntry("KI270465.1", "KI270465.1", 1774, 1774, Classification.CONTIG),
        new FastaEntry("KI270383.1", "KI270383.1", 1750, 1750, Classification.CONTIG),
        new FastaEntry("KI270384.1", "KI270384.1", 1658, 1658, Classification.CONTIG),
        new FastaEntry("KI270330.1", "KI270330.1", 1652, 1652, Classification.CONTIG),
        new FastaEntry("KI270372.1", "KI270372.1", 1650, 1650, Classification.CONTIG),
        new FastaEntry("KI270548.1", "KI270548.1", 1599, 1599, Classification.CONTIG),
        new FastaEntry("KI270580.1", "KI270580.1", 1553, 1553, Classification.CONTIG),
        new FastaEntry("KI270387.1", "KI270387.1", 1537, 1537, Classification.CONTIG),
        new FastaEntry("KI270391.1", "KI270391.1", 1484, 1484, Classification.CONTIG),
        new FastaEntry("KI270305.1", "KI270305.1", 1472, 1472, Classification.CONTIG),
        new FastaEntry("KI270373.1", "KI270373.1", 1451, 1451, Classification.CONTIG),
        new FastaEntry("KI270422.1", "KI270422.1", 1445, 1445, Classification.CONTIG),
        new FastaEntry("KI270316.1", "KI270316.1", 1444, 1444, Classification.CONTIG),
        new FastaEntry("KI270340.1", "KI270340.1", 1428, 1428, Classification.CONTIG),
        new FastaEntry("KI270338.1", "KI270338.1", 1428, 1428, Classification.CONTIG),
        new FastaEntry("KI270583.1", "KI270583.1", 1400, 1400, Classification.CONTIG),
        new FastaEntry("KI270334.1", "KI270334.1", 1368, 1368, Classification.CONTIG),
        new FastaEntry("KI270429.1", "KI270429.1", 1361, 1361, Classification.CONTIG),
        new FastaEntry("KI270393.1", "KI270393.1", 1308, 1308, Classification.CONTIG),
        new FastaEntry("KI270516.1", "KI270516.1", 1300, 1300, Classification.CONTIG),
        new FastaEntry("KI270389.1", "KI270389.1", 1298, 1298, Classification.CONTIG),
        new FastaEntry("KI270466.1", "KI270466.1", 1233, 1233, Classification.CONTIG),
        new FastaEntry("KI270388.1", "KI270388.1", 1216, 1216, Classification.CONTIG),
        new FastaEntry("KI270544.1", "KI270544.1", 1202, 1202, Classification.CONTIG),
        new FastaEntry("KI270310.1", "KI270310.1", 1201, 1201, Classification.CONTIG),
        new FastaEntry("KI270412.1", "KI270412.1", 1179, 1179, Classification.CONTIG),
        new FastaEntry("KI270395.1", "KI270395.1", 1143, 1143, Classification.CONTIG),
        new FastaEntry("KI270376.1", "KI270376.1", 1136, 1136, Classification.CONTIG),
        new FastaEntry("KI270337.1", "KI270337.1", 1121, 1121, Classification.CONTIG),
        new FastaEntry("KI270335.1", "KI270335.1", 1048, 1048, Classification.CONTIG),
        new FastaEntry("KI270378.1", "KI270378.1", 1048, 1048, Classification.CONTIG),
        new FastaEntry("KI270379.1", "KI270379.1", 1045, 1045, Classification.CONTIG),
        new FastaEntry("KI270329.1", "KI270329.1", 1040, 1040, Classification.CONTIG),
        new FastaEntry("KI270419.1", "KI270419.1", 1029, 1029, Classification.CONTIG),
        new FastaEntry("KI270336.1", "KI270336.1", 1026, 1026, Classification.CONTIG),
        new FastaEntry("KI270312.1", "KI270312.1", 998, 998, Classification.CONTIG),
        new FastaEntry("KI270539.1", "KI270539.1", 993, 993, Classification.CONTIG),
        new FastaEntry("KI270385.1", "KI270385.1", 990, 990, Classification.CONTIG),
        new FastaEntry("KI270423.1", "KI270423.1", 981, 981, Classification.CONTIG),
        new FastaEntry("KI270392.1", "KI270392.1", 971, 971, Classification.CONTIG),
        new FastaEntry("KI270394.1", "KI270394.1", 970, 970, Classification.CONTIG),
]

ReferenceGenomeService referenceGenomeService = ctx.referenceGenomeService
referenceGenomeService.loadReferenceGenome(name, species, speciesWithStrain, path, fileNamePrefix, cytosinePositionsIndex, chromosomePrefix, chromosomeSuffix,
        fastaEntries, fingerPrintingFileName, defaultStatSizeFileName, furtherStatSizeFileNames)
