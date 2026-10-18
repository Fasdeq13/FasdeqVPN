use crate::model::{Group, Server, Settings};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::PathBuf;

#[derive(Default, Serialize, Deserialize)]
#[serde(default)]
pub struct Store {
    pub servers: Vec<Server>,
    pub groups: Vec<Group>,
    pub selected: Option<String>,
    pub settings: Settings,
    pub checks: HashMap<String, [i32; 3]>,
    pub checked_at: u64,
}

static DIR: std::sync::OnceLock<PathBuf> = std::sync::OnceLock::new();

pub fn set_dir(path: PathBuf) {
    let _ = std::fs::create_dir_all(&path);
    let _ = DIR.set(path);
}

#[cfg(not(target_os = "android"))]
fn default_dir() -> PathBuf {
    directories::ProjectDirs::from("app", "fasdeq", "FasdeqVPN")
        .map(|d| d.data_dir().to_path_buf())
        .unwrap_or_else(|| std::env::temp_dir().join("FasdeqVPN"))
}

#[cfg(target_os = "android")]
fn default_dir() -> PathBuf {
    std::env::temp_dir().join("FasdeqVPN")
}

pub fn data_dir() -> PathBuf {
    let dir = DIR.get().cloned().unwrap_or_else(default_dir);
    let _ = std::fs::create_dir_all(&dir);
    dir
}

fn file() -> PathBuf {
    data_dir().join("state.json")
}

#[derive(Default, Serialize, Deserialize)]
#[serde(default)]
struct Shared {
    servers: Vec<Server>,
    groups: Vec<Group>,
    selected: Option<String>,
}

fn shared_file() -> PathBuf {
    data_dir().join("shared.json")
}

impl Store {
    pub fn load() -> Store {
        let mut store: Store = std::fs::read_to_string(file())
            .ok()
            .and_then(|raw| serde_json::from_str(&raw).ok())
            .unwrap_or_default();
        if cfg!(mobile) {
            if let Some(shared) = std::fs::read_to_string(shared_file())
                .ok()
                .and_then(|raw| serde_json::from_str::<Shared>(&raw).ok())
            {
                store.servers = shared.servers;
                store.groups = shared.groups;
                store.selected = shared.selected;
            }
        }
        store
    }

    pub fn save(&self) {
        if cfg!(mobile) {
            let shared = Shared {
                servers: self.servers.clone(),
                groups: self.groups.clone(),
                selected: self.selected.clone(),
            };
            if let Ok(raw) = serde_json::to_string(&shared) {
                let path = shared_file();
                let tmp = path.with_extension("tmp");
                if std::fs::write(&tmp, raw).is_ok() {
                    let _ = std::fs::rename(&tmp, &path);
                }
            }
        }
        if let Ok(raw) = serde_json::to_string(self) {
            let path = file();
            let tmp = path.with_extension("tmp");
            if std::fs::write(&tmp, raw).is_ok() {
                let _ = std::fs::rename(&tmp, &path);
            }
        }
    }
}

pub fn now_millis() -> u64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|d| d.as_millis() as u64)
        .unwrap_or(0)
}
