param(
    [Parameter(Mandatory = $true)]
    [string]$Panel,

    [Parameter(Mandatory = $true)]
    [string]$SmallButton,

    [Parameter(Mandatory = $true)]
    [string]$WideButton,

    [Parameter(Mandatory = $true)]
    [string]$Indicator,

    [string]$OutputDirectory = "app/src/main/res/drawable-nodpi"
)

$outputPath = Join-Path (Get-Location) $OutputDirectory
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null

Add-Type -ReferencedAssemblies "System.Drawing.dll" -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;

public static class SettingsAssetProcessor {
    public static void CropAlpha(string input, string output, int maxWidth, byte threshold) {
        using (var source = new Bitmap(input))
        using (var cropped = Crop(source, threshold))
        using (var resized = ResizeToWidth(cropped, maxWidth)) {
            resized.Save(output, ImageFormat.Png);
        }
    }

    public static void ExtractCheckerSprite(string input, string output, int maxWidth) {
        using (var source = new Bitmap(input))
        {
            var bounds = FindColoredBounds(source);
            using (var isolated = new Bitmap(bounds.Width, bounds.Height, PixelFormat.Format32bppArgb)) {
            for (var y = 0; y < bounds.Height; y++) {
                for (var x = 0; x < bounds.Width; x++) {
                    var sourceX = bounds.Left + x;
                    var sourceY = bounds.Top + y;
                    var pixel = source.GetPixel(sourceX, sourceY);
                    var maximum = Math.Max(pixel.R, Math.Max(pixel.G, pixel.B));
                    var minimum = Math.Min(pixel.R, Math.Min(pixel.G, pixel.B));
                    var saturation = maximum - minimum;
                    var luminance = (pixel.R * 54 + pixel.G * 183 + pixel.B * 19) / 256;
                    var colorAlpha = Clamp((saturation - 7) * 12);
                    var shadowAlpha = Clamp((120 - luminance) * 5);
                    isolated.SetPixel(x, y, Color.FromArgb(Math.Max(colorAlpha, shadowAlpha), pixel.R, pixel.G, pixel.B));
                }
            }

            using (var cropped = Crop(isolated, 8))
            using (var resized = ResizeToWidth(cropped, maxWidth)) {
                resized.Save(output, ImageFormat.Png);
            }
            }
        }
    }

    public static void ExtractWarmIndicator(string input, string output, int maxWidth) {
        using (var source = new Bitmap(input))
        using (var isolated = new Bitmap(source.Width, source.Height, PixelFormat.Format32bppArgb)) {
            for (var y = 0; y < source.Height; y++) {
                for (var x = 0; x < source.Width; x++) {
                    var pixel = source.GetPixel(x, y);
                    var warmth = pixel.R - pixel.B;
                    var warmthAlpha = Clamp((warmth - 2) * 6);
                    var alpha = pixel.A * warmthAlpha / 255;
                    isolated.SetPixel(x, y, Color.FromArgb(alpha, pixel.R, pixel.G, pixel.B));
                }
            }

            using (var cropped = Crop(isolated, 5))
            using (var resized = ResizeToWidth(cropped, maxWidth)) {
                resized.Save(output, ImageFormat.Png);
            }
        }
    }

    private static Bitmap Crop(Bitmap source, byte threshold) {
        var left = source.Width;
        var top = source.Height;
        var right = -1;
        var bottom = -1;

        for (var y = 0; y < source.Height; y++) {
            for (var x = 0; x < source.Width; x++) {
                if (source.GetPixel(x, y).A <= threshold) continue;
                left = Math.Min(left, x);
                top = Math.Min(top, y);
                right = Math.Max(right, x);
                bottom = Math.Max(bottom, y);
            }
        }

        if (right < left || bottom < top) throw new InvalidOperationException("The source has no visible pixels.");
        var padding = Math.Max(4, Math.Min(source.Width, source.Height) / 100);
        left = Math.Max(0, left - padding);
        top = Math.Max(0, top - padding);
        right = Math.Min(source.Width - 1, right + padding);
        bottom = Math.Min(source.Height - 1, bottom + padding);
        return source.Clone(
            new Rectangle(left, top, right - left + 1, bottom - top + 1),
            PixelFormat.Format32bppArgb
        );
    }

    private static Rectangle FindColoredBounds(Bitmap source) {
        var left = source.Width;
        var top = source.Height;
        var right = -1;
        var bottom = -1;

        for (var y = 0; y < source.Height; y++) {
            for (var x = 0; x < source.Width; x++) {
                var pixel = source.GetPixel(x, y);
                var maximum = Math.Max(pixel.R, Math.Max(pixel.G, pixel.B));
                var minimum = Math.Min(pixel.R, Math.Min(pixel.G, pixel.B));
                var saturation = maximum - minimum;
                var luminance = (pixel.R * 54 + pixel.G * 183 + pixel.B * 19) / 256;
                if (saturation <= 18 || luminance >= 225) continue;
                left = Math.Min(left, x);
                top = Math.Min(top, y);
                right = Math.Max(right, x);
                bottom = Math.Max(bottom, y);
            }
        }

        if (right < left || bottom < top) throw new InvalidOperationException("The button could not be isolated.");
        var padding = Math.Max(12, Math.Min(source.Width, source.Height) / 32);
        left = Math.Max(0, left - padding);
        top = Math.Max(0, top - padding);
        right = Math.Min(source.Width - 1, right + padding);
        bottom = Math.Min(source.Height - 1, bottom + padding);
        return new Rectangle(left, top, right - left + 1, bottom - top + 1);
    }

    private static Bitmap ResizeToWidth(Bitmap source, int maxWidth) {
        if (source.Width <= maxWidth) return new Bitmap(source);
        var height = Math.Max(1, (int)Math.Round(source.Height * (maxWidth / (double)source.Width)));
        var result = new Bitmap(maxWidth, height, PixelFormat.Format32bppArgb);
        using (var graphics = Graphics.FromImage(result)) {
            graphics.CompositingMode = CompositingMode.SourceCopy;
            graphics.CompositingQuality = CompositingQuality.HighQuality;
            graphics.InterpolationMode = InterpolationMode.HighQualityBicubic;
            graphics.SmoothingMode = SmoothingMode.HighQuality;
            graphics.PixelOffsetMode = PixelOffsetMode.HighQuality;
            graphics.DrawImage(source, new Rectangle(0, 0, maxWidth, height));
        }
        return result;
    }

    private static int Clamp(int value) {
        return Math.Max(0, Math.Min(255, value));
    }
}
'@

$panelPath = (Resolve-Path -LiteralPath $Panel).Path
$smallButtonPath = (Resolve-Path -LiteralPath $SmallButton).Path
$wideButtonPath = (Resolve-Path -LiteralPath $WideButton).Path
$indicatorPath = (Resolve-Path -LiteralPath $Indicator).Path

[SettingsAssetProcessor]::CropAlpha(
    $panelPath,
    (Join-Path $outputPath "settings_panel.png"),
    768,
    4
)
[SettingsAssetProcessor]::ExtractCheckerSprite(
    $smallButtonPath,
    (Join-Path $outputPath "settings_button_small.png"),
    512
)
[SettingsAssetProcessor]::ExtractCheckerSprite(
    $wideButtonPath,
    (Join-Path $outputPath "settings_button_wide.png"),
    768
)
[SettingsAssetProcessor]::ExtractWarmIndicator(
    $indicatorPath,
    (Join-Path $outputPath "settings_indicator.png"),
    384
)

Write-Host "Settings assets exported to $outputPath"
