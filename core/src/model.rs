use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct Server {
    pub id: String,
    pub name: String,
    pub conf: String,
    pub endpoint: String,
    #[serde(default)]
    pub group: String,
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
pub struct Group {
    pub name: String,
    #[serde(default)]
    pub url: String,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum Status {
    Off,
    Connecting,
    On,
    Stopping,
}

#[derive(Clone, Copy, Debug, Default, PartialEq)]
pub struct PingResult {
    pub tcp: Option<i32>,
    pub udp: Option<i32>,
    pub icmp: Option<i32>,
    pub busy: bool,
}

impl PingResult {
    pub fn best(&self) -> i32 {
        [self.tcp, self.udp, self.icmp]
            .iter()
            .flatten()
            .copied()
            .filter(|v| *v > 0)
            .min()
            .unwrap_or(i32::MAX)
    }
}

#[derive(Clone, Copy, Debug, Default, PartialEq)]
pub struct ServiceCheck {
    pub ms: [Option<i32>; 3],
    pub busy: bool,
}

pub const SERVICE_NAMES: [&str; 3] = ["Discord", "Telegram", "YouTube"];
pub const SERVICE_URLS: [&str; 3] = [
    "https://discord.com/api/gateway",
    "https://telegram.org/",
    "https://www.youtube.com/generate_204",
];

pub const THEME_NAMES: [&str; 3] = ["Системная", "Светлая", "Тёмная"];
pub const PALETTE_STYLES: [&str; 3] = ["Стандарт", "Мягкая", "Яркая"];
pub const PALETTE_FACTORS: [f32; 3] = [1.0, 0.5, 1.3];
pub const LAUNCH_NAMES: [&str; 5] = ["Печенька", "Пульс", "Орбита", "Волна", "Неон"];
pub const MOTION_NAMES: [&str; 4] = ["Минимум", "Быстро", "Обычно", "Плавно"];
pub const MOTION_FACTORS: [f32; 4] = [0.3, 0.65, 1.0, 1.7];
pub const PING_MODES: [&str; 5] = ["Авто", "TCP", "UDP", "Оба", "ICMP"];
pub const PING_TIMEOUT_NAMES: [&str; 3] = ["2 с", "4 с", "8 с"];
pub const PING_TIMEOUTS: [u64; 3] = [2000, 4000, 8000];
pub const DNS_NAMES: [&str; 5] = ["Cloudflare", "Google", "Quad9", "AdGuard", "Свой"];
pub const DNS_SERVERS: [[&str; 2]; 4] = [
    ["1.1.1.1", "1.0.0.1"],
    ["8.8.8.8", "8.8.4.4"],
    ["9.9.9.9", "149.112.112.112"],
    ["94.140.14.14", "94.140.15.15"],
];
pub const MTU_VALUES: [i32; 3] = [1280, 1400, 1500];

pub const ACCENT_NAMES: [&str; 16] = [
    "Индиго",
    "Синий",
    "Бирюзовый",
    "Зелёный",
    "Лайм",
    "Янтарный",
    "Оранжевый",
    "Красный",
    "Розовый",
    "Небесный",
    "Мятный",
    "Жёлтый",
    "Коралловый",
    "Малиновый",
    "Пурпурный",
    "Фиолетовый",
];
pub const ACCENT_HUES: [f32; 16] = [
    250.0, 215.0, 175.0, 135.0, 80.0, 45.0, 22.0, 355.0, 320.0, 200.0, 155.0, 58.0, 8.0, 338.0,
    290.0, 270.0,
];

#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(default)]
pub struct Settings {
    pub theme: usize,
    pub accent: usize,
    pub palette: usize,
    pub amoled: bool,
    pub burn_in: bool,
    pub contrast: usize,
    pub bg_style: usize,
    pub launch_style: usize,
    pub anim_speed: usize,
    pub power_scale: f32,
    pub text_scale: f32,
    pub ping_mode: usize,
    pub ping_timeout: usize,
    pub auto_ping: bool,
    pub sort_ping: bool,
    pub auto_connect: bool,
    pub dns_preset: usize,
    pub dns_custom: String,
    pub mtu: i32,
    pub auto_check: bool,
    pub check_interval: f32,
    pub active_core: String,
    pub close_to_tray: bool,
    pub autostart: bool,
}

impl Default for Settings {
    fn default() -> Self {
        Settings {
            theme: 0,
            accent: 0,
            palette: 0,
            amoled: false,
            burn_in: false,
            contrast: 0,
            bg_style: 0,
            launch_style: 0,
            anim_speed: 2,
            power_scale: 1.0,
            text_scale: 1.0,
            ping_mode: 0,
            ping_timeout: 1,
            auto_ping: false,
            sort_ping: false,
            auto_connect: false,
            dns_preset: 0,
            dns_custom: String::new(),
            mtu: 1500,
            auto_check: true,
            check_interval: 60.0,
            active_core: String::new(),
            close_to_tray: true,
            autostart: false,
        }
    }
}

impl Settings {
    pub fn dns_list(&self) -> Vec<String> {
        if self.dns_preset < DNS_SERVERS.len() {
            DNS_SERVERS[self.dns_preset]
                .iter()
                .map(|s| s.to_string())
                .collect()
        } else {
            let custom: Vec<String> = self
                .dns_custom
                .split(|c: char| c == ',' || c == ' ' || c == ';' || c == '\n')
                .map(|s| s.trim().to_string())
                .filter(|s| {
                    !s.is_empty()
                        && s.chars().all(|c| c.is_ascii_hexdigit() || c == ':' || c == '.')
                        && (s.contains('.') || s.contains(':'))
                })
                .collect();
            if custom.is_empty() {
                DNS_SERVERS[0].iter().map(|s| s.to_string()).collect()
            } else {
                custom
            }
        }
    }
}

pub fn format_bytes(bytes: u64) -> String {
    let units = ["Б", "КБ", "МБ", "ГБ", "ТБ"];
    let mut value = bytes as f64;
    let mut i = 0;
    while value >= 1024.0 && i < units.len() - 1 {
        value /= 1024.0;
        i += 1;
    }
    if i == 0 {
        format!("{} Б", bytes)
    } else {
        format!("{:.1} {}", value, units[i])
    }
}

pub fn format_speed(bytes: u64) -> String {
    format!("{}/с", format_bytes(bytes))
}

pub fn format_time(seconds: u64) -> String {
    format!(
        "{:02}:{:02}:{:02}",
        seconds / 3600,
        (seconds % 3600) / 60,
        seconds % 60
    )
}
