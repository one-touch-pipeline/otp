import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

/**
 * removes the complete content of a project from the OTP database
 * -> but not the files in the file-system. They are needed to include again to OTP.
 * When they are in OTP again, it is easy to remove the project manually from the filesystem.
 */

// input area
//----------------------

String projectName = ""

//script area
//-----------------------------

assert projectName

// `flush: true` is intentionally left out at certain places to improve performance
Project.withTransaction {

    Project project = exactlyOneElement(Project.findAllByName(projectName))

    Individual.findAllByProject(project).each { Individual individual ->

        ClusterJob.findAllByIndividual(individual)*.delete()

        Sample.findAllByIndividual(individual).each { Sample sample ->

            SampleIdentifier.findAllBySample(sample)*.delete(flush: true)

            /*
             * Delete seqTrack and all corresponding information and processing results (fastq, fastqc, bam, qa)
             */
            SeqTrack.findAllBySample(sample).each {
                ctx.dataSwapService.deleteSeqTrack(it)
            }

            SeqScan.findAllBySample(sample).each {
                ctx.dataSwapService.deleteSeqScanAndCorrespondingInformation(it)
            }

            sample.delete(flush: true)
        }
        individual.delete(flush: true)
    }

    /*
     * There are files which are not connected to a seqTrack -> they have to be deleted, too
     */
    DataFile.findAllByProject(project).each {
        ctx.dataSwapService.deleteDataFile(it)
    }

    //just used while testing to make sure that the DB changes are rolled back -> otherwise each time a new DB dump has to be included
    assert false
}
