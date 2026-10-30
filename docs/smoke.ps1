<#
.SYNOPSIS
  Smoke test da API Heaven (docs/contrato-api.md): saude, usuarios, auth, tipos de acao e acoes.

.DESCRIPTION
  Cria duas contas de teste (e-mails unicos em example.test), exercita sucesso e os erros 400/401/404/409
  de cada recurso e, ao final, desativa as contas com DELETE /usuarios/me (mesmo se algum caso falhar).
  Nao imprime senha nem token. Faz uma chamada por vez, sem retentativas.
  Arquivo somente ASCII de proposito: o PowerShell 5.1 le arquivo sem BOM como ANSI.

.PARAMETER BaseUrl
  Raiz do servidor, sem /api. Ex.: http://localhost:8080 ou https://meu-app.onrender.com

.PARAMETER TimeoutSec
  Tempo maximo por chamada (o primeiro acesso a um servidor dormindo pode demorar).

.EXAMPLE
  powershell -File docs/smoke.ps1 -BaseUrl http://localhost:8080
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
function Invoke-Api([string]$Metodo, [string]$Caminho, $Corpo = $null, [string]$Token = $null, [string]$CorpoBruto = $null) {
    $uri = $Raiz + '/api' + $Caminho
    $cab = @{}
    if ($Token) { $cab['Authorization'] = 'Bearer ' + $Token }
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

# ---------- dados de teste ----------

$carimbo = (Get-Date).ToString('yyyyMMddHHmmss') + '-' + (Get-Random -Maximum 99999)
$emailA = "smoke.$carimbo.a@example.test"
$emailB = "smoke.$carimbo.b@example.test"
$nomeA = 'Jo' + [char]0xE3 + 'o Smoke'          # acento montado por codigo: o arquivo fica ASCII
$senhaA = 'Sm0ke-' + [guid]::NewGuid().ToString('N')
$senhaB = 'Sm0ke-' + [guid]::NewGuid().ToString('N')
$novaSenhaA = 'Nova-' + [guid]::NewGuid().ToString('N')
$tokenA = $null; $tokenB = $null
$camposUsuario = 'id', 'nome', 'email', 'pontos', 'reputacao', 'nivel', 'streak', 'maiorStreak', 'dataCadastro'
$camposTipo = 'id', 'nome', 'desafio', 'categoria', 'unidade', 'dificuldade'
$camposAcao = 'id', 'tipo', 'quantidade', 'evidencia', 'status', 'pontosPrevistos', 'dataHora', 'dataConclusao'

Write-Host "Smoke Heaven em $Raiz  (contas: $emailA / $emailB)"

try {
    # ===== Saude =====
    Write-Host "`n== Saude =="
    $r = Invoke-Api GET '/saude'
    Check 'GET /saude' $r 200 { param($x) Campos-Exatos $x.Json 'status', 'dataHora' }
    $r = Invoke-Api GET '/saude/banco'
    Check 'GET /saude/banco' $r 200 { param($x) Campos-Exatos $x.Json 'status', 'dataHora' }

    # ===== POST /usuarios =====
    Write-Host "`n== POST /usuarios =="
    $r = Invoke-Api POST '/usuarios' @{ nome = $nomeA; email = $emailA; senha = $senhaA }
    Check 'cadastro A' $r 201 {
        param($x)
        $e = Campos-Exatos $x.Json $camposUsuario; if ($e) { return $e }
        $e = Sem-Segredos $x; if ($e) { return $e }
        if (-not $x.Location) { return 'sem header Location' }
        if ($x.Json.nome -ne $nomeA) { return 'nome com acento nao voltou igual' }
        if ($x.Json.email -ne $emailA) { return 'email diferente do enviado' }
    }
    $r = Invoke-Api POST '/usuarios' @{ nome = 'Bia Smoke'; email = $emailB; senha = $senhaB }
    Check 'cadastro B' $r 201 { param($x) Campos-Exatos $x.Json $camposUsuario }
    $r = Invoke-Api POST '/usuarios' @{ nome = 'Curta'; email = "smoke.$carimbo.c@example.test"; senha = 'curta' }
    Check '400 senha curta' $r 400 { param($x) $e = Erro-Contrato $x 'VALIDACAO'; if ($e) { return $e }; if (-not ($x.Json.campos | Where-Object { $_.campo -eq 'senha' })) { return 'campos sem "senha"' } }
    $r = Invoke-Api POST '/usuarios' @{ senha = 'umasenhaboa1' }
    Check '400 nome e email ausentes' $r 400 { param($x) $e = Erro-Contrato $x 'VALIDACAO'; if ($e) { return $e }; if ($x.Json.campos.Count -lt 2) { return 'esperava 2 campos com erro' } }
    $r = Invoke-Api POST '/usuarios' -CorpoBruto '{ isto nao e json'
    Check '400 JSON malformado' $r 400 { param($x) Erro-Contrato $x 'VALIDACAO' }
    $r = Invoke-Api POST '/usuarios' @{ nome = 'Dup'; email = $emailA.ToUpper(); senha = 'umasenhaboa1' }
    Check '409 e-mail repetido' $r 409 { param($x) Erro-Contrato $x 'CONFLITO' }

    # ===== POST /auth/login =====
    Write-Host "`n== POST /auth/login =="
    $r = Invoke-Api POST '/auth/login' @{ email = $emailA; senha = $senhaA }
    Check 'login A' $r 200 {
        param($x)
        $e = Campos-Exatos $x.Json 'token', 'expiraEm', 'usuario'; if ($e) { return $e }
        $e = Campos-Exatos $x.Json.usuario $camposUsuario; if ($e) { return $e }
        $e = Sem-Segredos $x; if ($e) { return $e }
        if (-not $x.Json.token) { return 'token vazio' }
    }
    if ($r.Status -eq 200) { $tokenA = $r.Json.token }
    $r = Invoke-Api POST '/auth/login' @{ email = $emailB; senha = $senhaB }
    Check 'login B' $r 200
    if ($r.Status -eq 200) { $tokenB = $r.Json.token }
    $r = Invoke-Api POST '/auth/login' @{ email = $emailA }
    Check '400 sem senha' $r 400 { param($x) Erro-Contrato $x 'VALIDACAO' }
    $r1 = Invoke-Api POST '/auth/login' @{ email = $emailA; senha = 'senha-errada-123' }
    Check '401 senha errada' $r1 401 { param($x) Erro-Contrato $x 'NAO_AUTENTICADO' }
    $r2 = Invoke-Api POST '/auth/login' @{ email = "naoexiste.$carimbo@example.test"; senha = 'senha-errada-123' }
    Check '401 e-mail inexistente (mesma mensagem)' $r2 401 { param($x) $e = Erro-Contrato $x 'NAO_AUTENTICADO'; if ($e) { return $e }; if ($x.Json.mensagem -ne $r1.Json.mensagem) { return 'mensagem difere da de senha errada' } }

    if (-not $tokenA -or -not $tokenB) { throw 'Login falhou: nao ha como continuar os casos autenticados.' }
    $adulterado = $tokenA.Substring(0, $tokenA.Length - 1) + $(if ($tokenA.EndsWith('A')) { 'B' } else { 'A' })

    # ===== /usuarios/me =====
    Write-Host "`n== /usuarios/me =="
    $r = Invoke-Api GET '/usuarios/me' -Token $tokenA
    Check 'GET /me' $r 200 { param($x) $e = Campos-Exatos $x.Json $camposUsuario; if ($e) { return $e }; Sem-Segredos $x }
    $r = Invoke-Api GET '/usuarios/me'
    Check 'GET /me 401 sem token' $r 401 { param($x) Erro-Contrato $x 'NAO_AUTENTICADO' }
    $r = Invoke-Api GET '/usuarios/me' -Token $adulterado
    Check 'GET /me 401 token adulterado' $r 401 { param($x) Erro-Contrato $x 'NAO_AUTENTICADO' }
    $r = Invoke-Api PUT '/usuarios/me' @{ nome = 'Joao Renomeado'; email = $emailA } -Token $tokenA
    Check 'PUT /me renomeia' $r 200 { param($x) if ($x.Json.nome -ne 'Joao Renomeado') { return 'nome nao mudou' }; Sem-Segredos $x }
    $r = Invoke-Api PUT '/usuarios/me' @{ nome = 'X'; email = 'sem-arroba'; } -Token $tokenA
    Check 'PUT /me 400 e-mail invalido' $r 400 { param($x) Erro-Contrato $x 'VALIDACAO' }
    $r = Invoke-Api PUT '/usuarios/me' @{ nome = 'X'; email = $emailA; novaSenha = 'curta' } -Token $tokenA
    Check 'PUT /me 400 novaSenha curta' $r 400 { param($x) $e = Erro-Contrato $x 'VALIDACAO'; if ($e) { return $e }; if (-not ($x.Json.campos | Where-Object { $_.campo -eq 'novaSenha' })) { return 'campos sem "novaSenha"' } }
    $r = Invoke-Api PUT '/usuarios/me' @{ nome = 'X'; email = $emailA }
    Check 'PUT /me 401 sem token' $r 401 { param($x) Erro-Contrato $x 'NAO_AUTENTICADO' }
    $r = Invoke-Api PUT '/usuarios/me' @{ nome = 'Joao'; email = $emailB } -Token $tokenA
    Check 'PUT /me 409 e-mail de outro usuario' $r 409 { param($x) Erro-Contrato $x 'CONFLITO' }
    $r = Invoke-Api PUT '/usuarios/me' @{ nome = 'Joao Renomeado'; email = $emailA; novaSenha = $novaSenhaA } -Token $tokenA
    Check 'PUT /me troca a senha' $r 200
    $r = Invoke-Api POST '/auth/login' @{ email = $emailA; senha = $senhaA }
    Check 'login com senha antiga' $r 401
    $r = Invoke-Api POST '/auth/login' @{ email = $emailA; senha = $novaSenhaA }
    Check 'login com senha nova' $r 200
    $r = Invoke-Api DELETE '/usuarios/me'
    Check 'DELETE /me 401 sem token' $r 401 { param($x) Erro-Contrato $x 'NAO_AUTENTICADO' }

    # ===== Tipos de acao =====
    Write-Host "`n== /tipos-acao =="
    $r = Invoke-Api GET '/tipos-acao'
    Check 'GET /tipos-acao' $r 200 { param($x) if ($x.Raw.TrimStart()[0] -ne '[') { return 'nao e lista' }; $e = Sem-Segredos $x; if ($e) { return $e }; if (@($x.Json).Count -gt 0) { Campos-Exatos @($x.Json)[0] $camposTipo } }
    $tipoId = $null
    if ($r.Status -eq 200 -and @($r.Json).Count -gt 0) { $tipoId = @($r.Json)[0].id }
    if ($null -eq $tipoId) { throw 'Nao ha tipo de acao ativo no banco: impossivel testar acoes.' }
    $r = Invoke-Api GET "/tipos-acao/$tipoId"
    Check 'GET /tipos-acao/{id}' $r 200 { param($x) $e = Campos-Exatos $x.Json $camposTipo; if ($e) { return $e }; if (-not $x.Json.desafio) { return 'desafio vazio (DESCRICAO)' } }
    $r = Invoke-Api GET '/tipos-acao/999999999'
    Check 'GET /tipos-acao/{id} 404' $r 404 { param($x) Erro-Contrato $x 'NAO_ENCONTRADO' }
    $r = Invoke-Api GET '/tipos-acao/abc'
    Check 'GET /tipos-acao/abc 400' $r 400 { param($x) Erro-Contrato $x 'VALIDACAO' }

    # ===== Acoes =====
    Write-Host "`n== /acoes =="
    $r = Invoke-Api POST '/acoes' @{ idTipoAcao = $tipoId; quantidade = 1.5; evidencia = 'Smoke test automatizado' } -Token $tokenA
    Check 'POST /acoes' $r 201 {
        param($x)
        $e = Campos-Exatos $x.Json $camposAcao; if ($e) { return $e }
        $e = Campos-Exatos $x.Json.tipo 'id', 'nome', 'unidade'; if ($e) { return $e }
        $e = Sem-Segredos $x; if ($e) { return $e }
        if (-not $x.Location) { return 'sem header Location' }
        if ($x.Json.status -ne 'PENDENTE') { return "status $($x.Json.status), esperado PENDENTE" }
        if ($x.Raw -match '(?i)validacoes|votos|idUsuario') { return 'resposta expoe votos ou autor' }
    }
    $acaoId = $null
    if ($r.Status -eq 201) { $acaoId = $r.Json.id }
    if ($null -eq $acaoId) { throw 'Acao nao foi criada: impossivel continuar os casos de acao.' }
    $r = Invoke-Api POST '/acoes' @{ idTipoAcao = $tipoId; quantidade = 0; evidencia = 'x' } -Token $tokenA
    Check 'POST /acoes 400 quantidade zero' $r 400 { param($x) Erro-Contrato $x 'VALIDACAO' }
    $r = Invoke-Api POST '/acoes' @{ idTipoAcao = $tipoId; evidencia = 'x' } -Token $tokenA
    Check 'POST /acoes 400 sem quantidade' $r 400 { param($x) Erro-Contrato $x 'VALIDACAO' }
    $r = Invoke-Api POST '/acoes' @{ idTipoAcao = $tipoId; quantidade = 1; evidencia = 'x' }
    Check 'POST /acoes 401 sem token' $r 401 { param($x) Erro-Contrato $x 'NAO_AUTENTICADO' }
    $r = Invoke-Api POST '/acoes' @{ idTipoAcao = 999999999; quantidade = 1; evidencia = 'x' } -Token $tokenA
    Check 'POST /acoes 404 tipo inexistente' $r 404 { param($x) Erro-Contrato $x 'NAO_ENCONTRADO' }
    # usuarioId no corpo e ignorado: a acao e sempre do dono do token (B nao ganha a acao de A).
    $r = Invoke-Api GET '/acoes/minhas' -Token $tokenA
    Check 'GET /acoes/minhas' $r 200 { param($x) $l = @($x.Json); if (@($l | Where-Object { $_.id -eq $acaoId }).Count -ne 1) { return 'acao criada nao aparece' }; Campos-Exatos $l[0] $camposAcao }
    $r = Invoke-Api GET '/acoes/minhas' -Token $tokenB
    Check 'GET /acoes/minhas de B nao traz a de A' $r 200 { param($x) if (@($x.Json | Where-Object { $_.id -eq $acaoId }).Count -ne 0) { return 'B enxerga acao de A' } }
    $r = Invoke-Api GET '/acoes/minhas'
    Check 'GET /acoes/minhas 401' $r 401 { param($x) Erro-Contrato $x 'NAO_AUTENTICADO' }
    $r = Invoke-Api GET "/acoes/$acaoId" -Token $tokenA
    Check 'GET /acoes/{id}' $r 200 { param($x) $e = Campos-Exatos $x.Json $camposAcao; if ($e) { return $e }; Sem-Segredos $x }
    $r = Invoke-Api GET "/acoes/$acaoId"
    Check 'GET /acoes/{id} 401' $r 401 { param($x) Erro-Contrato $x 'NAO_AUTENTICADO' }
    $r = Invoke-Api GET '/acoes/999999999' -Token $tokenA
    Check 'GET /acoes/{id} 404 inexistente' $r 404 { param($x) Erro-Contrato $x 'NAO_ENCONTRADO' }
    $r = Invoke-Api GET "/acoes/$acaoId" -Token $tokenB
    Check 'GET /acoes/{id} 404 acao de outro usuario' $r 404 { param($x) Erro-Contrato $x 'NAO_ENCONTRADO' }
    $r = Invoke-Api DELETE "/acoes/$acaoId"
    Check 'DELETE /acoes/{id} 401' $r 401 { param($x) Erro-Contrato $x 'NAO_AUTENTICADO' }
    $r = Invoke-Api DELETE "/acoes/$acaoId" -Token $tokenB
    Check 'DELETE /acoes/{id} 404 acao de outro usuario' $r 404 { param($x) Erro-Contrato $x 'NAO_ENCONTRADO' }
    $r = Invoke-Api DELETE "/acoes/$acaoId" -Token $tokenA
    Check 'DELETE /acoes/{id} cancela' $r 204
    $r = Invoke-Api DELETE "/acoes/$acaoId" -Token $tokenA
    Check 'DELETE /acoes/{id} 409 ja cancelada' $r 409 { param($x) Erro-Contrato $x 'CONFLITO' }
    $r = Invoke-Api GET "/acoes/$acaoId" -Token $tokenA
    Check 'GET /acoes/{id} agora CANCELADA' $r 200 { param($x) if ($x.Json.status -ne 'CANCELADA') { return "status $($x.Json.status)" } }
}
finally {
    # ===== Limpeza: desativa as contas de teste, mesmo se algo acima falhou =====
    Write-Host "`n== Limpeza (DELETE /usuarios/me) =="
    foreach ($par in @(@{ N = 'A'; E = $emailA; Senhas = @($novaSenhaA, $senhaA) }, @{ N = 'B'; E = $emailB; Senhas = @($senhaB) })) {
        try {
            $l = $null; $senhaOk = $null
            foreach ($s in $par.Senhas) {
                $l = Invoke-Api POST '/auth/login' @{ email = $par.E; senha = $s }
                if ($l.Status -eq 200) { $senhaOk = $s; break }
            }
            if ($senhaOk) {
                $d = Invoke-Api DELETE '/usuarios/me' -Token $l.Json.token
                Check "desativa conta $($par.N)" $d 204
                $m = Invoke-Api GET '/usuarios/me' -Token $l.Json.token
                Check "conta $($par.N) inativa nao acessa /me (401)" $m 401 { param($x) Erro-Contrato $x 'NAO_AUTENTICADO' }
                $l2 = Invoke-Api POST '/auth/login' @{ email = $par.E; senha = $senhaOk }
                Check "login com senha certa em conta inativa $($par.N) (403)" $l2 403 { param($x) Erro-Contrato $x 'PROIBIDO' }
            } else {
                $script:Falhas++; $script:Total++
                Write-Host "[FALHA] nao foi possivel logar na conta $($par.N) para desativar ($($l.Status)); desative manualmente: $($par.E)"
            }
        } catch {
            $script:Falhas++; $script:Total++
            Write-Host "[FALHA] limpeza da conta $($par.N): $($_.Exception.Message) -- desative manualmente: $($par.E)"
        }
    }
}

Write-Host ''
if ($script:Falhas -eq 0) { Write-Host "TUDO OK: $($script:Total) verificacoes." ; exit 0 }
Write-Host "$($script:Falhas) de $($script:Total) verificacoes FALHARAM."
exit 1
