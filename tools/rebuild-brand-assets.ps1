param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

function Save-Png {
    param(
        [System.Drawing.Bitmap]$Bitmap,
        [string]$Path
    )

    $temporaryPath = "$Path.generated.png"
    if (Test-Path -LiteralPath $temporaryPath) {
        Remove-Item -LiteralPath $temporaryPath -Force
    }
    $Bitmap.Save($temporaryPath, [System.Drawing.Imaging.ImageFormat]::Png)
    Remove-Item -LiteralPath $Path -Force
    Move-Item -LiteralPath $temporaryPath -Destination $Path
}

function Resize-Logo {
    param(
        [string]$SourcePath,
        [string]$DestinationPath,
        [int]$Size
    )

    $loadedSource = [System.Drawing.Bitmap]::FromFile($SourcePath)
    try {
        $source = [System.Drawing.Bitmap]::new($loadedSource)
    } finally {
        $loadedSource.Dispose()
    }

    try {
        $output = New-Object System.Drawing.Bitmap(
            $Size,
            $Size,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
        )
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($output)
            try {
                $graphics.CompositingMode =
                    [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $graphics.CompositingQuality =
                    [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
                $graphics.InterpolationMode =
                    [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $graphics.PixelOffsetMode =
                    [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $graphics.SmoothingMode =
                    [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
                $graphics.DrawImage(
                    $source,
                    (New-Object System.Drawing.Rectangle(0, 0, $Size, $Size))
                )
            } finally {
                $graphics.Dispose()
            }
            Save-Png $output $DestinationPath
        } finally {
            $output.Dispose()
        }
    } finally {
        $source.Dispose()
    }
}

$storeIcon = Join-Path $ProjectRoot "play-store-assets\play-store-icon-512.png"
$launcherSizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

foreach ($entry in $launcherSizes.GetEnumerator()) {
    $folder = Join-Path $ProjectRoot "app\src\main\res\$($entry.Key)"
    Resize-Logo $storeIcon (Join-Path $folder "ic_launcher.png") $entry.Value
    Resize-Logo $storeIcon (Join-Path $folder "ic_launcher_round.png") $entry.Value
}

Write-Output "Android launcher densities rebuilt from the 512 px store icon."
