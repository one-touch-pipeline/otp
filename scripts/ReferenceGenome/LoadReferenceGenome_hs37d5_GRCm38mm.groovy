import java.util.List
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification

/*
 *  This scripts inserts a new ReferenceGenome object
 *  as well as ReferenceGenomeEntry objects obtained from the fasta file
 *  which lies in ${otp.processing.root.path}/reference_genomes/bwa06_hs37d5_GRCm38mm/
 *  A python helper script 'scripts/ReferenceGenome/getReferenceGenomeInfo.py' which extracts
 *  information from the fasta file was used to generate the fastaEntries content.
*/

// get list of all standard chromosomes which is: 1..22, X, Y
List<String> standardChromosomes = Chromosomes.allLabels()
standardChromosomes.remove("M")
assert standardChromosomes.size() == 24

/*
 * Function to map the classification strings to enums to match
 * the ReferenceGenomeEntry object.
 * Default classification is UNDEFINED
 */
Classification mapClassification(String classificationInput) {
    Classification classification = Classification.UNDEFINED
    if ( classificationInput.equals("CHROMOSOME")) {
        classification = Classification.CHROMOSOME
    }
    if ( classificationInput.equals("MITOCHONDRIAL")) {
        classification = Classification.MITOCHONDRIAL
    }
    if ( classificationInput.equals("CONTIG")) {
        classification = Classification.CONTIG
    }
    return classification
}

ReferenceGenome refGen = new ReferenceGenome()
refGen.name = "hs37d5_GRCm38mm"
refGen.path = "bwa06_hs37d5_GRCm38mm"
refGen.fileNamePrefix = "hs37d5_GRCm38mm"
// Random values which have to be set due to the new constraint in the reference genome. They will be overwritten.
refGen.length = 100
refGen.lengthRefChromosomes = 100
refGen.lengthRefChromosomesWithoutN = 100
refGen.lengthWithoutN = 100
if (refGen.validate()) {
    refGen.save(flush: true)
    println("Inserted reference genome: " + refGen)
}
else {
    refGen.errors.allErrors.each {
        println it
    }
}

// list which holds information about all entries in the ref. genome fasta file
Map<String, Integer> fastaEntriesColumn = ["name":0, "alias":1, "length":2, "lengthWithoutN":3, "classification":4]
List<List<String, Long, Long>> fastaEntries = [
        ["1", "1", 249250621, 225280621, "CHROMOSOME"],
        ["2", "2", 243199373, 238204518, "CHROMOSOME"],
        ["3", "3", 198022430, 194797138, "CHROMOSOME"],
        ["4", "4", 191154276, 187661676, "CHROMOSOME"],
        ["5", "5", 180915260, 177695260, "CHROMOSOME"],
        ["6", "6", 171115067, 167395066, "CHROMOSOME"],
        ["7", "7", 159138663, 155353663, "CHROMOSOME"],
        ["8", "8", 146364022, 142888922, "CHROMOSOME"],
        ["9", "9", 141213431, 120143431, "CHROMOSOME"],
        ["10", "10", 135534747, 131314738, "CHROMOSOME"],
        ["11", "11", 135006516, 131129516, "CHROMOSOME"],
        ["12", "12", 133851895, 130481393, "CHROMOSOME"],
        ["13", "13", 115169878, 95589878, "CHROMOSOME"],
        ["14", "14", 107349540, 88289540, "CHROMOSOME"],
        ["15", "15", 102531392, 81694766, "CHROMOSOME"],
        ["16", "16", 90354753, 78884753, "CHROMOSOME"],
        ["17", "17", 81195210, 77795210, "CHROMOSOME"],
        ["18", "18", 78077248, 74657229, "CHROMOSOME"],
        ["19", "19", 59128983, 55808983, "CHROMOSOME"],
        ["20", "20", 63025520, 59505520, "CHROMOSOME"],
        ["21", "21", 48129895, 35106642, "CHROMOSOME"],
        ["22", "22", 51304566, 34894545, "CHROMOSOME"],
        ["X", "X", 155270560, 151100560, "CHROMOSOME"],
        ["Y", "Y", 59373566, 22984529, "CHROMOSOME"],
        ["MT", "MT", 16569, 16568, "MITOCHONDRIAL"],
        ["GL000207.1", "GL000207.1", 4262, 4262, "CONTIG"],
        ["GL000226.1", "GL000226.1", 15008, 15008, "CONTIG"],
        ["GL000229.1", "GL000229.1", 19913, 19913, "CONTIG"],
        ["GL000231.1", "GL000231.1", 27386, 27386, "CONTIG"],
        ["GL000210.1", "GL000210.1", 27682, 27582, "CONTIG"],
        ["GL000239.1", "GL000239.1", 33824, 33824, "CONTIG"],
        ["GL000235.1", "GL000235.1", 34474, 34474, "CONTIG"],
        ["GL000201.1", "GL000201.1", 36148, 36148, "CONTIG"],
        ["GL000247.1", "GL000247.1", 36422, 36422, "CONTIG"],
        ["GL000245.1", "GL000245.1", 36651, 36651, "CONTIG"],
        ["GL000197.1", "GL000197.1", 37175, 37075, "CONTIG"],
        ["GL000203.1", "GL000203.1", 37498, 37498, "CONTIG"],
        ["GL000246.1", "GL000246.1", 38154, 38154, "CONTIG"],
        ["GL000249.1", "GL000249.1", 38502, 38502, "CONTIG"],
        ["GL000196.1", "GL000196.1", 38914, 38914, "CONTIG"],
        ["GL000248.1", "GL000248.1", 39786, 39786, "CONTIG"],
        ["GL000244.1", "GL000244.1", 39929, 39929, "CONTIG"],
        ["GL000238.1", "GL000238.1", 39939, 39939, "CONTIG"],
        ["GL000202.1", "GL000202.1", 40103, 40103, "CONTIG"],
        ["GL000234.1", "GL000234.1", 40531, 40531, "CONTIG"],
        ["GL000232.1", "GL000232.1", 40652, 40652, "CONTIG"],
        ["GL000206.1", "GL000206.1", 41001, 41001, "CONTIG"],
        ["GL000240.1", "GL000240.1", 41933, 41933, "CONTIG"],
        ["GL000236.1", "GL000236.1", 41934, 41934, "CONTIG"],
        ["GL000241.1", "GL000241.1", 42152, 42152, "CONTIG"],
        ["GL000243.1", "GL000243.1", 43341, 43341, "CONTIG"],
        ["GL000242.1", "GL000242.1", 43523, 43523, "CONTIG"],
        ["GL000230.1", "GL000230.1", 43691, 43691, "CONTIG"],
        ["GL000237.1", "GL000237.1", 45867, 45867, "CONTIG"],
        ["GL000233.1", "GL000233.1", 45941, 45941, "CONTIG"],
        ["GL000204.1", "GL000204.1", 81310, 81310, "CONTIG"],
        ["GL000198.1", "GL000198.1", 90085, 90085, "CONTIG"],
        ["GL000208.1", "GL000208.1", 92689, 92689, "CONTIG"],
        ["GL000191.1", "GL000191.1", 106433, 106433, "CONTIG"],
        ["GL000227.1", "GL000227.1", 128374, 128374, "CONTIG"],
        ["GL000228.1", "GL000228.1", 129120, 129120, "CONTIG"],
        ["GL000214.1", "GL000214.1", 137718, 137718, "CONTIG"],
        ["GL000221.1", "GL000221.1", 155397, 155397, "CONTIG"],
        ["GL000209.1", "GL000209.1", 159169, 159169, "CONTIG"],
        ["GL000218.1", "GL000218.1", 161147, 161147, "CONTIG"],
        ["GL000220.1", "GL000220.1", 161802, 161802, "CONTIG"],
        ["GL000213.1", "GL000213.1", 164239, 164239, "CONTIG"],
        ["GL000211.1", "GL000211.1", 166566, 166566, "CONTIG"],
        ["GL000199.1", "GL000199.1", 169874, 169874, "CONTIG"],
        ["GL000217.1", "GL000217.1", 172149, 172149, "CONTIG"],
        ["GL000216.1", "GL000216.1", 172294, 172294, "CONTIG"],
        ["GL000215.1", "GL000215.1", 172545, 172545, "CONTIG"],
        ["GL000205.1", "GL000205.1", 174588, 174588, "CONTIG"],
        ["GL000219.1", "GL000219.1", 179198, 179198, "CONTIG"],
        ["GL000224.1", "GL000224.1", 179693, 179693, "CONTIG"],
        ["GL000223.1", "GL000223.1", 180455, 180455, "CONTIG"],
        ["GL000195.1", "GL000195.1", 182896, 182896, "CONTIG"],
        ["GL000212.1", "GL000212.1", 186858, 186858, "CONTIG"],
        ["GL000222.1", "GL000222.1", 186861, 186861, "CONTIG"],
        ["GL000200.1", "GL000200.1", 187035, 187035, "CONTIG"],
        ["GL000193.1", "GL000193.1", 189789, 189789, "CONTIG"],
        ["GL000194.1", "GL000194.1", 191469, 191469, "CONTIG"],
        ["GL000225.1", "GL000225.1", 211173, 211173, "CONTIG"],
        ["GL000192.1", "GL000192.1", 547496, 547496, "CONTIG"],
        ["NC_007605", "NC_007605", 171823, 171823, "UNDEFINED"],
        ["hs37d5", "hs37d5", 35477943, 35477373, "UNDEFINED"],
        ["chr1", "chr1", 195471971, 192205969, "CHROMOSOME"],
        ["chr10", "chr10", 130694993, 127077067, "CHROMOSOME"],
        ["chr11", "chr11", 122082543, 118746045, "CHROMOSOME"],
        ["chr12", "chr12", 120129022, 116929021, "CHROMOSOME"],
        ["chr13", "chr13", 120421639, 117221639, "CHROMOSOME"],
        ["chr14", "chr14", 124902244, 121602243, "CHROMOSOME"],
        ["chr15", "chr15", 104043685, 100745472, "CHROMOSOME"],
        ["chr16", "chr16", 98207768, 95107762, "CHROMOSOME"],
        ["chr17", "chr17", 94987271, 91824270, "CHROMOSOME"],
        ["chr18", "chr18", 90702639, 87602638, "CHROMOSOME"],
        ["chr19", "chr19", 61431566, 58206056, "CHROMOSOME"],
        ["chr2", "chr2", 182113224, 178382925, "CHROMOSOME"],
        ["chr3", "chr3", 160039680, 156449717, "CHROMOSOME"],
        ["chr4", "chr4", 156508116, 152603261, "CHROMOSOME"],
        ["chr5", "chr5", 151834684, 148277684, "CHROMOSOME"],
        ["chr6", "chr6", 149736546, 146536546, "CHROMOSOME"],
        ["chr7", "chr7", 145441459, 142186438, "CHROMOSOME"],
        ["chr8", "chr8", 129401213, 125779213, "CHROMOSOME"],
        ["chr9", "chr9", 124595110, 121317703, "CHROMOSOME"],
        ["chrMT", "chrMT", 16299, 16299, "MITOCHONDRIAL"],
        ["chrX", "chrX", 171031299, 164308898, "CHROMOSOME"],
        ["chrY", "chrY", 91744698, 88548698, "CHROMOSOME"],
        ["chrJH584295.1", "chrJH584295.1", 1976, 1976, "CONTIG"],
        ["chrJH584292.1", "chrJH584292.1", 14945, 14945, "CONTIG"],
        ["chrGL456368.1", "chrGL456368.1", 20208, 20208, "CONTIG"],
        ["chrGL456396.1", "chrGL456396.1", 21240, 21240, "CONTIG"],
        ["chrGL456359.1", "chrGL456359.1", 22974, 22974, "CONTIG"],
        ["chrGL456382.1", "chrGL456382.1", 23158, 23158, "CONTIG"],
        ["chrGL456392.1", "chrGL456392.1", 23629, 23629, "CONTIG"],
        ["chrGL456394.1", "chrGL456394.1", 24323, 24323, "CONTIG"],
        ["chrGL456390.1", "chrGL456390.1", 24668, 24668, "CONTIG"],
        ["chrGL456387.1", "chrGL456387.1", 24685, 24685, "CONTIG"],
        ["chrGL456381.1", "chrGL456381.1", 25871, 25871, "CONTIG"],
        ["chrGL456370.1", "chrGL456370.1", 26764, 26764, "CONTIG"],
        ["chrGL456372.1", "chrGL456372.1", 28664, 28664, "CONTIG"],
        ["chrGL456389.1", "chrGL456389.1", 28772, 28772, "CONTIG"],
        ["chrGL456378.1", "chrGL456378.1", 31602, 31602, "CONTIG"],
        ["chrGL456360.1", "chrGL456360.1", 31704, 31704, "CONTIG"],
        ["chrGL456385.1", "chrGL456385.1", 35240, 35240, "CONTIG"],
        ["chrGL456383.1", "chrGL456383.1", 38659, 38659, "CONTIG"],
        ["chrGL456213.1", "chrGL456213.1", 39340, 39340, "CONTIG"],
        ["chrGL456239.1", "chrGL456239.1", 40056, 40056, "CONTIG"],
        ["chrGL456367.1", "chrGL456367.1", 42057, 42057, "CONTIG"],
        ["chrGL456366.1", "chrGL456366.1", 47073, 47073, "CONTIG"],
        ["chrGL456393.1", "chrGL456393.1", 55711, 55711, "CONTIG"],
        ["chrGL456216.1", "chrGL456216.1", 66673, 66673, "CONTIG"],
        ["chrGL456379.1", "chrGL456379.1", 72385, 72385, "CONTIG"],
        ["chrJH584304.1", "chrJH584304.1", 114452, 114452, "CONTIG"],
        ["chrGL456212.1", "chrGL456212.1", 153618, 153618, "CONTIG"],
        ["chrJH584302.1", "chrJH584302.1", 155838, 155838, "CONTIG"],
        ["chrJH584303.1", "chrJH584303.1", 158099, 158099, "CONTIG"],
        ["chrGL456210.1", "chrGL456210.1", 169725, 169725, "CONTIG"],
        ["chrGL456219.1", "chrGL456219.1", 175968, 175968, "CONTIG"],
        ["chrJH584300.1", "chrJH584300.1", 182347, 182347, "CONTIG"],
        ["chrJH584298.1", "chrJH584298.1", 184189, 184189, "CONTIG"],
        ["chrJH584294.1", "chrJH584294.1", 191905, 191905, "CONTIG"],
        ["chrGL456354.1", "chrGL456354.1", 195993, 195993, "CONTIG"],
        ["chrJH584296.1", "chrJH584296.1", 199368, 199368, "CONTIG"],
        ["chrJH584297.1", "chrJH584297.1", 205776, 205776, "CONTIG"],
        ["chrGL456221.1", "chrGL456221.1", 206961, 206961, "CONTIG"],
        ["chrJH584293.1", "chrJH584293.1", 207968, 207968, "CONTIG"],
        ["chrGL456350.1", "chrGL456350.1", 227966, 227965, "CONTIG"],
        ["chrGL456211.1", "chrGL456211.1", 241735, 241735, "CONTIG"],
        ["chrJH584301.1", "chrJH584301.1", 259875, 259875, "CONTIG"],
        ["chrGL456233.1", "chrGL456233.1", 336933, 336933, "CONTIG"],
        ["chrJH584299.1", "chrJH584299.1", 953012, 953012, "CONTIG"],
]

// init counter for overall length values
long length = 0
long lengthWithoutN = 0
long lengthRefChromosomes = 0
long lengthRefChromosomesWithoutN = 0

// put fastaEntry as ReferenceGenomeEntry information into database
fastaEntries.each { entry ->
    ReferenceGenomeEntry refGenEntry = new ReferenceGenomeEntry()
    refGenEntry.name = entry[fastaEntriesColumn.name]
    refGenEntry.alias = entry[fastaEntriesColumn.alias]
    refGenEntry.length = entry[fastaEntriesColumn.length]
    refGenEntry.lengthWithoutN = entry[fastaEntriesColumn.lengthWithoutN]
    refGenEntry.classification = mapClassification(entry[fastaEntriesColumn.classification])
    refGenEntry.referenceGenome = refGen
    if (refGenEntry.validate()) {
        refGenEntry.save(flush: true)
        println "Inserted ReferenceGenomeEntry: " + refGenEntry
    }
    else {
        refGenEntry.errors.allErrors.each {
            println it
        }
    }
    // overall counting
    length += entry[fastaEntriesColumn.length]
    lengthWithoutN += entry[fastaEntriesColumn.lengthWithoutN]
    // counting if entry is a standardChromosome
    if ( standardChromosomes.contains(refGenEntry.alias) ) {
        lengthRefChromosomes += refGenEntry.length
        lengthRefChromosomesWithoutN += refGenEntry.lengthWithoutN
    }
}

// put length values into database
refGen.length = length
refGen.lengthWithoutN = lengthWithoutN
refGen.lengthRefChromosomes = lengthRefChromosomes
refGen.lengthRefChromosomesWithoutN = lengthRefChromosomesWithoutN
refGen.save(flush: true)
