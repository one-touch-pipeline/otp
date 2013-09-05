import java.util.List
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification

/*
 *  This scripts inserts a new ReferenceGenome object
 *  as well as ReferenceGenomeEntry objects obtained from the fasta file
 *  which lies in ${otp.processing.root.path}/reference_genomes/bwa06_hg19_chr/
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
refGen.name = "hg19"
refGen.path = "bwa06_hg19_chr"
refGen.fileNamePrefix = "hg19_1-22_X_Y_M"
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
    ["chr1", "1", 249250621, 225280621, "CHROMOSOME"],
    ["chr10", "10", 135534747, 131314742, "CHROMOSOME"],
    ["chr11", "11", 135006516, 131129516, "CHROMOSOME"],
    ["chr12", "12", 133851895, 130481394, "CHROMOSOME"],
    ["chr13", "13", 115169878, 95589878, "CHROMOSOME"],
    ["chr14", "14", 107349540, 88289540, "CHROMOSOME"],
    ["chr15", "15", 102531392, 81694769, "CHROMOSOME"],
    ["chr16", "16", 90354753, 78884753, "CHROMOSOME"],
    ["chr17", "17", 81195210, 77795210, "CHROMOSOME"],
    ["chr18", "18", 78077248, 74657233, "CHROMOSOME"],
    ["chr19", "19", 59128983, 55808983, "CHROMOSOME"],
    ["chr2", "2", 243199373, 238204522, "CHROMOSOME"],
    ["chr20", "20", 63025520, 59505520, "CHROMOSOME"],
    ["chr21", "21", 48129895, 35106692, "CHROMOSOME"],
    ["chr22", "22", 51304566, 34894562, "CHROMOSOME"],
    ["chr3", "3", 198022430, 194797136, "CHROMOSOME"],
    ["chr4", "4", 191154276, 187661676, "CHROMOSOME"],
    ["chr5", "5", 180915260, 177695260, "CHROMOSOME"],
    ["chr6", "6", 171115067, 167395067, "CHROMOSOME"],
    ["chr7", "7", 159138663, 155353663, "CHROMOSOME"],
    ["chr8", "8", 146364022, 142888922, "CHROMOSOME"],
    ["chr9", "9", 141213431, 120143431, "CHROMOSOME"],
    ["chrM", "M", 16571, 16571, "MITOCHONDRIAL"],
    ["chrX", "X", 155270560, 151100560, "CHROMOSOME"],
    ["chrY", "Y", 59373566, 25653566, "CHROMOSOME"]
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
