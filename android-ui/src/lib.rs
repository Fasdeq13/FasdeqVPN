use fasdeq_core::{importer, links, model::*, ping};
use iced::{
    alignment::{Horizontal, Vertical},
    widget::{
        button, column, container, horizontal_space, row, scrollable, text,
        text_input, vertical_space, Column, Row,
    },
    Color, Element, Length, Subscription, Task, Theme,
};
use std::collections::HashMap;

pub mod store;
pub mod theme;

use store::Store;
pub use theme::Palette;

#[derive(Clone, Debug)]
pub enum Msg {
    Power,
    SelectServer(String),
    OpenAdd,
    CloseAdd,
    AddInput(String),
    AddSubmit,
    ImportDone(Result<Vec<(String, String, String)>, String>),
    OpenSettings,
    CloseSettings,
    Connected(Result<String, String>),
    Disconnected,
    Second,
    PingServer(String),
    PingDone(String, PingResult),
    SetTheme(usize),
    SetDnsPreset(usize),
    SetDnsCustom(String),
    DeleteServer(String),
    RenameOpen(String, String),
    RenameInput(String),
    RenameApply,
    RenameCancel,
    ErrorDismiss,
    Noop,
}

#[derive(Clone, PartialEq)]
pub enum Screen {
    Main,
    Add,
    Settings,
}

pub struct App {
    servers: Vec<Server>,
    groups: Vec<Group>,
    selected: Option<String>,
    status: Status,
    error: Option<String>,
    uptime: u64,
    since: Option<std::time::Instant>,
    pings: HashMap<String, PingResult>,
    screen: Screen,
    add_text: String,
    add_error: Option<String>,
    add_loading: bool,
    settings_dns_preset: usize,
    settings_dns_custom: String,
    settings_theme: usize,
    rename: Option<(String, String)>,
    palette: Palette,
}

impl App {
    pub fn new() -> (App, Task<Msg>) {
        let store = Store::load();
        let palette = theme::build(store.theme_mode);
        let selected = store
            .selected
            .clone()
            .filter(|id| store.servers.iter().any(|s| &s.id == id))
            .or_else(|| store.servers.first().map(|s| s.id.clone()));
        let app = App {
            servers: store.servers,
            groups: store.groups,
            selected,
            status: Status::Off,
            error: None,
            uptime: 0,
            since: None,
            pings: HashMap::new(),
            screen: Screen::Main,
            add_text: String::new(),
            add_error: None,
            add_loading: false,
            settings_dns_preset: store.dns_preset,
            settings_dns_custom: store.dns_custom.clone(),
            settings_theme: store.theme_mode,
            rename: None,
            palette,
        };
        (app, Task::none())
    }

    fn save(&self) {
        Store {
            servers: self.servers.clone(),
            groups: self.groups.clone(),
            selected: self.selected.clone(),
            theme_mode: self.settings_theme,
            dns_preset: self.settings_dns_preset,
            dns_custom: self.settings_dns_custom.clone(),
        }
        .save();
    }

    fn selected_server(&self) -> Option<&Server> {
        self.servers.iter().find(|s| Some(&s.id) == self.selected.as_ref())
    }

    fn dns_list(&self) -> Vec<String> {
        let presets: [&[&str]; 4] = [
            &["1.1.1.1", "1.0.0.1"],
            &["8.8.8.8", "8.8.4.4"],
            &["9.9.9.9", "149.112.112.112"],
            &["94.140.14.14", "94.140.15.15"],
        ];
        if self.settings_dns_preset < presets.len() {
            presets[self.settings_dns_preset].iter().map(|s| s.to_string()).collect()
        } else {
            self.settings_dns_custom
                .split(',')
                .map(|s| s.trim().to_string())
                .filter(|s| !s.is_empty())
                .collect()
        }
    }

    fn short_id() -> String {
        format!("fq{:x}", rand::random::<u32>() % 0xEFFFFF + 0x100000)
    }

    pub fn update(&mut self, msg: Msg) -> Task<Msg> {
        match msg {
            Msg::Power => match self.status {
                Status::Off => {
                    if let Some(server) = self.selected_server().cloned() {
                        self.status = Status::Connecting;
                        self.error = None;
                        let dns = self.dns_list();
                        return Task::perform(
                            {
                                let conf = server.conf.clone();
                                let id = server.id.clone();
                                tokio::task::spawn_blocking(move || -> Result<String, String> {
                                    let _parsed = links::parse(&conf)?;
                                    Ok(id)
                                })
                                .map(|r| r.unwrap_or_else(|e| Err(e.to_string())))
                            },
                            Msg::Connected,
                        );
                    }
                    Task::none()
                }
                Status::On | Status::Connecting => {
                    self.status = Status::Stopping;
                    Task::perform(
                        tokio::task::spawn_blocking(|| ()).map(|_| ()),
                        |_| Msg::Disconnected,
                    )
                }
                Status::Stopping => Task::none(),
            },
            Msg::Connected(res) => {
                match res {
                    Ok(_) => {
                        self.status = Status::On;
                        self.since = Some(std::time::Instant::now());
                        self.uptime = 0;
                    }
                    Err(e) => {
                        self.status = Status::Off;
                        self.error = Some(e);
                    }
                }
                Task::none()
            }
            Msg::Disconnected => {
                self.status = Status::Off;
                self.since = None;
                self.uptime = 0;
                Task::none()
            }
            Msg::Second => {
                if self.status == Status::On {
                    if let Some(since) = self.since {
                        self.uptime = since.elapsed().as_secs();
                    }
                }
                Task::none()
            }
            Msg::SelectServer(id) => {
                self.selected = Some(id);
                self.save();
                Task::none()
            }
            Msg::OpenAdd => {
                self.screen = Screen::Add;
                self.add_text.clear();
                self.add_error = None;
                Task::none()
            }
            Msg::CloseAdd => {
                self.screen = Screen::Main;
                Task::none()
            }
            Msg::AddInput(s) => {
                self.add_text = s;
                self.add_error = None;
                Task::none()
            }
            Msg::AddSubmit => {
                if self.add_loading {
                    return Task::none();
                }
                let text = self.add_text.trim().to_string();
                if text.is_empty() {
                    self.add_error = Some("Вставьте ссылку или конфигурацию".into());
                    return Task::none();
                }
                self.add_loading = true;
                self.add_error = None;
                Task::perform(
                    tokio::task::spawn_blocking(move || -> Result<Vec<(String, String, String)>, String> {
                        let prepared = importer::prepare(&text);
                        if let Some(e) = prepared.error {
                            return Err(e);
                        }
                        let mut results = Vec::new();
                        for src in prepared.items {
                            if src.is_url {
                                let fetched = links::fetch_meta(&src.value)
                                    .map_err(|e| format!("Ошибка загрузки: {}", e))?;
                                let body = links::decode_subscription(&fetched.body);
                                for line in links::extract(&body) {
                                    if let Ok(p) = links::parse(&line) {
                                        results.push((p.name, line, fetched.group.clone()));
                                    }
                                }
                            } else if let Ok(p) = links::parse(&src.value) {
                                results.push((p.name, src.value, String::new()));
                            }
                        }
                        if results.is_empty() {
                            Err("Ничего не распознано".into())
                        } else {
                            Ok(results)
                        }
                    })
                    .map(|r| r.unwrap_or_else(|e| Err(e.to_string()))),
                    Msg::ImportDone,
                )
            }
            Msg::ImportDone(res) => {
                self.add_loading = false;
                match res {
                    Ok(items) => {
                        for (name, conf, group) in items {
                            if !self.servers.iter().any(|s| s.conf == conf) {
                                let id = Self::short_id();
                                let title = if name.is_empty() {
                                    format!("Сервер {}", self.servers.len() + 1)
                                } else {
                                    name
                                };
                                if !group.is_empty() {
                                    if !self.groups.iter().any(|g| g.name == group) {
                                        self.groups.push(Group { name: group.clone(), url: String::new() });
                                    }
                                }
                                self.servers.push(Server {
                                    id: id.clone(),
                                    name: title,
                                    conf,
                                    endpoint: String::new(),
                                    group,
                                });
                                if self.selected.is_none() {
                                    self.selected = Some(id);
                                }
                            }
                        }
                        self.save();
                        self.screen = Screen::Main;
                    }
                    Err(e) => {
                        self.add_error = Some(e);
                    }
                }
                Task::none()
            }
            Msg::OpenSettings => {
                self.screen = Screen::Settings;
                Task::none()
            }
            Msg::CloseSettings => {
                self.screen = Screen::Main;
                self.save();
                Task::none()
            }
            Msg::SetTheme(idx) => {
                self.settings_theme = idx;
                self.palette = theme::build(idx);
                Task::none()
            }
            Msg::SetDnsPreset(idx) => {
                self.settings_dns_preset = idx;
                Task::none()
            }
            Msg::SetDnsCustom(s) => {
                self.settings_dns_custom = s;
                Task::none()
            }
            Msg::DeleteServer(id) => {
                self.servers.retain(|s| s.id != id);
                if self.selected.as_deref() == Some(id.as_str()) {
                    self.selected = self.servers.first().map(|s| s.id.clone());
                }
                self.save();
                Task::none()
            }
            Msg::RenameOpen(id, name) => {
                self.rename = Some((id, name));
                Task::none()
            }
            Msg::RenameInput(s) => {
                if let Some((_, ref mut n)) = self.rename {
                    *n = s;
                }
                Task::none()
            }
            Msg::RenameApply => {
                if let Some((id, name)) = self.rename.take() {
                    if let Some(s) = self.servers.iter_mut().find(|s| s.id == id) {
                        s.name = name;
                    }
                    self.save();
                }
                Task::none()
            }
            Msg::RenameCancel => {
                self.rename = None;
                Task::none()
            }
            Msg::PingServer(id) => {
                if let Some(server) = self.servers.iter().find(|s| s.id == id).cloned() {
                    self.pings.insert(id.clone(), PingResult { busy: true, ..Default::default() });
                    let sid = id.clone();
                    return Task::perform(
                        tokio::task::spawn_blocking(move || ping::measure(&server, 0, 4000))
                            .map(|r| r.unwrap_or_default()),
                        move |r| Msg::PingDone(sid.clone(), r),
                    );
                }
                Task::none()
            }
            Msg::PingDone(id, result) => {
                self.pings.insert(id, result);
                Task::none()
            }
            Msg::ErrorDismiss => {
                self.error = None;
                Task::none()
            }
            Msg::Noop => Task::none(),
        }
    }

    pub fn view(&self) -> Element<Msg> {
        match self.screen {
            Screen::Main => self.view_main(),
            Screen::Add => self.view_add(),
            Screen::Settings => self.view_settings(),
        }
    }

    fn bg_style(p: Palette) -> impl Fn(&iced::Theme) -> container::Style {
        move |_| container::Style {
            background: Some(p.background.into()),
            ..Default::default()
        }
    }

    fn card_style(bg: Color, radius: f32) -> impl Fn(&iced::Theme, button::Status) -> button::Style {
        move |_, _| button::Style {
            background: Some(bg.into()),
            border: iced::Border { radius: radius.into(), ..Default::default() },
            ..Default::default()
        }
    }

    fn view_main(&self) -> Element<Msg> {
        let p = self.palette;
        let on = self.status == Status::On;
        let connecting = self.status == Status::Connecting;

        let status_text = match self.status {
            Status::Off => "Отключено",
            Status::Connecting => "Подключение…",
            Status::On => "Защищено",
            Status::Stopping => "Отключение…",
        };

        let power_bg = if on { p.primary } else if connecting { p.secondary } else { p.surface_variant };
        let power_fg = if on { p.on_primary } else if connecting { p.on_secondary } else { p.on_surface_variant };

        let power_icon = text("⏻").size(52).color(power_fg);

        let power_btn = button(
            container(power_icon)
                .width(Length::Fixed(112.0))
                .height(Length::Fixed(112.0))
                .align_x(Horizontal::Center)
                .align_y(Vertical::Center)
                .style(move |_| container::Style {
                    background: Some(power_bg.into()),
                    border: iced::Border { radius: 56.0.into(), ..Default::default() },
                    ..Default::default()
                }),
        )
        .on_press(Msg::Power)
        .style(|_, _| button::Style { background: None, ..Default::default() });

        let title_color = if on { p.primary } else { p.on_surface };
        let selected_name = self.selected_server().map(|s| s.name.as_str()).unwrap_or("Сервер не выбран");

        let hero = column![
            vertical_space().height(28),
            container(power_btn).width(Length::Fill).align_x(Horizontal::Center),
            vertical_space().height(18),
            container(text(status_text).size(24).color(title_color))
                .width(Length::Fill)
                .align_x(Horizontal::Center),
            vertical_space().height(4),
            container(text(selected_name).size(14).color(p.on_surface_variant))
                .width(Length::Fill)
                .align_x(Horizontal::Center),
            vertical_space().height(12),
        ]
        .spacing(0);

        let stats_area: Element<Msg> = if on {
            let t = self.uptime;
            let uptime_str = format!("{:02}:{:02}:{:02}", t / 3600, (t % 3600) / 60, t % 60);
            row![
                text(uptime_str).size(12).color(p.on_surface_variant),
            ]
            .padding([0, 24])
            .into()
        } else if let Some(err) = &self.error {
            button(
                container(text(err.as_str()).size(12).color(p.on_error_container))
                    .padding([8, 16])
                    .style(move |_| container::Style {
                        background: Some(p.error_container.into()),
                        border: iced::Border { radius: 20.0.into(), ..Default::default() },
                        ..Default::default()
                    }),
            )
            .on_press(Msg::ErrorDismiss)
            .style(|_, _| button::Style { background: None, ..Default::default() })
            .into()
        } else {
            vertical_space().height(0).into()
        };

        let top_bar = row![
            container(text("FasdeqVPN").size(19).color(p.on_surface)).padding([0, 4]),
            horizontal_space(),
            button(
                container(text("⚙").size(22).color(p.on_surface_variant))
                    .padding([4, 8])
            )
            .on_press(Msg::OpenSettings)
            .style(|_, _| button::Style { background: None, ..Default::default() }),
        ]
        .padding([12, 16])
        .align_y(Vertical::Center);

        let servers_list = self.view_servers_list();

        let fab = button(
            row![
                text("＋").size(16).color(p.on_primary),
                text("Добавить").size(14).color(p.on_primary),
            ]
            .spacing(6)
            .align_y(Vertical::Center),
        )
        .on_press(Msg::OpenAdd)
        .padding([12, 20])
        .style(move |_, _| button::Style {
            background: Some(p.primary.into()),
            border: iced::Border { radius: 28.0.into(), ..Default::default() },
            text_color: p.on_primary,
            ..Default::default()
        });

        let content = column![
            container(top_bar)
                .width(Length::Fill)
                .style(move |_| container::Style {
                    background: Some(p.surface.into()),
                    ..Default::default()
                }),
            hero,
            container(stats_area).width(Length::Fill).align_x(Horizontal::Center),
            vertical_space().height(8),
            scrollable(servers_list).height(Length::Fill),
            container(fab)
                .width(Length::Fill)
                .align_x(Horizontal::Right)
                .padding([12, 20]),
        ]
        .spacing(0);

        container(content)
            .width(Length::Fill)
            .height(Length::Fill)
            .style(Self::bg_style(p))
            .into()
    }

    fn view_servers_list(&self) -> Element<Msg> {
        let p = self.palette;
        if self.servers.is_empty() {
            return container(
                column![
                    vertical_space().height(32),
                    container(
                        text("Серверов нет. Нажмите + Добавить")
                            .size(14)
                            .color(p.on_surface_variant),
                    )
                    .width(Length::Fill)
                    .align_x(Horizontal::Center),
                ],
            )
            .into();
        }

        let mut col = Column::new().spacing(6).padding([0, 12, 0, 12]);

        for server in &self.servers {
            let active = self.selected.as_deref() == Some(server.id.as_str());
            let ping_val = self.pings.get(&server.id);
            let ping_text = match ping_val {
                Some(r) if r.busy => "…".to_string(),
                Some(r) => r.best().map(|v| format!("{} мс", v)).unwrap_or_else(|| "—".to_string()),
                None => String::new(),
            };

            let bg = if active { p.primary_container } else { p.surface };
            let fg = if active { p.on_primary_container } else { p.on_surface };
            let id = server.id.clone();

            let proto = if server.conf.starts_with("vless://") {
                "VLESS"
            } else if server.conf.starts_with("vmess://") {
                "VMess"
            } else if server.conf.starts_with("trojan://") {
                "Trojan"
            } else if server.conf.starts_with("ss://") {
                "SS"
            } else if server.conf.contains("[Interface]") || server.conf.contains("[interface]") {
                "WG"
            } else {
                "?"
            };

            let ping_id = server.id.clone();
            let card = button(
                row![
                    column![
                        text(server.name.as_str()).size(15).color(fg),
                        row![
                            text(proto).size(11).color(p.secondary),
                            text("  ").size(11),
                            text(server.endpoint.as_str()).size(11).color(p.on_surface_variant),
                        ]
                        .spacing(0),
                    ]
                    .spacing(3),
                    horizontal_space(),
                    column![
                        if !ping_text.is_empty() {
                            text(ping_text.as_str()).size(11).color(p.on_surface_variant)
                        } else {
                            text("").size(11)
                        },
                        button(text("⚡").size(11).color(p.on_surface_variant))
                            .on_press(Msg::PingServer(ping_id))
                            .padding([2, 4])
                            .style(|_, _| button::Style { background: None, ..Default::default() }),
                    ]
                    .spacing(2)
                    .align_x(Horizontal::Center),
                ]
                .align_y(Vertical::Center)
                .padding([12, 14]),
            )
            .width(Length::Fill)
            .on_press(Msg::SelectServer(id))
            .style(move |_, _| button::Style {
                background: Some(bg.into()),
                border: iced::Border { radius: 18.0.into(), ..Default::default() },
                text_color: fg,
                ..Default::default()
            });
            col = col.push(card);
        }
        col.into()
    }

    fn view_add(&self) -> Element<Msg> {
        let p = self.palette;
        let content = column![
            container(
                row![
                    button(text("← Назад").size(14).color(p.primary))
                        .on_press(Msg::CloseAdd)
                        .style(|_, _| button::Style { background: None, ..Default::default() }),
                    horizontal_space(),
                    text("Добавить").size(18).color(p.on_surface),
                    horizontal_space(),
                ]
                .align_y(Vertical::Center)
            )
            .padding([12, 16])
            .width(Length::Fill)
            .style(move |_| container::Style {
                background: Some(p.surface.into()),
                ..Default::default()
            }),
            scrollable(
                column![
                    vertical_space().height(16),
                    text("Ссылка, подписка или конфиг").size(13).color(p.on_surface_variant),
                    vertical_space().height(8),
                    text_input(
                        "vless://, vmess://, ss://, trojan://, wireguard://…",
                        &self.add_text,
                    )
                    .on_input(Msg::AddInput)
                    .on_submit(Msg::AddSubmit)
                    .size(14)
                    .padding(16)
                    .style(move |_, _| text_input::Style {
                        background: p.surface.into(),
                        border: iced::Border {
                            color: p.outline,
                            width: 1.5,
                            radius: 18.0.into(),
                        },
                        icon: p.on_surface_variant,
                        placeholder: p.on_surface_variant,
                        value: p.on_surface,
                        selection: p.primary,
                    }),
                    if let Some(err) = &self.add_error {
                        text(err.as_str()).size(12).color(p.error).into()
                    } else {
                        vertical_space().height(4).into()
                    },
                    vertical_space().height(12),
                    button(
                        container(if self.add_loading {
                            text("Загрузка…").size(15).color(p.on_primary)
                        } else {
                            text("Добавить").size(15).color(p.on_primary)
                        })
                        .width(Length::Fill)
                        .align_x(Horizontal::Center)
                        .padding([15, 0]),
                    )
                    .width(Length::Fill)
                    .on_press(Msg::AddSubmit)
                    .style(move |_, _| button::Style {
                        background: Some(p.primary.into()),
                        border: iced::Border { radius: 18.0.into(), ..Default::default() },
                        text_color: p.on_primary,
                        ..Default::default()
                    }),
                ]
                .spacing(0)
                .padding([0, 20, 32, 20])
            )
            .height(Length::Fill),
        ]
        .spacing(0);

        container(content)
            .width(Length::Fill)
            .height(Length::Fill)
            .style(Self::bg_style(p))
            .into()
    }

    fn view_settings(&self) -> Element<Msg> {
        let p = self.palette;
        let themes = ["Системная", "Светлая", "Тёмная"];
        let dns_presets = ["Cloudflare", "Google", "Quad9", "AdGuard", "Свой"];

        let chip_style = move |sel: bool| {
            move |_, _: button::Status| button::Style {
                background: Some(
                    if sel { p.secondary_container } else { p.surface }.into(),
                ),
                border: iced::Border {
                    radius: 16.0.into(),
                    color: if sel { Color::TRANSPARENT } else { p.outline },
                    width: if sel { 0.0 } else { 1.0 },
                },
                text_color: if sel { p.on_secondary_container } else { p.on_surface },
                ..Default::default()
            }
        };

        let mut theme_row = Row::new().spacing(8);
        for (i, name) in themes.iter().enumerate() {
            let sel = i == self.settings_theme;
            theme_row = theme_row.push(
                button(text(*name).size(13).color(if sel { p.on_secondary_container } else { p.on_surface }))
                    .padding([8, 14])
                    .on_press(Msg::SetTheme(i))
                    .style(chip_style(sel)),
            );
        }

        let mut dns_row = Row::new().spacing(8);
        for (i, name) in dns_presets.iter().enumerate() {
            let sel = i == self.settings_dns_preset;
            dns_row = dns_row.push(
                button(text(*name).size(13).color(if sel { p.on_secondary_container } else { p.on_surface }))
                    .padding([8, 14])
                    .on_press(Msg::SetDnsPreset(i))
                    .style(chip_style(sel)),
            );
        }

        let content = column![
            container(
                row![
                    button(text("← Назад").size(14).color(p.primary))
                        .on_press(Msg::CloseSettings)
                        .style(|_, _| button::Style { background: None, ..Default::default() }),
                    horizontal_space(),
                    text("Настройки").size(18).color(p.on_surface),
                    horizontal_space(),
                ]
                .align_y(Vertical::Center)
            )
            .padding([12, 16])
            .width(Length::Fill)
            .style(move |_| container::Style {
                background: Some(p.surface.into()),
                ..Default::default()
            }),
            scrollable(
                column![
                    self.settings_section(p, "Тема", theme_row.into()),
                    self.settings_section(p, "DNS", {
                        let dns_content: Element<Msg> = if self.settings_dns_preset == 4 {
                            column![
                                dns_row,
                                text_input("1.1.1.1, 8.8.8.8", &self.settings_dns_custom)
                                    .on_input(Msg::SetDnsCustom)
                                    .size(14)
                                    .padding(12),
                            ]
                            .spacing(10)
                            .into()
                        } else {
                            dns_row.into()
                        };
                        dns_content
                    }),
                    vertical_space().height(16),
                    container(
                        column![
                            text("FasdeqVPN").size(18).color(p.on_primary_container),
                            text("Версия 1.6").size(13).color(p.on_primary_container),
                        ]
                        .spacing(4),
                    )
                    .width(Length::Fill)
                    .padding([20, 22])
                    .style(move |_| container::Style {
                        background: Some(p.primary_container.into()),
                        border: iced::Border { radius: 28.0.into(), ..Default::default() },
                        ..Default::default()
                    }),
                    vertical_space().height(32),
                ]
                .spacing(16)
                .padding([16, 16, 32, 16])
            )
            .height(Length::Fill),
        ]
        .spacing(0);

        container(content)
            .width(Length::Fill)
            .height(Length::Fill)
            .style(Self::bg_style(p))
            .into()
    }

    fn settings_section<'a>(&self, p: Palette, title: &'a str, content: Element<'a, Msg>) -> Element<'a, Msg> {
        column![
            text(title).size(13).color(p.on_surface_variant),
            container(content)
                .width(Length::Fill)
                .padding([14, 16])
                .style(move |_| container::Style {
                    background: Some(p.surface.into()),
                    border: iced::Border { radius: 20.0.into(), ..Default::default() },
                    ..Default::default()
                }),
        ]
        .spacing(6)
        .into()
    }

    pub fn subscription(&self) -> Subscription<Msg> {
        iced::time::every(std::time::Duration::from_secs(1)).map(|_| Msg::Second)
    }

    pub fn theme(&self) -> Theme {
        match self.settings_theme {
            1 => Theme::Light,
            2 => Theme::Dark,
            _ => Theme::default(),
        }
    }
}

pub fn format_bytes(bytes: u64) -> String {
    let units = ["Б", "КБ", "МБ", "ГБ"];
    let mut value = bytes as f64;
    let mut i = 0;
    while value >= 1024.0 && i < units.len() - 1 {
        value /= 1024.0;
        i += 1;
    }
    if i == 0 {
        format!("{} Б", bytes)
    } else {
        format!("{:.1} {}", value, units[i])
    }
}

pub fn run() -> iced::Result {
    iced::application("FasdeqVPN", App::update, App::view)
        .subscription(App::subscription)
        .theme(App::theme)
        .window_size((420.0, 780.0))
        .run_with(App::new)
}

#[cfg(target_os = "android")]
#[no_mangle]
fn android_main(app: iced::winit::platform::android::activity::AndroidApp) {
    use android_logger::Config;
    android_logger::init_once(Config::default().with_max_level(log::LevelFilter::Info));
    iced::application("FasdeqVPN", App::update, App::view)
        .subscription(App::subscription)
        .theme(App::theme)
        .run_with(App::new)
        .expect("iced android run");
}
