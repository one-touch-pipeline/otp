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
assert standardChromosomes.size() == 21

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
refGen.name = "GRCm38mm10"
refGen.path = "bwa06_GRCm38mm10"
refGen.fileNamePrefix = "GRCm38mm10"
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
    ["1", "1", 195471971, 192205969, "CHROMOSOME"],
    ["10", "10", 130694993, 127077067, "CHROMOSOME"],
    ["11", "11", 122082543, 118746045, "CHROMOSOME"],
    ["12", "12", 120129022, 116929021, "CHROMOSOME"],
    ["13", "13", 120421639, 117221639, "CHROMOSOME"],
    ["14", "14", 124902244, 121602243, "CHROMOSOME"],
    ["15", "15", 104043685, 100745472, "CHROMOSOME"],
    ["16", "16", 98207768, 95107762, "CHROMOSOME"],
    ["17", "17", 94987271, 91824270, "CHROMOSOME"],
    ["18", "18", 90702639, 87602638, "CHROMOSOME"],
    ["19", "19", 61431566, 58206056, "CHROMOSOME"],
    ["2", "2", 182113224, 178382925, "CHROMOSOME"],
    ["3", "3", 160039680, 156449717, "CHROMOSOME"],
    ["4", "4", 156508116, 152603261, "CHROMOSOME"],
    ["5", "5", 151834684, 148277684, "CHROMOSOME"],
    ["6", "6", 149736546, 146536546, "CHROMOSOME"],
    ["7", "7", 145441459, 142186438, "CHROMOSOME"],
    ["8", "8", 129401213, 125779213, "CHROMOSOME"],
    ["9", "9", 124595110, 121317703, "CHROMOSOME"],
    ["X", "X", 171031299, 164308898, "CHROMOSOME"],
    ["Y", "Y", 91744698, 88548698, "CHROMOSOME"],
    ["MT", "MT", 16299, 16299, "MITOCHONDRIAL"],
    ["JH584295.1", "JH584295.1", 1976, 1976, "CONTIG"],
    ["JH584292.1", "JH584292.1", 14945, 14945, "CONTIG"],
    ["GL456368.1", "GL456368.1", 20208, 20208, "CONTIG"],
    ["GL456396.1", "GL456396.1", 21240, 21240, "CONTIG"],
    ["GL456359.1", "GL456359.1", 22974, 22974, "CONTIG"],
    ["GL456382.1", "GL456382.1", 23158, 23158, "CONTIG"],
    ["GL456392.1", "GL456392.1", 23629, 23629, "CONTIG"],
    ["GL456394.1", "GL456394.1", 24323, 24323, "CONTIG"],
    ["GL456390.1", "GL456390.1", 24668, 24668, "CONTIG"],
    ["GL456387.1", "GL456387.1", 24685, 24685, "CONTIG"],
    ["GL456381.1", "GL456381.1", 25871, 25871, "CONTIG"],
    ["GL456370.1", "GL456370.1", 26764, 26764, "CONTIG"],
    ["GL456372.1", "GL456372.1", 28664, 28664, "CONTIG"],
    ["GL456389.1", "GL456389.1", 28772, 28772, "CONTIG"],
    ["GL456378.1", "GL456378.1", 31602, 31602, "CONTIG"],
    ["GL456360.1", "GL456360.1", 31704, 31704, "CONTIG"],
    ["GL456385.1", "GL456385.1", 35240, 35240, "CONTIG"],
    ["GL456383.1", "GL456383.1", 38659, 38659, "CONTIG"],
    ["GL456213.1", "GL456213.1", 39340, 39340, "CONTIG"],
    ["GL456239.1", "GL456239.1", 40056, 40056, "CONTIG"],
    ["GL456367.1", "GL456367.1", 42057, 42057, "CONTIG"],
    ["GL456366.1", "GL456366.1", 47073, 47073, "CONTIG"],
    ["GL456393.1", "GL456393.1", 55711, 55711, "CONTIG"],
    ["GL456216.1", "GL456216.1", 66673, 66673, "CONTIG"],
    ["GL456379.1", "GL456379.1", 72385, 72385, "CONTIG"],
    ["JH584304.1", "JH584304.1", 114452, 114452, "CONTIG"],
    ["GL456212.1", "GL456212.1", 153618, 153618, "CONTIG"],
    ["JH584302.1", "JH584302.1", 155838, 155838, "CONTIG"],
    ["JH584303.1", "JH584303.1", 158099, 158099, "CONTIG"],
    ["GL456210.1", "GL456210.1", 169725, 169725, "CONTIG"],
    ["GL456219.1", "GL456219.1", 175968, 175968, "CONTIG"],
    ["JH584300.1", "JH584300.1", 182347, 182347, "CONTIG"],
    ["JH584298.1", "JH584298.1", 184189, 184189, "CONTIG"],
    ["JH584294.1", "JH584294.1", 191905, 191905, "CONTIG"],
    ["GL456354.1", "GL456354.1", 195993, 195993, "CONTIG"],
    ["JH584296.1", "JH584296.1", 199368, 199368, "CONTIG"],
    ["JH584297.1", "JH584297.1", 205776, 205776, "CONTIG"],
    ["GL456221.1", "GL456221.1", 206961, 206961, "CONTIG"],
    ["JH584293.1", "JH584293.1", 207968, 207968, "CONTIG"],
    ["GL456350.1", "GL456350.1", 227966, 227965, "CONTIG"],
    ["GL456211.1", "GL456211.1", 241735, 241735, "CONTIG"],
    ["JH584301.1", "JH584301.1", 259875, 259875, "CONTIG"],
    ["GL456233.1", "GL456233.1", 336933, 336933, "CONTIG"],
    ["JH584299.1", "JH584299.1", 953012, 953012, "CONTIG"],
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
