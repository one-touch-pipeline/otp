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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main"/>
        <title><g:message code="egaSubmission.title"/></title>
    </head>

    <body>
        <div class="body">
            <h1>User documentation</h1>
            <p>
                To start a new submission click on the menu item "EGA".
                If you would like to create one submission for several projects, please create them individually for each project with the same name.
                After that you will be taken to the overview page with all existing submissions for your chosen project.
                This documentation explains the OTP part of a submission.
                For more information on EGA see their <a href="https://ega-archive.org/submission/quickguide" target="_blank">Submission Quick Guide</a>
                or <a href="https://ega-archive.org/submission/FAQ" target="_blank">FAQ</a>.
            </p>

            <h2 id="overview">Overview page</h2>
            <p>
                You can choose three actions:
                <ul>
                    <li>change project</li>
                    <li>start new submission -> click New Submission</li>
                    <li>continue existing submission -> click Continue with submission</li>
                </ul>
            </p>
            <img class="centerImage" alt="overview" src="${assetPath(src: 'egaSubmission/overview_select.png')}">
            <figcaption>Overview page</figcaption>
            <p>
                If you edit an existing submission you will be taken to the last unsaved step.<br>
                If you create a new submission you will be taken to the following page.
            </p>

            <h2 id="new">New submission page</h2>
            <p>
                Please enter the following information on this page:
                <ul>
                    <li><g:message code="egaSubmission.newSubmission.egaBox"/> -> submitter account for your project, <a href="https://ega-archive.org/submission-form.php" target="_blank">request one at EGA</a> if you do not have one yet</li>
                    <li><g:message code="egaSubmission.submissionName"/> -> short informal label for internal communication, e.g. year-submittername-paperkeyword.</li>
                    <li><g:message code="egaSubmission.newSubmission.studyName"/> -> title of the study to display on EGA (usually publication title)</li>
                    <li><g:message code="egaSubmission.newSubmission.studyType"/> -> type of data you are submitting</li>
                    <li><g:message code="egaSubmission.newSubmission.studyAbstract"/> -> description of the study to display on EGA (usually publication abstract)</li>
                    <li><g:message code="egaSubmission.newSubmission.pubMedId"/> -> if already assigned, your publications PubMed-ID (e.g. <a href="https://www.ncbi.nlm.nih.gov/pubmed/28803971" target="_blank">28803971</a>)</li>
                </ul>
            </p>
            <img class="centerImage" alt="new" src="${assetPath(src: 'egaSubmission/new_leer.png')}">
            <figcaption>Create a new submission</figcaption>
            <p>
                If you click the "<g:message code="egaSubmission.newSubmission.submit"/>" button the submission will be created and
                you will be taken to the next page to select samples.
            </p>

            <h2 id="selectSamples">Select samples page</h2>
            <p>
                On this page all samples in the selected project are displayed.
                By clicking on the checkboxes you can select samples.
                Above the table is a filter for sequence types to facilitate the selection.
            </p>
            <img class="centerImage" alt="select_samples" src="${assetPath(src: 'egaSubmission/select_samples.png')}">
            <figcaption>Select your samples</figcaption>
            <p>
                If you click the "<g:message code="egaSubmission.selectSamples.next"/>" button, all samples will be linked to the submission and
                you will be taken to the next page to select the files to submit for each sample.
            </p>

            <h2 id="sampleInformation">Sample information page</h2>
            <p>
                On this page you should complete the following information for each sample:
                <ul>
                    <li><g:message code="egaSubmission.sampleAlias"/> -> an alias for the sample which will be published and displayed on EGA</li>
                    <li><g:message code="egaSubmission.fastq"/> or <g:message code="egaSubmission.bam"/> -> select which file type do you want to submit</li>
                </ul>
                Here you can choose whether you want to configure the information on the web interface or rather in a CSV file.
                If you want to work with the CSV file click on the button "<g:message code="egaSubmission.downloadCsv"/>".
                Now you can open and edit the file in a spreadsheet program.
                Then you reimport your changes with the "<g:message code="egaSubmission.uploadCsv"/>" mask above the table.
            </p>
            <img class="centerImage" alt="samples_info" src="${assetPath(src: 'egaSubmission/samples_info.png')}">
            <figcaption>Display of selected samples and configuration of aliases</figcaption>
            <p>
                If you click the button "<g:message code="egaSubmission.sampleInformation.next"/>", all information will be stored and
                you will be taken to the next page to select the concrete files to upload.
            </p>

            <h2 id="selectFastq">Select FASTQ Files page</h2>
            <p>
                If you choose FASTQ in the previous step, you will enter this page.
                First, you have to choose what data you want to submit.
                Please make sure to have at least one file per sample selected.
                Then please click on "<g:message code="egaSubmission.selectFiles.saveSelection"/>".
                Now you can configure filenames which will be published on EGA.
                Again, you have the choice between Web interface and CSV file.

            </p>
            <img class="centerImage" alt="select_files" src="${assetPath(src: 'egaSubmission/select_files.png')}">
            <figcaption>Selection of files</figcaption>
            <p>
                If you click the "<g:message code="egaSubmission.selectFiles.saveAliases"/>" button, all information will be stored and
                you will be taken to the next page.
                If you have not selected any BAM files, your submission is now finished and we can start with the upload.
            </p>

            <h2 id="selectBam">Select BAM Files page</h2>
            <p>
                If you choose BAM on the sample information page, you will enter this page.
                At the moment there is the restriction that only OTP-produced BAM files can be uploaded.
                Imported files will not work. Therefore there is a deactivated preselection.
                First click on "<g:message code="egaSubmission.selectFiles.saveSelection"/>" and
                then you can configure filenames similar to those on the FASTQ files page.
                Once more, you can use both the web interface and the CSV file.
            </p>
            <p>
                If you click the "<g:message code="egaSubmission.selectFiles.saveAliases"/>" button, all information will be stored and
                you will be redirected back to the overview page.
                Now we can start with the upload.
            </p>
        </div>
    </body>
</html>
