<%@ page import="de.dkfz.tbi.otp.project.Project" %>
%{--
  - Copyright 2011-2024 The OTP authors
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

<div class="table-with-fixed-header">
    <table>
        <thead>
        <tr>
            <th><g:message code="home.project"/></th>
            <th><g:message code="home.pis"/></th>
            <th><g:message code="home.desc"/></th>
            <th><g:message code="home.seqType"/></th>
        </tr>
        </thead>
        <g:each in="${projects}" var="project">
            <tr>
                <td>
                    <g:if test="${linkProjectName}">
                        <g:link controller="projectOverview" action="index" params="[(projectParameter): project.name]">${project.displayName}</g:link>
                    </g:if>
                    <g:else>
                        ${project.displayName}
                    </g:else>
                </td>
                <td>${project.pis?.join(", ") ?: "-"}</td>
                <td>
                    <otp:expandableText shortened="${project.shortDescription ?: "-"}"
                                        full="${project.description ?: "-"}"
                                        expandable="${project.shortDescription != project.description}"/>
                </td>
                <td>
                    ${project.st.collect { "${it.seqType} (${it.numberOfSamples})" }.join(", ") ?: g.message(code: "home.data.none")}
                    <g:if test="${project.projectType == de.dkfz.tbi.otp.project.Project.ProjectType.USER_MANAGEMENT}">
                        <g:message code="home.data.userManagement"/>
                    </g:if>
                </td>
            </tr>
        </g:each>
    </table>
</div>
