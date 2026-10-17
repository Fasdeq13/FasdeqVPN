use eframe::egui;
use fasdeq_core::{importer, links};

struct Entry {
    name: String,
    protocol: String,
    raw: String,
}

struct App {
    input: String,
    entries: Vec<Entry>,
    selected: Option<usize>,
    on: bool,
    message: String,
}

impl App {
    fn new() -> Self {
        App {
            input: String::new(),
            entries: Vec::new(),
            selected: None,
            on: false,
            message: String::new(),
        }
    }

    fn import(&mut self) {
        let prepared = importer::prepare(&self.input);
        if let Some(error) = prepared.error {
            self.message = error;
            return;
        }
        let mut added = 0;
        for source in prepared.items {
            if source.is_url {
                match links::fetch_meta(&source.value) {
                    Ok(fetched) => {
                        for line in links::extract(&links::decode_subscription(&fetched.body)) {
                            added += self.add(&line);
                        }
                    }
                    Err(e) => self.message = e,
                }
            } else {
                added += self.add(&source.value);
            }
        }
        if added > 0 {
            self.message = format!("Добавлено: {}", added);
            self.input.clear();
        } else if self.message.is_empty() {
            self.message = "Ничего не распознано".into();
        }
    }

    fn add(&mut self, raw: &str) -> usize {
        match links::parse(raw) {
            Ok(parsed) => {
                self.entries.push(Entry {
                    name: parsed.name,
                    protocol: links::protocol(raw),
                    raw: raw.to_string(),
                });
                if self.selected.is_none() {
                    self.selected = Some(0);
                }
                1
            }
            Err(_) => 0,
        }
    }
}

impl eframe::App for App {
    fn logic(&mut self, _ctx: &egui::Context, _frame: &mut eframe::Frame) {}

    fn ui(&mut self, ui: &mut egui::Ui, _frame: &mut eframe::Frame) {
        egui::ScrollArea::vertical().show(ui, |ui| {
            ui.heading("FasdeqVPN");
            ui.add_space(12.0);
            let size = egui::vec2(ui.available_width().min(220.0), 220.0);
            let (rect, response) = ui.allocate_exact_size(size, egui::Sense::click());
            let color = if self.on {
                egui::Color32::from_rgb(103, 80, 164)
            } else {
                egui::Color32::from_rgb(232, 222, 248)
            };
            ui.painter().circle_filled(rect.center(), rect.height() * 0.4, color);
            if response.clicked() && self.selected.is_some() {
                self.on = !self.on;
            }
            ui.add_space(12.0);
            ui.add(
                egui::TextEdit::multiline(&mut self.input)
                    .hint_text("Ссылка, подписка или конфиг")
                    .desired_width(f32::INFINITY),
            );
            if ui.button("Импорт").clicked() {
                self.import();
            }
            if !self.message.is_empty() {
                ui.label(&self.message);
            }
            ui.separator();
            for (i, entry) in self.entries.iter().enumerate() {
                let label = format!("{}  ·  {}", entry.name, entry.protocol);
                if ui.selectable_label(self.selected == Some(i), label).clicked() {
                    self.selected = Some(i);
                }
                let _ = &entry.raw;
            }
        });
    }
}

#[cfg(not(target_os = "android"))]
pub fn run_desktop() -> eframe::Result {
    let options = eframe::NativeOptions::default();
    eframe::run_native("FasdeqVPN", options, Box::new(|_| Ok(Box::new(App::new()))))
}

#[cfg(target_os = "android")]
#[no_mangle]
fn android_main(app: eframe::egui_winit::winit::platform::android::activity::AndroidApp) {
    android_logger::init_once(
        android_logger::Config::default().with_max_level(log::LevelFilter::Info),
    );
    let options = eframe::NativeOptions {
        android_app: Some(app),
        ..Default::default()
    };
    let _ = eframe::run_native("FasdeqVPN", options, Box::new(|_| Ok(Box::new(App::new()))));
}
