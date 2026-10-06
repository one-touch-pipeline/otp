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

class Module {

    /**
     * The modules OTP may load during the workflow tests.
     *
     * They are the values of the activation commands set by 'initModuleSystem' in 'src/workflow-test/resources/mockedWorkflowTestInit.groovy',
     * which overwrites the values of 'scripts/initializations/LoadSoftwareModules.groovy'.
     */
    static final List<String> MODULES_TO_LOAD = [
            "FastQC/0.11.5",    // Fastqc workflow
            "Groovy/4.0.22",    // bam import and roddy workflows
            "Java/1.8.0_131",   // roddy workflows
            "SAMtools/1.20",    // bam import
    ].asImmutable()

    static void main(String[] args) {
        if (!args) {
            System.err.println("No args")
            System.exit(1)
        }

        assert args.length == 2
        assert args[0] == 'load'
        assert args[1] in MODULES_TO_LOAD
    }
}
