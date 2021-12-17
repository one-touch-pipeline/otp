/*
 * Copyright 2011-2019 The OTP authors
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

import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.SeqType

// DO NOT USE THIS SCRIPT IF YOU DONT KNOW WHAT YOU ARE DOING!!!

List<String> errors = []

Project.withTransaction {
    Pipeline pipeline = Pipeline.findByName(Pipeline.Name.RODDY_INDEL)

    [ctx.seqTypeService.wholeGenomePairedSeqType, ctx.seqTypeService.exomePairedSeqType].each { SeqType seqType ->

        Map<String, List<RoddyWorkflowConfig>> configsByVersion = Project.all.findResults { project ->
            RoddyWorkflowConfig config = CollectionUtils.atMostOneElement(RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineAndIndividualIsNullAndObsoleteDateIsNull(project, seqType, pipeline))
            return config
        }.groupBy { it.programVersion }

        [
                ["IndelCallingWorkflow:1.2.177", "1.2.177-601"],
                ["IndelCallingWorkflow:2.0.0", "2.0.0-101"],
                ["IndelCallingWorkflow:2.2.0", "2.2.2"],
                ["IndelCallingWorkflow:2.2.1", "2.2.2"],
                ["IndelCallingWorkflow:2.4.0", "2.4.1-1"],
                ["IndelCallingWorkflow:2.4.1", "2.4.1-1"]
        ].each { k, v ->
            configsByVersion[k].each { RoddyWorkflowConfig config ->

                String md5sum = new File(config.configFilePath).text.encodeAsMD5() as String

                if (config.md5sum == md5sum) {
                    RoddyConfiguration configuration = new RoddyConfiguration(pluginName: "IndelCallingWorkflow",
                            programVersion: v,
                            baseProjectConfig: "otpIndelCallingWorkflow-1.1",
                            configVersion: ctx.workflowConfigService.getNextConfigVersion(config.configVersion),
                            project: config.project,
                            seqType: config.seqType)
                    ctx.projectService.configureIndelPipelineProject(configuration)
                } else {
                    errors.add("Changes for indel version ${config.configVersion} in project ${config.project} with seqType ${config.seqType} found.")
                }
            }
        }
    }
}

println errors.join('\n')
