package operations.referenceGenome

import de.dkfz.tbi.otp.ngsdata.*

/**
 * Create a list of possible stat size files for all registered reference genomes.
 */

ReferenceGenomeService referenceGenomeService = ctx.referenceGenomeService

ReferenceGenome.list().sort {
    it.id
}.each { ReferenceGenome referenceGenome ->
    println " *\n * - ${referenceGenome.name}:"
    File statDir = referenceGenomeService.pathToChromosomeSizeFilesPerReference(referenceGenome, false)
    statDir.list()?.findAll {
        it ==~ ReferenceGenomeProjectSeqType.TAB_FILE_PATTERN
    }?.sort().each {
        println " *   - ${it}"
    }
}
''
