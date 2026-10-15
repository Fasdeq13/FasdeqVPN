#![cfg_attr(all(not(debug_assertions), target_os = "windows"), windows_subsystem = "windows")]

mod anim;
mod app;
mod draw;
mod importer;
mod links;
mod model;
mod ping;
mod store;
mod theme;
mod tray;
mod ui;
mod views;
mod xray;

use iced::{window, Size};

fn main() -> iced::Result {
    tray::init();
    let (rgba, w, h) = tray::raster(256);
    let icon = window::icon::from_rgba(rgba, w, h).ok();
    iced::application(app::App::boot, app::App::update, views::view)
        .title(app::App::title)
        .subscription(app::App::subscription)
        .theme(|_: &app::App| iced::Theme::Dark)
        .font(include_bytes!("../assets/fonts/Roboto-Regular.ttf").as_slice())
        .font(include_bytes!("../assets/fonts/Roboto-Medium.ttf").as_slice())
        .font(include_bytes!("../assets/fonts/Roboto-Bold.ttf").as_slice())
        .default_font(ui::font(iced::font::Weight::Normal))
        .antialiasing(true)
        .window(window::Settings {
            size: Size::new(460.0, 900.0),
            min_size: Some(Size::new(380.0, 600.0)),
            icon,
            exit_on_close_request: false,
            ..window::Settings::default()
        })
        .run()
}
