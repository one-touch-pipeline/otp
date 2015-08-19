/**
 * Script to create the pancan workflow object in the database. It is executed one time.
 */

import de.dkfz.tbi.otp.dataprocessing.Workflow



Workflow.withTransaction {
    Workflow workflow = new Workflow(
            type: Workflow.Type.ALIGNMENT,
            name: Workflow.Name.PANCAN_ALIGNMENT,
    )
    assert workflow.save(flush: true, failOnError: true)
}
