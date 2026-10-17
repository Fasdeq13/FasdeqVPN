use fasdeq_core::model::{Group, Server};
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

#[derive(Default, Serialize, Deserialize)]
#[serde(default)]
pub struct Store {
    pub servers: Vec<Server>,
    pub groups: Vec<Group>,
    pub selected: Option<String>,
    pub theme_mode: usize,
    pub dns_preset: usize,
    pub dns_custom: String,
}

fn data_dir() -> PathBuf {
    #[cfg(target_os = "android")]
    {
        std::path::PathBuf::from("/data/data/app.fasdeq.vpn/files")
    }
    #[cfg(not(target_os = "android"))]
    {
        directories::ProjectDirs::from("app", "fasdeq", "FasdeqVPN")
            .map(|d| d.data_dir().to_path_buf())
            .unwrap_or_else(|| std::env::temp_dir().join("FasdeqVPN"))
    }
}

fn file() -> PathBuf {
    let dir = data_dir();
    let _ = std::fs::create_dir_all(&dir);
    dir.join("state_ui.json")
}

impl Store {
    pub fn load() -> Store {
        std::fs::read_to_string(file())
            .ok()
            .and_then(|raw| serde_json::from_str(&raw).ok())
            .unwrap_or_default()
    }

    pub fn save(&self) {
        if let Ok(raw) = serde_json::to_string(self) {
            let path = file();
            let tmp = path.with_extension("tmp");
            if std::fs::write(&tmp, raw).is_ok() {
                let _ = std::fs::rename(&tmp, &path);
            }
        }
    }
}
