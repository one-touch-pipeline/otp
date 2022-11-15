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
import de.dkfz.tbi.otp.ngsdata.taxonomy.Species
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.utils.CollectionUtils

/*
 *  This scripts inserts a new ReferenceGenome object
 *  as well as StatSizeFileName and ReferenceGenomeEntry objects obtained from the fasta file
 *  which lies in ${OtpProperty#BASE_PATH_REFERENCE_GENOME}/${path}/
 *  A python helper script 'scripts/ReferenceGenome/getReferenceGenomeInfo.py' which extracts
 *  information from the fasta file was used to generate the fastaEntries content.
*/

String name = "hs37d5_GRCm38mm"
Set<Species> species = [
        CollectionUtils.exactlyOneElement(Species.where { scientificName == "Mus musculus" }.list()),
] as Set
Set<SpeciesWithStrain> speciesWithStrain = [
        CollectionUtils.exactlyOneElement(SpeciesWithStrain.where { species.scientificName == "Homo sapiens" && strain.name == "No strain available" }.list()),
] as Set
@Field
String path = "bwa06_hs37d5_GRCm38mm"
String fileNamePrefix = "hs37d5_GRCm38mm"
String cytosinePositionsIndex = null
String chromosomePrefix = ""
String chromosomeSuffix = ""
String fingerPrintingFileName = "snp138Common.n1000.vh20140318.bed"
String defaultStatSizeFileName = "hs37d5_GRCm38mm.fa.chrLenOnlyACGT_realChromosomes.tab"
@Field
List<String> furtherStatSizeFileNames = [
        "hs37d5_GRCm38mm.fa.chrLenOnlyACGT.tab",
]

List<FastaEntry> fastaEntries = [
        new FastaEntry("1", "1", 249250621, 225280621, Classification.CHROMOSOME),
        new FastaEntry("2", "2", 243199373, 238204518, Classification.CHROMOSOME),
        new FastaEntry("3", "3", 198022430, 194797138, Classification.CHROMOSOME),
        new FastaEntry("4", "4", 191154276, 187661676, Classification.CHROMOSOME),
        new FastaEntry("5", "5", 180915260, 177695260, Classification.CHROMOSOME),
        new FastaEntry("6", "6", 171115067, 167395066, Classification.CHROMOSOME),
        new FastaEntry("7", "7", 159138663, 155353663, Classification.CHROMOSOME),
        new FastaEntry("8", "8", 146364022, 142888922, Classification.CHROMOSOME),
        new FastaEntry("9", "9", 141213431, 120143431, Classification.CHROMOSOME),
        new FastaEntry("10", "10", 135534747, 131314738, Classification.CHROMOSOME),
        new FastaEntry("11", "11", 135006516, 131129516, Classification.CHROMOSOME),
        new FastaEntry("12", "12", 133851895, 130481393, Classification.CHROMOSOME),
        new FastaEntry("13", "13", 115169878, 95589878, Classification.CHROMOSOME),
        new FastaEntry("14", "14", 107349540, 88289540, Classification.CHROMOSOME),
        new FastaEntry("15", "15", 102531392, 81694766, Classification.CHROMOSOME),
        new FastaEntry("16", "16", 90354753, 78884753, Classification.CHROMOSOME),
        new FastaEntry("17", "17", 81195210, 77795210, Classification.CHROMOSOME),
        new FastaEntry("18", "18", 78077248, 74657229, Classification.CHROMOSOME),
        new FastaEntry("19", "19", 59128983, 55808983, Classification.CHROMOSOME),
        new FastaEntry("20", "20", 63025520, 59505520, Classification.CHROMOSOME),
        new FastaEntry("21", "21", 48129895, 35106642, Classification.CHROMOSOME),
        new FastaEntry("22", "22", 51304566, 34894545, Classification.CHROMOSOME),
        new FastaEntry("X", "X", 155270560, 151100560, Classification.CHROMOSOME),
        new FastaEntry("Y", "Y", 59373566, 22984529, Classification.CHROMOSOME),
        new FastaEntry("MT", "MT", 16569, 16568, Classification.MITOCHONDRIAL),
        new FastaEntry("GL000207.1", "GL000207.1", 4262, 4262, Classification.CONTIG),
        new FastaEntry("GL000226.1", "GL000226.1", 15008, 15008, Classification.CONTIG),
        new FastaEntry("GL000229.1", "GL000229.1", 19913, 19913, Classification.CONTIG),
        new FastaEntry("GL000231.1", "GL000231.1", 27386, 27386, Classification.CONTIG),
        new FastaEntry("GL000210.1", "GL000210.1", 27682, 27582, Classification.CONTIG),
        new FastaEntry("GL000239.1", "GL000239.1", 33824, 33824, Classification.CONTIG),
        new FastaEntry("GL000235.1", "GL000235.1", 34474, 34474, Classification.CONTIG),
        new FastaEntry("GL000201.1", "GL000201.1", 36148, 36148, Classification.CONTIG),
        new FastaEntry("GL000247.1", "GL000247.1", 36422, 36422, Classification.CONTIG),
        new FastaEntry("GL000245.1", "GL000245.1", 36651, 36651, Classification.CONTIG),
        new FastaEntry("GL000197.1", "GL000197.1", 37175, 37075, Classification.CONTIG),
        new FastaEntry("GL000203.1", "GL000203.1", 37498, 37498, Classification.CONTIG),
        new FastaEntry("GL000246.1", "GL000246.1", 38154, 38154, Classification.CONTIG),
        new FastaEntry("GL000249.1", "GL000249.1", 38502, 38502, Classification.CONTIG),
        new FastaEntry("GL000196.1", "GL000196.1", 38914, 38914, Classification.CONTIG),
        new FastaEntry("GL000248.1", "GL000248.1", 39786, 39786, Classification.CONTIG),
        new FastaEntry("GL000244.1", "GL000244.1", 39929, 39929, Classification.CONTIG),
        new FastaEntry("GL000238.1", "GL000238.1", 39939, 39939, Classification.CONTIG),
        new FastaEntry("GL000202.1", "GL000202.1", 40103, 40103, Classification.CONTIG),
        new FastaEntry("GL000234.1", "GL000234.1", 40531, 40531, Classification.CONTIG),
        new FastaEntry("GL000232.1", "GL000232.1", 40652, 40652, Classification.CONTIG),
        new FastaEntry("GL000206.1", "GL000206.1", 41001, 41001, Classification.CONTIG),
        new FastaEntry("GL000240.1", "GL000240.1", 41933, 41933, Classification.CONTIG),
        new FastaEntry("GL000236.1", "GL000236.1", 41934, 41934, Classification.CONTIG),
        new FastaEntry("GL000241.1", "GL000241.1", 42152, 42152, Classification.CONTIG),
        new FastaEntry("GL000243.1", "GL000243.1", 43341, 43341, Classification.CONTIG),
        new FastaEntry("GL000242.1", "GL000242.1", 43523, 43523, Classification.CONTIG),
        new FastaEntry("GL000230.1", "GL000230.1", 43691, 43691, Classification.CONTIG),
        new FastaEntry("GL000237.1", "GL000237.1", 45867, 45867, Classification.CONTIG),
        new FastaEntry("GL000233.1", "GL000233.1", 45941, 45941, Classification.CONTIG),
        new FastaEntry("GL000204.1", "GL000204.1", 81310, 81310, Classification.CONTIG),
        new FastaEntry("GL000198.1", "GL000198.1", 90085, 90085, Classification.CONTIG),
        new FastaEntry("GL000208.1", "GL000208.1", 92689, 92689, Classification.CONTIG),
        new FastaEntry("GL000191.1", "GL000191.1", 106433, 106433, Classification.CONTIG),
        new FastaEntry("GL000227.1", "GL000227.1", 128374, 128374, Classification.CONTIG),
        new FastaEntry("GL000228.1", "GL000228.1", 129120, 129120, Classification.CONTIG),
        new FastaEntry("GL000214.1", "GL000214.1", 137718, 137718, Classification.CONTIG),
        new FastaEntry("GL000221.1", "GL000221.1", 155397, 155397, Classification.CONTIG),
        new FastaEntry("GL000209.1", "GL000209.1", 159169, 159169, Classification.CONTIG),
        new FastaEntry("GL000218.1", "GL000218.1", 161147, 161147, Classification.CONTIG),
        new FastaEntry("GL000220.1", "GL000220.1", 161802, 161802, Classification.CONTIG),
        new FastaEntry("GL000213.1", "GL000213.1", 164239, 164239, Classification.CONTIG),
        new FastaEntry("GL000211.1", "GL000211.1", 166566, 166566, Classification.CONTIG),
        new FastaEntry("GL000199.1", "GL000199.1", 169874, 169874, Classification.CONTIG),
        new FastaEntry("GL000217.1", "GL000217.1", 172149, 172149, Classification.CONTIG),
        new FastaEntry("GL000216.1", "GL000216.1", 172294, 172294, Classification.CONTIG),
        new FastaEntry("GL000215.1", "GL000215.1", 172545, 172545, Classification.CONTIG),
        new FastaEntry("GL000205.1", "GL000205.1", 174588, 174588, Classification.CONTIG),
        new FastaEntry("GL000219.1", "GL000219.1", 179198, 179198, Classification.CONTIG),
        new FastaEntry("GL000224.1", "GL000224.1", 179693, 179693, Classification.CONTIG),
        new FastaEntry("GL000223.1", "GL000223.1", 180455, 180455, Classification.CONTIG),
        new FastaEntry("GL000195.1", "GL000195.1", 182896, 182896, Classification.CONTIG),
        new FastaEntry("GL000212.1", "GL000212.1", 186858, 186858, Classification.CONTIG),
        new FastaEntry("GL000222.1", "GL000222.1", 186861, 186861, Classification.CONTIG),
        new FastaEntry("GL000200.1", "GL000200.1", 187035, 187035, Classification.CONTIG),
        new FastaEntry("GL000193.1", "GL000193.1", 189789, 189789, Classification.CONTIG),
        new FastaEntry("GL000194.1", "GL000194.1", 191469, 191469, Classification.CONTIG),
        new FastaEntry("GL000225.1", "GL000225.1", 211173, 211173, Classification.CONTIG),
        new FastaEntry("GL000192.1", "GL000192.1", 547496, 547496, Classification.CONTIG),
        new FastaEntry("NC_007605", "NC_007605", 171823, 171823, Classification.UNDEFINED),
        new FastaEntry("hs37d5", "hs37d5", 35477943, 35477373, Classification.UNDEFINED),
        new FastaEntry("chr1", "chr1", 195471971, 192205969, Classification.CHROMOSOME),
        new FastaEntry("chr10", "chr10", 130694993, 127077067, Classification.CHROMOSOME),
        new FastaEntry("chr11", "chr11", 122082543, 118746045, Classification.CHROMOSOME),
        new FastaEntry("chr12", "chr12", 120129022, 116929021, Classification.CHROMOSOME),
        new FastaEntry("chr13", "chr13", 120421639, 117221639, Classification.CHROMOSOME),
        new FastaEntry("chr14", "chr14", 124902244, 121602243, Classification.CHROMOSOME),
        new FastaEntry("chr15", "chr15", 104043685, 100745472, Classification.CHROMOSOME),
        new FastaEntry("chr16", "chr16", 98207768, 95107762, Classification.CHROMOSOME),
        new FastaEntry("chr17", "chr17", 94987271, 91824270, Classification.CHROMOSOME),
        new FastaEntry("chr18", "chr18", 90702639, 87602638, Classification.CHROMOSOME),
        new FastaEntry("chr19", "chr19", 61431566, 58206056, Classification.CHROMOSOME),
        new FastaEntry("chr2", "chr2", 182113224, 178382925, Classification.CHROMOSOME),
        new FastaEntry("chr3", "chr3", 160039680, 156449717, Classification.CHROMOSOME),
        new FastaEntry("chr4", "chr4", 156508116, 152603261, Classification.CHROMOSOME),
        new FastaEntry("chr5", "chr5", 151834684, 148277684, Classification.CHROMOSOME),
        new FastaEntry("chr6", "chr6", 149736546, 146536546, Classification.CHROMOSOME),
        new FastaEntry("chr7", "chr7", 145441459, 142186438, Classification.CHROMOSOME),
        new FastaEntry("chr8", "chr8", 129401213, 125779213, Classification.CHROMOSOME),
        new FastaEntry("chr9", "chr9", 124595110, 121317703, Classification.CHROMOSOME),
        new FastaEntry("chrMT", "chrMT", 16299, 16299, Classification.MITOCHONDRIAL),
        new FastaEntry("chrX", "chrX", 171031299, 164308898, Classification.CHROMOSOME),
        new FastaEntry("chrY", "chrY", 91744698, 88548698, Classification.CHROMOSOME),
        new FastaEntry("chrJH584295.1", "chrJH584295.1", 1976, 1976, Classification.CONTIG),
        new FastaEntry("chrJH584292.1", "chrJH584292.1", 14945, 14945, Classification.CONTIG),
        new FastaEntry("chrGL456368.1", "chrGL456368.1", 20208, 20208, Classification.CONTIG),
        new FastaEntry("chrGL456396.1", "chrGL456396.1", 21240, 21240, Classification.CONTIG),
        new FastaEntry("chrGL456359.1", "chrGL456359.1", 22974, 22974, Classification.CONTIG),
        new FastaEntry("chrGL456382.1", "chrGL456382.1", 23158, 23158, Classification.CONTIG),
        new FastaEntry("chrGL456392.1", "chrGL456392.1", 23629, 23629, Classification.CONTIG),
        new FastaEntry("chrGL456394.1", "chrGL456394.1", 24323, 24323, Classification.CONTIG),
        new FastaEntry("chrGL456390.1", "chrGL456390.1", 24668, 24668, Classification.CONTIG),
        new FastaEntry("chrGL456387.1", "chrGL456387.1", 24685, 24685, Classification.CONTIG),
        new FastaEntry("chrGL456381.1", "chrGL456381.1", 25871, 25871, Classification.CONTIG),
        new FastaEntry("chrGL456370.1", "chrGL456370.1", 26764, 26764, Classification.CONTIG),
        new FastaEntry("chrGL456372.1", "chrGL456372.1", 28664, 28664, Classification.CONTIG),
        new FastaEntry("chrGL456389.1", "chrGL456389.1", 28772, 28772, Classification.CONTIG),
        new FastaEntry("chrGL456378.1", "chrGL456378.1", 31602, 31602, Classification.CONTIG),
        new FastaEntry("chrGL456360.1", "chrGL456360.1", 31704, 31704, Classification.CONTIG),
        new FastaEntry("chrGL456385.1", "chrGL456385.1", 35240, 35240, Classification.CONTIG),
        new FastaEntry("chrGL456383.1", "chrGL456383.1", 38659, 38659, Classification.CONTIG),
        new FastaEntry("chrGL456213.1", "chrGL456213.1", 39340, 39340, Classification.CONTIG),
        new FastaEntry("chrGL456239.1", "chrGL456239.1", 40056, 40056, Classification.CONTIG),
        new FastaEntry("chrGL456367.1", "chrGL456367.1", 42057, 42057, Classification.CONTIG),
        new FastaEntry("chrGL456366.1", "chrGL456366.1", 47073, 47073, Classification.CONTIG),
        new FastaEntry("chrGL456393.1", "chrGL456393.1", 55711, 55711, Classification.CONTIG),
        new FastaEntry("chrGL456216.1", "chrGL456216.1", 66673, 66673, Classification.CONTIG),
        new FastaEntry("chrGL456379.1", "chrGL456379.1", 72385, 72385, Classification.CONTIG),
        new FastaEntry("chrJH584304.1", "chrJH584304.1", 114452, 114452, Classification.CONTIG),
        new FastaEntry("chrGL456212.1", "chrGL456212.1", 153618, 153618, Classification.CONTIG),
        new FastaEntry("chrJH584302.1", "chrJH584302.1", 155838, 155838, Classification.CONTIG),
        new FastaEntry("chrJH584303.1", "chrJH584303.1", 158099, 158099, Classification.CONTIG),
        new FastaEntry("chrGL456210.1", "chrGL456210.1", 169725, 169725, Classification.CONTIG),
        new FastaEntry("chrGL456219.1", "chrGL456219.1", 175968, 175968, Classification.CONTIG),
        new FastaEntry("chrJH584300.1", "chrJH584300.1", 182347, 182347, Classification.CONTIG),
        new FastaEntry("chrJH584298.1", "chrJH584298.1", 184189, 184189, Classification.CONTIG),
        new FastaEntry("chrJH584294.1", "chrJH584294.1", 191905, 191905, Classification.CONTIG),
        new FastaEntry("chrGL456354.1", "chrGL456354.1", 195993, 195993, Classification.CONTIG),
        new FastaEntry("chrJH584296.1", "chrJH584296.1", 199368, 199368, Classification.CONTIG),
        new FastaEntry("chrJH584297.1", "chrJH584297.1", 205776, 205776, Classification.CONTIG),
        new FastaEntry("chrGL456221.1", "chrGL456221.1", 206961, 206961, Classification.CONTIG),
        new FastaEntry("chrJH584293.1", "chrJH584293.1", 207968, 207968, Classification.CONTIG),
        new FastaEntry("chrGL456350.1", "chrGL456350.1", 227966, 227965, Classification.CONTIG),
        new FastaEntry("chrGL456211.1", "chrGL456211.1", 241735, 241735, Classification.CONTIG),
        new FastaEntry("chrJH584301.1", "chrJH584301.1", 259875, 259875, Classification.CONTIG),
        new FastaEntry("chrGL456233.1", "chrGL456233.1", 336933, 336933, Classification.CONTIG),
        new FastaEntry("chrJH584299.1", "chrJH584299.1", 953012, 953012, Classification.CONTIG),
]

ReferenceGenomeService referenceGenomeService = ctx.referenceGenomeService
referenceGenomeService.loadReferenceGenome(name, species, speciesWithStrain, path, fileNamePrefix, cytosinePositionsIndex, chromosomePrefix, chromosomeSuffix,
        fastaEntries, fingerPrintingFileName, defaultStatSizeFileName, furtherStatSizeFileNames)
