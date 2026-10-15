use iced::futures::Stream;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Mutex, OnceLock};
use tokio::sync::mpsc::{unbounded_channel, UnboundedReceiver, UnboundedSender};

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum TrayCmd {
    Show,
    Toggle,
    Quit,
}

static TX: OnceLock<UnboundedSender<TrayCmd>> = OnceLock::new();
static RX: Mutex<Option<UnboundedReceiver<TrayCmd>>> = Mutex::new(None);
static READY: AtomicBool = AtomicBool::new(false);

pub fn available() -> bool {
    READY.load(Ordering::Relaxed)
}

pub fn send(cmd: TrayCmd) {
    if let Some(tx) = TX.get() {
        let _ = tx.send(cmd);
    }
}

pub fn raster(size: u32) -> (Vec<u8>, u32, u32) {
    let data = include_bytes!("../assets/app.svg");
    let tree = resvg::usvg::Tree::from_data(data, &resvg::usvg::Options::default())
        .expect("app icon");
    let mut pixmap = resvg::tiny_skia::Pixmap::new(size, size).expect("pixmap");
    let scale = size as f32 / tree.size().width();
    resvg::render(
        &tree,
        resvg::tiny_skia::Transform::from_scale(scale, scale),
        &mut pixmap.as_mut(),
    );
    let mut out = Vec::with_capacity((size * size * 4) as usize);
    for p in pixmap.pixels() {
        let c = p.demultiply();
        out.extend_from_slice(&[c.red(), c.green(), c.blue(), c.alpha()]);
    }
    (out, size, size)
}

fn build() -> Result<tray_icon::TrayIcon, String> {
    use tray_icon::menu::{Menu, MenuEvent, MenuItem, PredefinedMenuItem};
    use tray_icon::{Icon, MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent};
    let (rgba, w, h) = raster(64);
    let icon = Icon::from_rgba(rgba, w, h).map_err(|e| e.to_string())?;
    let menu = Menu::new();
    let show = MenuItem::with_id("show", "Открыть FasdeqVPN", true, None);
    let toggle = MenuItem::with_id("toggle", "Подключить / Отключить", true, None);
    let quit = MenuItem::with_id("quit", "Выход", true, None);
    menu.append(&show).map_err(|e| e.to_string())?;
    menu.append(&toggle).map_err(|e| e.to_string())?;
    menu.append(&PredefinedMenuItem::separator())
        .map_err(|e| e.to_string())?;
    menu.append(&quit).map_err(|e| e.to_string())?;
    MenuEvent::set_event_handler(Some(|ev: MenuEvent| match ev.id.0.as_str() {
        "show" => send(TrayCmd::Show),
        "toggle" => send(TrayCmd::Toggle),
        "quit" => send(TrayCmd::Quit),
        _ => {}
    }));
    TrayIconEvent::set_event_handler(Some(|ev: TrayIconEvent| {
        if let TrayIconEvent::Click {
            button: MouseButton::Left,
            button_state: MouseButtonState::Up,
            ..
        } = ev
        {
            send(TrayCmd::Show);
        }
    }));
    TrayIconBuilder::new()
        .with_menu(Box::new(menu))
        .with_tooltip("FasdeqVPN")
        .with_icon(icon)
        .build()
        .map_err(|e| e.to_string())
}

pub fn init() {
    let (tx, rx) = unbounded_channel();
    let _ = TX.set(tx);
    *RX.lock().unwrap() = Some(rx);
    #[cfg(target_os = "linux")]
    {
        let (done_tx, done_rx) = std::sync::mpsc::channel::<bool>();
        std::thread::spawn(move || {
            if gtk::init().is_err() {
                let _ = done_tx.send(false);
                return;
            }
            match build() {
                Ok(tray) => {
                    READY.store(true, Ordering::Relaxed);
                    let _ = done_tx.send(true);
                    gtk::main();
                    drop(tray);
                }
                Err(_) => {
                    let _ = done_tx.send(false);
                }
            }
        });
        let _ = done_rx.recv_timeout(std::time::Duration::from_millis(2500));
    }
    #[cfg(target_os = "windows")]
    {
        if let Ok(tray) = build() {
            READY.store(true, Ordering::Relaxed);
            Box::leak(Box::new(tray));
        }
    }
}

pub fn stream() -> impl Stream<Item = TrayCmd> {
    iced::stream::channel(16, |mut output: iced::futures::channel::mpsc::Sender<TrayCmd>| async move {
        use iced::futures::SinkExt;
        let rx = RX.lock().unwrap().take();
        if let Some(mut rx) = rx {
            while let Some(cmd) = rx.recv().await {
                let _ = output.send(cmd).await;
            }
        }
        iced::futures::future::pending::<()>().await;
    })
}
