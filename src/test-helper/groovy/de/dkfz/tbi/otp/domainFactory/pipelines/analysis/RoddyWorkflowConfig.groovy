/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.domainFactory.pipelines.analysis

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.HelperUtils

trait RoddyWorkflowConfig extends WorkflowConfig {

    abstract Pipeline.Name getPipelineName()

    Pipeline findOrCreatePipeline() {
        Pipeline.Name name = this.pipelineName
        return findOrCreateDomainObject(Pipeline, [
                name: name,
                type: name.type,
        ], [:], [:])
    }

    @Override
    Map getConfigProperties(Map properties = [:]) {
        Pipeline pipeline = properties.containsKey('pipeline') ? properties.pipeline : findOrCreatePipeline()
        SeqType seqType = properties.containsKey('seqType') ? properties.seqType : createSeqType()
        String programVersion = properties.containsKey('programVersion') ? properties.programVersion : "programVersion:1.1.${nextId}"
        String configVersion = properties.containsKey('configVersion') ? properties.configVersion : "v1_${nextId}"
        return [
                pipeline             : pipeline,
                seqType              : seqType,
                configFilePath       : {
                    "${TestCase.uniqueNonExistentPath}/${pipeline.name.name()}_${seqType.roddyName}_${seqType.libraryLayout}_${programVersion.substring(programVersion.indexOf(':') + 1)}_${configVersion}.xml"
                },
                programVersion       : programVersion,
                configVersion        : configVersion,
                project              : { properties.individual?.project ?: createProject() },
                dateCreated          : { new Date() },
                lastUpdated          : { new Date() },
                adapterTrimmingNeeded: { seqType.isWgbs() || seqType.isRna() || seqType.isChipSeq() },
                nameUsedInConfig     : de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig.getNameUsedInConfig(pipeline.name, seqType, programVersion, configVersion),
                md5sum               : HelperUtils.randomMd5sum,
        ]
    }

    Class configPerProjectAndSeqTypeClass = de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
}
