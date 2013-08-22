import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*

/*
 *  This scripts inserts a new ReferenceGenome object
 *  as well as ReferenceChromosome objects obtained from the fasta file
 *  which lives in ${otp.processing.root.path}/reference_genomes/bwa06_hg19_chr
 *  A python helper script 'scripts/getReferenceGenomeInfo.py' which extracts
 *  information from the fasta file was used to generate the chrList content.
*/

ReferenceGenome refGen = new ReferenceGenome()
refGen.name = "hg19"
refGen.path = "bwa06_hg19_chr"
refGen.fileNamePrefix = "hg19_1-22_X_Y_M"
refGen.chromosomeNamePrefix = "chr"
refGen.chromosomeNameSuffix = ""
refGen.save(flush: true)
System.out.println("Inserted reference genome: " + refGen)

// insert chromosome length information for reference genome

// identifier, length, lengthWithoutN
def chrColumn = ["name":0, "length":1, "lengthWithoutN":2]
def chrList = [
    ["chr1", 249250621, 225280621],
    ["chr10", 135534747, 131314742],
    ["chr11", 135006516, 131129516],
    ["chr12", 133851895, 130481394],
    ["chr13", 115169878, 95589878],
    ["chr14", 107349540, 88289540],
    ["chr15", 102531392, 81694769],
    ["chr16", 90354753, 78884753],
    ["chr17", 81195210, 77795210],
    ["chr18", 78077248, 74657233],
    ["chr19", 59128983, 55808983],
    ["chr2", 243199373, 238204522],
    ["chr20", 63025520, 59505520],
    ["chr21", 48129895, 35106692],
    ["chr22", 51304566, 34894562],
    ["chr3", 198022430, 194797136],
    ["chr4", 191154276, 187661676],
    ["chr5", 180915260, 177695260],
    ["chr6", 171115067, 167395067],
    ["chr7", 159138663, 155353663],
    ["chr8", 146364022, 142888922],
    ["chr9", 141213431, 120143431],
    ["chrM", 16571, 16571],
    ["chrX", 155270560, 151100560],
    ["chrY", 59373566, 25653566]
]

// init counter for overall length and lengthWithoutN
    long length = 0
    long lengthWithoutN = 0

// put chromosome information into database
chrList.each { chr ->
    ReferenceChromosome refChr = new ReferenceChromosome()
    refChr.name = chr[chrColumn.name]
    refChr.length = chr[chrColumn.length]
    refChr.lengthWithoutN = chr[chrColumn.lengthWithoutN]
    refChr.referenceGenome = refGen
    refChr.save(flush: true)
    System.out.println("Inserted reference chromosome: " + refChr)
    length += chr[chrColumn.length]
    lengthWithoutN += chr[chrColumn.lengthWithoutN]
}

// put length and lengthWithoutN of reference chromosome into database
refGen.length = length
refGen.lengthWithoutN = lengthWithoutN
refGen.save(flush: true)
