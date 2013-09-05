import java.util.List
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification

/*
 *  This scripts inserts a new ReferenceGenome object
 *  as well as ReferenceGenomeEntry objects obtained from the fasta file
 *  which lies in ${otp.processing.root.path}/reference_genomes/bwa06_1KGRef/
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
refGen.name = "hs37d5"
refGen.path = "bwa06_1KGRef"
refGen.fileNamePrefix = "hs37d5"
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
    ["MT", "M", 16569, 16568, "MITOCHONDRIAL"],
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
    ["hs37d5", "hs37d5", 35477943, 35477373, "UNDEFINED"]
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
