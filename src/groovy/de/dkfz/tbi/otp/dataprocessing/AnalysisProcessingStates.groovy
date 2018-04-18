package de.dkfz.tbi.otp.dataprocessing

/**
 * The different values in this ENUM represent the different processing states of {@link BamFilePairAnalysis}.
 * This enum does not tell anything about content of the produced files, e.g. if the snv-calling has been finished
 * successfully, it does not mean that the produced files are meaningful from the scientific point of view.
 */

public enum AnalysisProcessingStates {
    /**
     * At the moment a ${@link BamFilePairAnalysis} is created, the analysis workflow starts working on it.
     * Therefore the first state is already {@link AnalysisProcessingStates#IN_PROGRESS}.
     */
    IN_PROGRESS,
    /**
     * When the analysis workflow finished successfully the state of the ${@link BamFilePairAnalysis} is set to
     * {@link AnalysisProcessingStates#FINISHED}
     */
    FINISHED,
}
