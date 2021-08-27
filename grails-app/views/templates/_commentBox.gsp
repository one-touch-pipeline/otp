%{--
  - Copyright 2011-2019 The OTP authors
  -
  - Permission is hereby granted, free of charge, to any person obtaining a copy
  - of this software and associated documentation files (the "Software"), to deal
  - in the Software without restriction, including without limitation the rights
  - to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  - copies of the Software, and to permit persons to whom the Software is
  - furnished to do so, subject to the following conditions:
  -
  - The above copyright notice and this permission notice shall be included in all
  - copies or substantial portions of the Software.
  -
  - THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  - IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  - FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  - AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  - LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  - OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  - SOFTWARE.
  --}%

%{--
To be used in conjunction with: common/CommentBox.js
--}%
<%@ page import="de.dkfz.tbi.util.TimeFormats" %>
<div id="comment-box-container" class="card">
    <g:set var="comment" value="${commentable?.comment}"/>
    <input type="hidden" id="entity-id" name="entityId" value="${commentable?.id}">

    <div class="card-body p-2">
        <strong><g:message code="commentBox.header"/>:</strong>
        <sec:ifNotGranted roles="ROLE_OPERATOR">
            <textarea id="comment-content" class="form-control" rows="${rows ?: 5}" cols="${cols ?: 80}" readonly>${comment?.comment}</textarea>
        </sec:ifNotGranted>
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <textarea id="comment-content" class="form-control" rows="${rows ?: 5}" cols="${cols ?: 80}">${comment?.comment}</textarea>
        </sec:ifAllGranted>
    </div>
    <sec:ifAllGranted roles="ROLE_OPERATOR">
        <div class="card-footer">
            <button id="button-save" class="btn btn-sm btn-primary float-right ml-1" data-controller="${targetController}" data-action="${targetAction}" type="button" disabled>
                <g:message code="commentBox.save"/>
            </button>
            <button id="button-cancel" class="btn btn-sm btn-secondary float-right" type="button" disabled>
                <g:message code="commentBox.cancel"/>
            </button>
            <span id="authorSpan">${TimeFormats.WEEKDAY_DATE_TIME.getFormatted(comment?.modificationDate)} ${comment?.author}</span>
        </div>
    </sec:ifAllGranted>
</div>
