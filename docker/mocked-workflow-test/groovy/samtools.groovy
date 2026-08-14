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

/**
 * Mock of `samtools view`, reading the alignment file from stdin and writing SAM records to stdout.
 *
 * It is used by the bam import workflow:
 *   cat file.bam  | tee target.bam  | samtools view - | groovy bamMaxReadLength.groovy
 *   cat file.cram | tee target.cram | samtools view -T reference.fa - | groovy bamMaxReadLength.groovy
 *
 * The longest of the written records defines the maximum read length the workflow stores in the database.
 */
class Samtools {

    /** the cigar strings of the mocked records, the longest one defines the maximum read length */
    static final List<String> CIGARS = [
            "50M",
            "60M40S",
            "100M",
            "*",
    ].asImmutable()

    static void main(String[] args) {
        if (!args) {
            System.err.println("No args")
            System.exit(1)
        }

        assert args[0] == 'view'
        assert args.last() == '-'

        if (args.length == 4) {
            // cram files need the reference genome the cram was created with
            assert args[1] == '-T'
            Path referenceGenome = Paths.get(args[2])
            assert Files.isReadable(referenceGenome)
        } else {
            assert args.length == 2
        }

        // the alignment file is streamed into the command, therefore it has to be consumed
        assert System.in.text

        CIGARS.eachWithIndex { String cigar, int index ->
            int length = cigar == '*' ? 50 : cigar.findAll(/(\d+)[MIS]/) { match, count -> count as Integer }.sum() as Integer
            println([
                    "read_${index + 1}",
                    '0',
                    '1',
                    "${index + 1}",
                    '60',
                    cigar,
                    '*',
                    '0',
                    '0',
                    'A' * length,
                    'I' * length,
            ].join('\t'))
        }
    }
}
