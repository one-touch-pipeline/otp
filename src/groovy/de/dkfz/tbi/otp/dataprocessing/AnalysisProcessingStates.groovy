package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*

/**
 * The different values in this ENUM represent the different processing states of {@link BamFilePairAnalysis} and
 * {@link SnvJobResult};
 * This enum does not tell anything about content of the produced files, e.g. if the snv-calling has been finished
 * successfully, it does not mean that the produced files are meaningful from the scientific point of view.
 */

public enum AnalysisProcessingStates {
    /**
     * At the moment a ${@link BamFilePairAnalysis} is created, the analysis workflow starts working on it.
     * Therefore the first state is already {@link AnalysisProcessingStates#IN_PROGRESS}.
     * Is used for {@link SnvJobResult} to indicate that the corresponding files have not been produced yet.
     */
    IN_PROGRESS,
    /**
     * When the analysis workflow finished successfully the state of the ${@link BamFilePairAnalysis} is set to
     * {@link AnalysisProcessingStates#FINISHED}
     * Is used {@link SnvJobResult} to indicate that the corresponding files have been produced.
     * {@link AnalysisProcessingStates#FINISHED}, non withdrawn {@link SnvJobResult}s can be re-used even if the corresponding
     * {@link SnvCallingInstance} has {@link SnvCallingInstance#withdrawn} state.
     */
    FINISHED,
}
