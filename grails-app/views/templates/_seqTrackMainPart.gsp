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

<tr>
    <td class="myKey"><g:message code="seqTrack.show.details.laneId"/></td>
    <td class="myValue">${seqTrack.laneId}</td>
</tr>
<tr>
    <td class="myKey"><g:message code="seqTrack.show.run"/></td>
    <td><g:link controller="run" action="show" id="${seqTrack.run.id}">${seqTrack.run.name}</g:link></td>
</tr>
      <tr>
    <td class="myKey"><g:message code="seqTrack.show.sampleType"/></td>
    <td class="myValue">${seqTrack.sample.individual.mockPid} ${seqTrack.sample.sampleType.name}</td>
</tr>
<tr>
    <td class="myKey"><g:message code="seqTrack.show.seqType"/></td>
    <td class="myValue">${seqTrack.seqType.name}</td>
</tr>
