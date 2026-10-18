pub const MOBILE: bool = cfg!(mobile);

#[cfg(not(mobile))]
pub fn open_url(url: &str) {
    #[cfg(target_os = "windows")]
    {
        let _ = std::process::Command::new("cmd").args(["/C", "start", "", url]).spawn();
    }
    #[cfg(target_os = "macos")]
    {
        let _ = std::process::Command::new("open").arg(url).spawn();
    }
    #[cfg(target_os = "linux")]
    {
        let _ = std::process::Command::new("xdg-open").arg(url).spawn();
    }
}

#[cfg(mobile)]
pub fn open_url(url: &str) {
    crate::bridge::open_url(url);
}

#[cfg(mobile)]
pub fn mark_ready() {
    use std::sync::atomic::{AtomicBool, Ordering};
    static DONE: AtomicBool = AtomicBool::new(false);
    if DONE.swap(true, Ordering::Relaxed) {
        return;
    }
    let _ = std::fs::write(crate::store::data_dir().join("ui_ok"), b"1");
}

#[cfg(not(mobile))]
pub fn mark_ready() {}

#[cfg(mobile)]
pub fn install_panic_hook() {
    std::panic::set_hook(Box::new(|info| {
        let text = format!("{}", info);
        #[cfg(target_os = "android")]
        log::error!("fasdeq panic: {}", text);
        #[cfg(not(target_os = "android"))]
        eprintln!("fasdeq panic: {}", text);
        let _ = std::fs::write(crate::store::data_dir().join("ui_error.txt"), text);
    }));
}

#[cfg(mobile)]
pub fn pick_backend() {
    let n = std::fs::read_to_string(crate::store::data_dir().join("iced_try"))
        .ok()
        .and_then(|t| t.trim().parse::<u32>().ok())
        .unwrap_or(0);
    #[allow(unused_unsafe)]
    unsafe {
        match n {
            0 => {}
            1 => std::env::set_var("WGPU_BACKEND", "gl"),
            _ => std::env::set_var("ICED_BACKEND", "tiny-skia"),
        }
    }
}
