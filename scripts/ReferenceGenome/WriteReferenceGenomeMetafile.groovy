import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeService
import static org.springframework.util.Assert.*

/**
 * Script to create a tsv file including the reference genome meta information:
 * reference genome entry names
 * length of reference genome entry
 * lengthWithoutN of reference genome entry
 *
 * This script is supposed to be called from (web)console, so no
 * arguments can be specified but are inserted manually.
 *
 */

ReferenceGenomeService referenceGenomeService = new ReferenceGenomeService()

// the referenceGenomeName always has to be changed to the used referenceGenome
String referenceGenomeName = "methylCtools_mm9_PhiX_Lambda" // e.g. "hs37d5" or "hg19"
ReferenceGenome referenceGenome = ReferenceGenome.findByName(referenceGenomeName)
notNull referenceGenome

// prepare to store the meta information
String referenceMetaDataPath = referenceGenomeService.referenceGenomeMetaInformationPath(referenceGenome).absolutePath
File referenceMetaData = new File(referenceMetaDataPath)
referenceMetaData.createNewFile()
isTrue referenceMetaData.canWrite()

// store the names and length values for each reference genome entry of the reference genome
List<ReferenceGenomeEntry> referenceGenomeEntries = ReferenceGenomeEntry.findAllByReferenceGenome(referenceGenome)
String entry = ""
referenceGenomeEntries.each { ReferenceGenomeEntry referenceGenomeEntry ->
    entry += referenceGenomeEntry.name + "\t" + referenceGenomeEntry.length + "\t" + referenceGenomeEntry.lengthWithoutN + "\n"
}
referenceMetaData.write(entry)
