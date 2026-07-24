param(
    [Parameter(Mandatory = $true)]
    [string]$Source,

    [string]$OutputDirectory = "app/src/main/res/drawable-nodpi"
)

Add-Type -AssemblyName System.Drawing

$sourcePath = (Resolve-Path -LiteralPath $Source).Path
$outputPath = Join-Path (Get-Location) $OutputDirectory
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null

$sourceImage = [System.Drawing.Bitmap]::new($sourcePath)

function Export-CircularSprite {
    param(
        [string]$Name,
        [int]$CenterX,
        [int]$CenterY,
        [int]$Diameter
    )

    $sprite = [System.Drawing.Bitmap]::new(
        $Diameter,
        $Diameter,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    $graphics = [System.Drawing.Graphics]::FromImage($sprite)
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $path.AddEllipse(1, 1, $Diameter - 2, $Diameter - 2)
    $graphics.SetClip($path)
    $sourceRectangle = [System.Drawing.Rectangle]::new(
        $CenterX - [int]($Diameter / 2),
        $CenterY - [int]($Diameter / 2),
        $Diameter,
        $Diameter
    )
    $destinationRectangle = [System.Drawing.Rectangle]::new(0, 0, $Diameter, $Diameter)
    $graphics.DrawImage($sourceImage, $destinationRectangle, $sourceRectangle, [System.Drawing.GraphicsUnit]::Pixel)

    $target = Join-Path $outputPath $Name
    $sprite.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)

    $path.Dispose()
    $graphics.Dispose()
    $sprite.Dispose()
}

$backgroundImage = $sourceImage.Clone()
$backgroundTarget = Join-Path $outputPath "mainframe_background.png"
$backgroundImage.Save($backgroundTarget, [System.Drawing.Imaging.ImageFormat]::Png)
$backgroundImage.Dispose()

# The original timer artwork used minute labels. The Android timer runs from
# Off to 12 hours, so OpenCV removes only the glyph pixels and preserves the
# nearby dial marks and panel texture.
$inpaintScript = Join-Path $PSScriptRoot "inpaint-mainframe-timer.py"
python $inpaintScript $backgroundTarget
if ($LASTEXITCODE -ne 0) {
    throw "Timer label cleanup failed. Install opencv-python-headless for Python."
}

# Coordinates are measured against the 1024 x 1535 production artwork.
Export-CircularSprite "mainframe_knob_volume.png" 520 718 224
Export-CircularSprite "mainframe_knob_bass.png" 318 979 82
Export-CircularSprite "mainframe_knob_low_mids.png" 463 979 82
Export-CircularSprite "mainframe_knob_timer.png" 679 984 122

$sourceImage.Dispose()

Write-Host "Mainframe assets exported to $outputPath"
