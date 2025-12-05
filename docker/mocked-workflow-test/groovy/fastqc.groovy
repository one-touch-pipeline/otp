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

class Fastqc {

    static final String PID = "asdf"
    static final String REFERENCE_QC_FILE = "/workflows/reference-data/fastqFiles/fastqc/asdf_fastqc.zip"

    static final String HTML_FILE = "${PID}_fastqc.html"
    static final String ZIP_FILE = "${PID}_fastqc.zip"

    static void main(String[] args) {
        if (!args) {
            System.err.println("No args")
            System.exit(1)
        }

        assert args.length == 5

        assert args[0].startsWith("/workflows/tests/")
        assert args[0].contains(PID)
        assert args[1] == "--noextract"
        assert args[2] == "--nogroup"
        assert args[3] == "-o"
        assert args[4].startsWith("/workflows/tests/")
        assert args[4] ==~ "/workflows/tests/.*/baseFolder/([0-9a-f]{2}/){2}([0-9a-f]{4}-){4}[0-9a-f]{12}"

        Path directory = Paths.get(args[4])

        Path htmlFile = directory.resolve(HTML_FILE)
        Files.writeString(htmlFile, "Some data")

        Path zipFile = directory.resolve(ZIP_FILE)
        Files.copy(Paths.get(REFERENCE_QC_FILE), zipFile)
    }
}
