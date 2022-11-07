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

<%@ page import="de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage.Status" %>

<g:if test="${mwp.status == Status.FINAL}"><b>${g.message(code: "cellRanger.selection.finalRun")}</b></g:if>
<g:if test="${mwp.status == Status.DELETED}"><span class="deleted">${g.message(code: "cellRanger.selection.deletedRun")}</g:if>
<g:if test="${mwp.expectedCells}">${g.message(code: "cellRanger.selection.expectedCells", args: [mwp.expectedCells])}</g:if>
<g:if test="${mwp.enforcedCells}">${g.message(code: "cellRanger.selection.enforcedCells", args: [mwp.enforcedCells])}</g:if>
<g:if test="${!(mwp.expectedCells || mwp.enforcedCells)}">${g.message(code: "cellRanger.selection.default")}</g:if>
<g:if test="${mwp.status == Status.DELETED}"></span></g:if>
