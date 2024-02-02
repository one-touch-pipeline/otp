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

String name = "GRCm38mm10_PhiX"
Set<Species> species = [
        CollectionUtils.exactlyOneElement(Species.where { scientificName == "Mus musculus" }.list()),
] as Set
Set<SpeciesWithStrain> speciesWithStrain = [] as Set
@Field
String path = "bwa06_GRCm38mm10_PhiX"
String fileNamePrefix = "GRCm38mm10_PhiX"
String cytosinePositionsIndex = null
String chromosomePrefix = ""
String chromosomeSuffix = ""
String fingerPrintingFileName = null
String defaultStatSizeFileName = "GRCm38mm10.fa.chrLenOnlyACGT_realChromosomes.tab"
@Field
List<String> furtherStatSizeFileNames = [
        "GRCm38mm10.fa.chrLenOnlyACGT.tab",
]

List<FastaEntry> fastaEntries = [
        new FastaEntry("1", "1", 195471971, 192205969, Classification.CHROMOSOME),
        new FastaEntry("10", "10", 130694993, 127077067, Classification.CHROMOSOME),
        new FastaEntry("11", "11", 122082543, 118746045, Classification.CHROMOSOME),
        new FastaEntry("12", "12", 120129022, 116929021, Classification.CHROMOSOME),
        new FastaEntry("13", "13", 120421639, 117221639, Classification.CHROMOSOME),
        new FastaEntry("14", "14", 124902244, 121602243, Classification.CHROMOSOME),
        new FastaEntry("15", "15", 104043685, 100745472, Classification.CHROMOSOME),
        new FastaEntry("16", "16", 98207768, 95107762, Classification.CHROMOSOME),
        new FastaEntry("17", "17", 94987271, 91824270, Classification.CHROMOSOME),
        new FastaEntry("18", "18", 90702639, 87602638, Classification.CHROMOSOME),
        new FastaEntry("19", "19", 61431566, 58206056, Classification.CHROMOSOME),
        new FastaEntry("2", "2", 182113224, 178382925, Classification.CHROMOSOME),
        new FastaEntry("3", "3", 160039680, 156449717, Classification.CHROMOSOME),
        new FastaEntry("4", "4", 156508116, 152603261, Classification.CHROMOSOME),
        new FastaEntry("5", "5", 151834684, 148277684, Classification.CHROMOSOME),
        new FastaEntry("6", "6", 149736546, 146536546, Classification.CHROMOSOME),
        new FastaEntry("7", "7", 145441459, 142186438, Classification.CHROMOSOME),
        new FastaEntry("8", "8", 129401213, 125779213, Classification.CHROMOSOME),
        new FastaEntry("9", "9", 124595110, 121317703, Classification.CHROMOSOME),
        new FastaEntry("MT", "MT", 16299, 16299, Classification.MITOCHONDRIAL),
        new FastaEntry("X", "X", 171031299, 164308898, Classification.CHROMOSOME),
        new FastaEntry("Y", "Y", 91744698, 88548698, Classification.CHROMOSOME),
        new FastaEntry("JH584295.1", "JH584295.1", 1976, 1976, Classification.CONTIG),
        new FastaEntry("JH584292.1", "JH584292.1", 14945, 14945, Classification.CONTIG),
        new FastaEntry("GL456368.1", "GL456368.1", 20208, 20208, Classification.CONTIG),
        new FastaEntry("GL456396.1", "GL456396.1", 21240, 21240, Classification.CONTIG),
        new FastaEntry("GL456359.1", "GL456359.1", 22974, 22974, Classification.CONTIG),
        new FastaEntry("GL456382.1", "GL456382.1", 23158, 23158, Classification.CONTIG),
        new FastaEntry("GL456392.1", "GL456392.1", 23629, 23629, Classification.CONTIG),
        new FastaEntry("GL456394.1", "GL456394.1", 24323, 24323, Classification.CONTIG),
        new FastaEntry("GL456390.1", "GL456390.1", 24668, 24668, Classification.CONTIG),
        new FastaEntry("GL456387.1", "GL456387.1", 24685, 24685, Classification.CONTIG),
        new FastaEntry("GL456381.1", "GL456381.1", 25871, 25871, Classification.CONTIG),
        new FastaEntry("GL456370.1", "GL456370.1", 26764, 26764, Classification.CONTIG),
        new FastaEntry("GL456372.1", "GL456372.1", 28664, 28664, Classification.CONTIG),
        new FastaEntry("GL456389.1", "GL456389.1", 28772, 28772, Classification.CONTIG),
        new FastaEntry("GL456378.1", "GL456378.1", 31602, 31602, Classification.CONTIG),
        new FastaEntry("GL456360.1", "GL456360.1", 31704, 31704, Classification.CONTIG),
        new FastaEntry("GL456385.1", "GL456385.1", 35240, 35240, Classification.CONTIG),
        new FastaEntry("GL456383.1", "GL456383.1", 38659, 38659, Classification.CONTIG),
        new FastaEntry("GL456213.1", "GL456213.1", 39340, 39340, Classification.CONTIG),
        new FastaEntry("GL456239.1", "GL456239.1", 40056, 40056, Classification.CONTIG),
        new FastaEntry("GL456367.1", "GL456367.1", 42057, 42057, Classification.CONTIG),
        new FastaEntry("GL456366.1", "GL456366.1", 47073, 47073, Classification.CONTIG),
        new FastaEntry("GL456393.1", "GL456393.1", 55711, 55711, Classification.CONTIG),
        new FastaEntry("GL456216.1", "GL456216.1", 66673, 66673, Classification.CONTIG),
        new FastaEntry("GL456379.1", "GL456379.1", 72385, 72385, Classification.CONTIG),
        new FastaEntry("JH584304.1", "JH584304.1", 114452, 114452, Classification.CONTIG),
        new FastaEntry("GL456212.1", "GL456212.1", 153618, 153618, Classification.CONTIG),
        new FastaEntry("JH584302.1", "JH584302.1", 155838, 155838, Classification.CONTIG),
        new FastaEntry("JH584303.1", "JH584303.1", 158099, 158099, Classification.CONTIG),
        new FastaEntry("GL456210.1", "GL456210.1", 169725, 169725, Classification.CONTIG),
        new FastaEntry("GL456219.1", "GL456219.1", 175968, 175968, Classification.CONTIG),
        new FastaEntry("JH584300.1", "JH584300.1", 182347, 182347, Classification.CONTIG),
        new FastaEntry("JH584298.1", "JH584298.1", 184189, 184189, Classification.CONTIG),
        new FastaEntry("JH584294.1", "JH584294.1", 191905, 191905, Classification.CONTIG),
        new FastaEntry("GL456354.1", "GL456354.1", 195993, 195993, Classification.CONTIG),
        new FastaEntry("JH584296.1", "JH584296.1", 199368, 199368, Classification.CONTIG),
        new FastaEntry("JH584297.1", "JH584297.1", 205776, 205776, Classification.CONTIG),
        new FastaEntry("GL456221.1", "GL456221.1", 206961, 206961, Classification.CONTIG),
        new FastaEntry("JH584293.1", "JH584293.1", 207968, 207968, Classification.CONTIG),
        new FastaEntry("GL456350.1", "GL456350.1", 227966, 227965, Classification.CONTIG),
        new FastaEntry("GL456211.1", "GL456211.1", 241735, 241735, Classification.CONTIG),
        new FastaEntry("JH584301.1", "JH584301.1", 259875, 259875, Classification.CONTIG),
        new FastaEntry("GL456233.1", "GL456233.1", 336933, 336933, Classification.CONTIG),
        new FastaEntry("JH584299.1", "JH584299.1", 953012, 953012, Classification.CONTIG),
        new FastaEntry("phiX174", "phiX174", 5386, 5386, Classification.UNDEFINED),
]

ReferenceGenomeService referenceGenomeService = ctx.referenceGenomeService
referenceGenomeService.loadReferenceGenome(name, species, speciesWithStrain, path, fileNamePrefix, cytosinePositionsIndex, chromosomePrefix, chromosomeSuffix,
        fastaEntries, fingerPrintingFileName, defaultStatSizeFileName, furtherStatSizeFileNames)
