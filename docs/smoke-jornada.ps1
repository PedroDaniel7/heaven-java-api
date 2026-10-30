<#
.SYNOPSIS
  Jornada completa da API Heaven no Oracle real: cadastro, acao, sorteio, voto, pontos, reputacao, streak e ranking.

.DESCRIPTION
  Cria 4 usuarios de teste (e-mails unicos em example.test). A registra duas acoes; B, C e D sorteiam
  ate a oferta ser a acao de A (no maximo 40 vezes) e votam APROVAR na primeira e REJEITAR na segunda.
  So vota em acoes de A: nunca toca nas acoes da carga de BD. Confere status, pontos, reputacao e streak,
  testa os erros do contrato e desativa os 4 usuarios no fim. Nao imprime senha nem token.

  Exige que o servidor esteja com HEAVEN_SORTEIO_RESORTEAR=true (modo de teste que troca a oferta a cada
  sorteio). Sem ele a oferta fica fixa por 10 minutos e o script falha dizendo isso. NUNCA deixe o modo
  ligado em producao. Arquivo somente ASCII de proposito (PowerShell 5.1 le arquivo sem BOM como ANSI).

.PARAMETER BaseUrl
  Raiz do servidor, sem /api. Ex.: http://localhost:8080 ou https://meu-app.onrender.com

.EXAMPLE
  powershell -File docs/smoke-jornada.ps1 -BaseUrl http://localhost:8080
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

# ---------- jornada ----------

$MaxSorteios = 40

# Sorteia ate a oferta ser a acao $AcaoId (no maximo $MaxSorteios vezes). Nunca vota em outra acao.
function Obter-Oferta([string]$Quem, [string]$Token, [long]$AcaoId) {
    for ($i = 1; $i -le $MaxSorteios; $i++) {
        $r = Invoke-Api POST '/validacoes/sorteio' -Token $Token
        if ($r.Status -eq 200 -and $r.Json.acao.id -eq $AcaoId) {
            $script:Total++
            Write-Host ("[OK   ] sorteio de $Quem achou a acao de A na tentativa $i (de $MaxSorteios)")
            Write-Host ("        " + (Resumo $r.Raw))
            return $r
        }
        if ($r.Status -ne 200 -and $r.Status -ne 204) {
            $script:Total++; $script:Falhas++
            Write-Host ("[FALHA] sorteio de $Quem -> $($r.Status) (esperado 200 ou 204)")
            Write-Host ("        " + (Resumo $r.Raw))
            throw "Sorteio de $Quem falhou com status $($r.Status)."
        }
    }
    $script:Total++; $script:Falhas++
    Write-Host ("[FALHA] sorteio de $Quem nao entregou a acao de A em $MaxSorteios tentativas.")
    Write-Host ("        Causa provavel: o servidor esta com HEAVEN_SORTEIO_RESORTEAR desligado (padrao). Nesse modo a oferta")
    Write-Host ("        fica fixa por 10 minutos e so cai na acao de A se a primeira oferta for ela. Ligue o modo de teste")
    Write-Host ("        (HEAVEN_SORTEIO_RESORTEAR=true) no servidor, rode de novo e DESLIGUE depois: nunca em producao.")
    throw "Nao foi possivel obter a acao de A para $Quem; nenhum voto foi dado."
}

function Votar-Rodada([string]$Voto, [long]$AcaoId, $Usuarios) {
    $n = 0
    foreach ($u in $Usuarios) {
        $n++
        $o = Obter-Oferta $u.Nome $u.Token $AcaoId
        $esperaConcluida = ($n -eq $Usuarios.Count)
        $r = Invoke-Api POST '/validacoes' @{ idAcao = $AcaoId; voto = $Voto } -Token $u.Token
        $conc = $esperaConcluida
        Check "voto $Voto de $($u.Nome) (acaoConcluida=$conc)" $r 201 {
            param($x)
            $e = Campos-Exatos $x.Json 'idAcao', 'voto', 'acaoConcluida'; if ($e) { return $e }
            if ($x.Json.voto -ne $Voto) { return 'voto diferente do enviado' }
            if ([bool]$x.Json.acaoConcluida -ne $conc) { return "acaoConcluida $($x.Json.acaoConcluida), esperado $conc" }
        }
        if ($r.Status -ne 201) { throw "Voto de $($u.Nome) falhou; abortando a jornada." }
    }
}

function Me($token) { return (Invoke-Api GET '/usuarios/me' -Token $token) }

function Registrar-Acao($tokenA, $tipoId, $evidencia) {
    $r = Invoke-Api POST '/acoes' @{ idTipoAcao = $tipoId; quantidade = 2; evidencia = $evidencia } -Token $tokenA
    Check "A registra acao ($evidencia)" $r 201
    if ($r.Status -ne 201) { throw 'Nao foi possivel registrar a acao de A.' }
    return $r.Json
}

$carimbo = (Get-Date).ToString('yyyyMMddHHmmss') + '-' + (Get-Random -Maximum 99999)
$senha = 'Jor-' + [guid]::NewGuid().ToString('N')
$usuarios = @()
foreach ($nome in 'A', 'B', 'C', 'D') {
    $usuarios += [pscustomobject]@{ Nome = $nome; Email = "jornada.$carimbo.$($nome.ToLower())@example.test"; Token = $null }
}
$ua = $usuarios[0]; $validadores = @($usuarios[1], $usuarios[2], $usuarios[3])
$camposAcao = 'id', 'tipo', 'quantidade', 'evidencia', 'status', 'pontosPrevistos', 'dataHora', 'dataConclusao'

Write-Host "Jornada Heaven em $Raiz  (usuarios de teste: jornada.$carimbo.[a-d]@example.test)"

try {
    Write-Host "`n== a. Cadastro e login dos 4 usuarios =="
    foreach ($u in $usuarios) {
        $r = Invoke-Api POST '/usuarios' @{ nome = "Jornada $($u.Nome)"; email = $u.Email; senha = $senha }
        Check "cadastro $($u.Nome)" $r 201
        if ($r.Status -ne 201) { throw "Cadastro de $($u.Nome) falhou." }
    }
    foreach ($u in $usuarios) {
        $r = Invoke-Api POST '/auth/login' @{ email = $u.Email; senha = $senha }
        Check "login $($u.Nome)" $r 200
        if ($r.Status -ne 200) { throw "Login de $($u.Nome) falhou." }
        $u.Token = $r.Json.token
    }

    $r = Invoke-Api GET '/tipos-acao'
    Check 'GET /tipos-acao' $r 200
    $tipoId = @($r.Json)[0].id
    if ($null -eq $tipoId) { throw 'Sem tipo de acao ativo.' }

    $antesA = (Me $ua.Token).Json
    $repAntes = @{}
    foreach ($v in $validadores) { $repAntes[$v.Nome] = [decimal](Me $v.Token).Json.reputacao }
    Write-Host ("  antes: A pontos=$($antesA.pontos) streak=$($antesA.streak); reputacao B/C/D = " + (($validadores | ForEach-Object { $repAntes[$_.Nome] }) -join '/'))

    # ===== b/c/d: APROVAR =====
    Write-Host "`n== b-d. Acao 1 de A: tres APROVAR =="
    $acao1 = Registrar-Acao $ua.Token $tipoId 'Jornada acao 1 (aprovar)'
    Votar-Rodada 'APROVAR' $acao1.id $validadores
    $r = Invoke-Api GET "/acoes/$($acao1.id)" -Token $ua.Token
    Check 'A: acao 1 passou a APROVADA' $r 200 { param($x) $e = Campos-Exatos $x.Json $camposAcao; if ($e) { return $e }; if ($x.Json.status -ne 'APROVADA') { return "status $($x.Json.status)" }; if (-not $x.Json.dataConclusao) { return 'sem dataConclusao' } }
    $previstos = [long]$r.Json.pontosPrevistos
    $r = Me $ua.Token
    Check 'A: pontos aumentaram e streak = 1' $r 200 {
        param($x)
        if ([long]$x.Json.pontos -le [long]$antesA.pontos) { return "pontos $($x.Json.pontos), antes $($antesA.pontos)" }
        if ([long]$x.Json.streak -ne 1) { return "streak $($x.Json.streak), esperado 1" }
    }
    $pontosApos1 = [long]$r.Json.pontos
    Write-Host ("  A: pontos $($antesA.pontos) -> $pontosApos1 (pontosPrevistos da acao: $previstos); streak=$($r.Json.streak) maiorStreak=$($r.Json.maiorStreak)")
    foreach ($v in $validadores) {
        $m = Me $v.Token
        Check "reputacao de $($v.Nome) subiu +1 (convergiu com APROVADA)" $m 200 { param($x) $esp = $repAntes[$v.Nome] + 1; if ([decimal]$x.Json.reputacao -ne $esp) { return "reputacao $($x.Json.reputacao), esperado $esp" } }
    }
    $repApos1 = @{}
    foreach ($v in $validadores) { $repApos1[$v.Nome] = [decimal](Me $v.Token).Json.reputacao }

    # ===== e: REJEITAR =====
    Write-Host "`n== e. Acao 2 de A: tres REJEITAR =="
    $acao2 = Registrar-Acao $ua.Token $tipoId 'Jornada acao 2 (rejeitar)'
    Votar-Rodada 'REJEITAR' $acao2.id $validadores
    $r = Invoke-Api GET "/acoes/$($acao2.id)" -Token $ua.Token
    Check 'A: acao 2 passou a REJEITADA' $r 200 { param($x) if ($x.Json.status -ne 'REJEITADA') { return "status $($x.Json.status)" } }
    $r = Me $ua.Token
    Check 'A: pontos nao mudaram com a rejeicao; streak segue 1' $r 200 { param($x) if ([long]$x.Json.pontos -ne $pontosApos1) { return "pontos $($x.Json.pontos), esperado $pontosApos1" }; if ([long]$x.Json.streak -ne 1) { return "streak $($x.Json.streak)" } }
    foreach ($v in $validadores) {
        $m = Me $v.Token
        Check "reputacao de $($v.Nome) subiu +1 (alinhada com REJEITADA)" $m 200 { param($x) $esp = $repApos1[$v.Nome] + 1; if ([decimal]$x.Json.reputacao -ne $esp) { return "reputacao $($x.Json.reputacao), esperado $esp" } }
    }

    # ===== f: erros =====
    Write-Host "`n== f. Erros do contrato =="
    $r = Invoke-Api POST '/validacoes' @{ idAcao = $acao1.id; voto = 'APROVAR' } -Token $ua.Token
    Check 'A vota na propria acao (403)' $r 403 { param($x) Erro-Contrato $x 'PROIBIDO' }
    $r = Invoke-Api POST '/validacoes' @{ idAcao = $acao1.id; voto = 'APROVAR' } -Token $validadores[0].Token
    Check 'voto repetido de B na acao 1 (409)' $r 409 { param($x) if ($x.Json.codigo -ne 'OFERTA_EXPIRADA' -and $x.Json.codigo -ne 'CONFLITO') { return "codigo $($x.Json.codigo)" } }
    $r = Invoke-Api POST '/validacoes' @{ idAcao = $acao1.id; voto = 'APROVAR' } -Token $validadores[2].Token
    Check 'voto sem sorteio previo (409 OFERTA_EXPIRADA)' $r 409 { param($x) Erro-Contrato $x 'OFERTA_EXPIRADA' }
    $r = Invoke-Api POST '/validacoes' @{ idAcao = $acao1.id; voto = 'TALVEZ' } -Token $validadores[0].Token
    Check 'voto invalido (400)' $r 400 { param($x) Erro-Contrato $x 'VALIDACAO' }
    $r = Invoke-Api POST '/validacoes' @{ idAcao = $acao1.id; voto = 'APROVAR' }
    Check 'voto sem token (401)' $r 401 { param($x) Erro-Contrato $x 'NAO_AUTENTICADO' }
    $r = Invoke-Api POST '/validacoes/sorteio'
    Check 'sorteio sem token (401)' $r 401 { param($x) Erro-Contrato $x 'NAO_AUTENTICADO' }
    $r = Invoke-Api GET '/ranking?limite=0'
    Check 'ranking limite=0 (400)' $r 400 { param($x) Erro-Contrato $x 'VALIDACAO' }
    $r = Invoke-Api GET '/ranking?limite=101'
    Check 'ranking limite=101 (400)' $r 400 { param($x) Erro-Contrato $x 'VALIDACAO' }
    $r = Invoke-Api GET '/ranking?limite=abc'
    Check 'ranking limite=abc (400)' $r 400 { param($x) Erro-Contrato $x 'VALIDACAO' }
    $r = Invoke-Api GET '/ranking'
    Check 'GET /ranking publico (posicao 1 com o maior numero de pontos)' $r 200 {
        param($x)
        $l = @($x.Json)
        if ($l.Count -lt 1) { return 'ranking vazio' }
        $e = Campos-Exatos $l[0] 'posicao', 'nome', 'pontos', 'nivel', 'streak'; if ($e) { return $e }
        if ($l[0].posicao -ne 1) { return "primeiro item com posicao $($l[0].posicao)" }
        $max = ($l | Measure-Object -Property pontos -Maximum).Maximum
        if ([long]$l[0].pontos -ne [long]$max) { return "posicao 1 tem $($l[0].pontos) pontos, maximo da lista e $max" }
        if ($x.Raw -match '(?i)email|senha|idUsuario') { return 'ranking expoe dado pessoal' }
    }
    $r = Invoke-Api GET '/ranking?limite=3'
    Check 'GET /ranking?limite=3 (no maximo 3 itens)' $r 200 { param($x) if (@($x.Json).Count -gt 3) { return "$(@($x.Json).Count) itens" } }
}
catch {
    $script:Falhas++
    Write-Host "ABORTADO: $($_.Exception.Message)"
}
finally {
    Write-Host "`n== g. Limpeza (DELETE /usuarios/me) =="
    foreach ($u in $usuarios) {
        if (-not $u.Token) { continue }
        try {
            if ($u.Nome -eq 'A') {
                # Cancela as acoes PENDENTES de A antes de desativar a conta (tambem quando a jornada abortou no meio).
                $m = Invoke-Api GET '/acoes/minhas' -Token $u.Token
                Check 'A: lista as proprias acoes antes da limpeza' $m 200
                foreach ($p in @($m.Json | Where-Object { $_.status -eq 'PENDENTE' })) {
                    $c = Invoke-Api DELETE "/acoes/$($p.id)" -Token $u.Token
                    Check "A: cancela a acao pendente $($p.id)" $c 204
                }
            }
            $d = Invoke-Api DELETE '/usuarios/me' -Token $u.Token
            Check "desativa $($u.Nome)" $d 204
            $m = Invoke-Api GET '/usuarios/me' -Token $u.Token
            Check "$($u.Nome) inativo: /me devolve 401" $m 401 { param($x) Erro-Contrato $x 'NAO_AUTENTICADO' }
        } catch {
            $script:Falhas++; $script:Total++
            Write-Host "[FALHA] limpeza de $($u.Nome): $($_.Exception.Message) -- desative manualmente: $($u.Email)"
        }
    }
    $semToken = @($usuarios | Where-Object { -not $_.Token })
    if ($semToken.Count -gt 0) { Write-Host ("Sem login (verifique/desative se foram criados): " + (($semToken | ForEach-Object { $_.Email }) -join ', ')) }
}

Write-Host ''
if ($script:Falhas -eq 0) { Write-Host "TUDO OK: $($script:Total) verificacoes."; exit 0 }
Write-Host "$($script:Falhas) de $($script:Total) verificacoes FALHARAM."
exit 1
