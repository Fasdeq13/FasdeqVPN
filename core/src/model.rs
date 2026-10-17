use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct Server {
    pub id: String,
    pub name: String,
    pub conf: String,
    pub endpoint: String,
    #[serde(default)]
    pub group: String,
}

#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct Group {
    pub name: String,
    #[serde(default)]
    pub url: String,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(default)]
pub struct Settings {
    pub active_core: String,
    pub theme: usize,
    pub palette: usize,
    pub bg: usize,
    pub contrast: usize,
    pub accent: usize,
    pub launch_style: usize,
    pub anim_speed: usize,
    pub power_scale: f32,
    pub text_scale: f32,
    pub amoled: bool,
    pub burn_in: bool,
    pub auto_connect: bool,
    pub dns_preset: usize,
    pub dns_custom: String,
    pub mtu: usize,
    pub ping_mode: usize,
    pub ping_timeout: usize,
    pub auto_ping: bool,
    pub sort_ping: bool,
    pub auto_check: bool,
    pub check_interval: f32,
    pub close_to_tray: bool,
    pub autostart: bool,
}

impl Settings {
    pub fn dns_list(&self) -> Vec<String> {
        let presets = [
            vec!["1.1.1.1".to_string(), "1.0.0.1".to_string()],
            vec!["8.8.8.8".to_string(), "8.8.4.4".to_string()],
            vec!["9.9.9.9".to_string(), "149.112.112.112".to_string()],
            vec!["94.140.14.14".to_string(), "94.140.15.15".to_string()],
        ];
        if self.dns_preset < presets.len() {
            presets[self.dns_preset].clone()
        } else {
            self.dns_custom
                .split(',')
                .map(|s| s.trim().to_string())
                .filter(|s| !s.is_empty())
                .collect()
        }
    }
}

impl Default for Settings {
    fn default() -> Self {
        Settings {
            active_core: String::new(),
            theme: 0,
            palette: 0,
            bg: 0,
            contrast: 0,
            accent: 0,
            launch_style: 0,
            anim_speed: 1,
            power_scale: 1.0,
            text_scale: 1.0,
            amoled: false,
            burn_in: false,
            auto_connect: false,
            dns_preset: 0,
            dns_custom: String::new(),
            mtu: 0,
            ping_mode: 0,
            ping_timeout: 1,
            auto_ping: false,
            sort_ping: false,
            auto_check: false,
            check_interval: 30.0,
            close_to_tray: false,
            autostart: false,
        }
    }
}

#[derive(Clone, Debug, Default)]
pub struct PingResult {
    pub tcp: Option<i32>,
    pub udp: Option<i32>,
    pub icmp: Option<i32>,
    pub busy: bool,
}

impl PingResult {
    pub fn best(&self) -> Option<i32> {
        [self.tcp, self.udp, self.icmp]
            .iter()
            .filter_map(|v| *v)
            .filter(|v| *v > 0)
            .min()
    }
}

#[derive(Clone, Debug, Default)]
pub struct ServiceCheck {
    pub ms: [Option<i32>; 3],
    pub busy: bool,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Default)]
pub enum Status {
    #[default]
    Off,
    Connecting,
    On,
    Stopping,
}

pub const MOTION_FACTORS: &[f32] = &[0.0, 1.0, 0.6, 0.3];
pub const PING_TIMEOUTS: &[u64] = &[2000, 4000, 8000];
pub const PING_TIMEOUT_NAMES: &[&str] = &["2 с", "4 с", "8 с"];
pub const PING_MODES: &[&str] = &["Авто", "TCP", "UDP", "Оба", "ICMP"];
pub const DNS_NAMES: &[&str] = &["Cloudflare", "Google", "Quad9", "AdGuard", "Свой"];
pub const MTU_VALUES: &[usize] = &[1280, 1400, 1500];
pub const ACCENT_HUES: &[f32] = &[264.0, 220.0, 180.0, 142.0, 30.0, 350.0, 300.0, 60.0];
pub const PALETTE_FACTORS: &[f32] = &[0.8, 1.0, 1.2, 1.5, 1.8];
pub const SERVICE_URLS: &[(&str, &str)] = &[
    ("Discord", "https://discord.com/api/gateway"),
    ("Telegram", "https://telegram.org/"),
    ("YouTube", "https://www.youtube.com/generate_204"),
];
