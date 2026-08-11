/*
 * Copyright 2011-2026 The OTP authors
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
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.jpa.HibernateMetrics

import de.dkfz.tbi.otp.ProjectLinkGenerator
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.handler.CustomExceptionResolver
import de.dkfz.tbi.otp.testing.PinningDataSourceRegistrar

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

    grailsLinkGenerator(ProjectLinkGenerator, grailsApplication.config.getProperty("grails.serverURL", String.class)) { bean ->
        bean.autowire = true
    }

    hibernateMetrics(HibernateMetrics, ref('sessionFactory'), 'hibernate', [] as Iterable<Tag>)

    // Feature-flagged database isolation for end-to-end (Cypress) tests. Only when 'otp.testing.endpoints.enabled=true'
    // do we replace GORM's data source connection source factory so a single spec file's writes can be rolled back.
    // Never registered in production, so it has zero effect there. See TestingController / PinningDataSource.
    if (Environment.current in [Environment.DEVELOPMENT, Environment.PRODUCTION]) {
        boolean testingEndpointsEnabled = false
        try {
            testingEndpointsEnabled = Boolean.parseBoolean(ConfigService.parsePropertiesFile().getProperty(
                    OtpProperty.TESTING_ENDPOINTS_ENABLED.key, OtpProperty.TESTING_ENDPOINTS_ENABLED.defaultValue))
        } catch (IOException ignored) {
            // no readable properties file -> leave the testing endpoints disabled
        }
        if (testingEndpointsEnabled) {
            pinningDataSourceRegistrar(PinningDataSourceRegistrar)
        }
    }
}
