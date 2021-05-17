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

import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification
import groovy.transform.Field

import de.dkfz.tbi.otp.ngsdata.referencegenome.FastaEntry
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService

/*
 *  This scripts inserts a new ReferenceGenome object
 *  as well as StatSizeFileName and ReferenceGenomeEntry objects obtained from the fasta file
 *  which lies in ${OtpProperty#PATH_PROCESSING_ROOT}/reference_genomes/${path}/
 *  A python helper script 'scripts/ReferenceGenome/getReferenceGenomeInfo.py' which extracts
 *  information from the fasta file was used to generate the fastaEntries content.
*/

String name = "1KGRef_PhiX"
@Field
String path = "bwa06_1KGRef_PhiX"
String fileNamePrefix = "hs37d5_PhiX"
String cytosinePositionsIndex = null
String chromosomePrefix = ""
String chromosomeSuffix = ""
String fingerPrintingFileName = "snp138Common.n1000.vh20140318.bed"
@Field
List<String> statSizeFileNames = [
        "hs37d5_PhiX.fa.chrLenOnlyACGT_realChromosomes.tab",
        "hs37d5_PhiX.fa.chrLenOnlyACGT.tab",
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
        new FastaEntry("MT", "M", 16569, 16568, Classification.MITOCHONDRIAL),
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
        new FastaEntry("phiX174", "phiX174", 5386, 5386, Classification.UNDEFINED),
]

ReferenceGenomeService referenceGenomeService = ctx.referenceGenomeService
referenceGenomeService.loadReferenceGenome(name, path, fileNamePrefix, cytosinePositionsIndex, chromosomePrefix, chromosomeSuffix,
        fastaEntries, fingerPrintingFileName, statSizeFileNames)
