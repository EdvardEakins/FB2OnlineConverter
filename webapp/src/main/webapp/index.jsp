<%@page contentType="text/html" pageEncoding="UTF-8" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Mobipocket/ePub Converter</title>
    <link href="styles/ll2e.css" type="text/css" rel="stylesheet"/>
</head>
<body>
<%--<div style="text-align: center;">--%><center>
    <table width=80%>
        <tr>
            <td>
                <H3>Mobipocket/ePub Converter web service</H3>
        </tr>
        <tr>
            <td align=left>
                <p>This service allows to convert eBook files from one format to another.</p>

                <p>Supported source formats: fb2, docx, rtf. Supported target formats: epub, mobi.</p>

                <p>Service based on <a href="http://code.google.com/p/epub-tools/wiki/EPUBGen">epub-tools</a> converter
                    engine and
                    <a href="http://www.amazon.com/gp/feature.html?ie=UTF8&docId=1000765211">KindleGen</a> tool.
                </p>

                <p>
                    The Converter can be used as a so-called REST Web-Service, simply use the following URL to convert
                    your files available on the WEB:<br>
                    <span style="white-space: nowrap;"><span style="font-weight: bold;">http://&lt;hostname&gt;/converter/get/convert?url=</span>[url of your file]<span
                            style="font-weight: bold;">&out=</span>[output format epub/mobi]</span><br/>
                    Source file can be compressed by zip.
                </p>

                <p>Optional request parameters:<br/>
                    md5 - md5 hash sum of uncompressed source file. Service can cache files and skip downloading of
                    cached files.<br/>
                    src - source file format. Service try to determine source file format by file name extension. This
                    parameter set source file format explicitly.<br/>
                </p>

                <p>

                <div style="background:yellow;padding:10px;border:solid 1px #999999;">
                    <span style="font-weight: bold; font-style: italic;">Disclaimer:</span>

                    <p>Please be aware that
                    <ul>
                        <li> the service is provided "as it is", hence, we do not guarantee functionality of this
                            service for any eBooks or quality of resultant eBooks;
                        <li> we do not take any responsibility for content of your eBooks which may not comply with any
                            legal process, regulation, registration or usage in the country of your origin.
                    </ul>
                </div>
            </td>
        </tr>
    </table>
<%--</div>--%>
</center></body>
</html>
