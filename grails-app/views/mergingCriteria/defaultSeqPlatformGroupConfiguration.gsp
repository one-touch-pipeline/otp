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

<%@ page import="de.dkfz.tbi.otp.dataprocessing.MergingCriteria" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>${g.message(code: "mergingCriteria.seqPlatformDefinition")}</title>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <h1>${g.message(code: "mergingCriteria.seqPlatformDefinition")}</h1>

    <h2>${g.message(code: "mergingCriteria.seqPlatformDefinition.default")}</h2>

    <div>
        <g:if test="${!allSeqPlatformsWithoutGroup.empty}">
            <div class="small-frame-box">
                New group
                <ul>
                    <li>
                        <g:form action="createNewDefaultGroupAndAddPlatform">
                            <g:select id="select_seqPlat_newgroup" name="platform.id" class="use-select-2"
                                      from="${allSeqPlatformsWithoutGroup}" optionKey="id" noSelection="${[null: 'Select to create new group']}"/>
                            <g:submitButton name="Save"/>
                        </g:form>
                    </li>
                </ul>
            </div>
        </g:if>
        <g:each in="${seqPlatformGroups}" var="seqPlatformGroup">
            <div class="small-frame-box">
                <ul>
                    <g:each in="${seqPlatformGroup.seqPlatforms}" var="seqPlatform">
                        <li>${seqPlatform}
                        <g:form action="removePlatformFromSeqPlatformGroup" style="display: inline;">
                            <g:hiddenField name="group.id" value="${seqPlatformGroup.id}"/>
                            <g:hiddenField name="platform.id" value="${seqPlatform.id}"/>
                            <g:submitButton name="Remove"/>
                        </g:form>
                        </li>
                    </g:each>
                    <g:if test="${!allSeqPlatformsWithoutGroup.empty}">
                        <li>
                            <g:form action="addPlatformToExistingSeqPlatformGroup">
                                <g:select id="select_seqPlat_${seqPlatformGroup.id}" name="platform.id" class="use-select-2"
                                          from="${allSeqPlatformsWithoutGroup}" optionKey="id"
                                          noSelection="${[null: 'Select to add to group']}"/>
                                <g:hiddenField name="group.id" value="${seqPlatformGroup.id}"/>
                                <g:submitButton name="Save"/>
                            </g:form>
                        </li>
                    </g:if>
                </ul>
                <g:form action="deleteSeqPlatformGroup">
                    <g:hiddenField name="group.id" value="${seqPlatformGroup.id}"/>
                    <g:submitButton name="Delete group"/>
                </g:form>
            </div>
        </g:each>
    </div>

</div>
</body>
</html>
