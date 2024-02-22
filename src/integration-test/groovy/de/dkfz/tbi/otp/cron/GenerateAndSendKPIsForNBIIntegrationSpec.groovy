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
package de.dkfz.tbi.otp.cron

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.tracking.DeNbiKpiService
import de.dkfz.tbi.otp.administration.MailHelperService

import java.time.ZoneId

@Rollback
@Integration
class GenerateAndSendKPIsForNBIIntegrationSpec extends Specification {

    GenerateAndSendKPIsForNBI job = new GenerateAndSendKPIsForNBI()

    void setup() {
        job.configService = Mock(ConfigService) {
            getTimeZoneId() >> {
                return ZoneId.of("Europe/Berlin")
            }
        }

        job.processingOptionService = Mock(ProcessingOptionService) {
            _ * findOptionAsString(_) >> {
                return "test@otp.de"
            }
        }

        job.deNbiKpiService = new DeNbiKpiService()
    }

    void "wrappedExecute, sends mail with list of all required workflows when job runs"() {
        when:
        job.mailHelperService = Mock(MailHelperService) {
            1 * saveMail(_, _)  >> { String subject, String body ->
                assert subject.startsWith("KPIs for de.NBI - ")
                assert body.contains("Workflow: PanCan")
                assert body.contains("Workflow: Rna alignment")
                assert body.contains("Workflow: CellRanger")
                assert body.contains("Workflow: Snv")
                assert body.contains("Workflow: Indel")
                assert body.contains("Workflow: Sophia")
                assert body.contains("Workflow: Aceseq")
                assert body.contains("Workflow: runYapsa")
                assert body.contains("Workflow: otp workflow")
                assert body.contains("Workflow: roddy")
                assert body.contains("Workflow: otp cluster jobs")
            }
        }

        job.wrappedExecute()

        then:
        noExceptionThrown()
    }
}
