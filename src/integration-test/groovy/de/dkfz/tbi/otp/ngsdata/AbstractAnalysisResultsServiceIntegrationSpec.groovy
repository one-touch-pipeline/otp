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

package de.dkfz.tbi.otp.ngsdata

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqResultsService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelResultsService
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaResultsService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvResultsService
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaResultsService
import de.dkfz.tbi.otp.security.UserAndRoles

@Rollback
@Integration
class AbstractAnalysisResultsServiceIntegrationSpec extends Specification implements UserAndRoles {

    AbstractAnalysisResultsService abstractAnalysisResultsService

    void setup() {
        createUserAndRoles()
    }

    @Unroll
    void "getCallingInstancesForProject with #analysis Instance"() {
        given:
        BamFilePairAnalysis analysisInstance = DomainFactory."create${analysis}InstanceWithRoddyBamFiles"()
        abstractAnalysisResultsService = service.newInstance()
        abstractAnalysisResultsService.projectService = [
                getProjectByName: { projectName -> analysisInstance.project }
        ] as ProjectService

        when:
        List callingInstances
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            callingInstances = abstractAnalysisResultsService.getCallingInstancesForProject(analysisInstance.samplePair.project.name)
        }

        then:
        callingInstances.size() == 1

        where:
        service                | analysis       | instance
        SnvResultsService      | "RoddySnv"     | RoddySnvCallingInstance
        IndelResultsService    | "IndelCalling" | IndelCallingInstance
        AceseqResultsService   | "Aceseq"       | AceseqInstance
        SophiaResultsService   | "Sophia"       | SophiaInstance
        RunYapsaResultsService | "RunYapsa"     | RunYapsaInstance
    }

    void "checkFile with no callingInstance"() {
        when:
        abstractAnalysisResultsService = Mock(AbstractAnalysisResultsService)
        File result
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            result = abstractAnalysisResultsService.getFiles(null, null)
        }

        then:
        result == null
    }

    @Unroll
    void "checkFile with #instance and no File"() {
        given:
        abstractAnalysisResultsService = Mock(AbstractAnalysisResultsService)
        BamFilePairAnalysis analysisInstance = DomainFactory."create${analysis}InstanceWithRoddyBamFiles"()
        DomainFactory.createRealm()

        when:
        File file
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            file = abstractAnalysisResultsService.getFiles(analysisInstance, plotType) ? abstractAnalysisResultsService.getFiles(analysisInstance, plotType).first() : null
        }

        then:
        !file

        where:
        analysis       | instance                | plotType
        "RoddySnv"     | RoddySnvCallingInstance | PlotType.SNV
        "IndelCalling" | IndelCallingInstance    | PlotType.INDEL
        "IndelCalling" | IndelCallingInstance    | PlotType.INDEL_TINDA
        "Aceseq"       | AceseqInstance          | PlotType.ACESEQ_ALL
        "Aceseq"       | AceseqInstance          | PlotType.ACESEQ_WG_COVERAGE
        "Sophia"       | SophiaInstance          | PlotType.SOPHIA
    }
}
