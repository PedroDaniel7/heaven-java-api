<#
.SYNOPSIS
  Smoke do CRUD administrativo de tipos de acao (/api/admin/tipos-acao).

.DESCRIPTION
  Cria um tipo de teste INATIVO, lista, atualiza, tenta apagar um tipo em uso (409) e apaga o de teste.
  Para garantir o "em uso" sem risco, um usuario auxiliar registra uma acao no tipo antes do DELETE; no fim
  a acao e cancelada e o usuario desativado. A chave vem da variavel de ambiente HEAVEN_ADMIN_KEY (a mesma
  do servidor) e nunca e impressa. Nao deixa tipo de teste no banco (a limpeza roda mesmo se algo falhar).
  Arquivo somente ASCII de proposito (PowerShell 5.1 le arquivo sem BOM como ANSI).

.PARAMETER BaseUrl
  Raiz do servidor, sem /api. Ex.: http://localhost:8080 ou https://meu-app.onrender.com

.EXAMPLE
  $env:HEAVEN_ADMIN_KEY = '...'; powershell -File docs/smoke-admin.ps1 -BaseUrl http://localhost:8080
#>
param(
    [Parameter(Mandatory = $true)][string]$BaseUrl,
    [int]$TimeoutSec = 90
)

$ErrorActionPreference = 'Stop'
$Raiz = $BaseUrl.TrimEnd('/')
if ($Raiz.EndsWith('/api')) { $Raiz = $Raiz.Substring(0, $Raiz.Length - 4) }

$script:Falhas = 0
$script:Total = 0

# ---------- infraestrutura ----------

function Read-Stream($stream) {
    if ($null -eq $stream) { return '' }
    $ms = New-Object System.IO.MemoryStream
    $stream.CopyTo($ms)
    return [System.Text.Encoding]::UTF8.GetString($ms.ToArray())
}

# Uma chamada HTTP. Devolve Status, Raw (texto UTF-8), Json (ou $null) e Location. Nunca lanca por status HTTP.
function Invoke-Api([string]$Metodo, [string]$Caminho, $Corpo = $null, [string]$Token = $null, [string]$CorpoBruto = $null, [string]$Chave = $null) {
    $uri = $Raiz + '/api' + $Caminho
    $cab = @{}
    if ($Token) { $cab['Authorization'] = 'Bearer ' + $Token }
    if ($Chave) { $cab['X-Admin-Key'] = $Chave }
    $p = @{ Uri = $uri; Method = $Metodo; Headers = $cab; UseBasicParsing = $true; TimeoutSec = $TimeoutSec }
    $texto = $null
    if ($CorpoBruto) { $texto = $CorpoBruto } elseif ($null -ne $Corpo) { $texto = ($Corpo | ConvertTo-Json -Compress) }
    if ($null -ne $texto) {
        $p['Body'] = [System.Text.Encoding]::UTF8.GetBytes($texto)
        $p['ContentType'] = 'application/json; charset=utf-8'
    }
    $status = 0; $raw = ''; $loc = $null
    try {
        $r = Invoke-WebRequest @p
        $status = [int]$r.StatusCode
        if ($r.RawContentStream) { $r.RawContentStream.Position = 0; $raw = Read-Stream $r.RawContentStream } else { $raw = [string]$r.Content }
        $loc = $r.Headers['Location']
    } catch {
        $resp = $_.Exception.Response
        if ($null -eq $resp) { throw "Sem resposta de $Metodo $Caminho (servidor fora do ar?): $($_.Exception.Message)" }
        $status = [int]$resp.StatusCode
        if ($_.Exception.GetType().Name -eq 'HttpResponseException') {
            $raw = [string]$_.ErrorDetails.Message           # PowerShell 7+
        } else {
            $raw = Read-Stream $resp.GetResponseStream()     # Windows PowerShell 5.1
        }
        try { $loc = $resp.Headers['Location'] } catch { $loc = $null }
    }
    if ($loc -is [System.Array]) { $loc = $loc[0] }
    $json = $null
    $inicio = ''
    if ($raw) { $inicio = $raw.TrimStart() }
    if ($inicio.StartsWith('{') -or $inicio.StartsWith('[')) {
        try { $json = $raw | ConvertFrom-Json } catch { $json = $null }
    }
    return [pscustomobject]@{ Status = $status; Raw = $raw; Json = $json; Location = $loc }
}

# Resumo seguro para imprimir: token oculto, texto truncado.
function Resumo([string]$raw) {
    if (-not $raw) { return '(sem corpo)' }
    $s = [regex]::Replace($raw, '"token"\s*:\s*"[^"]*"', '"token":"<oculto>"')
    $s = [regex]::Replace($s, '\s+', ' ')
    if ($s.Length -gt 170) { $s = $s.Substring(0, 170) + '...' }
    return $s
}

function Check([string]$Nome, $Resp, [int]$Esperado, [scriptblock]$Extra = $null) {
    $script:Total++
    $ok = ($Resp.Status -eq $Esperado)
    $obs = ''
    if ($ok -and $Extra) {
        try { $msg = & $Extra $Resp; if ($msg) { $ok = $false; $obs = " <- $msg" } } catch { $ok = $false; $obs = " <- $($_.Exception.Message)" }
    }
    if (-not $ok) { $script:Falhas++ }
    $marca = if ($ok) { 'OK   ' } else { 'FALHA' }
    Write-Host ("[{0}] {1} -> {2} (esperado {3}){4}" -f $marca, $Nome, $Resp.Status, $Esperado, $obs)
    Write-Host ("        " + (Resumo $Resp.Raw))
}

# Devolve texto de erro (ou $null se tudo certo) para o formato de erro do contrato.
function Erro-Contrato($r, [string]$codigo) {
    if ($null -eq $r.Json) { return 'corpo de erro nao e JSON' }
    $p = $r.Json.PSObject.Properties.Name
    foreach ($n in 'status', 'codigo', 'mensagem', 'campos', 'dataHora') { if ($p -notcontains $n) { return "falta o campo $n" } }
    if ($r.Json.status -ne $r.Status) { return 'status do corpo difere do HTTP' }
    if ($r.Json.codigo -ne $codigo) { return "codigo $($r.Json.codigo), esperado $codigo" }
    if ($r.Raw -match 'ORA-|SQLException|\bat br\.com|jdbc:') { return 'corpo vaza detalhe tecnico' }
    return $null
}

function Sem-Segredos($r) {
    if ($r.Raw -match '(?i)senhaHash|senha_hash|pbkdf2|peso|reputacaoMinima|idNivel|nivelInterno') { return 'resposta expoe campo interno' }
    return $null
}

function Campos-Exatos($obj, [string[]]$esperados) {
    $tem = ($obj.PSObject.Properties.Name | Sort-Object) -join ','
    $deve = ($esperados | Sort-Object) -join ','
    if ($tem -ne $deve) { return "campos [$tem], esperado [$deve]" }
    return $null
}

# ---------- admin ----------

$chave = $env:HEAVEN_ADMIN_KEY
if (-not $chave) { Write-Host 'Defina HEAVEN_ADMIN_KEY no ambiente (a mesma chave do servidor). Ela nunca e impressa.'; exit 2 }

$camposAdmin = 'id', 'nome', 'desafio', 'categoria', 'unidade', 'impactoAmbientalEstimado', 'pontosBase', 'dificuldade', 'ativo'
$carimbo = (Get-Date).ToString('yyyyMMddHHmmss') + '-' + (Get-Random -Maximum 99999)
$nomeTeste = "Smoke admin $carimbo"
$corpoOk = @{ nome = $nomeTeste; desafio = 'Tipo criado pelo smoke-admin'; categoria = 'OUTRO'; unidade = 'un'; impactoAmbientalEstimado = 1.25; pontosBase = 5; dificuldade = 'FACIL'; ativo = $false }
$idTeste = $null
$emailU = "admin.$carimbo@example.test"
$senhaU = 'Adm-' + [guid]::NewGuid().ToString('N')
$tokenU = $null; $acaoU = $null

Write-Host "Smoke admin em $Raiz  (tipo de teste: $nomeTeste)"

try {
    Write-Host "`n== Chave =="
    $r = Invoke-Api GET '/admin/tipos-acao'
    Check 'sem chave (401)' $r 401 { param($x) Erro-Contrato $x 'NAO_AUTENTICADO' }
    $r = Invoke-Api GET '/admin/tipos-acao' -Chave ($chave + 'x')
    Check 'chave errada (401)' $r 401 { param($x) Erro-Contrato $x 'NAO_AUTENTICADO' }
    $r = Invoke-Api POST '/admin/tipos-acao' $corpoOk
    Check 'POST sem chave (401)' $r 401
    $r = Invoke-Api DELETE '/admin/tipos-acao/1'
    Check 'DELETE sem chave (401)' $r 401

    Write-Host "`n== GET lista =="
    $r = Invoke-Api GET '/admin/tipos-acao' -Chave $chave
    Check 'GET /admin/tipos-acao' $r 200 { param($x) $l = @($x.Json); if ($l.Count -lt 1) { return 'lista vazia' }; Campos-Exatos $l[0] $camposAdmin }
    $existentes = @($r.Json)
    $outroNome = $existentes[0].nome

    Write-Host "`n== POST =="
    $r = Invoke-Api POST '/admin/tipos-acao' $corpoOk -Chave $chave
    Check 'cria tipo de teste INATIVO' $r 201 { param($x) $e = Campos-Exatos $x.Json $camposAdmin; if ($e) { return $e }; if ($x.Json.ativo -ne $false) { return 'ativo deveria ser false' } }
    if ($r.Status -ne 201) { throw 'Tipo de teste nao foi criado.' }
    $idTeste = [long]$r.Json.id
    $ruim = @{ nome = ''; categoria = 'INEXISTENTE'; unidade = 'un'; impactoAmbientalEstimado = 0; pontosBase = 1000000; dificuldade = 'IMPOSSIVEL' }
    $r = Invoke-Api POST '/admin/tipos-acao' $ruim -Chave $chave
    Check 'POST 400 (nome, categoria, impacto, pontos, dificuldade)' $r 400 { param($x) $e = Erro-Contrato $x 'VALIDACAO'; if ($e) { return $e }; if ($x.Json.campos.Count -lt 5) { return "$($x.Json.campos.Count) campos com erro, esperado 5" } }
    $r = Invoke-Api POST '/admin/tipos-acao' $corpoOk -Chave $chave
    Check 'POST 409 nome repetido' $r 409 { param($x) Erro-Contrato $x 'CONFLITO' }

    Write-Host "`n== Visibilidade do inativo =="
    $r = Invoke-Api GET '/admin/tipos-acao' -Chave $chave
    Check 'admin lista o tipo inativo' $r 200 { param($x) $t = @($x.Json | Where-Object { $_.id -eq $idTeste }); if ($t.Count -ne 1) { return 'tipo de teste nao aparece' } }
    $r = Invoke-Api GET '/tipos-acao'
    Check 'lista publica NAO traz o inativo' $r 200 { param($x) if (@($x.Json | Where-Object { $_.id -eq $idTeste }).Count -ne 0) { return 'inativo apareceu na lista publica' } }
    $r = Invoke-Api GET "/tipos-acao/$idTeste"
    Check 'GET publico do inativo (404)' $r 404

    Write-Host "`n== PUT =="
    $novo = @{} + $corpoOk; $novo.pontosBase = 11; $novo.desafio = 'Atualizado pelo smoke-admin'
    $r = Invoke-Api PUT "/admin/tipos-acao/$idTeste" $novo -Chave $chave
    Check 'PUT atualiza' $r 200 { param($x) if ([long]$x.Json.pontosBase -ne 11 -or $x.Json.desafio -ne 'Atualizado pelo smoke-admin') { return 'campos nao mudaram' } }
    $semAtivo = @{} + $novo; $semAtivo.Remove('ativo'); $semAtivo.pontosBase = 12
    $r = Invoke-Api PUT "/admin/tipos-acao/$idTeste" $semAtivo -Chave $chave
    Check 'PUT sem "ativo" mantem o valor (false)' $r 200 { param($x) if ($x.Json.ativo -ne $false) { return 'ativo mudou' } }
    $r = Invoke-Api PUT '/admin/tipos-acao/999999999' $novo -Chave $chave
    Check 'PUT 404' $r 404 { param($x) Erro-Contrato $x 'NAO_ENCONTRADO' }
    $r = Invoke-Api PUT "/admin/tipos-acao/$idTeste" $ruim -Chave $chave
    Check 'PUT 400' $r 400 { param($x) Erro-Contrato $x 'VALIDACAO' }
    $dup = @{} + $novo; $dup.nome = $outroNome
    $r = Invoke-Api PUT "/admin/tipos-acao/$idTeste" $dup -Chave $chave
    Check 'PUT 409 nome de outro tipo' $r 409 { param($x) Erro-Contrato $x 'CONFLITO' }
    $r = Invoke-Api PUT "/admin/tipos-acao/$idTeste" $novo
    Check 'PUT sem chave (401)' $r 401

    Write-Host "`n== DELETE de tipo em uso (409) =="
    # Garante o "em uso": um usuario de teste registra uma acao num tipo ativo e o admin tenta apagar esse tipo.
    $r = Invoke-Api POST '/usuarios' @{ nome = 'Admin Smoke'; email = $emailU; senha = $senhaU }
    Check 'cadastra usuario auxiliar' $r 201
    $r = Invoke-Api POST '/auth/login' @{ email = $emailU; senha = $senhaU }
    Check 'login do auxiliar' $r 200
    if ($r.Status -ne 200) { throw 'Login do auxiliar falhou.' }
    $tokenU = $r.Json.token
    $r = Invoke-Api GET '/tipos-acao'
    $tipoUso = @($r.Json)[0].id
    $r = Invoke-Api POST '/acoes' @{ idTipoAcao = $tipoUso; quantidade = 1; evidencia = 'Smoke admin: garante tipo em uso' } -Token $tokenU
    Check 'auxiliar registra acao no tipo' $r 201
    if ($r.Status -ne 201) { throw 'Acao auxiliar nao foi criada; nao e seguro testar o DELETE de tipo em uso.' }
    $acaoU = $r.Json.id
    $r = Invoke-Api DELETE "/admin/tipos-acao/$tipoUso" -Chave $chave
    Check 'DELETE tipo com acoes (409, sem ORA-)' $r 409 { param($x) Erro-Contrato $x 'CONFLITO' }
    $r = Invoke-Api GET "/tipos-acao/$tipoUso"
    Check 'o tipo em uso continua existindo' $r 200

    Write-Host "`n== DELETE do tipo de teste =="
    $r = Invoke-Api DELETE "/admin/tipos-acao/$idTeste" -Chave $chave
    Check 'DELETE tipo de teste' $r 204
    if ($r.Status -eq 204) { $idTeste = $null }
    $r = Invoke-Api DELETE '/admin/tipos-acao/999999999' -Chave $chave
    Check 'DELETE 404' $r 404 { param($x) Erro-Contrato $x 'NAO_ENCONTRADO' }
}
catch {
    $script:Falhas++
    Write-Host "ABORTADO: $($_.Exception.Message)"
}
finally {
    Write-Host "`n== Limpeza =="
    if ($null -ne $idTeste) {
        $d = Invoke-Api DELETE "/admin/tipos-acao/$idTeste" -Chave $chave
        Check 'apaga o tipo de teste que sobrou' $d 204
    }
    if ($tokenU) {
        if ($acaoU) { $c = Invoke-Api DELETE "/acoes/$acaoU" -Token $tokenU; Check 'cancela a acao auxiliar' $c 204 }
        $d = Invoke-Api DELETE '/usuarios/me' -Token $tokenU
        Check 'desativa o usuario auxiliar' $d 204
    }
}

Write-Host ''
if ($script:Falhas -eq 0) { Write-Host "TUDO OK: $($script:Total) verificacoes."; exit 0 }
Write-Host "$($script:Falhas) de $($script:Total) verificacoes FALHARAM."
exit 1
