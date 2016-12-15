package operations.referenceGenome

import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType

/**
 * Create a list of possible stat size files for all registered reference genomes.
 */

Project anyProject = Project.first()

ReferenceGenome.list().sort {
    it.id
}.each { ReferenceGenome referenceGenome ->
    println " *\n * - ${referenceGenome.name}:"
    File statDir = ctx.referenceGenomeService.pathToChromosomeSizeFilesPerReference(anyProject, referenceGenome, false)
    statDir.list()?.findAll {
        it ==~ ReferenceGenomeProjectSeqType.TAB_FILE_PATTERN
    }?.sort().each {
        println " *   - ${it}"
    }
}
''
