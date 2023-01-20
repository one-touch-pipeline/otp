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
import groovy.transform.CompileDynamic
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.utils.CollectionUtils

@CompileDynamic
@Transactional
class IlseSubmissionService {

    CommentService commentService

    IlseSubmission findById(long id) {
        return IlseSubmission.get(id)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<IlseSubmission> sortedBlacklistedIlseSubmissions() {
        return IlseSubmission.findAllByWarning(true, [sort: 'ilseNumber', order: 'desc'])
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    boolean checkIfIlseNumberDoesNotExist(int ilseNumber) {
        return !CollectionUtils.atMostOneElement(IlseSubmission.findAllByIlseNumber(ilseNumber))
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
        ilseSubmission.save(flush: true)
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
