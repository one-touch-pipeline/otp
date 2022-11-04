%{--
  - Copyright 2011-2021 The OTP authors
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


<%@ page import="de.dkfz.tbi.otp.ngsdata.mergingCriteria.SelectorViewState;" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>${g.message(code: "mergingCriteria.seqPlatformDefinition")}</title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="pages/defaultSeqPlatformGroup/index.js"/>
</head>

<body>
<div class="container-fluid mb-5">

    <g:render template="/templates/messages"/>

    <h1>${g.message(code: "mergingCriteria.seqPlatformDefinition")}</h1>

    <h5>${g.message(code: "mergingCriteria.createNewGroup")}</h5>
    <g:render template="/templates/bootstrap/seqPlatformGroupSelector/seqPlatformGroupSelector"
              model="${[allSeqPlatformsWithoutGroup: allSeqPlatformsWithoutGroup,
                        selectorState              : SelectorViewState.CREATE,
              ]}"/>

    <h5>${g.message(code: "mergingCriteria.currentConfigTitle")}</h5>
    <g:if test="${seqPlatformGroups}">
        <g:each in="${seqPlatformGroups}" var="seqPlatformGroup">
            <g:render template="/templates/bootstrap/seqPlatformGroupSelector/seqPlatformGroupSelector"
                      model="${[seqPlatformGroup           : seqPlatformGroup,
                                allSeqPlatformsWithoutGroup: allSeqPlatformsWithoutGroup,
                                selectorState              : SelectorViewState.EDIT,
                      ]}"/>
        </g:each>
    </g:if>
    <g:else>
        <div class="alert alert-info text-center" role="alert">
            ${g.message(code: "mergingCriteria.noGroupConfigured")}
        </div>
    </g:else>
</div>
</body>
</html>
