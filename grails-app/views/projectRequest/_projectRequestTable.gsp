%{--
  - Copyright 2011-2020 The OTP authors
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

<g:if test="${projectRequests}">
    <g:each var="projectRequest" in="${projectRequests}">
        <div class="project-request-container">
            <div class="name">
                <g:link action="view" id="${projectRequest.id}">${projectRequest.name}</g:link>
            </div>
            <div class="status">
                <span style="color: ${projectRequest.status.color}">${projectRequest.formattedStatus}</span>
            </div>
            <div class="created">
                opened on ${projectRequest.dateCreated.format("yyyy-MM-dd HH:mm")} by ${projectRequest.requester}
            </div>
            <div class="updated">
                last updated on ${projectRequest.lastUpdated.format("yyyy-MM-dd HH:mm")}
            </div>
        </div>
    </g:each>
</g:if>
<g:else>
    <ul>
        <li>${g.message(code: "projectRequest.none")}</li>
    </ul>
</g:else>
