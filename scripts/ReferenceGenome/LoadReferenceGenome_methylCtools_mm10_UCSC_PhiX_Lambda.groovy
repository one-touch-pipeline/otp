import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry.Classification

/*
 *  This scripts inserts a new ReferenceGenome object
 *  as well as StatSizeFileName and ReferenceGenomeEntry objects obtained from the fasta file
 *  which lies in ${OtpProperty#PATH_PROCESSING_ROOT}/reference_genomes/${path}/
 *  A python helper script 'scripts/ReferenceGenome/getReferenceGenomeInfo.py' which extracts
 *  information from the fasta file was used to generate the fastaEntries content.
*/

String name = "methylCtools_mm10_UCSC_PhiX_Lambda"
String path = "bwa06_methylCtools_mm10_UCSC_PhiX_Lambda"
String fileNamePrefix = "mm10_PhiX_Lambda.conv"
String cytosinePositionsIndex = "mm10_PhiX_Lambda.pos.gz"
String chromosomePrefix = "chr"
String chromosomeSuffix = "chr"
List<String> statSizeFileNames = [
        "mm10_PhiX_Lambda.fa.chrLenOnlyACGT.tab",
]

List<FastaEntry> fastaEntries = [
        new FastaEntry("chr1", "1", 195471971, 191909418, Classification.CHROMOSOME),
        new FastaEntry("chr2", "2", 182113224, 178326651, Classification.CHROMOSOME),
        new FastaEntry("chr3", "3", 160039680, 156398857, Classification.CHROMOSOME),
        new FastaEntry("chr4", "4", 156508116, 152055711, Classification.CHROMOSOME),
        new FastaEntry("chr5", "5", 151834684, 147919774, Classification.CHROMOSOME),
        new FastaEntry("chr6", "6", 149736546, 146336545, Classification.CHROMOSOME),
        new FastaEntry("chr7", "7", 145441459, 141855674, Classification.CHROMOSOME),
        new FastaEntry("chr8", "8", 129401213, 125611432, Classification.CHROMOSOME),
        new FastaEntry("chr9", "9", 124595110, 121157240, Classification.CHROMOSOME),
        new FastaEntry("chr10", "10", 130694993, 127067862, Classification.CHROMOSOME),
        new FastaEntry("chr11", "11", 122082543, 118745945, Classification.CHROMOSOME),
        new FastaEntry("chr12", "12", 120129022, 116922421, Classification.CHROMOSOME),
        new FastaEntry("chr13", "13", 120421639, 117121193, Classification.CHROMOSOME),
        new FastaEntry("chr14", "14", 124902244, 121442112, Classification.CHROMOSOME),
        new FastaEntry("chr15", "15", 104043685, 100653315, Classification.CHROMOSOME),
        new FastaEntry("chr16", "16", 98207768, 95019762, Classification.CHROMOSOME),
        new FastaEntry("chr17", "17", 94987271, 91707663, Classification.CHROMOSOME),
        new FastaEntry("chr18", "18", 90702639, 87452638, Classification.CHROMOSOME),
        new FastaEntry("chr19", "19", 61431566, 58205856, Classification.CHROMOSOME),
        new FastaEntry("chrX", "X", 171031299, 163489665, Classification.CHROMOSOME),
        new FastaEntry("chrY", "Y", 91744698, 88124698, Classification.CHROMOSOME),
        new FastaEntry("chrM", "M", 16299, 16299, Classification.MITOCHONDRIAL),
        new FastaEntry("chrL", "L", 48502, 48502, Classification.UNDEFINED),
        new FastaEntry("phix", "phix", 5386, 5386, Classification.UNDEFINED),
]

ReferenceGenomeService referenceGenomeService = ctx.referenceGenomeService
referenceGenomeService.loadReferenceGenome(name, path, fileNamePrefix, cytosinePositionsIndex, chromosomePrefix, chromosomeSuffix,
        fastaEntries, statSizeFileNames)
