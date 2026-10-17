use std::process::{Command, Stdio};

fn names() -> Vec<String> {
    #[cfg(target_os = "linux")]
    {
        let mut out = Vec::new();
        if let Ok(dir) = std::fs::read_dir("/proc") {
            for e in dir.flatten() {
                let name = e.file_name();
                let Some(pid) = name.to_str().filter(|s| s.chars().all(|c| c.is_ascii_digit())) else {
                    continue;
                };
                if let Ok(comm) = std::fs::read_to_string(format!("/proc/{}/comm", pid)) {
                    out.push(comm.trim().to_string());
                }
            }
        }
        out
    }
    #[cfg(target_os = "windows")]
    {
        let mut cmd = Command::new("tasklist");
        cmd.args(["/FO", "CSV", "/NH"]).stdout(Stdio::piped()).stderr(Stdio::null());
        crate::xray::hide_window(&mut cmd);
        match cmd.output() {
            Ok(o) => String::from_utf8_lossy(&o.stdout)
                .lines()
                .filter_map(|l| l.trim_start_matches('"').split('"').next().map(|s| s.to_string()))
                .collect(),
            Err(_) => Vec::new(),
        }
    }
    #[cfg(target_os = "macos")]
    {
        match Command::new("ps").args(["-axo", "comm="]).stdout(Stdio::piped()).stderr(Stdio::null()).output() {
            Ok(o) => String::from_utf8_lossy(&o.stdout)
                .lines()
                .map(|l| l.rsplit('/').next().unwrap_or(l).trim().to_string())
                .collect(),
            Err(_) => Vec::new(),
        }
    }
}

fn label(raw: &str) -> Option<&'static str> {
    let n = raw.to_lowercase();
    let n = n.trim_end_matches(".exe").replace([' ', '-', '_'], "");
    if n == "happ" || n.starts_with("happdesktop") || n.starts_with("happvpn") {
        Some("Happ")
    } else if n.starts_with("v2raytun") {
        Some("v2RayTun")
    } else {
        None
    }
}

pub fn running() -> Vec<String> {
    let mut found: Vec<String> = Vec::new();
    for raw in names() {
        if let Some(l) = label(&raw) {
            if !found.iter().any(|f| f == l) {
                found.push(l.to_string());
            }
        }
    }
    found
}
