/*
 * Copyright 2011-2024 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing

/**
 * The different values in this ENUM represent the different processing states of {@link BamFilePairAnalysis}.
 * This enum does not tell anything about content of the produced files, e.g. if the snv-calling has been finished
 * successfully, it does not mean that the produced files are meaningful from the scientific point of view.
 */

enum AnalysisProcessingStates {
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
