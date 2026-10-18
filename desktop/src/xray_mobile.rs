use crate::bridge;
use crate::links;
use crate::model::SERVICE_URLS;
use serde_json::Value;
use std::path::{Path, PathBuf};

#[derive(Clone, Debug, PartialEq)]
pub struct CoreInfo {
    pub id: String,
    pub label: String,
    pub version: String,
    pub path: PathBuf,
}

#[derive(Clone, Debug)]
pub struct Release {
    pub tag: String,
    pub url: String,
    pub size: u64,
}

pub use fasdeq_core::proc::hide_window;

pub fn list() -> Vec<CoreInfo> {
    Vec::new()
}

pub fn delete(_id: &str) {}

pub fn find_binary(_active: &str) -> Option<PathBuf> {
    Some(PathBuf::from("builtin"))
}

pub fn version(_path: &Path) -> Result<String, String> {
    Ok(String::new())
}

pub fn install(_raw: &[u8], _label: &str) -> Result<CoreInfo, String> {
    Err("Недоступно на Android".into())
}

pub fn install_file(_path: &Path) -> Result<CoreInfo, String> {
    Err("Недоступно на Android".into())
}

pub fn releases() -> Result<Vec<Release>, String> {
    Err("Недоступно на Android".into())
}

pub fn download(_url: &str) -> Result<Vec<u8>, String> {
    Err("Недоступно на Android".into())
}

pub fn free_port() -> u16 {
    0
}

#[derive(Debug)]
pub struct Proc;

impl Proc {
    pub fn alive(&mut self) -> bool {
        bridge::state().map(|s| s.up).unwrap_or(true)
    }

    pub fn stop(&mut self) {
        bridge::stop();
    }
}

pub fn start(_bin: &Path, _config: &Value, _wait_port: u16) -> Result<Proc, String> {
    Err("Недоступно на Android".into())
}

pub fn start_vpn(payload: &Value) -> Result<Proc, String> {
    bridge::start(payload)?;
    let begin = std::time::Instant::now();
    loop {
        if let Some(s) = bridge::state() {
            if s.up {
                return Ok(Proc);
            }
        }
        if begin.elapsed() > std::time::Duration::from_secs(25) {
            bridge::stop();
            return Err("Не удалось подключиться: истекло время ожидания".into());
        }
        std::thread::sleep(std::time::Duration::from_millis(300));
    }
}

pub fn query_stats(_bin: &Path, _api_port: u16) -> Option<(u64, u64)> {
    None
}

pub fn check_server(_bin: &Path, conf: &str, _timeout: u64) -> Result<[i32; 3], String> {
    let mut parsed = links::parse(conf)?;
    links::resolve(&mut parsed.outbound);
    let handles: Vec<_> = SERVICE_URLS
        .iter()
        .map(|url| {
            let outbound = parsed.outbound.clone();
            let url = url.to_string();
            std::thread::spawn(move || bridge::service_check(&outbound, &url))
        })
        .collect();
    let mut result = [-1i32; 3];
    for (i, h) in handles.into_iter().enumerate() {
        result[i] = h.join().unwrap_or(-1);
    }
    Ok(result)
}

pub fn set_system_proxy(_http_port: u16, _socks_port: u16) {}

pub fn clear_system_proxy() {}
