// Spotify w grze: launcher czyta, co gra w Windowsie (sesja multimediów – Spotify, przeglądarka, inne),
// i udostępnia to modowi Void Client przez lokalny serwer http://127.0.0.1:47823 (tylko ten komputer).
// Bez logowania do Spotify i bez Premium – to te same dane, które Windows pokazuje przy klawiszach głośności.
const http = require('http');
const { spawn } = require('child_process');

const PORT = 47823;

const PS = String.raw`
$ErrorActionPreference = 'SilentlyContinue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation` + '`' + String.raw`1' })[0]
function Await($op, $type) { $t = $asTask.MakeGenericMethod($type).Invoke($null, @($op)); $t.Wait(3000) | Out-Null; $t.Result }
[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime] | Out-Null
[Windows.Storage.Streams.IRandomAccessStreamWithContentType,Windows.Storage.Streams,ContentType=WindowsRuntime] | Out-Null
$mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
if ($mgr -eq $null) { Write-Output '{"error":"no-manager"}'; exit }
$stdin = [Console]::OpenStandardInput()
$inBuf = New-Object byte[] 256
$read = $stdin.ReadAsync($inBuf, 0, 256)
$lastKey = ''
while ($true) {
  $s = $null
  foreach ($x in $mgr.GetSessions()) { if ($x.SourceAppUserModelId -like '*Spotify*') { $s = $x } }
  if ($s -eq $null) { $s = $mgr.GetCurrentSession() }
  if ($read.IsCompleted) {
    $n = $read.Result
    if ($n -le 0) { exit }
    foreach ($cmd in ([Text.Encoding]::ASCII.GetString($inBuf, 0, $n) -split "\s+")) {
      if ($s -eq $null) { break }
      if ($cmd -eq 'next') { Await ($s.TrySkipNextAsync()) ([bool]) | Out-Null }
      elseif ($cmd -eq 'prev') { Await ($s.TrySkipPreviousAsync()) ([bool]) | Out-Null }
      elseif ($cmd -eq 'toggle') { Await ($s.TryTogglePlayPauseAsync()) ([bool]) | Out-Null }
    }
    $read = $stdin.ReadAsync($inBuf, 0, 256)
  }
  if ($s -eq $null) { Write-Output '{"none":true}'; Start-Sleep -Milliseconds 1000; continue }
  $p = Await ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
  $tl = $s.GetTimelineProperties()
  $pb = $s.GetPlaybackInfo()
  $key = "$($p.Title)|$($p.Artist)"
  $o = [ordered]@{ title = $p.Title; artist = $p.Artist; album = $p.AlbumTitle; app = $s.SourceAppUserModelId;
    pos = [math]::Round($tl.Position.TotalSeconds, 1); dur = [math]::Round($tl.EndTime.TotalSeconds, 1);
    playing = ("$($pb.PlaybackStatus)" -eq 'Playing') }
  Write-Output ($o | ConvertTo-Json -Compress)
  if ($key -ne $lastKey -and $p.Thumbnail -ne $null) {
    $lastKey = $key
    $st = Await ($p.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
    if ($st -ne $null) {
      $net = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($st)
      $ms = New-Object System.IO.MemoryStream
      $net.CopyTo($ms)
      Write-Output ('{"cover":"' + [Convert]::ToBase64String($ms.ToArray()) + '"}')
    }
  } elseif ($key -ne $lastKey) { $lastKey = $key; Write-Output '{"cover":""}' }
  Start-Sleep -Milliseconds 700
}
`;

let proc = null;
let server = null;
let state = { none: true };
let coverId = 0;
let coverHex = '';
let stamp = 0;
let buf = '';

function coverToHex(b64) {
  if (!b64) return '';
  try {
    const { nativeImage } = require('electron');
    const img = nativeImage.createFromBuffer(Buffer.from(b64, 'base64'));
    if (img.isEmpty()) return '';
    const bmp = img.resize({ width: 32, height: 32, quality: 'good' }).toBitmap(); // BGRA
    let out = '';
    for (let i = 0; i < 32 * 32; i++) {
      const b = bmp[i * 4], g = bmp[i * 4 + 1], r = bmp[i * 4 + 2];
      out += ((r << 16) | (g << 8) | b).toString(16).padStart(6, '0');
    }
    return out;
  } catch {
    return '';
  }
}

function onLine(line) {
  let d;
  try { d = JSON.parse(line); } catch { return; }
  if ('cover' in d) {
    coverHex = coverToHex(d.cover);
    coverId++;
    return;
  }
  state = d;
  stamp = Date.now();
}

function startReader() {
  if (proc || process.platform !== 'win32') return;
  const enc = Buffer.from(PS, 'utf16le').toString('base64');
  proc = spawn('powershell.exe', ['-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-EncodedCommand', enc], { windowsHide: true });
  proc.stdout.setEncoding('utf8');
  proc.stdout.on('data', chunk => {
    buf += chunk;
    let i;
    while ((i = buf.indexOf('\n')) >= 0) {
      const line = buf.slice(0, i).trim();
      buf = buf.slice(i + 1);
      if (line) onLine(line);
    }
  });
  proc.on('exit', () => { proc = null; state = { none: true }; });
  proc.on('error', () => { proc = null; });
}

function command(c) {
  if (!proc || !['next', 'prev', 'toggle'].includes(c)) return false;
  try { proc.stdin.write(c + '\n'); return true; } catch { return false; }
}

function start() {
  startReader();
  if (server) return;
  server = http.createServer((req, res) => {
    const url = req.url || '';
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    if (url.startsWith('/now')) {
      const fresh = Date.now() - stamp < 5000;
      res.end(JSON.stringify(fresh && !state.none ? { ...state, coverId } : { none: true }));
    } else if (url.startsWith('/cover')) {
      res.end(JSON.stringify({ id: coverId, rgb: coverHex }));
    } else if (url.startsWith('/cmd/') && req.method === 'POST') {
      res.end(JSON.stringify({ ok: command(url.slice(5)) }));
    } else {
      res.statusCode = 404;
      res.end('{}');
    }
  });
  server.on('error', () => { server = null; });
  server.listen(PORT, '127.0.0.1');
}

function stop() {
  if (proc) { try { proc.stdin.end(); proc.kill(); } catch { /* ignore */ } proc = null; }
  if (server) { try { server.close(); } catch { /* ignore */ } server = null; }
  state = { none: true };
}

module.exports = { start, stop, PORT };
