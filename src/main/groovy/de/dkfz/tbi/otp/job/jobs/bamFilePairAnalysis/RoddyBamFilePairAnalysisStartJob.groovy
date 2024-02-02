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
package de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis

import groovy.transform.CompileDynamic
import org.hibernate.Hibernate

import de.dkfz.tbi.otp.dataprocessing.ConfigPerProjectAndSeqType
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

@CompileDynamic
trait RoddyBamFilePairAnalysisStartJob implements BamFilePairAnalysisStartJobTrait {

    @Override
    String getInstanceName(ConfigPerProjectAndSeqType config) {
        assert RoddyWorkflowConfig.isAssignableFrom(Hibernate.getClass(config)):
                "Roddy startjobs should only ever be started with a RoddyWorkFlow, not something else; got ${ config.class }"
        return "results_${config.programVersion.replaceAll(":", "-")}_${config.configVersion}_${ formattedDate }"
    }

    @Override
    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    ConfigPerProjectAndSeqType getConfig(SamplePair samplePair) {
        Pipeline pipeline = bamFileAnalysisService.pipeline
        RoddyWorkflowConfig config = (RoddyWorkflowConfig)RoddyWorkflowConfig.getLatestForIndividual(
                samplePair.individual, samplePair.seqType, pipeline)

        if (config == null) {
            throw new RuntimeException("No ${RoddyWorkflowConfig.simpleName} found for ${Pipeline.simpleName} ${pipeline}, ${Individual.simpleName} " +
                    "${samplePair.individual} (${Project.simpleName} ${samplePair.project}), ${SeqType.simpleName} ${samplePair.seqType}")
        }
        return config
    }
}
