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

    static final Set<String> FLAGS_WITHOUT_VALUE = ['-H'] as Set
    static final List<String> REQUIRED = ['-env', '-J', '-H', '-cwd', '-oo', '-M', '-R', '-W', '-q']
    // option names that must not be consumed as the value of a preceding option
    static final Set<String> KNOWN_OPTIONS = (REQUIRED + FLAGS_WITHOUT_VALUE + ['-n']) as Set
    static final Path TESTS_DIR = Path.of('/workflows/tests')

    static void main(String[] args) {
        if (!args) {
            System.err.println("No arguments")
            System.exit(1)
        }
        // options may be repeated (e.g. -R) or optional (e.g. -n for multi-core jobs), so parse them by name
        Map<String, List<String>> options = [:]
        for (int i = 0; i < args.size(); i++) {
            String key = args[i]
            assert key.startsWith('-'): "Unexpected argument '${key}' at position ${i}"
            String value = ''
            if (!(key in FLAGS_WITHOUT_VALUE)) {
                assert i + 1 < args.size() && !(args[i + 1] in KNOWN_OPTIONS): "Missing value for parameter '${key}'"
                value = args[++i]
            }
            options.computeIfAbsent(key) { [] } << value
        }
        List<String> missing = REQUIRED - options.keySet()
        if (missing) {
            System.err.println("Missing parameters: ${missing}")
            System.exit(1)
        }
        assert options['-env'] == ['all']
        String file = options['-oo'].first()
        Path outputPath = Path.of(file)
        assert outputPath.absolute: "Value of '-oo' must be an absolute path, but was '${file}'"
        assert outputPath.normalize().startsWith(TESTS_DIR) && outputPath.normalize() != TESTS_DIR:
                "Value of '-oo' must be inside '${TESTS_DIR}', but was '${file}'"
        String queue = options['-q'].first()

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
