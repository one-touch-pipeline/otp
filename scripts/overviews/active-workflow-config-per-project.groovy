/*
 * Copyright 2011-2020 The OTP authors
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

import de.dkfz.tbi.otp.dataprocessing.ConfigPerProjectAndSeqType
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.SeqType

/**
 * Create a csv showing the configured version of the specified pipelines per project.
 *
 * You can configure the used pipelines in ConfigPerProjectGlobals.pipelines but by default
 * it contains all relevent pipelines at the time of creation of this script.
 */

class ConfigPerProjectGlobals {
    static List<Pipeline> pipelines = Pipeline.findAllByNameInList([
            Pipeline.Name.PANCAN_ALIGNMENT,
            Pipeline.Name.RODDY_RNA_ALIGNMENT,
            Pipeline.Name.RODDY_SNV,
            Pipeline.Name.RODDY_INDEL,
            Pipeline.Name.RODDY_SOPHIA,
            Pipeline.Name.RODDY_ACESEQ,
    ]).sort()
}

String pipelineHeader = ConfigPerProjectGlobals.pipelines.collect { Pipeline pipeline ->
    return pipeline.name.seqTypes.collect { SeqType seqType ->
        return "${pipeline.name} ${seqType}"
    }.join(",")
}.join(",")

String header = ["Project", pipelineHeader].join(",")

String getActiveWorkflowVersionOfProject(Project project) {
    ConfigPerProjectGlobals.pipelines.collect { Pipeline pipeline ->
        return pipeline.name.seqTypes.collect { SeqType seqType ->
            "${getConfiguredWorkflowVersion(project, seqType, pipeline)}"
        }.join(",")
    }.join(",")
}

String getConfiguredWorkflowVersion(Project project, SeqType seqType, Pipeline pipeline) {
    return ConfigPerProjectAndSeqType.createCriteria().get {
        eq("project", project)
        eq("seqType", seqType)
        eq("pipeline", pipeline)
        isNull("obsoleteDate")
    }?.programVersion ?: "-"
}

String output = Project.list().sort { it.name }.collect { Project project ->
    return [project, getActiveWorkflowVersionOfProject(project)].join(",")
}.join('\n')

println "${header}\n${output}"