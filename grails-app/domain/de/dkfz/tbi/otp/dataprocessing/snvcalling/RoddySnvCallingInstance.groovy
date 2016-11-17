package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome

class RoddySnvCallingInstance extends SnvCallingInstance implements RoddyResult {

    static hasMany = [
            roddyExecutionDirectoryNames: String,
    ]

    @Override
    Pipeline getPipeline() {
        return config.pipeline
    }

    @Override
    File getBaseDirectory() {
        return getWorkDirectory().parentFile
    }

    @Override
    File getWorkDirectory() {
        return getSnvInstancePath().absoluteDataManagementPath
    }

    @Override
    RoddyWorkflowConfig getConfig() {
        return super.getConfig()
    }

    ReferenceGenome getReferenceGenome() {
        //The reference genome of the control is used because the tumor can be Xenograft and for these reference genomes SNV fails.
        return sampleType2BamFile.referenceGenome
    }

}
