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

<div class="tab-menu">
    <g:link controller="metaDataFields" action="libraryPreparationKits" class="${actionName=="libraryPreparationKits" ? "active" : ""}"><g:message code="dataFields.banner.libPrepKit"/></g:link>
    <g:link controller="metaDataFields" action="antibodyTargets" class="${actionName=="antibodyTargets" ? "active" : ""}"><g:message code="dataFields.banner.antibodyTarget"/></g:link>
    <g:link controller="metaDataFields" action="seqCenters" class="${actionName=="seqCenters" ? "active" : ""}"><g:message code="dataFields.banner.seqCenter"/></g:link>
    <g:link controller="metaDataFields" action="seqPlatforms" class="${actionName=="seqPlatforms" ? "active" : ""}"><g:message code="dataFields.banner.seqPlatform"/></g:link>
    <g:link controller="metaDataFields" action="seqTypes" class="${actionName=="seqTypes" ? "active" : ""}"><g:message code="dataFields.banner.seqType"/></g:link>
    <g:link controller="speciesWithStrain" action="index" class="${controllerName=="speciesWithStrain" ? "active" : ""}"><g:message code="speciesWithStrain.title"/></g:link>
    <g:link controller="softwareTool" action="list" class="${controllerName=="softwareTool" ? "active" : ""}"><g:message code="otp.menu.softwareTool"/></g:link>
</div>
