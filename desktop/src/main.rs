#![cfg_attr(all(not(debug_assertions), target_os = "windows"), windows_subsystem = "windows")]

mod anim;
mod app;
mod conflict;
mod draw;
mod flags;
mod store;
mod theme;
mod tray;
mod ui;
mod views;
mod xray;

pub use fasdeq_core::{importer, links, model, ping};

use iced::window;

fn title(app: &app::App, _: window::Id) -> String {
    app.title()
}

fn theme(_: &app::App, _: window::Id) -> iced::Theme {
    iced::Theme::Dark
}

fn view(app: &app::App, _: window::Id) -> ui::El<'_> {
    views::view(app)
}

#[cfg(target_os = "linux")]
fn install_desktop_entry() {
    let Some(home) = std::env::var_os("HOME").map(std::path::PathBuf::from) else {
        return;
    };
    let exec = std::env::var("APPIMAGE")
        .ok()
        .or_else(|| std::env::current_exe().ok().map(|p| p.display().to_string()));
    let Some(exec) = exec else {
        return;
    };
    let icons = home.join(".local/share/icons/hicolor/256x256/apps");
    let apps = home.join(".local/share/applications");
    let _ = std::fs::create_dir_all(&icons);
    let _ = std::fs::create_dir_all(&apps);
    let icon = icons.join("FasdeqVPN.png");
    let png = tray::png(256);
    if std::fs::read(&icon).map(|c| c != png).unwrap_or(true) {
        let _ = std::fs::write(&icon, &png);
    }
    let entry = format!(
        "[Desktop Entry]\nType=Application\nName=FasdeqVPN\nExec=\"{}\"\nIcon={}\nCategories=Network;\nTerminal=false\nStartupWMClass=FasdeqVPN\n",
        exec,
        icon.display()
    );
    let path = apps.join("FasdeqVPN.desktop");
    if std::fs::read_to_string(&path).map(|c| c != entry).unwrap_or(true) {
        let _ = std::fs::write(&path, entry);
        let _ = std::process::Command::new("update-desktop-database").arg(&apps).status();
        let _ = std::process::Command::new("gtk-update-icon-cache")
            .args(["-f", "-t"])
            .arg(home.join(".local/share/icons/hicolor"))
            .status();
    }
}

const INSTANCE_PORT: u16 = 47653;
const INSTANCE_MAGIC: &[u8] = b"FASDEQVPN-SHOW";

fn single_instance() -> Option<std::net::TcpListener> {
    match std::net::TcpListener::bind(("127.0.0.1", INSTANCE_PORT)) {
        Ok(l) => Some(l),
        Err(_) => {
            use std::io::Write;
            if let Ok(mut s) = std::net::TcpStream::connect(("127.0.0.1", INSTANCE_PORT)) {
                let _ = s.write_all(INSTANCE_MAGIC);
                std::process::exit(0);
            }
            None
        }
    }
}

fn serve_instance(listener: std::net::TcpListener) {
    std::thread::spawn(move || {
        use std::io::Read;
        for stream in listener.incoming().flatten() {
            let mut stream = stream;
            let _ = stream.set_read_timeout(Some(std::time::Duration::from_millis(500)));
            let mut buf = [0u8; 32];
            if let Ok(n) = stream.read(&mut buf) {
                if &buf[..n] == INSTANCE_MAGIC {
                    tray::send(tray::TrayCmd::Show);
                }
            }
        }
    });
}

fn main() -> iced::Result {
    if let Some(i) = std::env::args().position(|a| a == "--icon-png") {
        if let Some(path) = std::env::args().nth(i + 1) {
            let _ = std::fs::write(path, tray::png(256));
            return Ok(());
        }
    }
    let guard = single_instance();
    #[cfg(target_os = "linux")]
    install_desktop_entry();
    tray::init();
    if let Some(l) = guard {
        serve_instance(l);
    }
    iced::daemon(app::App::boot, app::App::update, view)
        .title(title)
        .theme(theme)
        .subscription(app::App::subscription)
        .font(include_bytes!("../assets/fonts/Roboto-Regular.ttf").as_slice())
        .font(include_bytes!("../assets/fonts/Roboto-Medium.ttf").as_slice())
        .font(include_bytes!("../assets/fonts/Roboto-Bold.ttf").as_slice())
        .font(include_bytes!("../assets/fonts/TwemojiMozilla.ttf").as_slice())
        .default_font(ui::font(iced::font::Weight::Normal))
        .antialiasing(true)
        .run()
}
