/**
 * Script to create the pancan workflow object in the database. It is executed one time.
 */

import de.dkfz.tbi.otp.dataprocessing.Pipeline



Pipeline.withTransaction {
    Pipeline pipeline = new Pipeline(
            type: Pipeline.Type.ALIGNMENT,
            name: Pipeline.Name.PANCAN_ALIGNMENT,
    )
    assert pipeline.save(flush: true, failOnError: true)
}
