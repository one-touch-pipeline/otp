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

import de.dkfz.tbi.otp.ngsdata.*
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
*/

String name = "hg_GRCm38"
Set<Species> species = [
        CollectionUtils.exactlyOneElement(Species.where { scientificName == "Mus musculus" }.list()),
] as Set
Set<SpeciesWithStrain> speciesWithStrain = [] as Set
@Field
String path = "hg_GRCm38"
String fileNamePrefix = "hg_GRCm38"
String cytosinePositionsIndex = null
String chromosomePrefix = ""
String chromosomeSuffix = ""
String fingerPrintingFileName = null
String defaultStatSizeFileName = null
@Field
List<String> furtherStatSizeFileNames = [
]

// Autogenerated code: use python helper script 'scripts/ReferenceGenome/getReferenceGenomeInfo.py'
List<FastaEntry> fastaEntries = [
        new FastaEntry("1", "1", 195471971, 191909192, Classification.CHROMOSOME),
        new FastaEntry("10", "10", 130694993, 127067662, Classification.CHROMOSOME),
        new FastaEntry("11", "11", 122082543, 118745945, Classification.CHROMOSOME),
        new FastaEntry("12", "12", 120129022, 116922420, Classification.CHROMOSOME),
        new FastaEntry("13", "13", 120421639, 117121193, Classification.CHROMOSOME),
        new FastaEntry("14", "14", 124902244, 121442110, Classification.CHROMOSOME),
        new FastaEntry("15", "15", 104043685, 100653315, Classification.CHROMOSOME),
        new FastaEntry("16", "16", 98207768, 95019758, Classification.CHROMOSOME),
        new FastaEntry("17", "17", 94987271, 91707462, Classification.CHROMOSOME),
        new FastaEntry("18", "18", 90702639, 87452634, Classification.CHROMOSOME),
        new FastaEntry("19", "19", 61431566, 58205856, Classification.CHROMOSOME),
        new FastaEntry("2", "2", 182113224, 178326651, Classification.CHROMOSOME),
        new FastaEntry("3", "3", 160039680, 156398855, Classification.CHROMOSOME),
        new FastaEntry("4", "4", 156508116, 152055611, Classification.CHROMOSOME),
        new FastaEntry("5", "5", 151834684, 147919674, Classification.CHROMOSOME),
        new FastaEntry("6", "6", 149736546, 146336543, Classification.CHROMOSOME),
        new FastaEntry("7", "7", 145441459, 141855407, Classification.CHROMOSOME),
        new FastaEntry("8", "8", 129401213, 125611432, Classification.CHROMOSOME),
        new FastaEntry("9", "9", 124595110, 121157018, Classification.CHROMOSOME),
        new FastaEntry("MT", "M", 16299, 16299, Classification.MITOCHONDRIAL),
        new FastaEntry("X", "X", 171031299, 163487995, Classification.CHROMOSOME),
        new FastaEntry("Y", "Y", 91744698, 88124698, Classification.CHROMOSOME),
        new FastaEntry("JH584299.1", "JH584299.1", 953012, 953012, Classification.CONTIG),
        new FastaEntry("GL456233.1", "GL456233.1", 336933, 313982, Classification.CONTIG),
        new FastaEntry("JH584301.1", "JH584301.1", 259875, 259875, Classification.CONTIG),
        new FastaEntry("GL456211.1", "GL456211.1", 241735, 241735, Classification.CONTIG),
        new FastaEntry("GL456350.1", "GL456350.1", 227966, 227965, Classification.CONTIG),
        new FastaEntry("JH584293.1", "JH584293.1", 207968, 207968, Classification.CONTIG),
        new FastaEntry("GL456221.1", "GL456221.1", 206961, 206961, Classification.CONTIG),
        new FastaEntry("JH584297.1", "JH584297.1", 205776, 205776, Classification.CONTIG),
        new FastaEntry("JH584296.1", "JH584296.1", 199368, 199368, Classification.CONTIG),
        new FastaEntry("GL456354.1", "GL456354.1", 195993, 195993, Classification.CONTIG),
        new FastaEntry("JH584294.1", "JH584294.1", 191905, 191905, Classification.CONTIG),
        new FastaEntry("JH584298.1", "JH584298.1", 184189, 184189, Classification.CONTIG),
        new FastaEntry("JH584300.1", "JH584300.1", 182347, 182347, Classification.CONTIG),
        new FastaEntry("GL456219.1", "GL456219.1", 175968, 175968, Classification.CONTIG),
        new FastaEntry("GL456210.1", "GL456210.1", 169725, 169725, Classification.CONTIG),
        new FastaEntry("JH584303.1", "JH584303.1", 158099, 158099, Classification.CONTIG),
        new FastaEntry("JH584302.1", "JH584302.1", 155838, 155838, Classification.CONTIG),
        new FastaEntry("GL456212.1", "GL456212.1", 153618, 153618, Classification.CONTIG),
        new FastaEntry("JH584304.1", "JH584304.1", 114452, 114452, Classification.CONTIG),
        new FastaEntry("GL456379.1", "GL456379.1", 72385, 70080, Classification.CONTIG),
        new FastaEntry("GL456216.1", "GL456216.1", 66673, 63701, Classification.CONTIG),
        new FastaEntry("GL456393.1", "GL456393.1", 55711, 50940, Classification.CONTIG),
        new FastaEntry("GL456366.1", "GL456366.1", 47073, 41699, Classification.CONTIG),
        new FastaEntry("GL456367.1", "GL456367.1", 42057, 40625, Classification.CONTIG),
        new FastaEntry("GL456239.1", "GL456239.1", 40056, 40056, Classification.CONTIG),
        new FastaEntry("GL456213.1", "GL456213.1", 39340, 39340, Classification.CONTIG),
        new FastaEntry("GL456383.1", "GL456383.1", 38659, 31201, Classification.CONTIG),
        new FastaEntry("GL456385.1", "GL456385.1", 35240, 35140, Classification.CONTIG),
        new FastaEntry("GL456360.1", "GL456360.1", 31704, 31342, Classification.CONTIG),
        new FastaEntry("GL456378.1", "GL456378.1", 31602, 30423, Classification.CONTIG),
        new FastaEntry("GL456389.1", "GL456389.1", 28772, 25359, Classification.CONTIG),
        new FastaEntry("GL456372.1", "GL456372.1", 28664, 23417, Classification.CONTIG),
        new FastaEntry("GL456370.1", "GL456370.1", 26764, 20736, Classification.CONTIG),
        new FastaEntry("GL456381.1", "GL456381.1", 25871, 20755, Classification.CONTIG),
        new FastaEntry("GL456387.1", "GL456387.1", 24685, 21632, Classification.CONTIG),
        new FastaEntry("GL456390.1", "GL456390.1", 24668, 13148, Classification.CONTIG),
        new FastaEntry("GL456394.1", "GL456394.1", 24323, 21664, Classification.CONTIG),
        new FastaEntry("GL456392.1", "GL456392.1", 23629, 23167, Classification.CONTIG),
        new FastaEntry("GL456382.1", "GL456382.1", 23158, 22380, Classification.CONTIG),
        new FastaEntry("GL456359.1", "GL456359.1", 22974, 22874, Classification.CONTIG),
        new FastaEntry("GL456396.1", "GL456396.1", 21240, 20505, Classification.CONTIG),
        new FastaEntry("GL456368.1", "GL456368.1", 20208, 19889, Classification.CONTIG),
        new FastaEntry("JH584292.1", "JH584292.1", 14945, 14945, Classification.CONTIG),
        new FastaEntry("JH584295.1", "JH584295.1", 1976, 1976, Classification.CONTIG),
]
// END autogenerated code
ReferenceGenomeIndex.withTransaction {
    ToolName tool = CollectionUtils.atMostOneElement(ToolName.findAllByName("CELL_RANGER"))

    ReferenceGenomeService referenceGenomeService = ctx.referenceGenomeService
    referenceGenomeService.loadReferenceGenome(name, species, speciesWithStrain, path, fileNamePrefix, cytosinePositionsIndex, chromosomePrefix, chromosomeSuffix,
            fastaEntries, fingerPrintingFileName, defaultStatSizeFileName, furtherStatSizeFileNames)

    ReferenceGenome referenceGenome = CollectionUtils.atMostOneElement(ReferenceGenome.findAllByName(name))
    assert referenceGenome, "couldn't get/save, all is lost!"

    ReferenceGenomeIndex index = new ReferenceGenomeIndex(
            toolName: tool,
            referenceGenome: referenceGenome,
            path: '1.2.0',
            indexToolVersion: '1.2.0',
    )
    index.save(flush: true)
}
