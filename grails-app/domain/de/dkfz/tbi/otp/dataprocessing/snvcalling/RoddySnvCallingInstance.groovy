package de.dkfz.tbi.otp.dataprocessing.snvcalling

import org.hibernate.Hibernate

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyAnalysisResult
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome

class RoddySnvCallingInstance extends AbstractSnvCallingInstance implements RoddyAnalysisResult {

    static hasMany = [
            roddyExecutionDirectoryNames: String,
    ]

    static constraints = {
        config validator: { val, obj ->
            RoddyWorkflowConfig.isAssignableFrom(Hibernate.getClass(val))
        }
    }

    @Override
    RoddyWorkflowConfig getConfig() {
        return RoddyWorkflowConfig.get(super.config.id)
    }

    @Override
    ReferenceGenome getReferenceGenome() {
        //The reference genome of the control is used because the tumor can be Xenograft and for these reference genomes SNV fails.
        return sampleType2BamFile.referenceGenome
    }

}
