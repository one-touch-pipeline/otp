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
import grails.util.Environment

import de.dkfz.tbi.otp.ProjectLinkGenerator
import de.dkfz.tbi.otp.handler.CustomExceptionResolver
import de.dkfz.tbi.otp.utils.NumberConverter

beans = {
    exceptionHandler(CustomExceptionResolver) {
        // this is required so that calls to super work
        exceptionMappings = ['java.lang.Exception': '/error']
    }
    if (Environment.current == Environment.TEST) {
        // use Class.forName because classes in test-helper are not found in production env
        fileSystemService(Class.forName("de.dkfz.tbi.otp.job.processing.TestFileSystemService"))
    }
    if (Environment.current == Environment.TEST || Environment.current.name == "WORKFLOW_TEST") {
        configService(Class.forName("de.dkfz.tbi.otp.TestConfigService")) {
            processingOptionService = ref('processingOptionService')
        }
    }
    if (Environment.current == Environment.PRODUCTION || (Environment.current == Environment.DEVELOPMENT &&
            grailsApplication.config.getProperty("scheduler.enabled", Boolean.class))) {
        // proper thread pool
        xmlns task: "http://www.springframework.org/schema/task"
        task.executor(id: "taskExecutor", "pool-size": 10)
        task.scheduler(id: "taskScheduler", "pool-size": 10)
        task.'annotation-driven'(executor: "taskExecutor", scheduler: "taskScheduler")
    }

    // don't use the default locale specific value converter
    [
            Short, Short.TYPE,
            Integer, Integer.TYPE,
            Long, Long.TYPE,
            Float, Float.TYPE,
            Double, Double.TYPE,
            BigInteger,
            BigDecimal,
    ].each { numberType ->
        "defaultGrails${numberType.simpleName}Converter"(NumberConverter) {
            targetType = numberType
        }
    }

    grailsLinkGenerator(ProjectLinkGenerator, grailsApplication.config.getProperty("grails.serverURL", String.class)) { bean ->
        bean.autowire = true
    }
}
