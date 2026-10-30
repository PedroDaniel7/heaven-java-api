$ErrorActionPreference = 'Stop'
if (-not $env:JAVA_HOME) { throw 'Defina JAVA_HOME para um JDK 17 ou superior.' }
$heavenRoot = $PSScriptRoot
$heavenClasses = Join-Path $heavenRoot 'target/teste-local'
New-Item -ItemType Directory -Force -Path $heavenClasses | Out-Null
$heavenFontes = Get-ChildItem -LiteralPath (Join-Path $heavenRoot 'src/main/java') -Recurse -Filter '*.java' | ForEach-Object FullName
& (Join-Path $env:JAVA_HOME 'bin/javac.exe') --release 17 -encoding UTF-8 -d $heavenClasses $heavenFontes
if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação.' }
& (Join-Path $env:JAVA_HOME 'bin/java.exe') -cp $heavenClasses br.com.heaven.test.TesteRegras
if ($LASTEXITCODE -ne 0) { throw 'Falha nos testes.' }
