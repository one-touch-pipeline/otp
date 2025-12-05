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

import java.nio.file.*

class BSub {

    static void main(String[] args) {
        if (!args) {
            System.err.println("No arguments")
            System.exit(1)
        }
        if (args.size() != 17) {
            System.err.println("Unexpected count of parameters, expected 17, found ${args.size()}")
            System.exit(1)
        }
        assert args[0] == '-env'
        assert args[1] == 'all'
        assert args[2] == '-J'
        // job name, not checked
        assert args[4] == '-H'
        assert args[5] == '-cwd'
        // home name, not checked
        assert args[7] == '-oo'
        String file = args[8]
        assert args[9] == '-M'
        // memory value, not checked
        assert args[11] == '-R'
        // resource request, not checked
        assert args[13] == '-W'
        // walltime, not checked
        assert args[15] == '-q'
        String queue = args[16]

        String clusterId = System.nanoTime()

        Path path = Path.of(System.getenv("HOME"), 'jobs', clusterId)
        Files.createDirectories(path)

        Path state = path.resolve('state')
        state.text = 'PSUSP\n'

        String input = System.in.text

        Path inputFile = path.resolve("input")
        inputFile.text = input

        String extendedInput = """
#!/bin/bash

set -e

export LSB_JOBID=${clusterId}

bash ${inputFile}
"""

        Process process = ['bash', '-c', extendedInput].execute()
        StringBuffer stdout = new StringBuffer()
        process.waitForProcessOutput(stdout, stdout)

        Path outputFile = path.resolve("output")
        outputFile.text = stdout.toString()

        Path outputFileInStructure = Paths.get(file.replace('%J', clusterId.toString()))
        outputFileInStructure.text = stdout.toString()

        Path exitCode = path.resolve("exitCode")
        exitCode.text = "${process.exitValue()}\n"

        println "Job <$clusterId> is submitted to queue <$queue>."
    }
}
