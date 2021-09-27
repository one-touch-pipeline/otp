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

import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CommentService

@Transactional
class IlseSubmissionService {

    CommentService commentService

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<IlseSubmission> getSortedBlacklistedIlseSubmissions() {
        return IlseSubmission.findAllByWarning(true, [sort: 'ilseNumber', order: 'desc'])
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    boolean checkIfIlseNumberDoesNotExist(int ilseNumber) {
        return !IlseSubmission.findByIlseNumber(ilseNumber)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<IlseSubmission> createNewIlseSubmissions(List<Integer> ilseNumbers, String comment) {
        return ilseNumbers.collect {
            IlseSubmission ilseSubmission = new IlseSubmission(
                    ilseNumber: it,
                    warning: true,
            )
            commentService.saveComment(ilseSubmission, comment)
            return ilseSubmission
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void unBlacklistIlseSubmissions(IlseSubmission ilseSubmission) {
        ilseSubmission.warning = false
        ilseSubmission.save()
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    String buildIlseIdentifierFromSeqTracks(List<SeqTrack> seqTracks) {
        List<Integer> ilseNumbers = seqTracks.collect { it?.ilseSubmission?.ilseNumber }
        return buildIlseIdentifier(ilseNumbers)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    String buildIlseIdentifier(List<Integer> ilseNumbers) {
        List<Integer> validIlseNumbers = ilseNumbers.findAll()
        return validIlseNumbers ? "[S#${validIlseNumbers.unique().sort().join(',')}]" : ""
    }
}
