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

<g:if test="${type == 'single-line-string'}">
    ${value}
</g:if>
<g:elseif test="${type == 'multi-line-string'}">
    <div class="multiline-display">${value}</div>
</g:elseif>
<g:elseif test="${type == 'sorted-list'}">
    <ul>
        <g:each var="it" in="${(value as List)?.sort()}">
            <li>${it}</li>
        </g:each>
    </ul>
</g:elseif>
<g:elseif test="${type == 'comma-separated-sorted-list'}">
    <ul>
        <g:each var="it" in="${value?.split(",")?.sort()}">
            <li>${it}</li>
        </g:each>
    </ul>
</g:elseif>
<g:elseif test="${type == 'boolean'}">
    ${value}
</g:elseif>
<g:else>
    <span style="background-color: #ffaaaa" title="Unhandled type '${type}'">${value}</span>
</g:else>
