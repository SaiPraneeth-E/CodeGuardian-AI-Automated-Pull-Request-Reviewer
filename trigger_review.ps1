param (
    [Parameter(Mandatory=$true)]
    [string]$PrUrl,

    [string]$GithubToken = $env:GITHUB_TOKEN,
    [string]$WebhookUrl = "http://localhost:8080/github/webhook"
)

# Parse owner, repo, and PR number
if ($PrUrl -match "https://github.com/([^/]+)/([^/]+)/pull/(\d+)") {
    $owner = $Matches[1]
    $repo = $Matches[2]
    $prNumber = [int]$Matches[3]
} else {
    Write-Error "Invalid GitHub PR URL. Format must be: https://github.com/owner/repo/pull/num"
    exit 1
}

Write-Host "Fetching PR details for $owner/$repo PR #$prNumber..."

$headers = @{}
if ($GithubToken) {
    $headers["Authorization"] = "Bearer $GithubToken"
}
$headers["Accept"] = "application/vnd.github+json"

try {
    $pr = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/pulls/$prNumber" -Headers $headers -Method Get
} catch {
    Write-Error "Failed to fetch PR details from GitHub. Ensure your GITHUB_TOKEN is correct and the PR exists."
    exit 1
}

$headSha = $pr.head.sha
$baseSha = $pr.base.sha
$title = $pr.title
$state = $pr.state

Write-Host "PR Details Found:"
Write-Host "  Title   : $title"
Write-Host "  Head SHA: $headSha"
Write-Host "  Base SHA: $baseSha"

$payload = @{
  action = "opened"
  number = $prNumber
  pull_request = @{
    id = $pr.id
    number = $prNumber
    title = $title
    state = $state
    head = @{
      sha = $headSha
    }
    base = @{
      sha = $baseSha
    }
  }
  repository = @{
    name = $repo
    owner = @{
      login = $owner
    }
  }
} | ConvertTo-Json -Depth 5

Write-Host "Sending webhook payload to $WebhookUrl..."
try {
    $response = Invoke-RestMethod -Uri $WebhookUrl -Method Post -Body $payload -ContentType "application/json"
    Write-Host "Response: $response"
} catch {
    Write-Error "Failed to send webhook to CodeGuardian backend. Ensure the application is running."
}
