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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="speciesWithStrain.title"/></title>
</head>
<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <g:render template="/metaDataFields/tabMenu"/>

    <h1><g:message code="speciesWithStrain.header"/></h1>

    <g:if test="${helperParams.helper}">
        <otp:annotation type="info">
            <strong><g:message code="speciesWithStrain.helper.prefix"/></strong> ${helperParams.helper}
        </otp:annotation>
    </g:if>

    <div class="species-with-strain-grid-wrapper">

        <div class="grid-element" style="grid-row: 1; grid-column: 1 / span 2;">
            <g:set var="fieldWidth" value="40ch"/>
            <div class="form-and-listing-grid">
                <div class="form">
                    <h2><g:message code="speciesWithStrain.header.createSpeciesWithStrain"/></h2>
                    <g:form controller="speciesWithStrain" action="createSpeciesWithStrain" params="${helperParams}">
                        <table>
                            <tr>
                                <td><g:message code="speciesWithStrain.label.species"/></td>
                                <td>
                                    <g:if test="${!allSpecies}">
                                        <g:message code="speciesWithStrain.label.noSpeciesAvailable"/>
                                    </g:if>
                                    <g:else>
                                        <g:select id="species" name="speciesId" class="use-select-2" style="width: ${fieldWidth};"
                                                  from="${allSpecies}" optionKey="id"/>
                                    </g:else>
                                </td>
                            </tr>
                            <tr>
                                <td><g:message code="speciesWithStrain.label.strain"/></td>
                                <td>
                                    <g:if test="${!strains}">
                                        <g:message code="speciesWithStrain.label.noStrainsAvailable"/>
                                    </g:if>
                                    <g:else>
                                        <g:select id="strain" name="strainId" class="use-select-2" style="width: ${fieldWidth};"
                                                  from="${strains}" optionKey="id" />
                                    </g:else>
                                </td>
                            </tr>
                            <tr>
                                <td></td>
                                <td><g:submitButton name="Create"/></td>
                            </tr>
                        </table>
                    </g:form>
                </div>
                <div class="listing">
                    <h2><g:message code="speciesWithStrain.header.existingSpeciesWithStrains"/></h2>
                    <g:if test="${!speciesWithStrainsBySpecies}">
                        <g:message code="speciesWithStrain.label.noSpeciesWithStrainAvailable"/>
                    </g:if>
                    <div class="scrollable">
                        <ul>
                            <g:each var="species" in="${speciesWithStrainsBySpecies.keySet()}">
                                <li>
                                    ${species}
                                    <ul>
                                        <g:each var="speciesWithStrain" in="${speciesWithStrainsBySpecies[species]}">
                                            <li>${speciesWithStrain.strain}</li>
                                        </g:each>
                                    </ul>
                                </li>
                            </g:each>
                        </ul>
                    </div>
                </div>
            </div>
        </div>

        <div class="grid-element" style="grid-row: 2; grid-column: 1;">
            <div class="form-and-listing-grid">
                <div class="form">
                    <h2><g:message code="speciesWithStrain.header.createSpecies"/></h2>
                    <g:form controller="speciesWithStrain" action="createSpecies" params="${helperParams}">
                        <table>
                            <tr>
                                <td><g:message code="speciesWithStrain.label.commonName"/></td>
                                <td>
                                    <input type="text" list="commonNameList" name="commonNameName" value="${cachedCommonName}"
                                           autocomplete="off" style="width: ${fieldWidth};"/>
                                    <datalist id="commonNameList">
                                        <g:each var="commonName" in="${commonNames}">
                                            <option>${commonName}</option>
                                        </g:each>
                                    </datalist>
                                </td>
                            </tr>
                            <tr>
                                <td><g:message code="speciesWithStrain.label.scientificName"/></td>
                                <td>
                                    <input type="text" id="scientificName" name="scientificName" value="${cachedScientificName}"
                                           autocomplete="off" style="width: ${fieldWidth};"/>
                                </td>
                            </tr>
                            <tr>
                                <td></td>
                                <td><g:submitButton name="Create"/></td>
                            </tr>
                        </table>
                    </g:form>
                </div>
                <div class="listing">
                    <h2><g:message code="speciesWithStrain.header.existingSpecies"/></h2>
                    <g:if test="${!commonNames}">
                        <g:message code="speciesWithStrain.label.noSpeciesAvailable"/>
                    </g:if>
                    <div class="scrollable">
                        <ul>
                            <g:each var="commonName" in="${commonNames}">
                                <li>
                                    ${commonName}
                                    <ul>
                                        <g:each var="species" in="${speciesByCommonName[commonName]}">
                                            <li>${species.scientificName}</li>
                                        </g:each>
                                    </ul>
                                </li>
                            </g:each>
                        </ul>
                    </div>
                </div>
            </div>
        </div>

        <div class="grid-element" style="grid-row: 2; grid-column: 2;">
            <div class="form-and-listing-grid">
                <div class="form">
                    <h2><g:message code="speciesWithStrain.header.createStrain"/></h2>
                    <g:form controller="speciesWithStrain" action="createStrain" params="${helperParams}">
                        <table>
                            <tr>
                                <td><g:message code="speciesWithStrain.label.strainName"/></td>
                                <td><input type="text" name="newStrainName" value="${cachedStrainName}" autocomplete="off" style="width: ${fieldWidth};"/></td>
                            </tr>
                            <tr>
                                <td></td>
                                <td><g:submitButton name="Create"/></td>
                            </tr>
                        </table>
                    </g:form>
                </div>
                <div class="listing">
                    <h2><g:message code="speciesWithStrain.header.existingStrains"/></h2>
                    <g:if test="${!strains}">
                        <g:message code="speciesWithStrain.label.noStrainsAvailable"/>
                    </g:if>
                    <div class="scrollable">
                        <ul>
                            <g:each var="strain" in="${strains}">
                                <li>${strain}</li>
                            </g:each>
                        </ul>
                    </div>
                </div>
            </div>
        </div>

        <br>
    </div>
</div>
</body>
</html>
