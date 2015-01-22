package de.dkfz.tbi.otp.dataprocessing.snvcalling

/**
 * The different values in this ENUM represent the different states of each snv-calling instance.
 *
 *
 */
public enum SnvProcessingStates {
    /**
     * At the moment a ${@link SnvCallingInstance} is created, the snv-calling workflow starts working on it.
     * Therefore the first state is already "IN_PROGRESS".
     */
    IN_PROGRESS,
    /**
     * When the snv-calling workflow finished successfully the state of the ${@link SnvCallingInstance} is set to "FINISHED".
     */
    FINISHED,
    /**
     * It might happen that some tumor-control sample pairs shall not be processed with the snv-calling workflow.
     * To avoid that these sample pairs are picked again and again by the start job, a ${@link SnvCallingInstance} is created
     * and set to "IGNORED".
     */
    IGNORED,
}
