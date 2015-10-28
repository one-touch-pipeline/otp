import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.ngsdata.Project

Collection<String> projectNames =  // ['']
// Named priorities:
// ProcessingPriority.NORMAL_PRIORITY
// ProcessingPriority.FAST_TRACK_PRIORITY
short newProcessingPriority =  // ProcessingPriority.

Project.withTransaction {
    projectNames.each {
        Project project
        try {
            project = exactlyOneElement(Project.findAllByName(it))
        } catch (Throwable t) {
            throw new RuntimeException("Problem finding project ${it}", t)
        }
        println "Changing processing priority of ${project} from ${project.processingPriority} to ${newProcessingPriority}"
        project.processingPriority = newProcessingPriority
        project.save(failOnError: true, flush: true)
    }
}
''
