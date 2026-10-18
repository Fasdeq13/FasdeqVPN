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
    pub dynamic: bool,
    pub anim: AnimConfig,
    pub tuning: Tuning,
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
            dynamic: true,
            anim: AnimConfig::default(),
            tuning: Tuning::default(),
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

pub const LOG_NAMES: [&str; 5] = ["Нет", "Ошибки", "Предупреждения", "Инфо", "Отладка"];
pub const DOMAIN_NAMES: [&str; 3] = ["AsIs", "IPIfNonMatch", "IPOnDemand"];
pub const APP_MODES: [&str; 3] = ["Все", "Исключить", "Только"];

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
#[serde(default)]
pub struct Tuning {
    #[serde(rename = "ipv6")]
    pub ipv6: bool,
    #[serde(rename = "log")]
    pub log_level: usize,
    #[serde(rename = "sniff")]
    pub sniffing: bool,
    #[serde(rename = "lan")]
    pub bypass_lan: bool,
    #[serde(rename = "quic")]
    pub block_quic: bool,
    #[serde(rename = "ds")]
    pub domain_strategy: usize,
    #[serde(rename = "am")]
    pub app_mode: usize,
    pub apps: String,
    #[serde(rename = "unm")]
    pub unmetered: bool,
    #[serde(rename = "rc")]
    pub reconnect: bool,
    #[serde(rename = "rt")]
    pub retries: i32,
    #[serde(rename = "rd")]
    pub retry_delay: i32,
    #[serde(rename = "idle")]
    pub conn_idle: i32,
    #[serde(rename = "buf")]
    pub buffer_kb: i32,
    pub mux: bool,
    #[serde(rename = "muxc")]
    pub mux_concurrency: i32,
}

impl Default for Tuning {
    fn default() -> Self {
        Tuning {
            ipv6: true,
            log_level: 2,
            sniffing: false,
            bypass_lan: false,
            block_quic: false,
            domain_strategy: 0,
            app_mode: 0,
            apps: String::new(),
            unmetered: false,
            reconnect: true,
            retries: 3,
            retry_delay: 2,
            conn_idle: 300,
            buffer_kb: 0,
            mux: false,
            mux_concurrency: 8,
        }
    }
}

impl Tuning {
    pub fn app_list(&self) -> Vec<String> {
        let mut out: Vec<String> = Vec::new();
        for part in self.apps.split(|c: char| c == ',' || c == ' ' || c == ';' || c == '\n') {
            let p = part.trim();
            if !p.is_empty()
                && p.contains('.')
                && p.chars().all(|c| c.is_ascii_alphanumeric() || c == '_' || c == '.')
                && !out.iter().any(|x| x == p)
            {
                out.push(p.to_string());
            }
        }
        out
    }
}

#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
#[serde(default)]
pub struct AnimConfig {
    #[serde(rename = "en")]
    pub enabled: bool,
    #[serde(rename = "int")]
    pub intensity: f32,
    pub tempo: f32,
    pub burst: bool,
    #[serde(rename = "hap")]
    pub haptics: bool,
    #[serde(rename = "icon")]
    pub icon_fx: bool,
    #[serde(rename = "idle")]
    pub idle_motion: bool,
    pub press: bool,
}

impl Default for AnimConfig {
    fn default() -> Self {
        AnimConfig {
            enabled: true,
            intensity: 1.0,
            tempo: 1.0,
            burst: true,
            haptics: true,
            icon_fx: true,
            idle_motion: true,
            press: true,
        }
    }
}
