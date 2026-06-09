[System.Reflection.Assembly]::LoadWithPartialName("System.IO.Compression.FileSystem")
$zip = [System.IO.Compression.ZipFile]::OpenRead("ShopSense Smart Credit Tracking & Retail (1).docx")
$entry = $zip.Entries | Where-Object { $_.FullName -eq "word/document.xml" }
$stream = $entry.Open()
$reader = New-Object System.IO.StreamReader($stream)
$xmlText = $reader.ReadToEnd()
$reader.Close()
$stream.Close()
$zip.Dispose()
$xml = [xml]$xmlText
$ns = New-Object System.Xml.XmlNamespaceManager($xml.NameTable)
$ns.AddNamespace("w", "http://schemas.openxmlformats.org/wordprocessingml/2006/main")
$paragraphs = $xml.SelectNodes("//w:p", $ns)
$textList = [System.Collections.Generic.List[string]]::new()
foreach ($p in $paragraphs) {
    $pText = ""
    $runs = $p.SelectNodes(".//w:t", $ns)
    foreach ($r in $runs) {
        $pText += $r.InnerText
    }
    $textList.Add($pText)
}
$textList | Out-File -FilePath "extracted_text.txt" -Encoding utf8
