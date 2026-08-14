# Run Fretboard Lab theory locks headlessly (Chrome or Edge).
# Exit 0 = all lock fixtures green. Known-debt amber does not fail the run.
# Also checks brace/paren balance on core JS (no Node required).
#
#   .\tools\run-theory-tests.ps1
#   .\tools\run-theory-tests.ps1 -SkipBraceCheck
#
param(
  [switch]$SkipBraceCheck,
  [switch]$SkipBrowser
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $root 'test.html'))) {
  $root = $PSScriptRoot
}
if (-not (Test-Path (Join-Path $root 'test.html'))) {
  throw "Cannot find test.html (looked under $root)"
}

function Test-JsBalance {
  param([string]$Path)
  $text = [System.IO.File]::ReadAllText($Path)
  $brace = 0; $paren = 0; $bracket = 0
  $inStr = $null
  $escape = $false
  $inLine = $false
  $inBlock = $false
  for ($i = 0; $i -lt $text.Length; $i++) {
    $ch = $text[$i]
    $next = if ($i + 1 -lt $text.Length) { $text[$i + 1] } else { [char]0 }
    if ($inLine) {
      if ($ch -eq "`n") { $inLine = $false }
      continue
    }
    if ($inBlock) {
      if ($ch -eq '*' -and $next -eq '/') { $inBlock = $false; $i++; }
      continue
    }
    if ($inStr) {
      if ($escape) { $escape = $false; continue }
      if ($ch -eq '\') { $escape = $true; continue }
      if ($ch -eq $inStr) { $inStr = $null }
      continue
    }
    if ($ch -eq '/' -and $next -eq '/') { $inLine = $true; $i++; continue }
    if ($ch -eq '/' -and $next -eq '*') { $inBlock = $true; $i++; continue }
    if ($ch -eq '"' -or $ch -eq "'" -or $ch -eq '`') { $inStr = $ch; continue }
    if ($ch -eq '{') { $brace++ }
    elseif ($ch -eq '}') { $brace-- }
    elseif ($ch -eq '(') { $paren++ }
    elseif ($ch -eq ')') { $paren-- }
    elseif ($ch -eq '[') { $bracket++ }
    elseif ($ch -eq ']') { $bracket-- }
    if ($brace -lt 0 -or $paren -lt 0 -or $bracket -lt 0) {
      throw "Unbalanced closer early in $Path"
    }
  }
  if ($brace -ne 0 -or $paren -ne 0 -or $bracket -ne 0) {
    throw "Unbalanced braces/parens/brackets in $Path ({}=$brace ()=$paren []=$bracket)"
  }
}

if (-not $SkipBraceCheck) {
  $jsFiles = @(
    'js\music.js', 'js\chords.js', 'js\geometry.js', 'js\caged.js', 'js\quartal.js',
    'js\deeplink.js', 'js\i18n.js', 'js\tests\runner.js', 'js\tests\fixtures.js'
  )
  foreach ($rel in $jsFiles) {
    $full = Join-Path $root $rel
    if (-not (Test-Path $full)) { throw "Missing $rel" }
    Test-JsBalance -Path $full
    Write-Host "Balance OK  $rel"
  }
}

if ($SkipBrowser) {
  Write-Host 'Skipped browser run (-SkipBrowser).'
  exit 0
}

function Find-Browser {
  $candidates = @(
    (Join-Path ${env:ProgramFiles} 'Google\Chrome\Application\chrome.exe'),
    (Join-Path ${env:ProgramFiles(x86)} 'Google\Chrome\Application\chrome.exe'),
    (Join-Path ${env:ProgramFiles} 'Microsoft\Edge\Application\msedge.exe'),
    (Join-Path ${env:ProgramFiles(x86)} 'Microsoft\Edge\Application\msedge.exe')
  )
  foreach ($c in $candidates) {
    if ($c -and (Test-Path $c)) { return $c }
  }
  return $null
}

$browser = Find-Browser
if (-not $browser) {
  throw 'Chrome or Edge not found. Install one, or pass -SkipBrowser after brace check.'
}

$uri = 'file:///' + (($root -replace '\\', '/') -replace ' ', '%20') + '/test.html'
$tmp = Join-Path $env:TEMP ('fretlab-theory-' + [guid]::NewGuid().ToString('n') + '.html')
Write-Host "Running $uri"
Write-Host "Browser $browser"

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $browser
$psi.Arguments = "--headless=new --disable-gpu --allow-file-access-from-files --virtual-time-budget=15000 --dump-dom `"$uri`""
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true
$proc = [System.Diagnostics.Process]::Start($psi)
$stdout = $proc.StandardOutput.ReadToEnd()
$null = $proc.StandardError.ReadToEnd()
$proc.WaitForExit()
if (-not $stdout) {
  throw 'Browser produced empty DOM dump'
}
[System.IO.File]::WriteAllText($tmp, $stdout)

$html = Get-Content -Path $tmp -Raw -ErrorAction Stop
Remove-Item -Path $tmp -Force -ErrorAction SilentlyContinue

if ($html -notmatch 'data-strict-ok=') {
  throw 'test.html did not emit data-strict-ok (scripts may have failed to load)'
}

$strict = if ($html -match 'data-strict-ok="(1|0)"') { $Matches[1] } else { '' }
$lockPass = if ($html -match 'data-lock-pass="(\d+)"') { $Matches[1] } else { '?' }
$lockFail = if ($html -match 'data-lock-fail="(\d+)"') { $Matches[1] } else { '?' }
$knownFail = if ($html -match 'data-known-fail="(\d+)"') { $Matches[1] } else { '?' }
$knownPass = if ($html -match 'data-known-pass="(\d+)"') { $Matches[1] } else { '?' }

Write-Host "lock pass=$lockPass fail=$lockFail · known open=$knownFail cleared=$knownPass"

if ($strict -ne '1') {
  Write-Error "Theory lock broken (data-strict-ok=$strict). Open test.html for details."
  exit 1
}

Write-Host 'Strict lock green.'
exit 0
