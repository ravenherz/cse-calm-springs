# Regenerate i18n/en.js and i18n/ru.js from the JSON sources.
# Run from repo root or this folder. Requires PowerShell 5.1+ (no Node).
#
#   .\tools\regen-i18n.ps1
#
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $root 'i18n\en.json'))) {
  $root = $PSScriptRoot
}
$i18n = Join-Path $root 'i18n'
if (-not (Test-Path (Join-Path $i18n 'en.json'))) {
  throw "Cannot find i18n/en.json (looked under $root)"
}

$utf8 = New-Object System.Text.UTF8Encoding $false
foreach ($lang in @('en', 'ru')) {
  $jsonPath = Join-Path $i18n "$lang.json"
  $jsPath = Join-Path $i18n "$lang.js"
  $json = [System.IO.File]::ReadAllText($jsonPath, $utf8).Trim()
  try {
    $null = $json | ConvertFrom-Json
  } catch {
    throw "Invalid JSON in $jsonPath : $_"
  }
  $js = @(
    '(function (root) {'
    '  if (!root.FretI18n || !root.FretI18n.register) throw new Error(''FretI18n missing'');'
    "  root.FretI18n.register('$lang', $json"
    ');'
    '})(typeof window !== ''undefined'' ? window : this);'
    ''
  ) -join "`n"
  [System.IO.File]::WriteAllText($jsPath, $js, $utf8)
  Write-Host "Wrote $jsPath"
}

Write-Host 'Done. Reload the app (hard refresh) to pick up vocab changes.'
