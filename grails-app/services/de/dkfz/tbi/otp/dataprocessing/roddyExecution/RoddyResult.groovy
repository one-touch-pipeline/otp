package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType


/**
 * This interface must be implemented by all result objects which are created with a Roddy workflow.
 * With this interface it is ensured that all information, needed to call Roddy out of OTP, are provided.
 */

public interface RoddyResult {

    public Project getProject()

    public Individual getIndividual()

    public SeqType getSeqType()

    public Pipeline getPipeline()

    public RoddyWorkflowConfig getConfig()

    public File getWorkDirectory()

    public File getWorkExecutionStoreDirectory()

    public File getLatestWorkExecutionDirectory()

    public List<String> getRoddyExecutionDirectoryNames()
}
