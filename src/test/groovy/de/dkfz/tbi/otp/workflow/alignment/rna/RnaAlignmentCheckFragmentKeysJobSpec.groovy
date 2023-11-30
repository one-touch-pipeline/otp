/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment.rna

import de.dkfz.tbi.otp.workflow.jobs.AbstractCheckFragmentKeysJobSpec

class RnaAlignmentCheckFragmentKeysJobSpec extends AbstractCheckFragmentKeysJobSpec {

    @Override
    Set<String> getRequiredKeys() {
        return [
                "RODDY/cvalues/STAR_VERSION",
                "RODDY/cvalues/STAR_PARAMS_2PASS",
                "RODDY/cvalues/SAMBAMBA_VERSION",
        ] as Set
    }

    @Override
    Set<String> getMissingKeys() {
        return [
                "RODDY/cvalues/STAR_PARAMS_2PASS",
                "RODDY/cvalues/SAMBAMBA_VERSION",
        ] as Set
    }

    @Override
    protected String workflowName() {
        return RnaAlignmentWorkflow.WORKFLOW
    }

    @Override
    protected RnaAlignmentCheckFragmentKeysJob createJob() {
        return new RnaAlignmentCheckFragmentKeysJob()
    }

    @SuppressWarnings("GetterMethodCouldBeProperty")
    @Override
    protected String getCombinedConfig() {
        return """
{
  "RODDY": {
    "cvalues": {
      "STAR_VERSION": {
        "value": "2.0"
      },
      "STAR_PARAMS_2PASS": {
        "type": "string",
        "value": "0.6.5"
      },
      "STAR_PARAMS_OUT": {
        "type": "string",
        "value": "\\" -SOME_OPTS \\""
      },
      "STAR_PARAMS_CHIMERIC": {
        "type": "string",
        "value": "xxx"
      },
      "STAR_PARAMS_INTRONS": {
        "type": "string",
        "value": "xxx"
      },
      "SAMBAMBA_VERSION": {
        "type": "string",
        "value": "1.1.2"
      }
    }
  }
}
"""
    }

    @SuppressWarnings("GetterMethodCouldBeProperty")
    @Override
    String getCombinedConfigMissingKeys() {
        return """
{
  "RODDY": {
    "cvalues": {
      "STAR_VERSION": {
        "value": "2.0"
      },
      "STAR_PARAMS_OUT": {
        "type": "string",
        "value": "\\" -SOME_OPTS \\""
      },
      "STAR_PARAMS_CHIMERIC": {
        "type": "string",
        "value": "xxx"
      },
      "STAR_PARAMS_INTRONS": {
        "type": "string",
        "value": "xxx"
      }
    }
  }
}
"""
    }
}
