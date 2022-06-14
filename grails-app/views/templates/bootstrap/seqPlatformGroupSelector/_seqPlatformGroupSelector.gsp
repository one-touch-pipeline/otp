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

<%@ page import="de.dkfz.tbi.otp.ngsdata.mergingCriteria.SelectorViewState" %>

%{--
parameter to set:
    mergingCriteria:                            the current merging criteria to configure with this SeqPlatformGroup
    seqPlatformGroup:                           the current SeqPlatformGroup to configure
    selectorState:                              indicates whether the seqPlatformGroupSelector is used for CREATE, COPY, EDIT or SHOW
    seqPlatformGroupAlreadyInUse (optional):    a flag whether its allowed to copy this SeqPlatformGroup due to intersection, only used when selectorState=EDIT
    allSeqPlatformsWithoutGroup (optional):     all SeqPlatforms which have no group in the current merging criteria, only used when selectorState=COPY
    selectedProjectToCopyForm:                  a search parameter to keep with every request
    selectedSeqTypeToCopyFrom:                  a search parameter to keep with every request
--}%

<div class="card mb-2 seqPlatformGroupSelector">
    <g:if test="${selectorState == SelectorViewState.EDIT}">
        <g:render template="/templates/bootstrap/seqPlatformGroupSelector/header/edit" model="${[
                mergingCriteria            : mergingCriteria,
                seqPlatformGroup           : seqPlatformGroup,
                allSeqPlatformsWithoutGroup: allSeqPlatformsWithoutGroup,
                selectedProjectToCopyForm  : selectedProjectToCopyForm,
                selectedSeqTypeToCopyFrom  : selectedSeqTypeToCopyFrom,

        ]}"/>
        <g:render template="/templates/bootstrap/seqPlatformGroupSelector/list" model="${[
                mergingCriteria          : mergingCriteria,
                seqPlatformGroup         : seqPlatformGroup,
                editable                 : true,
                selectedProjectToCopyForm: selectedProjectToCopyForm,
                selectedSeqTypeToCopyFrom: selectedSeqTypeToCopyFrom,

        ]}"/>
    </g:if>
    <g:elseif test="${selectorState == SelectorViewState.COPY}">
        <g:render template="/templates/bootstrap/seqPlatformGroupSelector/header/copy" model="${[
                mergingCriteria             : mergingCriteria,
                seqPlatformGroup            : seqPlatformGroup,
                seqPlatformGroupAlreadyInUse: seqPlatformGroupAlreadyInUse,
                selectedProjectToCopyForm   : selectedProjectToCopyForm,
                selectedSeqTypeToCopyFrom   : selectedSeqTypeToCopyFrom,

        ]}"/>
        <g:render template="/templates/bootstrap/seqPlatformGroupSelector/list" model="${[
                seqPlatformGroup: seqPlatformGroup,
        ]}"/>
    </g:elseif>
    <g:elseif test="${selectorState == SelectorViewState.CREATE}">
        <g:render template="/templates/bootstrap/seqPlatformGroupSelector/header/new" model="${[
                mergingCriteria            : mergingCriteria,
                allSeqPlatformsWithoutGroup: allSeqPlatformsWithoutGroup,
                selectedProjectToCopyForm  : selectedProjectToCopyForm,
                selectedSeqTypeToCopyFrom  : selectedSeqTypeToCopyFrom,

        ]}"/>
    </g:elseif>
    <g:else>
        <div class="card-header"></div>
        <g:render template="/templates/bootstrap/seqPlatformGroupSelector/list" model="${[
                seqPlatformGroup: seqPlatformGroup,
        ]}"/>
    </g:else>
</div>
