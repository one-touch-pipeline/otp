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

<ul class="LinkBanner">
    <li><g:link controller="metaDataFields" action="libraryPreparationKits" class="${active=='libraryPreparationKits' ? 'active' : ''}"><g:message code="dataFields.banner.libPrepKit"/></g:link></li>
    <li><g:link controller="metaDataFields" action="antibodyTargets" class="${active=='antibodyTargets' ? 'active' : ''}"><g:message code="dataFields.banner.antibodyTarget"/></g:link></li>
    <li><g:link controller="metaDataFields" action="seqCenters" class="${active=='seqCenters' ? 'active' : ''}"><g:message code="dataFields.banner.seqCenter"/></g:link></li>
    <li><g:link controller="metaDataFields" action="seqPlatforms" class="${active=='seqPlatforms' ? 'active' : ''}"><g:message code="dataFields.banner.seqPlatform"/></g:link></li>
    <li><g:link controller="metaDataFields" action="seqTypes" class="${active=='seqTypes' ? 'active' : ''}"><g:message code="dataFields.banner.seqType"/></g:link></li>
    <li><g:link controller="speciesWithStrain" action="index" class="${active=='speciesWithStrain' ? 'active' : ''}"><g:message code="speciesWithStrain.title"/></g:link></li>
    <li><g:link controller="softwareTool" action="list" class="${active=='softwareTool' ? 'active' : ''}"><g:message code="otp.menu.softwareTool"/></g:link></li>
</ul>
