<#
.SYNOPSIS
    Xóa thư mục Backend/uploads khỏi TOÀN BỘ lịch sử git (Plan P1-08, quyết định D-04 = B).

.DESCRIPTION
    Script này AN TOÀN: nó chỉ làm việc trên một bản clone tạm lấy từ GitHub và KHÔNG BAO GIỜ tự push.
    Các bước:
      1. Kiểm tra điều kiện (working tree sạch, mọi nhánh local đã push).
      2. Backup toàn bộ repo (git clone --mirror).
      3. Bảo đảm có git-filter-repo (chỉ tự cài bằng pip khi có -InstallFilterRepo).
      4. Clone sạch từ GitHub, tạo nhánh local cho mọi nhánh remote, chạy filter-repo.
      5. Kiểm chứng: không còn dấu vết trong lịch sử; cây mã nguồn từng nhánh giống hệt bản gốc (trừ thư mục bị xóa).
      6. In ra các lệnh push --force để BẠN tự chạy (sau khi đọc kết quả kiểm chứng).

    Khi nào nên chạy: SAU KHI mọi commit đã được push và merge (mọi mã băm commit sẽ đổi).
    Sau khi force push: XÓA bản clone cũ trên máy làm việc và clone lại — không `git pull` vào bản cũ.

.PARAMETER RepoUrl
    URL repo trên GitHub. Mặc định lấy từ `git remote get-url origin`.

.PARAMETER WorkRoot
    Thư mục làm việc tạm (backup + bản clone sạch).

.PARAMETER RemovePath
    Đường dẫn cần xóa khỏi lịch sử. Mặc định Backend/uploads.

.PARAMETER InstallFilterRepo
    Cho phép script tự chạy `python -m pip install --user git-filter-repo` nếu chưa có.

.EXAMPLE
    .\scripts\git-remove-uploads-history.ps1 -InstallFilterRepo
#>
param(
    [string]$RepoUrl = "",
    [string]$WorkRoot = (Join-Path $env:USERPROFILE "taskify-history-rewrite"),
    [string]$RemovePath = "Backend/uploads",
    [switch]$InstallFilterRepo
)

$ErrorActionPreference = "Continue"   # lỗi được xác định qua $LASTEXITCODE (git in tiến trình ra stderr)

function Step($text) { Write-Host ""; Write-Host "=== $text ===" -ForegroundColor Cyan }
function Fail($text) { Write-Host "✗ $text" -ForegroundColor Red; exit 1 }
function Ok($text)   { Write-Host "✓ $text" -ForegroundColor Green }

function Invoke-Git {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$GitArgs)
    & git @GitArgs
    if ($LASTEXITCODE -ne 0) { Fail "git $($GitArgs -join ' ') thất bại (exit $LASTEXITCODE)" }
}

function Git-Out {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$GitArgs)
    $out = & git @GitArgs
    if ($LASTEXITCODE -ne 0) { Fail "git $($GitArgs -join ' ') thất bại (exit $LASTEXITCODE)" }
    return $out
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
if (-not $RepoUrl) { $RepoUrl = (Git-Out -C $repoRoot remote get-url origin | Select-Object -First 1).Trim() }
if (-not $RepoUrl) { Fail "Không xác định được URL repo. Truyền -RepoUrl." }

# ---------------------------------------------------------------------------------------------
Step "1/6 Kiểm tra điều kiện"
# ---------------------------------------------------------------------------------------------
$dirty = Git-Out -C $repoRoot status --porcelain
if ($dirty) { Fail "Working tree còn thay đổi chưa commit. Hãy commit/stash trước.`n$($dirty -join "`n")" }
Ok "Working tree sạch"

$unpushed = @()
foreach ($line in (Git-Out -C $repoRoot for-each-ref --format='%(refname:short)|%(upstream:short)' refs/heads)) {
    $name, $up = $line -split '\|'
    if (-not $up) { continue }
    $ahead = [int](Git-Out -C $repoRoot rev-list --count "$up..$name" | Select-Object -First 1)
    if ($ahead -gt 0) { $unpushed += "$name đang có $ahead commit chưa push lên $up" }
}
if ($unpushed.Count -gt 0) {
    Fail ("Còn commit chưa push — viết lại lịch sử bây giờ sẽ làm lệch các commit này.`n  " + ($unpushed -join "`n  "))
}
Ok "Mọi nhánh local có upstream đều đã push"

if ((Test-Path (Join-Path $repoRoot ".git\MERGE_HEAD")) -or (Test-Path (Join-Path $repoRoot ".git\rebase-merge"))) {
    Fail "Repo đang ở giữa merge/rebase."
}

$remoteTracked = Git-Out -C $repoRoot ls-tree -r --name-only origin/main -- $RemovePath
if ($remoteTracked) {
    Write-Host "ℹ origin/main hiện vẫn theo dõi $(@($remoteTracked).Count) file dưới $RemovePath (bình thường nếu commit gỡ file chưa được merge)." -ForegroundColor Yellow
}

# ---------------------------------------------------------------------------------------------
Step "2/6 Backup mirror"
# ---------------------------------------------------------------------------------------------
New-Item -ItemType Directory -Force $WorkRoot | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backup = Join-Path $WorkRoot "backup-$stamp.git"
Invoke-Git clone --mirror $RepoUrl $backup
Ok "Backup: $backup   (GIỮ NGUYÊN, không đẩy đi đâu; xóa sau ~30 ngày khi mọi thứ ổn định)"

# ---------------------------------------------------------------------------------------------
Step "3/6 git-filter-repo"
# ---------------------------------------------------------------------------------------------
& git filter-repo --version *> $null
if ($LASTEXITCODE -ne 0) {
    if (-not $InstallFilterRepo) {
        Write-Host "Chưa có git-filter-repo. Chạy lại với -InstallFilterRepo (script sẽ chạy: python -m pip install --user git-filter-repo)," -ForegroundColor Yellow
        Write-Host "hoặc tự cài: python -m pip install --user git-filter-repo" -ForegroundColor Yellow
        exit 2
    }
    & python -m pip install --user git-filter-repo
    if ($LASTEXITCODE -ne 0) { Fail "pip install git-filter-repo thất bại" }
    $scripts = (& python -c "import sysconfig; print(sysconfig.get_path('scripts', 'nt_user'))").Trim()
    if ($scripts -and (Test-Path $scripts)) { $env:Path = "$scripts;$env:Path" }
    & git filter-repo --version *> $null
    if ($LASTEXITCODE -ne 0) { Fail "Đã cài nhưng git vẫn không thấy filter-repo. Thêm thư mục Scripts của Python (user) vào PATH rồi chạy lại." }
}
Ok "git-filter-repo sẵn sàng"

# ---------------------------------------------------------------------------------------------
Step "4/6 Clone sạch từ GitHub và viết lại lịch sử"
# ---------------------------------------------------------------------------------------------
$clean = Join-Path $WorkRoot "clean"
if (Test-Path $clean) {
    if (-not $clean.StartsWith($WorkRoot, [System.StringComparison]::OrdinalIgnoreCase)) { Fail "Đường dẫn clean nằm ngoài WorkRoot — dừng." }
    Remove-Item -Recurse -Force $clean
}
Invoke-Git clone $RepoUrl $clean

$branches = @()
foreach ($ref in (Git-Out -C $clean for-each-ref --format='%(refname:short)' refs/remotes/origin)) {
    $b = $ref -replace '^origin/', ''
    if ($b -eq 'HEAD' -or $b -eq 'origin') { continue }
    $branches += $b
}
foreach ($b in $branches) {
    & git -C $clean rev-parse --verify --quiet "refs/heads/$b" *> $null
    if ($LASTEXITCODE -ne 0) { Invoke-Git -C $clean branch --track $b "origin/$b" }
}
Ok "Các nhánh sẽ được xử lý: $($branches -join ', ')"

# Ảnh chụp cây trước khi viết lại (loại thư mục bị xóa) để so sánh sau
$before = @{}
foreach ($b in $branches) {
    $before[$b] = @(Git-Out -C $clean ls-tree -r "origin/$b" | Where-Object { $_ -notmatch "`t$([regex]::Escape($RemovePath))/" })
}
$commitsBefore = [int](Git-Out -C $clean rev-list --all --count | Select-Object -First 1)

Invoke-Git -C $clean filter-repo --path $RemovePath --invert-paths
Ok "filter-repo hoàn tất"

# ---------------------------------------------------------------------------------------------
Step "5/6 Kiểm chứng"
# ---------------------------------------------------------------------------------------------
$problems = @()

$leftLog = Git-Out -C $clean log --all --oneline -- $RemovePath
if ($leftLog) { $problems += "Lịch sử vẫn còn commit chạm tới $RemovePath" } else { Ok "git log --all -- ${RemovePath}: rỗng" }

$leftObjects = Git-Out -C $clean rev-list --all --objects | Where-Object { $_ -match "\s$([regex]::Escape($RemovePath))(/|$)" }
if ($leftObjects) { $problems += "Vẫn còn object dưới $RemovePath trong lịch sử" } else { Ok "Không còn object nào dưới $RemovePath" }

foreach ($b in $branches) {
    $after = @(Git-Out -C $clean ls-tree -r $b)
    $diff = Compare-Object -ReferenceObject $before[$b] -DifferenceObject $after
    if ($diff) { $problems += "Cây mã nguồn nhánh '$b' KHÁC bản gốc ($(@($diff).Count) dòng khác)" }
    else { Ok "Nhánh '$b': cây mã nguồn giống hệt bản gốc (trừ $RemovePath)" }
}

$commitsAfter = [int](Git-Out -C $clean rev-list --all --count | Select-Object -First 1)
Write-Host "Số commit: $commitsBefore → $commitsAfter" -ForegroundColor Gray

if ($problems.Count -gt 0) {
    Write-Host ""
    foreach ($p in $problems) { Write-Host "✗ $p" -ForegroundColor Red }
    Fail "Kiểm chứng KHÔNG đạt — ĐỪNG push. Bản gốc an toàn (chưa đụng remote); backup ở $backup"
}

# ---------------------------------------------------------------------------------------------
Step "6/6 Kết quả — script KHÔNG tự push"
# ---------------------------------------------------------------------------------------------
Write-Host "Bản clone đã viết lại lịch sử: $clean" -ForegroundColor Green
Write-Host ""
Write-Host "Để áp dụng lên GitHub, BẠN tự chạy (thao tác KHÔNG hoàn tác được trên remote):" -ForegroundColor Yellow
Write-Host "  0. Nếu nhánh main đang được bảo vệ: tạm tắt ruleset/branch protection (hoặc dùng quyền bypass), xong bật lại."
Write-Host "  cd `"$clean`""
Write-Host "  git remote add origin $RepoUrl"
Write-Host "  git push --force --all origin"
Write-Host "  git push --force --tags origin"
Write-Host ""
Write-Host "Sau đó, trên máy làm việc (QUAN TRỌNG):" -ForegroundColor Yellow
Write-Host "  - Đổi tên/xóa thư mục repo cũ và CLONE LẠI. Đừng git pull vào bản cũ (sẽ kéo lịch sử cũ trở lại)."
Write-Host "  - Mọi cộng tác viên/fork cũng phải clone lại. PR đang mở có thể cần tạo lại."
Write-Host "  - Nếu đã từng có PR/fork chứa file: nhờ GitHub Support xóa cache các object cũ (tùy chọn)."
Write-Host "  - Backup mirror ($backup): giữ riêng tư, không đẩy đi đâu."
Write-Host ""
Write-Host "Kiểm tra sau cùng trên bản clone mới từ GitHub:  git log --all -- $RemovePath   (phải rỗng)" -ForegroundColor Gray
