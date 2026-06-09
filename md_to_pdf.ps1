# PowerShell Script to convert ShopSense Markdown Document to Premium PDF
# Uses built-in Microsoft Edge headless print-to-pdf engine. No external libraries needed!

$mdPath = "$PSScriptRoot\project_development_document.md"
$htmlPath = "$PSScriptRoot\project_development_document.html"
$pdfPath = "$PSScriptRoot\ShopSense_Development_Guide.pdf"

if (-not (Test-Path $mdPath)) {
    Write-Error "Source markdown file not found: $mdPath"
    Exit
}

Write-Host "Reading and parsing markdown..."
$lines = Get-Content -Path $mdPath
$htmlBody = ""
$inList = $false
$inCode = $false
$inTable = $false

# Simple Line-by-Line Markdown to HTML Parser
foreach ($line in $lines) {
    $trimmed = $line.Trim()
    
    # Handle Code Blocks
    if ($trimmed -like "```*") {
        if ($inCode) {
            $htmlBody += "</code></pre>`n"
            $inCode = $false
        } else {
            $htmlBody += "<pre><code>"
            $inCode = $true
        }
        continue
    }
    
    if ($inCode) {
        $htmlBody += [System.Web.HttpUtility]::HtmlEncode($line) + "`n"
        continue
    }

    # Handle Tables
    if ($trimmed -like "|*|") {
        # Skip separator rows like | :--- | :--- |
        if ($trimmed -like "*---*") {
            continue
        }
        
        if (-not $inTable) {
            $htmlBody += "<table>`n"
            $inTable = $true
            # Header Row
            $cols = $trimmed.Split('|') | Where-Object { $_ -ne "" }
            $htmlBody += "<thead><tr>`n"
            foreach ($col in $cols) {
                $cVal = $col.Trim() -replace '\*\*(.*?)\*\*', '<strong>$1</strong>'
                $htmlBody += "<th>$cVal</th>`n"
            }
            $htmlBody += "</tr></thead>`n<tbody>`n"
        } else {
            # Data Row
            $cols = $trimmed.Split('|') | Where-Object { $_ -ne "" }
            $htmlBody += "<tr>`n"
            foreach ($col in $cols) {
                $cVal = $col.Trim() -replace '\*\*(.*?)\*\*', '<strong>$1</strong>'
                $htmlBody += "<td>$cVal</td>`n"
            }
            $htmlBody += "</tr>`n"
        }
        continue
    } else {
        if ($inTable) {
            $htmlBody += "</tbody></table>`n"
            $inTable = $false
        }
    }

    # Handle Headers
    if ($trimmed -like "# *") {
        $val = $trimmed.Substring(2) -replace '\*\*(.*?)\*\*', '<strong>$1</strong>'
        $htmlBody += "<h1 class='page-break'>$val</h1>`n"
        continue
    }
    if ($trimmed -like "## *") {
        $val = $trimmed.Substring(3) -replace '\*\*(.*?)\*\*', '<strong>$1</strong>'
        $htmlBody += "<h2>$val</h2>`n"
        continue
    }
    if ($trimmed -like "### *") {
        $val = $trimmed.Substring(4) -replace '\*\*(.*?)\*\*', '<strong>$1</strong>'
        $htmlBody += "<h3>$val</h3>`n"
        continue
    }
    if ($trimmed -like "#### *") {
        $val = $trimmed.Substring(5) -replace '\*\*(.*?)\*\*', '<strong>$1</strong>'
        $htmlBody += "<h4>$val</h4>`n"
        continue
    }

    # Handle Horizontal Rule
    if ($trimmed -eq "---") {
        $htmlBody += "<hr/>`n"
        continue
    }

    # Handle List Items
    if ($trimmed -like "- *") {
        if (-not $inList) {
            $htmlBody += "<ul>`n"
            $inList = $true
        }
        $val = $trimmed.Substring(2) -replace '\*\*(.*?)\*\*', '<strong>$1</strong>'
        $htmlBody += "<li>$val</li>`n"
        continue
    } else {
        if ($inList) {
            $htmlBody += "</ul>`n"
            $inList = $false
        }
    }

    # Handle Paragraphs (skip empty lines)
    if ($trimmed -ne "") {
        # Parse inline bold and links
        $val = $trimmed -replace '\*\*(.*?)\*\*', '<strong>$1</strong>'
        $val = $val -replace '\[(.*?)\]\((.*?)\)', '<a href="$2">$1</a>'
        $htmlBody += "<p>$val</p>`n"
    }
}

# Close any open tags
if ($inList) { $htmlBody += "</ul>`n" }
if ($inTable) { $htmlBody += "</table>`n" }

# Premium HTML Shell with custom CSS styling and cover page
$htmlOutput = @"
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>ShopSense Development Documentation</title>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@500;700&family=Poppins:wght@600;700;800&display=swap');
        
        body {
            font-family: 'Inter', sans-serif;
            color: #1E2A14;
            background-color: #ffffff;
            line-height: 1.6;
            margin: 1.2in;
            font-size: 14px;
        }

        h1, h2, h3, h4 {
            font-family: 'Poppins', sans-serif;
            color: #1E2A14;
            font-weight: bold;
        }

        h1 {
            font-size: 24px;
            color: #7AB342;
            border-bottom: 2px solid #C8D9B5;
            padding-bottom: 8px;
            margin-top: 40px;
        }

        h2 {
            font-size: 18px;
            color: #5A8A2E;
            margin-top: 30px;
            border-bottom: 1px solid #EDF3E6;
            padding-bottom: 5px;
        }

        h3 {
            font-size: 15px;
            color: #1E2A14;
        }

        p {
            margin-bottom: 15px;
            text-align: justify;
        }

        ul {
            margin-bottom: 20px;
            padding-left: 20px;
        }

        li {
            margin-bottom: 5px;
        }

        a {
            color: #7AB342;
            text-decoration: none;
            font-weight: bold;
        }

        /* Table Styling */
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 25px 0;
            font-size: 13px;
            page-break-inside: avoid;
        }

        th {
            background-color: #EDF3E6;
            color: #1E2A14;
            text-align: left;
            font-weight: bold;
            padding: 10px;
            border: 1px solid #C8D9B5;
            font-family: 'Poppins', sans-serif;
        }

        td {
            padding: 10px;
            border: 1px solid #C8D9B5;
        }

        tr:nth-child(even) {
            background-color: #F5F7F2;
        }

        /* Code blocks */
        pre {
            background-color: #F5F7F2;
            border: 1px solid #C8D9B5;
            padding: 15px;
            border-radius: 8px;
            overflow-x: auto;
            font-family: 'JetBrains Mono', monospace;
            font-size: 12px;
            margin: 20px 0;
            page-break-inside: avoid;
        }

        /* Title Cover Page styling */
        .cover-page {
            height: 100vh;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            text-align: center;
            page-break-after: always;
            padding-top: 150px;
        }

        .cover-title {
            font-size: 36px;
            color: #7AB342;
            margin-bottom: 10px;
            font-family: 'Poppins', sans-serif;
            font-weight: 800;
        }

        .cover-subtitle {
            font-size: 18px;
            color: #5A6650;
            margin-bottom: 60px;
            font-family: 'Inter', sans-serif;
            font-weight: 500;
        }

        .cover-meta {
            margin-top: 100px;
            font-size: 13px;
            color: #1E2A14;
            line-height: 1.8;
            border-top: 1px solid #C8D9B5;
            padding-top: 20px;
            width: 80%;
        }

        /* Print Media formatting */
        @media print {
            .page-break {
                page-break-before: always;
            }
            body {
                margin: 0.8in;
            }
        }
    </style>
</head>
<body>

    <!-- Cover Page -->
    <div class="cover-page">
        <div class="cover-title">💎 ShopSense</div>
        <div class="cover-subtitle">Smart Credit Tracking & Retail Billing System</div>
        <div style="font-size: 16px; font-weight: bold; color: #1E2A14; margin-top: 20px;">
            Object-Oriented Programming (OOP) Open-Ended Lab Project
        </div>
        
        <div class="cover-meta">
            <strong>DEVELOPMENT TEAM MEMBERS</strong><br/>
            Ryan Nasir (Student ID: 74832) - Core OOP Architect<br/>
            Muhammad Umar (Student ID: 74786) - Backend & Persistence<br/>
            Muhammad Wahaj (Student ID: 74742) - UI Layout & Styling<br/>
            Muhammad Asim Abbasi (Student ID: 74844) - Ledger & Analytics<br/>
            <br/>
            <strong>INSTRUCTOR:</strong> Faiza Latif Abbasi<br/>
            <strong>SEMESTER:</strong> Spring 2026<br/>
            <strong>SLOT:</strong> 08:30 to 11:20 - WEDNESDAY
        </div>
    </div>

    <!-- Main Content -->
    $htmlBody

</body>
</html>
"@

# Write HTML output
$htmlOutput | Out-File -FilePath $htmlPath -Encoding utf8
Write-Host "HTML file created successfully: $htmlPath"

# Find Microsoft Edge executable path
$edgePaths = @(
    "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    "C:\Program Files\Microsoft\Edge\Application\msedge.exe",
    "msedge" # Fallback to path
)

$edgeExe = ""
foreach ($p in $edgePaths) {
    if (Test-Path $p) {
        $edgeExe = $p
        break
    }
}

if ($edgeExe -eq "") {
    $edgeExe = "msedge.exe"
}

Write-Host "Locating Microsoft Edge: $edgeExe"
Write-Host "Rendering and printing HTML to PDF..."

# Execute headless print-to-pdf command
Start-Process -FilePath $edgeExe -ArgumentList "--headless", "--disable-gpu", "--print-to-pdf=`"$pdfPath`"", "`"$htmlPath`"" -Wait

if (Test-Path $pdfPath) {
    Write-Host "PDF guide generated successfully: $pdfPath"
    # Remove temporary HTML file
    Remove-Item $htmlPath -Force
} else {
    Write-Error "Failed to generate PDF. Please ensure Microsoft Edge is installed."
}
