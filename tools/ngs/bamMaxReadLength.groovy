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

/**
 * Find the maximum read length contained in a BAM file
 *
 * Usage: samtools view example.bam | groovy bamMaxReadLength.groovy
 *    or: cat example.bam | samtools view - | groovy bamMaxReadLength.groovy
 *
 * @see <a href="https://samtools.github.io/hts-specs/SAMv1.pdf">Sequence Alignment/Map Format Specification</a>
 */
class MaxReadLength {
    static int calculateMaxReadLength(BufferedReader reader) {
        int maxLength = 0

        String line
        while ((line = reader.readLine()) != null) {
            List<String> fields = line.tokenize("\t")
            String cigarField = fields[5]
            if (cigarField == "*") {
                continue
            }
            List<Integer> lengths = cigarField.findAll(/(\d+)[MINSHP=X]/, { match, length ->
                length as Integer
            })
            Integer length = lengths.sum() as Integer

            if (maxLength < length) {
                maxLength = length
            }
        }
        return maxLength
    }

    static void main(String... args) {
        InputStreamReader stdinReader = new InputStreamReader(System.in)
        BufferedReader reader = new BufferedReader(stdinReader)

        try {
            int maxLength = calculateMaxReadLength(reader)
            println maxLength
        } catch (Throwable t) {
            t.printStackTrace(System.err)
            System.exit(1)
        } finally {
            reader.close()
            stdinReader.close()
        }
    }
}
