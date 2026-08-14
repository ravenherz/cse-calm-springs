# Regenerate js/readme.js from README.md (script-tag embed for About tab; works on file://).
# Run from repo root or this folder. Requires PowerShell 5.1+ (no Node).
#
#   .\tools\regen-readme.ps1
#
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $root 'README.md'))) {
  $root = $PSScriptRoot
}
$mdPath = Join-Path $root 'README.md'
$jsDir = Join-Path $root 'js'
$jsPath = Join-Path $jsDir 'readme.js'
if (-not (Test-Path $mdPath)) {
  throw "Cannot find README.md (looked under $root)"
}
if (-not (Test-Path $jsDir)) {
  throw "Cannot find js/ under $root"
}

$utf8 = New-Object System.Text.UTF8Encoding $false
$md = [System.IO.File]::ReadAllText($mdPath, $utf8)
# Normalize newlines for stable output
$md = $md -replace "`r`n", "`n" -replace "`r", "`n"
$json = ConvertTo-Json -InputObject $md -Compress
$js = @(
  '(function (root) {'
  "  root.FretReadme = { markdown: $json };"
  '})(typeof window !== ''undefined'' ? window : this);'
  ''
) -join "`n"
[System.IO.File]::WriteAllText($jsPath, $js, $utf8)
Write-Host "Wrote $jsPath"
Write-Host 'Done. Reload the app (hard refresh) to pick up README changes.'
