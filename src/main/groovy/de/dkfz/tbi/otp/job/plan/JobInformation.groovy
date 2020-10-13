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
package de.dkfz.tbi.otp.job.plan

import grails.util.Holders

import de.dkfz.tbi.otp.job.processing.*

/**
 * Class representing a serialized form of a JobDefinition and a
 * ProcessingStep for the JobDefinition.
 */
class JobInformation implements Serializable {
    Long id
    String name
    String bean
    boolean startJob
    boolean endStateAware
    List<ParameterInformation> constantParameters = []
    List<ParameterInformation> inputParameters = []
    List<ParameterInformation> outputParameters = []
    List<ParameterInformation> passthroughParameters = []

    // for executed Jobs
    Long processingStep
    Boolean created
    Boolean started
    Boolean finished
    Boolean succeeded
    Boolean failed

    /**
     * Creates a JobInformation from the given JobDefinition.
     * @param job
     */
    static JobInformation fromJob(JobDefinition job) {
        JobInformation ret = new JobInformation()
        def bean = Holders.grailsApplication.mainContext.getBean(job.bean)
        ret.startJob = (bean instanceof StartJob)
        ret.endStateAware = (bean instanceof EndStateAwareJob)
        ret.bean = job.bean
        ret.id = job.id
        ret.name = job.name
        job.constantParameters.each { param ->
            ret.constantParameters << new ParameterInformation(
                id: param.id,
                value: param.value,
                type: new ParameterTypeInformation(it)
            )
        }
        ParameterType.findAllByJobDefinitionAndParameterUsage(job, ParameterUsage.INPUT).each {
            ret.inputParameters << new ParameterInformation(type: new ParameterTypeInformation(it), mapping: ParameterMapping.findByJobAndTo(job, it)?.from?.id)
        }
        ParameterType.findAllByJobDefinitionAndParameterUsage(job, ParameterUsage.OUTPUT).each {
            ret.outputParameters << new ParameterInformation(type: new ParameterTypeInformation(it))
        }
        ParameterType.findAllByJobDefinitionAndParameterUsage(job, ParameterUsage.PASSTHROUGH).each {
            ret.passthroughParameters << new ParameterInformation(
                    type: new ParameterTypeInformation(it), mapping: ParameterMapping.findByJobAndTo(job, it)?.from?.id
            )
        }
        return ret
    }
}
