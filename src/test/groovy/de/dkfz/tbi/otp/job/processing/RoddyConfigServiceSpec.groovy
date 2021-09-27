/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.job.processing

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion

import java.nio.file.Paths

class RoddyConfigServiceSpec extends Specification implements ServiceUnitTest<RoddyConfigService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Workflow,
                WorkflowVersion,
        ]
    }

    void "test createRoddyXmlConfig"() {
        given:
        String combinedConfig = """
            {
              "RODDY": {
                "cvalues": {
                  "mergedBamSuffixList": {
                    "type": "string",
                    "value": "asdf"
                  },
                  "INSERT_SIZE_LIMIT": {
                    "type": "integer",
                    "value": 1
                  },
                  "runSlimWorkflow": {
                    "type": "boolean",
                    "value": "true"
                  },
                  "UseBioBamBamSort": {
                    "type": "boolean",
                    "value": "false"
                  },
                  "BWA_VERSION": {
                    "value": "0.7.8"
                  }
                },
                "resources": {
                  "CollectBamMetrics": {
                    "value": "picardCollectMetrics.sh",
                    "basepath": "qcPipeline",
                    "memory": "0.1",
                    "cores": 500,
                    "nodes": 1
                  },
                  "coveragePlot": {
                    "value": "genomeCoveragePlots.sh",
                    "basepath": "qcPipeline",
                    "memory": "7T",
                    "nodes": 3,
                    "walltime": "01:00:00"
                  },
                  "alignAndPair": {
                    "value": "bwaMemSort.sh",
                    "basepath": "qcPipeline",
                    "memory": "1G",
                    "cores": 1,
                    "nodes": 2,
                    "walltime": "00:10:00"
                  }
                }
              },
              "RODDY_FILENAMES": {
                "filenames": [
                  {
                    "class": "B1",
                    "pattern": "B2",
                    "selectiontag": "B3",
                    "derivedFrom": "B4"
                  },
                  {
                    "class": "a1",
                    "pattern": "a2",
                    "fileStage": "a3"
                  }
                ]
              }
            }"""

        String expected = """\
            <configuration name='config' configurationType='project' usedresourcessize='l'>
              <availableAnalyses>
                <analysis id='analysis' configuration='ACONF' useplugin='WFNAME:1.2.3' killswitches='FilenameSection' />
              </availableAnalyses>
              <configurationvalues>
                <cvalue name='inputBaseDirectory' value='/i' type='path' />
                <cvalue name='outputBaseDirectory' value='/o' type='path' />
                <cvalue name='ADAPTER_SEQ' value='ACGT' />
                <cvalue name='useSingleEndProcessing' value='true' />
                <cvalue name='BWA_VERSION' value='0.7.8' />
                <cvalue name='INSERT_SIZE_LIMIT' value='1' type='integer' />
                <cvalue name='mergedBamSuffixList' value='asdf' type='string' />
                <cvalue name='runSlimWorkflow' value='true' type='boolean' />
                <cvalue name='UseBioBamBamSort' value='false' type='boolean' />
              </configurationvalues>
              <processingTools>
                <tool name='alignAndPair' value='bwaMemSort.sh' basepath='qcPipeline' overrideresourcesets='true'>
                  <resourcesets>
                    <rset size='l' queue='QNAME' memory='1G' cores='1' nodes='2' walltime='00:10:00' />
                  </resourcesets>
                </tool>
                <tool name='CollectBamMetrics' value='picardCollectMetrics.sh' basepath='qcPipeline' overrideresourcesets='true'>
                  <resourcesets>
                    <rset size='l' queue='QNAME' memory='0.1' cores='500' nodes='1' />
                  </resourcesets>
                </tool>
                <tool name='coveragePlot' value='genomeCoveragePlots.sh' basepath='qcPipeline' overrideresourcesets='true'>
                  <resourcesets>
                    <rset size='l' queue='QNAME' memory='7T' nodes='3' walltime='01:00:00' />
                  </resourcesets>
                </tool>
              </processingTools>
              <filenames package='de.dkfz.b080.co.files' filestagesbase='de.dkfz.b080.co.files.COFileStage'>
                <filename class='a1' fileStage='a3' pattern='a2' />
                <filename class='B1' derivedFrom='B4' pattern='B2' selectiontag='B3' />
              </filenames>
            </configuration>""".stripIndent()

        expect:
        expected == service.createRoddyXmlConfig(combinedConfig,
                [useSingleEndProcessing: "true", "ADAPTER_SEQ": "ACGT"],
                "WFNAME", createWorkflowVersion(workflowVersion: "1.2.3"), "ACONF",
                Paths.get("/i"), Paths.get("/o"),
                "QNAME", true)
    }
}