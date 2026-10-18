pub mod anim;
pub mod app;
pub mod draw;
pub mod flags;
pub mod store;
pub mod theme;
pub mod ui;
pub mod views;

#[cfg(not(mobile))]
pub mod conflict;
#[cfg(mobile)]
#[path = "conflict_mobile.rs"]
pub mod conflict;

#[cfg(not(mobile))]
pub mod tray;
#[cfg(mobile)]
#[path = "tray_mobile.rs"]
pub mod tray;

#[cfg(not(mobile))]
pub mod xray;
#[cfg(mobile)]
#[path = "xray_mobile.rs"]
pub mod xray;

pub mod bridge;
pub mod platform;

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

pub fn run() -> iced::Result {
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

#[cfg(target_os = "android")]
#[no_mangle]
fn android_main(app: android_activity::AndroidApp) {
    android_logger::init_once(android_logger::Config::default().with_max_level(log::LevelFilter::Info));
    if let Some(path) = app.internal_data_path() {
        store::set_dir(path);
    }
    platform::install_panic_hook();
    platform::pick_backend();
    bridge::init(app.vm_as_ptr(), app.activity_as_ptr());
    bridge::set_app(app.clone());
    let _ = iced_winit::ANDROID_APP.set(app);
    if let Err(e) = run() {
        let text = format!("{}", e);
        log::error!("fasdeq iced run failed: {}", text);
        let _ = std::fs::write(store::data_dir().join("ui_error.txt"), text);
    }
}
