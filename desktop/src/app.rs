use crate::anim::{self, Spring, Tween};
use crate::draw::TAU;
use crate::conflict;
use crate::importer;
use crate::links;
use crate::model::*;
use crate::ping;
use crate::store::{now_millis, Store};
use crate::theme::{self, Scheme};
use crate::tray::{self, TrayCmd};
use crate::xray::{self, CoreInfo, Release};
use fasdeq_core::model::{AnimConfig, Tuning};
use iced::widget::text_editor;
use iced::{event, keyboard, mouse, window, Event, Point, Size, Subscription, Task};
use std::collections::{HashMap, HashSet};
use std::future::Future;
use std::path::PathBuf;
use std::sync::{Arc, Mutex};
use std::time::{Duration, Instant};

#[derive(Debug)]
pub struct Conn {
    pub proc: Mutex<Option<xray::Proc>>,
    pub api: u16,
    pub bin: PathBuf,
    pub server_id: String,
}

#[derive(Clone, Debug)]
pub struct ImportItem {
    pub link: String,
    pub group: String,
    pub url: String,
}

#[derive(Clone, Debug)]
pub enum Confirm {
    Selected,
    DeleteGroup(String),
    ClearGroup(String),
    Reset,
    Conflict(String),
}

#[derive(Clone, Debug)]
pub enum RenameTarget {
    Server(String),
    Group(String),
}

#[derive(Clone, Debug)]
pub enum Sheet {
    Add,
    Settings,
    Cores,
}

#[derive(Clone, Debug)]
pub enum Message {
    Frame(Instant),
    Second,
    Window(Option<window::Id>),
    CloseRequested(window::Id),
    Resized(Size),
    Cursor(Point),
    Escape,
    Tray(TrayCmd),
    Power,
    PowerDown(bool),
    Connected(Result<Arc<Conn>, String>),
    Disconnected,
    Stats(bool, Option<(u64, u64)>),
    Scrolled(f32),
    CardDown(String),
    CardUp(String),
    CardCancel,
    CardRight(String),
    HeaderTap(String),
    SelectionClear,
    SelectionAll,
    AskDeleteSelected,
    Ask(Confirm),
    ConfirmApply,
    ConfirmCancel,
    RenameOpen(RenameTarget, String),
    RenameInput(String),
    RenameApply,
    RenameCancel,
    MenuOpen(String),
    MenuClose,
    PingServer(String),
    PingGroup(String),
    PingAll,
    PingDone(String, PingResult),
    CheckAll,
    CheckDone(String, Result<[i32; 3], String>),
    RefreshGroup(String),
    SheetOpen(Sheet),
    SheetClose(Sheet),
    AddEdit(text_editor::Action),
    AddPaste,
    Pasted(Option<String>),
    AddFile,
    FilesPicked(Vec<(String, String)>),
    AddSubmit,
    Gathered(Result<Vec<ImportItem>, String>, String),
    SetTheme(usize),
    SetPalette(usize),
    SetBg(usize),
    SetContrast(usize),
    SetAccent(usize),
    SetLaunch(usize),
    SetAnim(usize),
    SetPowerScale(f32),
    SetTextScale(f32),
    SetAmoled(bool),
    SetBurnIn(bool),
    SetAutoConnect(bool),
    SetDnsPreset(usize),
    SetDnsCustom(String),
    SetMtu(usize),
    SetPingMode(usize),
    SetPingTimeout(usize),
    SetAutoPing(bool),
    SetSortPing(bool),
    SetAutoCheck(bool),
    SetInterval(String),
    SetCloseToTray(bool),
    SetAutostart(bool),
    CoreSelect(String),
    CoreDelete(String),
    CoreFile,
    CoreFilePicked(Option<PathBuf>),
    CoreReleases,
    CoreReleasesLoaded(Result<Vec<Release>, String>),
    CoreDownload(Release),
    CoreDone(Result<CoreInfo, String>),
    OpenUrl(String),
    ConflictChecked(String, Vec<String>),
    Env(Option<bool>, Option<Vec<i32>>),
    SetDynamic(bool),
    PasteInto(u8),
    PastedInto(u8, String),
    SetAnimCfg(AnimConfig),
    SetTuning(Tuning),
    SwitchUi(String),
    Noop,
}

pub enum Entry {
    Header {
        key: String,
        name: String,
        ids: Vec<String>,
        url: String,
        ungrouped: bool,
    },
    Row(Server),
}

pub struct Anims {
    pub collapse: Spring,
    pub rest: Spring,
    pub press: Spring,
    pub icon_scale: Spring,
    pub amp: Spring,
    pub speed: Tween,
    pub orbit: Tween,
    pub aura: Tween,
    pub level: Tween,
    pub burst: Tween,
    pub glow: Tween,
    pub on_t: Tween,
    pub wave: Tween,
    pub sheets: [Spring; 3],
    pub phase: f32,
    pub backdrop: f32,
    pub time: f32,
    pub cards: HashMap<String, Spring>,
}

impl Anims {
    fn new() -> Anims {
        let mut burst = Tween::new(1.0, 1200.0);
        burst.snap(1.0);
        let mut wave = Tween::new(1.0, 1500.0);
        wave.snap(1.0);
        Anims {
            collapse: Spring::new(0.0, 0.8, 300.0),
            rest: Spring::new(0.96, 0.4, 90.0),
            press: Spring::new(1.0, 0.35, 380.0),
            icon_scale: Spring::new(1.0, 0.35, 180.0),
            amp: Spring::new(0.0, 0.4, 50.0),
            speed: Tween::new(0.12, 1000.0),
            orbit: Tween::new(0.0, 600.0),
            aura: Tween::new(0.0, 900.0),
            level: Tween::new(0.0, 1400.0),
            burst,
            glow: Tween::new(0.0, 900.0),
            on_t: Tween::new(0.0, 700.0),
            wave,
            sheets: [
                Spring::new(0.0, 0.85, 400.0),
                Spring::new(0.0, 0.85, 400.0),
                Spring::new(0.0, 0.85, 400.0),
            ],
            phase: 0.0,
            backdrop: 0.0,
            time: 0.0,
            cards: HashMap::new(),
        }
    }
}

pub struct App {
    pub servers: Vec<Server>,
    pub groups: Vec<Group>,
    pub selected: Option<String>,
    pub s: Settings,
    pub pings: HashMap<String, PingResult>,
    pub checks: HashMap<String, ServiceCheck>,
    pub check_pending: usize,
    pub checked_at: u64,
    pub status: Status,
    pub error: Option<String>,
    pub error_age: u32,
    pub active_id: Option<String>,
    pub rx: u64,
    pub tx: u64,
    pub down_speed: u64,
    pub up_speed: u64,
    pub uptime: u64,
    pub since: Option<Instant>,
    pub conn: Option<Arc<Conn>>,
    pub pending_switch: Option<String>,
    pub selection: HashSet<String>,
    pub collapsed: HashSet<String>,
    pub refreshing: Option<String>,
    pub sheet: [bool; 3],
    pub confirm: Option<Confirm>,
    pub rename: Option<(RenameTarget, String)>,
    pub menu: Option<(String, Point)>,
    pub add_text: text_editor::Content,
    pub add_error: Option<String>,
    pub add_loading: bool,
    pub cores: Vec<CoreInfo>,
    pub releases: Option<Vec<Release>>,
    pub core_busy: Option<String>,
    pub core_msg: Option<String>,
    pub core_list_open: bool,
    pub window: Option<window::Id>,
    pub visible: bool,
    pub win_size: Size,
    pub cursor: Point,
    pub scroll_y: f32,
    pub press: Option<(String, Instant, bool)>,
    pub anims: Anims,
    pub last_frame: Option<Instant>,
    pub interval_text: String,
    pub reset_tick: u32,
    pub burn_idx: usize,
    pub burn_secs: u32,
    pub system_dark: bool,
    pub scheme: Scheme,
    pub auto_started: bool,
    pub conflict_id: Option<String>,
    pub sys_colors: Option<Vec<i32>>,
    pub env_tick: u32,
}

pub fn blocking<T: Send + 'static>(f: impl FnOnce() -> T + Send + 'static) -> impl Future<Output = T> {
    async move { tokio::task::spawn_blocking(f).await.expect("blocking task") }
}

fn short_id() -> String {
    format!("fq{:x}", rand::random::<u32>() % 0xEFFFFF + 0x100000)
}

impl App {
    pub fn boot() -> (App, Task<Message>) {
        let store = Store::load();
        let s = store.settings.clone();
        anim::set_scale(MOTION_FACTORS[s.anim_speed.min(MOTION_FACTORS.len() - 1)]);
        let system_dark = theme::system_is_dark();
        let scheme = theme::build(&s, system_dark, None);
        let mut checks = HashMap::new();
        for server in &store.servers {
            if let Some(v) = store.checks.get(&server.id) {
                checks.insert(
                    server.id.clone(),
                    ServiceCheck {
                        ms: [
                            if v[0] == 0 { None } else { Some(v[0]) },
                            if v[1] == 0 { None } else { Some(v[1]) },
                            if v[2] == 0 { None } else { Some(v[2]) },
                        ],
                        busy: false,
                    },
                );
            }
        }
        let selected = store
            .selected
            .clone()
            .filter(|id| store.servers.iter().any(|x| &x.id == id))
            .or_else(|| store.servers.first().map(|x| x.id.clone()));
        let interval_text = format!("{}", s.check_interval).trim_end_matches(".0").to_string();
        let mut app = App {
            servers: store.servers,
            groups: store.groups,
            selected,
            s,
            pings: HashMap::new(),
            checks,
            check_pending: 0,
            checked_at: store.checked_at,
            status: Status::Off,
            error: None,
            error_age: 0,
            active_id: None,
            rx: 0,
            tx: 0,
            down_speed: 0,
            up_speed: 0,
            uptime: 0,
            since: None,
            conn: None,
            pending_switch: None,
            selection: HashSet::new(),
            collapsed: HashSet::new(),
            refreshing: None,
            sheet: [false; 3],
            confirm: None,
            rename: None,
            menu: None,
            add_text: text_editor::Content::new(),
            add_error: None,
            add_loading: false,
            cores: xray::list(),
            releases: None,
            core_busy: None,
            core_msg: None,
            core_list_open: false,
            window: None,
            visible: true,
            win_size: Size::new(460.0, 900.0),
            cursor: Point::ORIGIN,
            scroll_y: 0.0,
            press: None,
            anims: Anims::new(),
            last_frame: None,
            interval_text,
            reset_tick: 0,
            burn_idx: 0,
            burn_secs: 0,
            system_dark,
            scheme,
            auto_started: false,
            conflict_id: None,
            sys_colors: None,
            env_tick: 0,
        };
        if !app.s.active_core.is_empty() && !app.cores.iter().any(|c| c.id == app.s.active_core) {
            app.s.active_core.clear();
        }
        let (id, open) = window::open(window_settings(app.win_size));
        app.window = Some(id);
        let mut tasks = vec![open.map(|_| Message::Noop)];
        if app.s.auto_ping && !app.servers.is_empty() {
            tasks.push(app.ping_tasks(app.servers.clone()));
        }
        (app, Task::batch(tasks))
    }

    pub fn title(&self) -> String {
        "FasdeqVPN".to_string()
    }

    pub fn subscription(&self) -> Subscription<Message> {
        let mut subs = vec![
            iced::time::every(Duration::from_secs(1)).map(|_| Message::Second),
            window::close_requests().map(Message::CloseRequested),
            window::resize_events().map(|(_, s)| Message::Resized(s)),
            Subscription::run(tray::stream).map(Message::Tray),
            event::listen_with(|e, _, _| match e {
                Event::Mouse(mouse::Event::CursorMoved { position }) => Some(Message::Cursor(position)),
                Event::Keyboard(keyboard::Event::KeyPressed {
                    key: keyboard::Key::Named(keyboard::key::Named::Escape),
                    ..
                }) => Some(Message::Escape),
                Event::Keyboard(keyboard::Event::KeyPressed {
                    key: keyboard::Key::Named(keyboard::key::Named::BrowserBack),
                    ..
                }) => Some(Message::Escape),
                _ => None,
            }),
        ];
        if self.visible {
            let fast = self.status != Status::Off
                || self.anims.wave.value < 1.0
                || self.press.is_some()
                || self.sheet.iter().any(|x| *x)
                || self.anims.sheets.iter().any(|x| !x.settled());
            if fast {
                subs.push(window::frames().map(Message::Frame));
            } else {
                subs.push(iced::time::every(Duration::from_millis(40)).map(Message::Frame));
            }
        }
        Subscription::batch(subs)
    }

    pub fn save(&self) {
        let mut checks = HashMap::new();
        for (id, c) in &self.checks {
            checks.insert(
                id.clone(),
                [c.ms[0].unwrap_or(0), c.ms[1].unwrap_or(0), c.ms[2].unwrap_or(0)],
            );
        }
        Store {
            servers: self.servers.clone(),
            groups: self.groups.clone(),
            selected: self.selected.clone(),
            settings: self.s.clone(),
            checks,
            checked_at: self.checked_at,
        }
        .save();
    }

    pub fn selected_server(&self) -> Option<&Server> {
        let id = self.selected.as_ref()?;
        self.servers.iter().find(|s| &s.id == id)
    }

    pub fn best_for(&self, index: usize) -> Option<String> {
        let mut best_id = None;
        let mut best = i32::MAX;
        for (id, check) in &self.checks {
            if let Some(v) = check.ms[index] {
                if v > 0 && v < best {
                    best = v;
                    best_id = Some(id.clone());
                }
            }
        }
        best_id
    }

    pub fn checking(&self) -> bool {
        self.check_pending > 0
    }

    pub fn entries(&self) -> Vec<Entry> {
        let order = |list: Vec<Server>| -> Vec<Server> {
            if self.s.sort_ping {
                let mut l = list;
                l.sort_by_key(|x| self.pings.get(&x.id).map(|p| p.best()).unwrap_or(i32::MAX));
                l
            } else {
                list
            }
        };
        let names: HashSet<&str> = self.groups.iter().map(|g| g.name.as_str()).collect();
        let loose: Vec<Server> = self
            .servers
            .iter()
            .filter(|s| s.group.is_empty() || !names.contains(s.group.as_str()))
            .cloned()
            .collect();
        let mut result = Vec::new();
        if !loose.is_empty() {
            if !names.is_empty() {
                let key = "g:\u{0}".to_string();
                result.push(Entry::Header {
                    key: key.clone(),
                    name: String::new(),
                    ids: loose.iter().map(|s| s.id.clone()).collect(),
                    url: String::new(),
                    ungrouped: true,
                });
                if !self.collapsed.contains(&key) {
                    for s in order(loose) {
                        result.push(Entry::Row(s));
                    }
                }
            } else {
                for s in order(loose) {
                    result.push(Entry::Row(s));
                }
            }
        }
        for group in &self.groups {
            let members: Vec<Server> = self
                .servers
                .iter()
                .filter(|s| s.group == group.name)
                .cloned()
                .collect();
            let key = format!("g:{}", group.name);
            result.push(Entry::Header {
                key: key.clone(),
                name: group.name.clone(),
                ids: members.iter().map(|s| s.id.clone()).collect(),
                url: group.url.clone(),
                ungrouped: false,
            });
            if !self.collapsed.contains(&key) {
                for s in order(members) {
                    result.push(Entry::Row(s));
                }
            }
        }
        result
    }

    pub fn hero_height(&self) -> f32 {
        let ts = self.s.text_scale;
        256.0 * self.s.power_scale + (36.0 + 24.0) * ts + 12.0 + 40.0 + 20.0
    }

    fn can_collapse(&self) -> bool {
        let rows = self.entries().len() as f32;
        rows * 84.0 + 120.0 > self.win_size.height - 200.0
    }

    pub fn sheet_t(&self, s: usize) -> f32 {
        self.anims.sheets[s].x
    }

    fn set_error(&mut self, msg: impl Into<String>) {
        self.error = Some(msg.into());
        self.error_age = 0;
    }

    fn ping_tasks(&mut self, list: Vec<Server>) -> Task<Message> {
        if list.is_empty() {
            return Task::none();
        }
        let mode = self.s.ping_mode;
        let timeout = PING_TIMEOUTS[self.s.ping_timeout.min(PING_TIMEOUTS.len() - 1)];
        for s in &list {
            self.pings.insert(
                s.id.clone(),
                PingResult {
                    busy: true,
                    ..Default::default()
                },
            );
        }
        Task::batch(list.into_iter().map(|server| {
            let id = server.id.clone();
            Task::perform(
                blocking(move || ping::measure(&server, mode, timeout)),
                move |r| Message::PingDone(id.clone(), r),
            )
        }))
    }

    fn check_tasks(&mut self) -> Task<Message> {
        if self.checking() || self.servers.is_empty() {
            return Task::none();
        }
        let Some(bin) = xray::find_binary(&self.s.active_core) else {
            self.set_error("Ядро Xray не найдено. Скачайте его в настройках");
            return Task::none();
        };
        self.checks.clear();
        let list = self.servers.clone();
        for s in &list {
            self.checks.insert(
                s.id.clone(),
                ServiceCheck {
                    busy: true,
                    ..Default::default()
                },
            );
        }
        self.check_pending = list.len();
        let sem = Arc::new(tokio::sync::Semaphore::new(8));
        Task::batch(list.into_iter().map(|server| {
            let sem = sem.clone();
            let bin = bin.clone();
            let id = server.id.clone();
            Task::perform(
                async move {
                    let _permit = sem.acquire_owned().await.ok();
                    blocking(move || xray::check_server(&bin, &server.conf, 12)).await
                },
                move |r| Message::CheckDone(id.clone(), r),
            )
        }))
    }

    fn connect(&mut self, id: &str) -> Task<Message> {
        let Some(server) = self.servers.iter().find(|s| s.id == id).cloned() else {
            return Task::none();
        };
        self.error = None;
        self.status = Status::Connecting;
        self.active_id = Some(server.id.clone());
        let active = self.s.active_core.clone();
        let dns = self.s.dns_list();
        if cfg!(mobile) {
            let tuning = self.s.tuning.clone();
            let mtu = self.s.mtu;
            let _ = &active;
            return Task::perform(
                blocking(move || -> Result<Arc<Conn>, String> {
                    let mut parsed = links::parse(&server.conf)?;
                    links::resolve(&mut parsed.outbound);
                    let payload = serde_json::json!({
                        "outbound": parsed.outbound.to_string(),
                        "dns": dns.join(","),
                        "mtu": mtu,
                        "tuning": serde_json::to_value(&tuning).unwrap_or_default().to_string(),
                        "name": server.name,
                        "id": server.id,
                    });
                    let proc = xray::start_vpn(&payload)?;
                    Ok(Arc::new(Conn {
                        proc: Mutex::new(Some(proc)),
                        api: 0,
                        bin: PathBuf::new(),
                        server_id: server.id.clone(),
                    }))
                }),
                Message::Connected,
            );
        }
        Task::perform(
            blocking(move || -> Result<Arc<Conn>, String> {
                let bin = xray::find_binary(&active)
                    .ok_or("Ядро Xray не найдено. Скачайте его в настройках")?;
                let mut parsed = links::parse(&server.conf)?;
                links::resolve(&mut parsed.outbound);
                let socks = xray::free_port();
                let http = xray::free_port();
                let api = xray::free_port();
                let cfg = links::inner_config(parsed.outbound, socks, http, Some(api), &dns);
                let proc = xray::start(&bin, &cfg, socks)?;
                xray::set_system_proxy(http, socks);
                Ok(Arc::new(Conn {
                    proc: Mutex::new(Some(proc)),
                    api,
                    bin,
                    server_id: server.id.clone(),
                }))
            }),
            Message::Connected,
        )
    }

    fn disconnect(&mut self) -> Task<Message> {
        if self.status != Status::On && self.status != Status::Connecting {
            return Task::none();
        }
        self.status = Status::Stopping;
        let conn = self.conn.take();
        Task::perform(
            blocking(move || {
                if let Some(c) = conn {
                    if let Some(mut p) = c.proc.lock().unwrap().take() {
                        p.stop();
                    }
                }
                xray::clear_system_proxy();
            }),
            |_| Message::Disconnected,
        )
    }

    fn finish_down(&mut self, message: Option<String>) {
        self.status = Status::Off;
        self.conn = None;
        self.active_id = None;
        self.down_speed = 0;
        self.up_speed = 0;
        self.rx = 0;
        self.tx = 0;
        self.uptime = 0;
        self.since = None;
        if let Some(m) = message {
            self.set_error(m);
        }
    }

    fn quit(&mut self) -> Task<Message> {
        let conn = self.conn.take();
        self.save();
        Task::perform(
            blocking(move || {
                if let Some(c) = conn {
                    if let Some(mut p) = c.proc.lock().unwrap().take() {
                        p.stop();
                    }
                    xray::clear_system_proxy();
                }
            }),
            |_| (),
        )
        .then(|_| iced::exit())
    }

    fn show_window(&mut self) -> Task<Message> {
        self.visible = true;
        match self.window {
            Some(id) => Task::batch([window::minimize(id, false), window::gain_focus(id)]),
            None => {
                let (id, open) = window::open(window_settings(self.win_size));
                self.window = Some(id);
                open.map(|_| Message::Noop)
            }
        }
    }

    fn rescheme(&mut self) {
        self.scheme = theme::build(&self.s, self.system_dark, self.sys_colors.as_deref());
    }

    fn ensure_group(&mut self, name: &str, url: &str) {
        match self.groups.iter().position(|g| g.name == name) {
            None => self.groups.push(Group {
                name: name.to_string(),
                url: url.to_string(),
            }),
            Some(i) => {
                if !url.is_empty() && self.groups[i].url != url {
                    self.groups[i].url = url.to_string();
                }
            }
        }
    }

    fn remove_many(&mut self, ids: &HashSet<String>) -> Task<Message> {
        if ids.is_empty() {
            return Task::none();
        }
        let mut task = Task::none();
        if let Some(active) = &self.active_id {
            if ids.contains(active) && (self.status == Status::On || self.status == Status::Connecting) {
                task = self.disconnect();
            }
        }
        self.servers.retain(|s| !ids.contains(&s.id));
        for id in ids {
            self.pings.remove(id);
            self.checks.remove(id);
        }
        if self.selected.as_ref().map(|s| ids.contains(s)).unwrap_or(false) {
            self.selected = self.servers.first().map(|s| s.id.clone());
        }
        self.save();
        task
    }

    fn apply_import(&mut self, items: Vec<ImportItem>, name_hint: &str) -> Option<String> {
        let mut added = 0;
        let mut failure: Option<String> = None;
        let mut fresh = Vec::new();
        let total = items.len();
        for item in items {
            match links::parse(&item.link) {
                Ok(parsed) => {
                    let existing = self.servers.iter().position(|s| s.conf == item.link);
                    match existing {
                        None => {
                            let hint = if total == 1 { name_hint } else { "" };
                            let mut title = if !hint.is_empty() {
                                hint.to_string()
                            } else {
                                parsed.name.clone()
                            };
                            if title.is_empty() {
                                title = format!("Сервер {}", self.servers.len() + 1);
                            }
                            let id = short_id();
                            let server = Server {
                                id: id.clone(),
                                name: title,
                                conf: item.link.clone(),
                                endpoint: parsed.endpoint.clone(),
                                group: item.group.trim().to_string(),
                            };
                            self.servers.push(server.clone());
                            fresh.push(server);
                            if self.selected.is_none() {
                                self.selected = Some(id);
                            }
                        }
                        Some(i) => {
                            if !item.group.trim().is_empty() && self.servers[i].group != item.group.trim() {
                                self.servers[i].group = item.group.trim().to_string();
                            }
                        }
                    }
                    if !item.group.trim().is_empty() {
                        self.ensure_group(item.group.trim(), &item.url);
                    }
                    added += 1;
                }
                Err(e) => {
                    if failure.is_none() {
                        failure = Some(e);
                    }
                }
            }
        }
        self.save();
        if added == 0 {
            Some(failure.unwrap_or_else(|| "Не удалось распознать".into()))
        } else {
            None
        }
    }

    fn gather(
        &self,
        text: String,
        name_hint: String,
        group_hint: String,
        force_group: Option<String>,
        force_url: String,
    ) -> Task<Message> {
        Task::perform(
            blocking(move || -> Result<Vec<ImportItem>, String> {
                let input = text.trim().to_string();
                if input.is_empty() {
                    return Err("Вставьте ссылку или конфигурацию".into());
                }
                let prepared = importer::prepare(&input);
                if let Some(e) = prepared.error {
                    return Err(e);
                }
                let mut items = Vec::new();
                let mut direct = Vec::new();
                for source in prepared.items {
                    if source.is_url {
                        let fetched = links::fetch_meta(&source.value)
                            .map_err(|e| format!("Не удалось загрузить список: {}", e))?;
                        let group_name = match &force_group {
                            Some(g) => g.clone(),
                            None => {
                                if source.name.is_empty() {
                                    fetched.group.clone()
                                } else {
                                    source.name.clone()
                                }
                            }
                        };
                        let body = links::decode_subscription(&fetched.body);
                        for link in importer::prepare(&body).items {
                            if link.is_url {
                                continue;
                            }
                            items.push(ImportItem {
                                link: link.value,
                                group: group_name.clone(),
                                url: if force_url.is_empty() {
                                    source.value.clone()
                                } else {
                                    force_url.clone()
                                },
                            });
                        }
                    } else {
                        direct.push(source.value);
                    }
                }
                let direct_group = match &force_group {
                    Some(g) => g.clone(),
                    None => {
                        if direct.len() > 1 {
                            group_hint.clone()
                        } else {
                            String::new()
                        }
                    }
                };
                for link in direct {
                    items.push(ImportItem {
                        link,
                        group: direct_group.clone(),
                        url: force_url.clone(),
                    });
                }
                Ok(items)
            }),
            move |r| Message::Gathered(r, name_hint.clone()),
        )
    }

    fn close_top(&mut self) -> bool {
        if self.confirm.is_some() {
            self.confirm = None;
            self.conflict_id = None;
            return true;
        }
        if self.rename.is_some() {
            self.rename = None;
            return true;
        }
        if self.menu.is_some() {
            self.menu = None;
            return true;
        }
        for i in [2usize, 1, 0] {
            if self.sheet[i] {
                self.sheet[i] = false;
                return true;
            }
        }
        if !self.selection.is_empty() {
            self.selection.clear();
            return true;
        }
        false
    }

    fn step_anims(&mut self, dt: f32) {
        let a = &mut self.anims;
        let on = self.status == Status::On;
        let connecting = self.status == Status::Connecting;
        let stopping = self.status == Status::Stopping;
        let busy = connecting || stopping;
        let cfg = self.s.anim.clone();
        let live = cfg.enabled;
        let k = if live { cfg.intensity.clamp(0.0, 2.0) } else { 0.0 };
        let tempo = if live { cfg.tempo.clamp(0.2, 3.0) } else { 0.0 };
        a.amp.to(
            (if connecting {
                0.09
            } else if stopping {
                0.06
            } else if on && cfg.idle_motion {
                0.05
            } else {
                0.0
            }) * k,
        );
        a.speed.to(
            (if connecting {
                2.8
            } else if stopping {
                3.4
            } else if on && cfg.idle_motion {
                0.35
            } else if cfg.idle_motion {
                0.12
            } else {
                0.0
            }) * tempo,
        );
        a.orbit.to(if connecting && live { 1.0 } else { 0.0 });
        a.aura.to(if on && live && cfg.idle_motion { 1.0 } else { 0.0 });
        a.level.to(if on {
            1.0
        } else if connecting {
            0.55
        } else {
            0.0
        });
        a.icon_scale.to(if busy && live && cfg.icon_fx { 0.78 } else { 1.0 });
        a.rest.to(if live {
            if on {
                1.0 + 0.04 * k
            } else {
                1.0 - 0.04 * k
            }
        } else {
            1.0
        });
        if !on && a.burst.value < 1.0 {
            a.burst.snap(1.0);
        }
        a.glow.to(if on && self.s.bg_style < 2 { 0.22 } else { 0.0 });
        a.on_t.to(if on { 1.0 } else { 0.0 });
        let range = self.hero_height();
        let target = if self.can_collapse() {
            self.scroll_y.clamp(0.0, range)
        } else {
            0.0
        };
        let a = &mut self.anims;
        a.collapse.to(target);
        a.collapse.step(dt);
        a.rest.step(dt);
        a.press.step(dt);
        a.icon_scale.step(dt);
        a.amp.step(dt);
        a.speed.step(dt);
        a.orbit.step(dt);
        a.aura.step(dt);
        a.level.step(dt);
        a.burst.step(dt);
        a.glow.step(dt);
        a.on_t.step(dt);
        a.wave.step(dt);
        for i in 0..3 {
            let t = if self.sheet[i] { 1.0 } else { 0.0 };
            a.sheets[i].to(t);
            a.sheets[i].step(dt);
        }
        let scale = anim::scale();
        a.phase = (a.phase + dt * a.speed.value / scale) % (TAU * 50.0);
        a.backdrop = (a.backdrop + dt * TAU / (26.0 * scale)) % TAU;
        a.time += dt;
        let selected: Vec<String> = self.selection.iter().cloned().collect();
        let chosen = self.selected.clone();
        let active = self.active_id.clone();
        let on_now = on;
        let mut ids: Vec<String> = self.servers.iter().map(|s| s.id.clone()).collect();
        ids.retain(|id| {
            let target = selected.contains(id) || (chosen.as_deref() == Some(id.as_str()) && !on_now && active.is_none()) || chosen.as_deref() == Some(id.as_str());
            let sp = self.anims.cards.entry(id.clone()).or_insert_with(|| Spring::new(0.0, 0.55, 300.0));
            sp.to(if target { 1.0 } else { 0.0 });
            sp.step(dt);
            true
        });
    }

    pub fn update(&mut self, message: Message) -> Task<Message> {
        match message {
            Message::Frame(now) => {
                crate::platform::mark_ready();
                let dt = match self.last_frame {
                    Some(prev) => (now - prev).as_secs_f32().min(0.05),
                    None => 0.016,
                };
                self.last_frame = Some(now);
                self.step_anims(dt);
                if let Some((id, t, fired)) = self.press.clone() {
                    if !fired && now.duration_since(t) >= Duration::from_millis(450) {
                        self.selection.insert(id.clone());
                        self.press = Some((id, t, true));
                    }
                }
                for i in 0..3 {
                    if !self.sheet[i] && self.anims.sheets[i].x < 0.002 && self.anims.sheets[i].settled() {
                        self.anims.sheets[i].snap(0.0);
                    }
                }
                Task::none()
            }
            Message::Second => {
                let mut tasks = Vec::new();
                if self.error.is_some() {
                    self.error_age += 1;
                    if self.error_age > 8 {
                        self.error = None;
                    }
                }
                if self.s.burn_in {
                    self.burn_secs += 1;
                    if self.burn_secs >= 60 {
                        self.burn_secs = 0;
                        self.burn_idx = (self.burn_idx + 1) % 5;
                    }
                } else {
                    self.burn_idx = 0;
                }
                #[cfg(mobile)]
                {
                    self.env_tick += 1;
                    if self.env_tick % 5 == 1 {
                        tasks.push(Task::perform(
                            blocking(|| {
                                let dark = crate::bridge::system_dark();
                                let colors = crate::bridge::system_colors(dark.unwrap_or(false));
                                (dark, colors)
                            }),
                            |(d, c)| Message::Env(d, c),
                        ));
                    }
                }
                if self.status == Status::On {
                    if let Some(since) = self.since {
                        self.uptime = since.elapsed().as_secs();
                    }
                    if let Some(conn) = self.conn.clone() {
                        tasks.push(Task::perform(
                            blocking(move || {
                                let alive = conn
                                    .proc
                                    .lock()
                                    .unwrap()
                                    .as_mut()
                                    .map(|p| p.alive())
                                    .unwrap_or(false);
                                let stats = if alive {
                                    xray::query_stats(&conn.bin, conn.api)
                                } else {
                                    None
                                };
                                (alive, stats)
                            }),
                            |(alive, stats)| Message::Stats(alive, stats),
                        ));
                    }
                    if self.s.auto_check && !self.checking() && !self.servers.is_empty() {
                        let due = self.checked_at + (self.s.check_interval * 60_000.0) as u64;
                        if now_millis() >= due {
                            tasks.push(self.check_tasks());
                        }
                    }
                }
                Task::batch(tasks)
            }
            Message::Window(id) => {
                self.window = id;
                if self.s.auto_connect && !self.auto_started {
                    self.auto_started = true;
                    if let Some(id) = self.selected.clone() {
                        return self.begin_connect(&id);
                    }
                }
                Task::none()
            }
            Message::CloseRequested(id) => {
                if self.s.close_to_tray && tray::available() {
                    self.visible = false;
                    self.window = None;
                    window::close(id)
                } else {
                    self.quit()
                }
            }
            Message::Resized(size) => {
                self.win_size = size;
                Task::none()
            }
            Message::Cursor(p) => {
                self.cursor = p;
                Task::none()
            }
            Message::Escape => {
                self.close_top();
                Task::none()
            }
            Message::Tray(cmd) => match cmd {
                TrayCmd::Show => self.show_window(),
                TrayCmd::Toggle => match self.status {
                    Status::On => self.disconnect(),
                    Status::Off => match self.selected.clone() {
                        Some(id) => self.begin_connect(&id),
                        None => Task::none(),
                    },
                    _ => Task::none(),
                },
                TrayCmd::Quit => self.quit(),
            },
            Message::Power => {
                self.anims.press.to(1.0);
                match self.status {
                    Status::Off => match self.selected.clone() {
                        Some(id) => self.begin_connect(&id),
                        None => {
                            self.set_error("Сначала добавьте сервер");
                            Task::none()
                        }
                    },
                    Status::On => self.disconnect(),
                    _ => Task::none(),
                }
            }
            Message::PowerDown(down) => {
                let pressed = down && self.s.anim.enabled && self.s.anim.press;
                self.anims.press.to(if pressed { 0.88 } else { 1.0 });
                if down && cfg!(mobile) && self.s.anim.haptics {
                    crate::bridge::haptic(false);
                }
                Task::none()
            }
            Message::Connected(result) => {
                if self.status != Status::Connecting {
                    if let Ok(conn) = result {
                        return Task::perform(
                            blocking(move || {
                                if let Some(mut p) = conn.proc.lock().unwrap().take() {
                                    p.stop();
                                }
                                xray::clear_system_proxy();
                            }),
                            |_| Message::Noop,
                        );
                    }
                    return Task::none();
                }
                match result {
                    Ok(conn) => {
                        self.conn = Some(conn);
                        self.status = Status::On;
                        self.since = Some(Instant::now());
                        self.rx = 0;
                        self.tx = 0;
                        if self.s.anim.enabled && self.s.anim.burst {
                            self.anims.burst.snap(0.0);
                            self.anims.burst.to(1.0);
                        }
                        self.anims.wave.snap(0.0);
                        self.anims.wave.to(1.0);
                        if cfg!(mobile) && self.s.anim.haptics {
                            crate::bridge::haptic(true);
                        }
                        Task::none()
                    }
                    Err(e) => {
                        self.finish_down(Some(e));
                        Task::none()
                    }
                }
            }
            Message::Disconnected => {
                self.finish_down(None);
                if let Some(id) = self.pending_switch.take() {
                    return self.connect(&id);
                }
                Task::none()
            }
            Message::Stats(alive, stats) => {
                if self.status != Status::On {
                    return Task::none();
                }
                if !alive {
                    let conn = self.conn.take();
                    self.finish_down(Some("Ядро Xray остановилось".into()));
                    drop(conn);
                    return Task::perform(blocking(xray::clear_system_proxy), |_| Message::Noop);
                }
                if let Some((down, up)) = stats {
                    self.down_speed = down.saturating_sub(self.rx);
                    self.up_speed = up.saturating_sub(self.tx);
                    self.rx = down;
                    self.tx = up;
                }
                Task::none()
            }
            Message::Scrolled(y) => {
                self.scroll_y = y;
                Task::none()
            }
            Message::CardDown(id) => {
                self.press = Some((id, Instant::now(), false));
                Task::none()
            }
            Message::CardUp(id) => {
                let Some((pid, _, fired)) = self.press.take() else {
                    return Task::none();
                };
                if fired || pid != id {
                    return Task::none();
                }
                if !self.selection.is_empty() {
                    if !self.selection.remove(&id) {
                        self.selection.insert(id);
                    }
                    Task::none()
                } else {
                    self.select(&id)
                }
            }
            Message::CardCancel => {
                self.press = None;
                Task::none()
            }
            Message::CardRight(id) => {
                self.press = None;
                if !self.selection.remove(&id) {
                    self.selection.insert(id);
                }
                Task::none()
            }
            Message::HeaderTap(key) => {
                let entries = self.entries();
                for e in entries {
                    if let Entry::Header { key: k, ids, .. } = e {
                        if k == key {
                            if !self.selection.is_empty() {
                                if !ids.is_empty() && ids.iter().all(|i| self.selection.contains(i)) {
                                    for i in &ids {
                                        self.selection.remove(i);
                                    }
                                } else {
                                    for i in ids {
                                        self.selection.insert(i);
                                    }
                                }
                            } else if !self.collapsed.remove(&key) {
                                self.collapsed.insert(key.clone());
                            }
                            break;
                        }
                    }
                }
                Task::none()
            }
            Message::SelectionClear => {
                self.selection.clear();
                Task::none()
            }
            Message::SelectionAll => {
                self.selection = self.servers.iter().map(|s| s.id.clone()).collect();
                Task::none()
            }
            Message::AskDeleteSelected => {
                self.confirm = Some(Confirm::Selected);
                Task::none()
            }
            Message::Ask(c) => {
                self.menu = None;
                self.confirm = Some(c);
                Task::none()
            }
            Message::ConfirmCancel => {
                self.confirm = None;
                self.conflict_id = None;
                Task::none()
            }
            Message::ConfirmApply => {
                let Some(c) = self.confirm.take() else {
                    return Task::none();
                };
                match c {
                    Confirm::Selected => {
                        let ids = std::mem::take(&mut self.selection);
                        self.remove_many(&ids)
                    }
                    Confirm::ClearGroup(name) => {
                        let ids: HashSet<String> = self
                            .servers
                            .iter()
                            .filter(|s| s.group == name)
                            .map(|s| s.id.clone())
                            .collect();
                        self.remove_many(&ids)
                    }
                    Confirm::DeleteGroup(name) => {
                        let ids: HashSet<String> = self
                            .servers
                            .iter()
                            .filter(|s| s.group == name)
                            .map(|s| s.id.clone())
                            .collect();
                        let task = self.remove_many(&ids);
                        self.groups.retain(|g| g.name != name);
                        self.save();
                        task
                    }
                    Confirm::Reset => {
                        self.reset_settings();
                        Task::none()
                    }
                    Confirm::Conflict(_) => match self.conflict_id.take() {
                        Some(id) => self.connect(&id),
                        None => Task::none(),
                    },
                }
            }
            Message::RenameOpen(target, current) => {
                self.menu = None;
                self.rename = Some((target, current));
                Task::none()
            }
            Message::RenameInput(v) => {
                if let Some((_, text)) = self.rename.as_mut() {
                    *text = v;
                }
                Task::none()
            }
            Message::RenameCancel => {
                self.rename = None;
                Task::none()
            }
            Message::RenameApply => {
                let Some((target, text)) = self.rename.take() else {
                    return Task::none();
                };
                let title = text.trim().to_string();
                if title.is_empty() {
                    return Task::none();
                }
                match target {
                    RenameTarget::Server(id) => {
                        if let Some(s) = self.servers.iter_mut().find(|s| s.id == id) {
                            s.name = title;
                        }
                    }
                    RenameTarget::Group(old) => {
                        if title != old {
                            if self.groups.iter().any(|g| g.name == title) {
                                self.set_error("Группа с таким названием уже есть");
                            } else if let Some(g) = self.groups.iter_mut().find(|g| g.name == old) {
                                g.name = title.clone();
                                for s in self.servers.iter_mut() {
                                    if s.group == old {
                                        s.group = title.clone();
                                    }
                                }
                                let from = format!("g:{}", old);
                                if self.collapsed.remove(&from) {
                                    self.collapsed.insert(format!("g:{}", title));
                                }
                            }
                        }
                    }
                }
                self.save();
                Task::none()
            }
            Message::MenuOpen(name) => {
                self.menu = Some((name, self.cursor));
                Task::none()
            }
            Message::MenuClose => {
                self.menu = None;
                Task::none()
            }
            Message::PingServer(id) => {
                let list: Vec<Server> = self.servers.iter().filter(|s| s.id == id).cloned().collect();
                self.ping_tasks(list)
            }
            Message::PingGroup(name) => {
                self.menu = None;
                let list: Vec<Server> = self.servers.iter().filter(|s| s.group == name).cloned().collect();
                self.ping_tasks(list)
            }
            Message::PingAll => {
                let list = self.servers.clone();
                self.ping_tasks(list)
            }
            Message::PingDone(id, result) => {
                if self.servers.iter().any(|s| s.id == id) {
                    self.pings.insert(id, result);
                }
                Task::none()
            }
            Message::CheckAll => self.check_tasks(),
            Message::CheckDone(id, result) => {
                self.check_pending = self.check_pending.saturating_sub(1);
                match result {
                    Ok(v) => {
                        let mut ms = [None; 3];
                        for i in 0..3 {
                            ms[i] = Some(if v[i] > 0 { v[i] } else { -1 });
                        }
                        self.checks.insert(id, ServiceCheck { ms, busy: false });
                    }
                    Err(e) => {
                        self.checks.insert(
                            id,
                            ServiceCheck {
                                ms: [Some(-1); 3],
                                busy: false,
                            },
                        );
                        if self.error.is_none() {
                            self.set_error(e);
                        }
                    }
                }
                if self.check_pending == 0 {
                    self.checked_at = now_millis();
                    self.save();
                }
                Task::none()
            }
            Message::RefreshGroup(name) => {
                self.menu = None;
                let Some(group) = self.groups.iter().find(|g| g.name == name).cloned() else {
                    self.set_error("Группа не найдена");
                    return Task::none();
                };
                if group.url.is_empty() {
                    self.set_error("У группы нет ссылки для обновления");
                    return Task::none();
                }
                self.refreshing = Some(name.clone());
                let url = group.url.clone();
                self.gather(url.clone(), String::new(), String::new(), Some(group.name), url)
            }
            Message::SheetOpen(sheet) => {
                let i = sheet_index(&sheet);
                self.sheet[i] = true;
                if let Sheet::Cores = sheet {
                    self.cores = xray::list();
                }
                Task::none()
            }
            Message::SheetClose(sheet) => {
                self.sheet[sheet_index(&sheet)] = false;
                Task::none()
            }
            Message::AddEdit(action) => {
                self.add_text.perform(action);
                self.add_error = None;
                Task::none()
            }
            #[cfg(not(mobile))]
            Message::AddPaste => iced::clipboard::read().map(Message::Pasted),
            #[cfg(mobile)]
            Message::AddPaste => Task::perform(blocking(crate::bridge::clipboard), |t| Message::Pasted(Some(t))),
            Message::Pasted(text) => {
                let text = text.unwrap_or_default();
                if text.trim().is_empty() {
                    self.add_error = Some("Буфер обмена пуст".into());
                    return Task::none();
                }
                self.add_text = text_editor::Content::with_text(&text);
                self.submit_add(text)
            }
            #[cfg(mobile)]
            Message::AddFile => Task::none(),
            #[cfg(not(mobile))]
            Message::AddFile => {
                self.add_loading = true;
                Task::perform(
                    async {
                        let Some(files) = rfd::AsyncFileDialog::new().pick_files().await else {
                            return Vec::new();
                        };
                        let mut out = Vec::new();
                        for f in files {
                            let name = f.file_name();
                            let bytes = f.read().await;
                            out.push((name, String::from_utf8_lossy(&bytes).to_string()));
                        }
                        out
                    },
                    Message::FilesPicked,
                )
            }
            Message::FilesPicked(files) => {
                self.add_loading = false;
                if files.is_empty() {
                    return Task::none();
                }
                self.add_loading = true;
                let mut tasks = Vec::new();
                for (name, content) in files {
                    let label = name.strip_suffix(".conf").unwrap_or(&name).trim().to_string();
                    let group = label.rsplit_once('.').map(|(a, _)| a.to_string()).unwrap_or(label.clone());
                    tasks.push(self.gather(content, label, group, None, String::new()));
                }
                Task::batch(tasks)
            }
            Message::AddSubmit => {
                let text = self.add_text.text();
                self.submit_add(text)
            }
            Message::Gathered(result, name_hint) => {
                self.add_loading = false;
                self.refreshing = None;
                match result {
                    Ok(items) => {
                        let fresh_before: HashSet<String> = self.servers.iter().map(|s| s.id.clone()).collect();
                        let err = self.apply_import(items, &name_hint);
                        match err {
                            None => {
                                self.sheet[0] = false;
                                self.add_text = text_editor::Content::new();
                                self.add_error = None;
                                if self.s.auto_ping {
                                    let list: Vec<Server> = self
                                        .servers
                                        .iter()
                                        .filter(|s| !fresh_before.contains(&s.id))
                                        .cloned()
                                        .collect();
                                    return self.ping_tasks(list);
                                }
                            }
                            Some(e) => {
                                if self.sheet[0] {
                                    self.add_error = Some(e);
                                } else {
                                    self.set_error(e);
                                }
                            }
                        }
                    }
                    Err(e) => {
                        if self.sheet[0] {
                            self.add_error = Some(e);
                        } else {
                            self.set_error(e);
                        }
                    }
                }
                Task::none()
            }
            Message::SetTheme(v) => self.setting(|s| s.theme = v),
            Message::SetPalette(v) => self.setting(|s| s.palette = v),
            Message::SetBg(v) => self.setting(|s| s.bg_style = v),
            Message::SetContrast(v) => self.setting(|s| s.contrast = v),
            Message::SetAccent(v) => self.setting(|s| s.accent = v),
            Message::SetLaunch(v) => self.setting(|s| s.launch_style = v),
            Message::SetAnim(v) => {
                anim::set_scale(MOTION_FACTORS[v.min(MOTION_FACTORS.len() - 1)]);
                self.setting(|s| s.anim_speed = v)
            }
            Message::SetPowerScale(v) => self.setting(|s| s.power_scale = v),
            Message::SetTextScale(v) => self.setting(|s| s.text_scale = v),
            Message::SetAmoled(v) => self.setting(|s| s.amoled = v),
            Message::SetBurnIn(v) => self.setting(|s| s.burn_in = v),
            Message::SetAutoConnect(v) => self.setting(|s| s.auto_connect = v),
            Message::SetDnsPreset(v) => self.setting(|s| s.dns_preset = v),
            Message::SetDnsCustom(v) => self.setting(|s| s.dns_custom = v),
            Message::SetMtu(i) => self.setting(|s| s.mtu = MTU_VALUES[i.min(MTU_VALUES.len() - 1)]),
            Message::SetPingMode(v) => self.setting(|s| s.ping_mode = v),
            Message::SetPingTimeout(v) => self.setting(|s| s.ping_timeout = v),
            Message::SetAutoPing(v) => self.setting(|s| s.auto_ping = v),
            Message::SetSortPing(v) => self.setting(|s| s.sort_ping = v),
            Message::SetAutoCheck(v) => self.setting(|s| s.auto_check = v),
            Message::SetInterval(text) => {
                self.interval_text = text.clone();
                if let Ok(v) = text.replace(',', ".").trim().parse::<f32>() {
                    if v >= 0.1 {
                        return self.setting(|s| s.check_interval = v);
                    }
                }
                Task::none()
            }
            Message::SetCloseToTray(v) => self.setting(|s| s.close_to_tray = v),
            Message::SetAutostart(v) => {
                self.s.autostart = v;
                self.save();
                Task::perform(blocking(move || set_autostart(v)), |_| Message::Noop)
            }
            Message::CoreSelect(id) => self.setting(|s| s.active_core = id),
            Message::CoreDelete(id) => {
                xray::delete(&id);
                self.cores = xray::list();
                if self.s.active_core == id {
                    self.s.active_core.clear();
                }
                self.save();
                Task::none()
            }
            #[cfg(mobile)]
            Message::CoreFile => Task::none(),
            #[cfg(not(mobile))]
            Message::CoreFile => {
                self.core_busy = Some("Проверяю ядро…".into());
                Task::perform(
                    async {
                        let file = rfd::AsyncFileDialog::new().pick_file().await?;
                        Some(file.path().to_path_buf())
                    },
                    Message::CoreFilePicked,
                )
            }
            Message::CoreFilePicked(path) => match path {
                None => {
                    self.core_busy = None;
                    Task::none()
                }
                Some(p) => Task::perform(blocking(move || xray::install_file(&p)), Message::CoreDone),
            },
            Message::CoreReleases => {
                self.core_list_open = !self.core_list_open;
                if self.core_list_open && self.releases.is_none() {
                    self.core_busy = Some("Загружаю список версий…".into());
                    return Task::perform(blocking(xray::releases), Message::CoreReleasesLoaded);
                }
                Task::none()
            }
            Message::CoreReleasesLoaded(result) => {
                self.core_busy = None;
                match result {
                    Ok(list) => self.releases = Some(list),
                    Err(e) => self.core_msg = Some(e),
                }
                Task::none()
            }
            Message::CoreDownload(release) => {
                self.core_busy = Some(format!("Скачиваю Xray {}…", release.tag));
                let label = format!("Xray {}", release.tag);
                Task::perform(
                    blocking(move || {
                        let bytes = xray::download(&release.url)?;
                        xray::install(&bytes, &label)
                    }),
                    Message::CoreDone,
                )
            }
            Message::CoreDone(result) => {
                self.core_busy = None;
                self.cores = xray::list();
                match result {
                    Ok(info) => {
                        self.core_msg = Some(format!("Ядро «{}» установлено и выбрано", info.label));
                        self.s.active_core = info.id;
                        self.save();
                    }
                    Err(e) => self.core_msg = Some(e),
                }
                Task::none()
            }
            Message::OpenUrl(url) => {
                open_url(&url);
                Task::none()
            }
            Message::ConflictChecked(id, names) => {
                if names.is_empty() {
                    return self.connect(&id);
                }
                self.conflict_id = Some(id);
                self.confirm = Some(Confirm::Conflict(names.join(", ")));
                self.show_window()
            }
            Message::Env(dark, colors) => {
                let d = dark.unwrap_or(self.system_dark);
                if d != self.system_dark || colors != self.sys_colors {
                    self.system_dark = d;
                    self.sys_colors = colors;
                    self.rescheme();
                }
                Task::none()
            }
            Message::SetDynamic(v) => self.setting(|s| s.dynamic = v),
            Message::PasteInto(target) => Task::perform(blocking(crate::bridge::clipboard), move |t| Message::PastedInto(target, t)),
            Message::PastedInto(target, text) => {
                let text = text.trim().to_string();
                if text.is_empty() {
                    return Task::none();
                }
                match target {
                    0 => self.setting(|s| s.tuning.apps = text),
                    _ => self.setting(|s| s.dns_custom = text),
                }
            }
            Message::SetAnimCfg(v) => self.setting(|s| s.anim = v),
            Message::SetTuning(v) => self.setting(|s| s.tuning = v),
            Message::SwitchUi(mode) => {
                self.save();
                crate::bridge::switch_ui(&mode);
                Task::none()
            }
            Message::Noop => Task::none(),
        }
    }

    fn begin_connect(&mut self, id: &str) -> Task<Message> {
        let id = id.to_string();
        Task::perform(blocking(conflict::running), move |names| {
            Message::ConflictChecked(id.clone(), names)
        })
    }

    fn submit_add(&mut self, text: String) -> Task<Message> {
        if self.add_loading {
            return Task::none();
        }
        self.add_loading = true;
        self.add_error = None;
        self.gather(text, String::new(), String::new(), None, String::new())
    }

    fn select(&mut self, id: &str) -> Task<Message> {
        if self.selected.as_deref() == Some(id) {
            return Task::none();
        }
        self.selected = Some(id.to_string());
        self.save();
        if self.status == Status::On {
            self.pending_switch = Some(id.to_string());
            return self.disconnect();
        }
        Task::none()
    }

    fn setting(&mut self, f: impl FnOnce(&mut Settings)) -> Task<Message> {
        f(&mut self.s);
        self.rescheme();
        self.save();
        Task::none()
    }

    fn reset_settings(&mut self) {
        let keep_core = self.s.active_core.clone();
        self.s = Settings::default();
        self.s.active_core = keep_core;
        anim::set_scale(MOTION_FACTORS[self.s.anim_speed]);
        self.interval_text = "60".into();
        self.reset_tick += 1;
        self.rescheme();
        self.save();
    }
}

pub fn window_settings(size: Size) -> window::Settings {
    let (rgba, w, h) = tray::raster(256);
    window::Settings {
        size,
        min_size: Some(Size::new(380.0, 600.0)),
        icon: window::icon::from_rgba(rgba, w, h).ok(),
        exit_on_close_request: false,
        #[cfg(target_os = "linux")]
        platform_specific: window::settings::PlatformSpecific {
            application_id: "FasdeqVPN".to_string(),
            ..Default::default()
        },
        ..window::Settings::default()
    }
}

pub fn sheet_index(s: &Sheet) -> usize {
    match s {
        Sheet::Add => 0,
        Sheet::Settings => 1,
        Sheet::Cores => 2,
    }
}

pub fn open_url(url: &str) {
    crate::platform::open_url(url);
}

#[cfg(mobile)]
fn set_autostart(_enable: bool) {}

#[cfg(not(mobile))]
fn set_autostart(enable: bool) {
    let Ok(exe) = std::env::current_exe() else {
        return;
    };
    let Some(path) = exe.to_str() else {
        return;
    };
    if let Ok(auto) = auto_launch::AutoLaunchBuilder::new()
        .set_app_name("FasdeqVPN")
        .set_app_path(path)
        .build()
    {
        if enable {
            let _ = auto.enable();
        } else {
            let _ = auto.disable();
        }
    }
}

pub fn ts_scheme(app: &App) -> Scheme {
    app.scheme
}
