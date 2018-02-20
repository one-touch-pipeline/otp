import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

/**
 * delete a complete project from the OTP database
 * run DeleteProjectContent.groovy first!
 */

// input area
//----------------------

String projectName

//script area
//-----------------------------

assert projectName


try {
    Project.withTransaction {

        Project project = exactlyOneElement(Project.findAllByName(projectName))

        /*
         * Deletes the connection of the project to the reference genome
         */
        ReferenceGenomeProjectSeqType.findAllByProject(project)*.delete()

        MergingCriteria.findAllByProject(project)*.delete()

        ProcessingThresholds.findAllByProject(project)*.delete()

        SampleTypePerProject.findAllByProject(project)*.delete()

        /*
         * Deletes the ProcessingOptions of the project
         */
        ProcessingOption.findAllByProject(project)*.delete(flush: true)

        ConfigPerProject.findAllByProjectAndPreviousConfigIsNotNull(project)*.delete(flush: true)
        ConfigPerProject.findAllByProject(project)*.delete(flush: true)

        UserProjectRole.findAllByProject(project)*.delete(flush: true)

        /*
         * Finally delete the project
         */
        project.delete(flush: true)

        //just used while testing to make sure that the DB changes are rolled back -> otherwise each time a new DB dump has to be included
        assert false
    }
} catch (Throwable e) {
    e.printStackTrace(System.out)
}
