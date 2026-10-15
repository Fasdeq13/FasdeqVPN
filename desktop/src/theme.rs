use crate::model::{Settings, ACCENT_HUES, PALETTE_FACTORS};
use iced::Color;

#[derive(Clone, Copy, Debug)]
pub struct Scheme {
    pub dark: bool,
    pub primary: Color,
    pub on_primary: Color,
    pub primary_container: Color,
    pub on_primary_container: Color,
    pub secondary: Color,
    pub on_secondary_container: Color,
    pub secondary_container: Color,
    pub tertiary: Color,
    pub tertiary_container: Color,
    pub on_tertiary_container: Color,
    pub background: Color,
    pub on_surface: Color,
    pub on_surface_variant: Color,
    pub outline: Color,
    pub outline_variant: Color,
    pub error: Color,
    pub on_error_container: Color,
    pub error_container: Color,
    pub container_low: Color,
    pub container_high: Color,
    pub container_highest: Color,
    pub inverse_surface: Color,
    pub inverse_on_surface: Color,
}

pub fn hsl(h: f32, s: f32, l: f32) -> Color {
    let h = ((h % 360.0) + 360.0) % 360.0;
    let s = s.clamp(0.0, 1.0);
    let l = l.clamp(0.0, 1.0);
    let c = (1.0 - (2.0 * l - 1.0).abs()) * s;
    let hp = h / 60.0;
    let x = c * (1.0 - (hp % 2.0 - 1.0).abs());
    let (r1, g1, b1) = match hp as i32 {
        0 => (c, x, 0.0),
        1 => (x, c, 0.0),
        2 => (0.0, c, x),
        3 => (0.0, x, c),
        4 => (x, 0.0, c),
        _ => (c, 0.0, x),
    };
    let m = l - c / 2.0;
    Color::from_rgb(r1 + m, g1 + m, b1 + m)
}

pub fn rgb(hex: u32) -> Color {
    Color::from_rgb8(((hex >> 16) & 255) as u8, ((hex >> 8) & 255) as u8, (hex & 255) as u8)
}

pub fn alpha(c: Color, a: f32) -> Color {
    Color { a, ..c }
}

pub fn lerp(a: Color, b: Color, t: f32) -> Color {
    let t = t.clamp(0.0, 1.0);
    Color {
        r: a.r + (b.r - a.r) * t,
        g: a.g + (b.g - a.g) * t,
        b: a.b + (b.b - a.b) * t,
        a: a.a + (b.a - a.a) * t,
    }
}

pub fn swatch(index: usize, style: usize) -> Color {
    hsl(
        ACCENT_HUES[index.min(ACCENT_HUES.len() - 1)],
        (0.65 * PALETTE_FACTORS[style.min(PALETTE_FACTORS.len() - 1)]).clamp(0.0, 1.0),
        0.52,
    )
}

fn light(h: f32, k: f32) -> Scheme {
    let c = |hh: f32, ss: f32, ll: f32| hsl(hh, (ss * k).clamp(0.0, 1.0), ll);
    Scheme {
        dark: false,
        primary: c(h, 0.62, 0.42),
        on_primary: Color::WHITE,
        primary_container: c(h, 0.90, 0.89),
        on_primary_container: c(h, 0.80, 0.14),
        secondary: c(h, 0.28, 0.40),
        secondary_container: c(h, 0.45, 0.88),
        on_secondary_container: c(h, 0.50, 0.14),
        tertiary: c(h + 55.0, 0.40, 0.40),
        tertiary_container: c(h + 55.0, 0.60, 0.89),
        on_tertiary_container: c(h + 55.0, 0.60, 0.14),
        background: c(h, 0.40, 0.98),
        on_surface: c(h, 0.20, 0.10),
        on_surface_variant: c(h, 0.18, 0.30),
        outline: c(h, 0.10, 0.46),
        outline_variant: c(h, 0.20, 0.80),
        error: rgb(0xBA1A1A),
        error_container: rgb(0xFFDAD6),
        on_error_container: rgb(0x410002),
        container_low: c(h, 0.45, 0.96),
        container_high: c(h, 0.32, 0.92),
        container_highest: c(h, 0.28, 0.90),
        inverse_surface: c(h, 0.12, 0.20),
        inverse_on_surface: c(h, 0.30, 0.95),
    }
}

fn dark(h: f32, k: f32) -> Scheme {
    let c = |hh: f32, ss: f32, ll: f32| hsl(hh, (ss * k).clamp(0.0, 1.0), ll);
    Scheme {
        dark: true,
        primary: c(h, 0.90, 0.78),
        on_primary: c(h, 0.80, 0.20),
        primary_container: c(h, 0.60, 0.30),
        on_primary_container: c(h, 0.90, 0.90),
        secondary: c(h, 0.40, 0.78),
        secondary_container: c(h, 0.28, 0.28),
        on_secondary_container: c(h, 0.45, 0.90),
        tertiary: c(h + 55.0, 0.50, 0.78),
        tertiary_container: c(h + 55.0, 0.35, 0.28),
        on_tertiary_container: c(h + 55.0, 0.60, 0.90),
        background: c(h, 0.14, 0.09),
        on_surface: c(h, 0.20, 0.90),
        on_surface_variant: c(h, 0.20, 0.80),
        outline: c(h, 0.10, 0.58),
        outline_variant: c(h, 0.12, 0.30),
        error: rgb(0xFFB4AB),
        error_container: rgb(0x93000A),
        on_error_container: rgb(0xFFDAD6),
        container_low: c(h, 0.13, 0.12),
        container_high: c(h, 0.12, 0.19),
        container_highest: c(h, 0.11, 0.23),
        inverse_surface: c(h, 0.20, 0.90),
        inverse_on_surface: c(h, 0.14, 0.18),
    }
}

fn with_contrast(mut s: Scheme, level: usize) -> Scheme {
    if level == 0 {
        return s;
    }
    let k = if level == 1 { 0.12 } else { 0.28 };
    let edge = if s.dark { Color::WHITE } else { Color::BLACK };
    s.on_surface = lerp(s.on_surface, edge, k * 2.0);
    s.on_surface_variant = lerp(s.on_surface_variant, edge, k * 2.0);
    s.outline = lerp(s.outline, edge, k * 3.0);
    s.outline_variant = lerp(s.outline_variant, edge, k * 2.0);
    s.container_high = lerp(s.container_high, edge, k * 0.25);
    s.container_highest = lerp(s.container_highest, edge, k * 0.35);
    s.primary_container = lerp(s.primary_container, edge, k * 0.2);
    s.secondary_container = lerp(s.secondary_container, edge, k * 0.2);
    s.tertiary_container = lerp(s.tertiary_container, edge, k * 0.2);
    s
}

fn pure_black(mut s: Scheme) -> Scheme {
    s.background = Color::BLACK;
    s.container_low = lerp(s.container_low, Color::BLACK, 0.6);
    s.container_high = lerp(s.container_high, Color::BLACK, 0.35);
    s.container_highest = lerp(s.container_highest, Color::BLACK, 0.2);
    s
}

pub fn build(settings: &Settings, system_dark: bool) -> Scheme {
    let is_dark = match settings.theme {
        1 => false,
        2 => true,
        _ => system_dark,
    };
    let hue = ACCENT_HUES[settings.accent.min(ACCENT_HUES.len() - 1)];
    let k = PALETTE_FACTORS[settings.palette.min(PALETTE_FACTORS.len() - 1)];
    let base = if is_dark { dark(hue, k) } else { light(hue, k) };
    let tinted = if is_dark && settings.amoled {
        pure_black(base)
    } else {
        base
    };
    with_contrast(tinted, settings.contrast)
}

pub fn system_is_dark() -> bool {
    matches!(
        dark_light_probe(),
        Some(true)
    )
}

fn dark_light_probe() -> Option<bool> {
    #[cfg(target_os = "linux")]
    {
        let out = std::process::Command::new("gsettings")
            .args(["get", "org.gnome.desktop.interface", "color-scheme"])
            .output()
            .ok()?;
        let text = String::from_utf8_lossy(&out.stdout).to_lowercase();
        if text.contains("dark") {
            return Some(true);
        }
        if text.contains("default") || text.contains("light") {
            return Some(false);
        }
        let theme = std::process::Command::new("gsettings")
            .args(["get", "org.gnome.desktop.interface", "gtk-theme"])
            .output()
            .ok()?;
        return Some(String::from_utf8_lossy(&theme.stdout).to_lowercase().contains("dark"));
    }
    #[cfg(target_os = "windows")]
    {
        let out = std::process::Command::new("reg")
            .args([
                "query",
                r"HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize",
                "/v",
                "AppsUseLightTheme",
            ])
            .output()
            .ok()?;
        let text = String::from_utf8_lossy(&out.stdout);
        return Some(text.contains("0x0"));
    }
    #[cfg(target_os = "macos")]
    {
        let out = std::process::Command::new("defaults")
            .args(["read", "-g", "AppleInterfaceStyle"])
            .output()
            .ok()?;
        return Some(String::from_utf8_lossy(&out.stdout).to_lowercase().contains("dark"));
    }
    #[allow(unreachable_code)]
    None
}
