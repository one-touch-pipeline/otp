/*
 * Copyright 2011-2020 The OTP authors
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

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * Script to create/update a bam comment.
 *
 * The bam files are specified by pid, sampleType, seqType and sequencing read type.
 *
 * If the bam file has already a comment, only the text is updated. Author and time stay the same.
 * Otherwise a new comment is created with current user and creation date.
 *
 * Also the author for new comments needs to be provided by the ldap name.
 */

//------------------------------
//input

//the new comment
String newComment = """"""

//the ldap name of the author for new comments. The author of existing comments are not changed.
String author = ""

//The bam file, defined by:
//PID SAMPLE_TYPE SEQ_TYPE LIBRARY_LAYOUT,
//separated by space, comma, semicolon or tab
String bamFileDefinition = """
#PID SAMPLE_TYPE SEQ_TYPE LIBRARY_LAYOUT
#PID2 TUMOR WHOLE_GENOME PAIRED

"""

//------------------------------
//work

CommentService commentService = ctx.commentService
SeqTypeService seqTypeService = ctx.seqTypeService

AbstractMergedBamFile.withTransaction {
    assert newComment: 'Comment may not be empty'
    assert author: 'Author may not be empty'
    User user = CollectionUtils.atMostOneElement(User.findAllByUsername(author))
    assert user: "No user with ldap name '${author}' could be found in OTP"

    List<AbstractMergedBamFile> bamFiles = bamFileDefinition.split('\n')*.trim().findAll { String line ->
        line && !line.startsWith('#')
    }.collect { String line ->
        List<String> split = line.split(' *[ ,;\t] *')*.trim()
        println split as List

        Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPidOrMockPidOrMockFullName(split[0], split[0], split[0]),
                "Could not find individual for '${split[0]}'")
        SampleType sampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByName(split[1]), "Could not find sampleType for '${split[1]}'")
        SeqType seqType = seqTypeService.findByNameOrImportAlias(split[2], [
                libraryLayout: SequencingReadType.getByName(split[3]),
                singleCell   : false,
        ])
        assert seqType: "Could not find seqType: ${split[2]} ${split[3]} bulk"

        Sample sample = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType),
                "Could not find sample for '${individual.pid}' and '${sampleType.name}'")

        AbstractMergedBamFile bamFile = CollectionUtils.exactlyOneElement(
                AbstractMergedBamFile.withCriteria {
                    workPackage {
                        eq('sample', sample)
                        eq('seqType', seqType)
                    }
                }
        )
        assert seqType: "Could not find bam file for: ${sample} ${seqType.displayNameWithLibraryLayout}"
        return bamFile
    }.each { AbstractMergedBamFile bamFile ->
        println "${bamFile}: ${bamFile.comment?.comment}"
    }
    assert false: "DEBUG: transaction intentionally failed to rollback changes"

    bamFiles.each {AbstractMergedBamFile bamFile ->
        if (bamFile.comment) {
            bamFile.comment.with {
                comment = newComment
                save(flush: true)
            }
        } else {
            commentService.createOrUpdateComment(bamFile, newComment, author)
        }
    }
}
println '\nall comments updated/created'
