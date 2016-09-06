package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*

class RoddySnvCallingInstance extends SnvCallingInstance implements RoddyResult {

    static hasMany = [
            roddyExecutionDirectoryNames: String,
    ]

    @Override
    Pipeline getPipeline() {
        return config.pipeline
    }

    @Override
    File getWorkDirectory() {
        return getSnvInstancePath().absoluteDataManagementPath
    }

    @Override
    RoddyWorkflowConfig getConfig() {
        return config
    }

}
