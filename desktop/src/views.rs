use crate::app::*;
use crate::draw::*;
use crate::model::*;
use crate::theme::{alpha, lerp, swatch};
use crate::ui::*;
use iced::widget::{column, container, mouse_area, row, scrollable, stack, text_editor};
use iced::{Alignment, Color, Length};

fn spinner<'a>(ctx: &Ctx, color: Color, size: f32) -> El<'a> {
    cv(
        Spinner {
            t: ctx.time,
            color,
            width: 2.5,
        },
        size,
        size,
    )
}

fn ping_label(p: &PingResult) -> Option<(String, bool)> {
    let vals = [p.tcp, p.udp, p.icmp];
    let best = p.best();
    if best != i32::MAX {
        return Some((format!("{} мс", best), true));
    }
    if vals.iter().any(|v| v.is_some()) {
        let silent = vals.iter().flatten().any(|v| *v == crate::ping::SILENT);
        return Some((if silent { "нет ответа".into() } else { "ошибка".into() }, false));
    }
    None
}

pub fn view(app: &App) -> El<'_> {
    let ctx = Ctx {
        c: app.scheme,
        ts: app.s.text_scale,
        time: app.anims.time,
    };
    let c = ctx.c;
    let mut layers: Vec<El<'_>> = Vec::new();
    layers.push(
        container(iced::widget::space())
            .width(Length::Fill)
            .height(Length::Fill)
            .style(move |_| rounded(c.background, 0.0))
            .into(),
    );
    if app.s.bg_style < 2 {
        layers.push(cv(
            Backdrop {
                t: app.anims.backdrop,
                c1: c.primary_container,
                c2: c.secondary_container,
                c3: c.tertiary_container,
            },
            app.win_size.width,
            app.win_size.height,
        ));
    }
    layers.push(home(app, &ctx));
    if app.anims.wave.value < 1.0 {
        layers.push(cv(
            ConnectWave {
                p: app.anims.wave.value,
                color: c.primary,
                y: 150.0 + 128.0 * app.s.power_scale,
            },
            app.win_size.width,
            app.win_size.height,
        ));
    }
    if app.sheet_t(0) > 0.001 {
        layers.push(sheet(app, &ctx, 0, add_sheet(app, &ctx), Message::SheetClose(Sheet::Add)));
    }
    if app.sheet_t(1) > 0.001 {
        layers.push(sheet(app, &ctx, 1, settings_sheet(app, &ctx), Message::SheetClose(Sheet::Settings)));
    }
    if app.sheet_t(2) > 0.001 {
        layers.push(sheet(app, &ctx, 2, cores_sheet(app, &ctx), Message::SheetClose(Sheet::Cores)));
    }
    if let Some(m) = menu(app, &ctx) {
        layers.push(m);
    }
    if let Some(d) = dialog(app, &ctx) {
        layers.push(d);
    }
    if let Some(e) = &app.error {
        layers.push(toast(&ctx, e.clone()));
    }
    stack(layers).width(Length::Fill).height(Length::Fill).into()
}

fn toast<'a>(ctx: &Ctx, msg: String) -> El<'a> {
    let c = ctx.c;
    let body = container(tx(ctx, Ty::BodyMedium, msg, c.inverse_on_surface))
        .padding(pad(14.0, 20.0, 14.0, 20.0))
        .max_width(480)
        .style(move |_| rounded(c.inverse_surface, 16.0));
    container(body)
        .width(Length::Fill)
        .height(Length::Fill)
        .align_x(Alignment::Center)
        .align_y(Alignment::End)
        .padding(pad(0.0, 16.0, 96.0, 16.0))
        .into()
}

fn top_bar<'a>(app: &'a App, ctx: &Ctx) -> El<'a> {
    let c = ctx.c;
    if !app.selection.is_empty() {
        let mut r = row![
            plain_icon(ctx, "close", 22.0, Some(Message::SelectionClear)),
            tx(
                ctx,
                Ty::TitleLarge,
                format!("Выбрано: {}", app.selection.len()),
                c.on_surface
            ),
            fill(),
            plain_icon(ctx, "select_all", 22.0, Some(Message::SelectionAll)),
        ]
        .spacing(8)
        .align_y(Alignment::Center);
        if app.selection.len() == 1 {
            let id = app.selection.iter().next().cloned().unwrap_or_default();
            let name = app
                .servers
                .iter()
                .find(|s| s.id == id)
                .map(|s| s.name.clone())
                .unwrap_or_default();
            r = r.push(plain_icon(
                ctx,
                "edit",
                22.0,
                Some(Message::RenameOpen(RenameTarget::Server(id), name)),
            ));
        }
        r = r.push(icon_btn(
            ctx,
            "delete",
            22.0,
            Some(c.error_container),
            Some(c.on_error_container),
            40.0,
            Some(Message::AskDeleteSelected),
        ));
        return container(r).padding(pad(12.0, 16.0, 8.0, 12.0)).width(Length::Fill).into();
    }
    let checking = app.checking();
    let check_btn: El<'_> = if checking {
        container(spinner(ctx, c.primary, 24.0))
            .center_x(Length::Fixed(40.0))
            .center_y(Length::Fixed(40.0))
            .into()
    } else {
        tonal_icon(ctx, "shield", 22.0, Some(Message::CheckAll))
    };
    row![
        cv(
            CookieLogo {
                spin: app.anims.time * 0.6,
                pulse: 0.06,
                primary: c.primary,
                tertiary: c.tertiary,
            },
            36.0,
            36.0
        ),
        tx(ctx, Ty::HeadlineSmall, "FasdeqVPN", c.on_surface),
        fill(),
        check_btn,
        tonal_icon(ctx, "bolt", 22.0, Some(Message::PingAll)),
        tonal_icon(ctx, "settings", 22.0, Some(Message::SheetOpen(Sheet::Settings))),
    ]
    .spacing(8)
    .align_y(Alignment::Center)
    .padding(pad(12.0, 16.0, 8.0, 20.0))
    .into()
}

fn hero<'a>(app: &'a App, ctx: &Ctx) -> El<'a> {
    let c = ctx.c;
    let a = &app.anims;
    let size = 256.0 * app.s.power_scale;
    let on_t = a.on_t.value;
    let launch = Launch {
        style: app.s.launch_style,
        phase: a.phase,
        amp: a.amp.x,
        orbit: a.orbit.value,
        aura: a.aura.value,
        level: a.level.value,
        burst: a.burst.value,
        body: lerp(c.primary_container, c.primary, on_t),
        idle: c.secondary_container,
        content: lerp(c.on_primary_container, c.on_primary, on_t),
        ring: c.primary,
        accent: c.tertiary,
        base_ratio: 0.34,
        scale: a.rest.x * a.press.x,
    };
    let glyph = (76.0 * app.s.power_scale * a.icon_scale.x).max(1.0);
    let layered = stack![
        cv(LaunchCanvas(launch), size, size),
        container(icon("power", glyph, lerp(c.on_primary_container, c.on_primary, on_t)))
            .width(size)
            .height(size)
            .center_x(Length::Fixed(size))
            .center_y(Length::Fixed(size))
    ];
    let power = mouse_area(layered)
        .on_press(Message::PowerDown(true))
        .on_release(Message::Power)
        .on_exit(Message::PowerDown(false));
    let (title, color) = match app.status {
        Status::Off => ("Отключено", c.on_surface),
        Status::Connecting => ("Подключение…", c.primary),
        Status::On => ("Защищено", c.primary),
        Status::Stopping => ("Отключение…", c.primary),
    };
    let name = app
        .selected_server()
        .map(|s| s.name.clone())
        .unwrap_or_else(|| "Сервер не выбран".into());
    let mut col = column![
        power,
        tx(ctx, Ty::HeadlineMedium, title, color),
        named(ctx, &name, Ty::TitleMedium, c.on_surface_variant, 18.0 * ctx.ts),
    ]
    .spacing(4)
    .align_x(Alignment::Center);
    if app.status == Status::On {
        let stat = |icon_name: &str, label: String| -> El<'_> {
            container(
                row![icon(icon_name, 16.0, c.on_secondary_container), txs(ctx, Ty::LabelLarge, label, c.on_secondary_container)]
                    .spacing(6)
                    .align_y(Alignment::Center),
            )
            .padding(pad(8.0, 14.0, 8.0, 12.0))
            .style(move |_| rounded(c.secondary_container, 18.0))
            .into()
        };
        col = col.push(vgap(8.0)).push(
            row![
                stat("arrow_down", format_speed(app.down_speed)),
                stat("arrow_up", format_speed(app.up_speed)),
                stat("bolt", format_time(app.uptime)),
            ]
            .spacing(8),
        );
    }
    container(col).width(Length::Fill).center_x(Length::Fill).into()
}

fn home<'a>(app: &'a App, ctx: &Ctx) -> El<'a> {
    let c = ctx.c;
    let fab = container(pill(
        ctx,
        Some("add"),
        "Добавить",
        Kind::Filled,
        56.0,
        Some(Message::SheetOpen(Sheet::Add)),
    ))
    .width(Length::Fill)
    .height(Length::Fill)
    .align_x(Alignment::End)
    .align_y(Alignment::End)
    .padding(pad(0.0, 20.0, 20.0, 0.0));
    let body: El<'_> = if app.servers.is_empty() {
        let surface = container(empty(app, ctx))
            .width(Length::Fill)
            .height(Length::Fill)
            .style(move |_| rounded4(c.container_low, 40.0, 40.0, 0.0, 0.0));
        column![top_bar(app, ctx), hero(app, ctx), vgap(12.0), surface]
            .width(Length::Fill)
            .height(Length::Fill)
            .into()
    } else {
        let list = container(server_list(app, ctx))
            .width(Length::Fill)
            .style(move |_| rounded4(c.container_low, 40.0, 40.0, 0.0, 0.0));
        let scroll = scrollbox(ctx, column![hero(app, ctx), vgap(12.0), list]).height(Length::Fill);
        column![top_bar(app, ctx), scroll].width(Length::Fill).height(Length::Fill).into()
    };
    stack![body, fab].into()
}

fn empty<'a>(app: &'a App, ctx: &Ctx) -> El<'a> {
    let c = ctx.c;
    let t = app.anims.time;
    let art = cv(
        Illustration {
            t,
            bob: (t * 1.6).sin(),
            back: c.container_highest,
            rack: c.container_high,
            rack_edge: c.outline_variant,
            primary: c.primary,
            secondary: c.secondary,
            tertiary: c.tertiary,
            shield_fill: c.primary_container,
            on_shield: c.on_primary_container,
        },
        240.0,
        180.0,
    );
    container(
        column![
            art,
            tx(ctx, Ty::TitleLarge, "Пока нет серверов", c.on_surface),
            tx(ctx, Ty::BodyMedium, "Добавьте ссылку, подписку или файл конфигурации", c.on_surface_variant),
        ]
        .spacing(8)
        .align_x(Alignment::Center),
    )
    .center_x(Length::Fill)
    .center_y(Length::Fill)
    .into()
}

fn server_list<'a>(app: &'a App, ctx: &Ctx) -> El<'a> {
    let c = ctx.c;
    let mut col = column![].spacing(8).padding(pad(16.0, 16.0, 96.0, 16.0));
    for e in app.entries() {
        match e {
            Entry::Header {
                key,
                name,
                ids,
                url,
                ungrouped,
            } => col = col.push(header_row(app, ctx, key, name, ids, url, ungrouped)),
            Entry::Row(s) => col = col.push(server_card(app, ctx, s)),
        }
    }
    let _ = c;
    col.into()
}

fn header_row<'a>(
    app: &'a App,
    ctx: &Ctx,
    key: String,
    name: String,
    ids: Vec<String>,
    url: String,
    ungrouped: bool,
) -> El<'a> {
    let c = ctx.c;
    let collapsed = app.collapsed.contains(&key);
    let all = !ids.is_empty() && ids.iter().all(|i| app.selection.contains(i));
    let title = if ungrouped { "Без группы".to_string() } else { name.clone() };
    let mut r = row![
        icon(if collapsed { "expand" } else { "arrow_down" }, 20.0, c.on_surface_variant),
        tx(ctx, Ty::TitleMedium, title, if all { c.primary } else { c.on_surface }),
        tx(ctx, Ty::BodySmall, format!("{}", ids.len()), c.on_surface_variant),
        fill(),
    ]
    .spacing(10)
    .align_y(Alignment::Center);
    if !ungrouped {
        let _ = url;
        r = r.push(icon_btn(
            ctx,
            "more",
            20.0,
            None,
            Some(c.on_surface_variant),
            36.0,
            Some(Message::MenuOpen(name.clone())),
        ));
    }
    let refreshing = app.refreshing.as_deref() == Some(name.as_str()) && !ungrouped;
    if refreshing {
        r = r.push(spinner(ctx, c.primary, 20.0));
    }
    mouse_area(container(r).padding(pad(6.0, 6.0, 6.0, 10.0)).width(Length::Fill))
        .on_press(Message::HeaderTap(key))
        .into()
}

fn server_card<'a>(app: &'a App, ctx: &Ctx, s: Server) -> El<'a> {
    let c = ctx.c;
    let sel = app.selected.as_deref() == Some(s.id.as_str());
    let picked = app.selection.contains(&s.id);
    let active = app.active_id.as_deref() == Some(s.id.as_str()) && app.status == Status::On;
    let t = app.anims.cards.get(&s.id).map(|x| x.x).unwrap_or(if sel { 1.0 } else { 0.0 });
    let bg = if picked {
        c.secondary_container
    } else {
        lerp(c.container_high, c.primary_container, t.clamp(0.0, 1.0))
    };
    let fg = if sel || picked {
        c.on_primary_container
    } else {
        c.on_surface
    };
    let sub = if s.endpoint.is_empty() {
        "—".to_string()
    } else {
        s.endpoint.clone()
    };
    let mut right = row![].spacing(10).align_y(Alignment::Center);
    if let Some(check) = app.checks.get(&s.id) {
        let mut logos = row![].spacing(6).align_y(Alignment::Center);
        for i in 0..3 {
            let v = check.ms[i];
            let (a, failed) = match v {
                _ if check.busy => (0.35, false),
                Some(ms) if ms > 0 => (1.0, false),
                Some(_) => (0.5, true),
                None => (0.35, false),
            };
            logos = logos.push(cv(
                ServiceLogo {
                    index: i,
                    alpha: a,
                    best: !check.busy && app.best_for(i).as_deref() == Some(s.id.as_str()),
                    failed,
                    ring: c.primary,
                    error: c.error,
                },
                22.0,
                22.0,
            ));
        }
        right = right.push(logos);
    }
    let ping = app.pings.get(&s.id).copied().unwrap_or_default();
    let ping_el: El<'_> = if ping.busy {
        spinner(ctx, c.primary, 20.0)
    } else {
        match ping_label(&ping) {
            Some((text, ok)) => container(txs(
                ctx,
                Ty::LabelMedium,
                text,
                if ok { c.on_secondary_container } else { c.on_error_container },
            ))
            .padding(pad(5.0, 10.0, 5.0, 10.0))
            .style(move |_| rounded(if ok { c.secondary_container } else { c.error_container }, 12.0))
            .into(),
            None => icon("bolt", 18.0, c.on_surface_variant).into(),
        }
    };
    right = right.push(mouse_area(ping_el).on_press(Message::PingServer(s.id.clone())));
    let lead: El<'_> = if picked {
        icon("check", 22.0, c.primary).into()
    } else if active {
        icon("shield", 22.0, c.primary).into()
    } else {
        icon("dns", 22.0, if sel { c.on_primary_container } else { c.on_surface_variant }).into()
    };
    let body = row![
        lead,
        column![
            named(ctx, &s.name, Ty::TitleMedium, fg, 16.0 * ctx.ts),
            txs(ctx, Ty::BodySmall, sub, c.on_surface_variant),
        ]
        .width(Length::Fill),
        right,
    ]
    .spacing(14)
    .align_y(Alignment::Center);
    let card = container(body)
        .padding(pad(14.0, 16.0, 14.0, 16.0))
        .width(Length::Fill)
        .style(move |_| rounded(bg, 24.0));
    mouse_area(card)
        .on_press(Message::CardDown(s.id.clone()))
        .on_release(Message::CardUp(s.id.clone()))
        .on_right_press(Message::CardRight(s.id.clone()))
        .on_exit(Message::CardCancel)
        .into()
}

fn sheet<'a>(app: &'a App, ctx: &Ctx, index: usize, body: El<'a>, close: Message) -> El<'a> {
    let c = ctx.c;
    let t = app.anims.sheets[index].x.clamp(0.0, 1.2);
    let h = app.win_size.height;
    let sheet_h = (h * 0.9).max(240.0);
    let scrim = mouse_area(
        container(iced::widget::space())
            .width(Length::Fill)
            .height(Length::Fill)
            .style(move |_| rounded(alpha(Color::BLACK, 0.4 * t.min(1.0)), 0.0)),
    )
    .on_press(close);
    let panel = container(body)
        .width(Length::Fill)
        .max_width(640)
        .height(Length::Fixed(sheet_h))
        .style(move |_| rounded4(c.container_low, 40.0, 40.0, 0.0, 0.0));
    let positioned = column![vgap((h - sheet_h * t).max(0.0)), container(panel).width(Length::Fill).center_x(Length::Fill)];
    stack![scrim, positioned].into()
}

fn sheet_header<'a>(ctx: &Ctx, title: &str, close: Message) -> El<'a> {
    let c = ctx.c;
    row![
        tx(ctx, Ty::HeadlineSmall, title.to_string(), c.on_surface),
        fill(),
        plain_icon(ctx, "close", 22.0, Some(close)),
    ]
    .align_y(Alignment::Center)
    .padding(pad(20.0, 16.0, 8.0, 24.0))
    .into()
}

fn add_sheet<'a>(app: &'a App, ctx: &Ctx) -> El<'a> {
    let c = ctx.c;
    let editor = text_editor(&app.add_text)
        .placeholder("vless://, vmess://, trojan://, ss://, WireGuard или ссылка на подписку")
        .on_action(Message::AddEdit)
        .size(15.0 * ctx.ts)
        .font(font(iced::font::Weight::Normal))
        .height(Length::Fixed(160.0))
        .padding(14)
        .style(move |_, status| {
            let focused = matches!(status, text_editor::Status::Focused { .. });
            text_editor::Style {
                background: iced::Background::Color(Color::TRANSPARENT),
                border: iced::Border {
                    color: if focused { c.primary } else { c.outline },
                    width: if focused { 2.0 } else { 1.0 },
                    radius: 18.0.into(),
                },
                placeholder: alpha(c.on_surface_variant, 0.7),
                value: c.on_surface,
                selection: alpha(c.primary, 0.35),
            }
        });
    let mut body = column![editor].spacing(14);
    if let Some(e) = &app.add_error {
        body = body.push(tx(ctx, Ty::BodyMedium, e.clone(), c.error));
    }
    let add_btn: El<'_> = if app.add_loading {
        pill_busy(ctx, spinner(ctx, c.on_primary, 20.0), "Загрузка…", Kind::Filled, None)
    } else {
        pill(ctx, Some("add"), "Добавить", Kind::Filled, 40.0, Some(Message::AddSubmit))
    };
    body = body.push(
        row![
            pill(ctx, Some("paste"), "Вставить", Kind::Tonal, 40.0, Some(Message::AddPaste)),
            pill(ctx, Some("file"), "Файл", Kind::Tonal, 40.0, Some(Message::AddFile)),
            fill(),
            add_btn,
        ]
        .spacing(8)
        .align_y(Alignment::Center),
    );
    column![
        sheet_header(ctx, "Новый сервер", Message::SheetClose(Sheet::Add)),
        container(body).padding(pad(8.0, 24.0, 24.0, 24.0))
    ]
    .into()
}

fn cores_sheet<'a>(app: &'a App, ctx: &Ctx) -> El<'a> {
    let c = ctx.c;
    let mut col = column![].spacing(10).padding(pad(8.0, 24.0, 24.0, 24.0));
    if let Some(b) = &app.core_busy {
        col = col.push(
            row![spinner(ctx, c.primary, 22.0), tx(ctx, Ty::BodyMedium, b.clone(), c.on_surface_variant)]
                .spacing(10)
                .align_y(Alignment::Center),
        );
    }
    if let Some(m) = &app.core_msg {
        col = col.push(tx(ctx, Ty::BodyMedium, m.clone(), c.primary));
    }
    if app.cores.is_empty() {
        col = col.push(tx(ctx, Ty::BodyMedium, "Ядро не установлено. Скачайте его или выберите файл xray", c.on_surface_variant));
    }
    for core in &app.cores {
        let active = app.s.active_core == core.id || (app.s.active_core.is_empty() && app.cores.first().map(|f| f.id == core.id).unwrap_or(false));
        let item = container(
            row![
                mouse_area(
                    row![
                        icon(if active { "check" } else { "memory" }, 22.0, if active { c.primary } else { c.on_surface_variant }),
                        column![
                            tx(ctx, Ty::TitleMedium, core.label.clone(), c.on_surface),
                            tx(ctx, Ty::BodySmall, core.version.clone(), c.on_surface_variant),
                        ]
                        .width(Length::Fill),
                    ]
                    .spacing(14)
                    .align_y(Alignment::Center)
                    .width(Length::Fill)
                )
                .on_press(Message::CoreSelect(core.id.clone())),
                plain_icon(ctx, "delete", 20.0, Some(Message::CoreDelete(core.id.clone()))),
            ]
            .align_y(Alignment::Center),
        )
        .padding(pad(10.0, 8.0, 10.0, 16.0))
        .style(move |_| rounded(if active { c.primary_container } else { c.container_high }, 20.0));
        col = col.push(item);
    }
    col = col.push(
        row![
            pill(ctx, Some("file"), "Выбрать файл", Kind::Tonal, 40.0, Some(Message::CoreFile)),
            pill(ctx, Some("arrow_down"), "Версии", Kind::Filled, 40.0, Some(Message::CoreReleases)),
        ]
        .spacing(8),
    );
    if app.core_list_open {
        if let Some(list) = &app.releases {
            for r in list.iter().take(15) {
                let mb = r.size as f64 / 1048576.0;
                col = col.push(
                    container(
                        row![
                            tx(ctx, Ty::TitleSmall, r.tag.clone(), c.on_surface),
                            tx(ctx, Ty::BodySmall, format!("{:.1} МБ", mb), c.on_surface_variant),
                            fill(),
                            text_button(ctx, "Скачать", c.primary, Message::CoreDownload(r.clone())),
                        ]
                        .spacing(10)
                        .align_y(Alignment::Center),
                    )
                    .padding(pad(2.0, 8.0, 2.0, 16.0))
                    .style(move |_| rounded(c.container_high, 20.0)),
                );
            }
        }
    }
    column![
        sheet_header(ctx, "Ядро Xray", Message::SheetClose(Sheet::Cores)),
        scrollbox(ctx, col).height(Length::Fill)
    ]
    .into()
}

fn anim_toggle<'a>(
    ctx: &Ctx,
    cfg: &AnimConfig,
    icon_name: &str,
    title: &str,
    sub: &str,
    set: fn(&mut AnimConfig, bool),
    get: fn(&AnimConfig) -> bool,
) -> El<'a> {
    let on = get(cfg);
    let mut next = cfg.clone();
    set(&mut next, !on);
    setting_item(
        ctx,
        icon_name,
        title,
        sub,
        false,
        Some(switch(ctx, on, Message::SetAnimCfg(next.clone()))),
        Some(Message::SetAnimCfg(next)),
    )
}

fn tuning_toggle<'a>(
    ctx: &Ctx,
    t: &Tuning,
    title: &str,
    sub: &str,
    set: fn(&mut Tuning, bool),
    get: fn(&Tuning) -> bool,
) -> El<'a> {
    let on = get(t);
    let mut next = t.clone();
    set(&mut next, !on);
    setting_item(
        ctx,
        "memory",
        title,
        sub,
        false,
        Some(switch(ctx, on, Message::SetTuning(next.clone()))),
        Some(Message::SetTuning(next)),
    )
}

fn tuning_slider<'a>(
    ctx: &Ctx,
    t: &Tuning,
    title: &str,
    shown: String,
    range: std::ops::RangeInclusive<f32>,
    value: f32,
    set: fn(&mut Tuning, f32),
) -> El<'a> {
    let base = t.clone();
    setting_item(
        ctx,
        "tune",
        title,
        &shown,
        true,
        Some(m3_slider(ctx, range, value, move |x| {
            let mut n = base.clone();
            set(&mut n, x);
            Message::SetTuning(n)
        })),
        None,
    )
}

fn settings_sheet<'a>(app: &'a App, ctx: &Ctx) -> El<'a> {
    let c = ctx.c;
    let s = &app.s;
    let mut col = column![].spacing(20).padding(pad(8.0, 20.0, 28.0, 20.0));

    let accent_row: El<'_> = {
        let mut r = row![].spacing(8);
        let mut rows = column![].spacing(8);
        for i in 0..ACCENT_NAMES.len() {
            let sw = swatch(i, s.palette);
            let sel = s.accent == i;
            let dot = container(if sel {
                Into::<El<'_>>::into(icon("check", 16.0, Color::WHITE))
            } else {
                gap(0.0, 0.0)
            })
            .center_x(Length::Fixed(32.0))
            .center_y(Length::Fixed(32.0))
            .style(move |_| rounded(sw, 16.0));
            r = r.push(mouse_area(dot).on_press(Message::SetAccent(i)));
            if i % 8 == 7 {
                rows = rows.push(r);
                r = row![].spacing(8);
            }
        }
        rows.into()
    };

    col = col.push(group(
        ctx,
        "Оформление",
        vec![
            setting_item(ctx, "theme_auto", "Тема", "Системная, светлая или тёмная", true,
                Some(segmented(ctx, &THEME_NAMES, s.theme, true, Some(&["theme_auto", "sun", "moon"]), Message::SetTheme)), None),
            setting_item(ctx, "palette", "Цвет акцента", ACCENT_NAMES[s.accent.min(15)], true, Some(accent_row), None),
            setting_item(ctx, "tune", "Насыщенность", "Стиль палитры", true,
                Some(segmented(ctx, &PALETTE_STYLES, s.palette, true, None, Message::SetPalette)), None),
            setting_item(ctx, "screen", "Контраст", "Стандартный, средний, высокий", true,
                Some(segmented(ctx, &["Стандарт", "Средний", "Высокий"], s.contrast, true, None, Message::SetContrast)), None),
            setting_item(ctx, "moon", "AMOLED", "Чисто чёрный фон в тёмной теме", false,
                Some(switch(ctx, s.amoled, Message::SetAmoled(!s.amoled))), Some(Message::SetAmoled(!s.amoled))),
            setting_item(ctx, "battery", "Защита от выгорания", "Сдвиг элементов раз в минуту", false,
                Some(switch(ctx, s.burn_in, Message::SetBurnIn(!s.burn_in))), Some(Message::SetBurnIn(!s.burn_in))),
            setting_item(ctx, "tile", "Фон", "Блики на главном экране", true,
                Some(segmented(ctx, &["Блики", "Мягкий", "Без фона"], s.bg_style, true, None, Message::SetBg)), None),
            setting_item(ctx, "power", "Размер кнопки запуска", &format!("{:.0}%", s.power_scale * 100.0), true,
                Some(m3_slider(ctx, 0.6..=1.4, s.power_scale, Message::SetPowerScale)), None),
            setting_item(ctx, "edit", "Размер текста", &format!("{:.0}%", s.text_scale * 100.0), true,
                Some(m3_slider(ctx, 0.8..=1.4, s.text_scale, Message::SetTextScale)), None),
        ],
    ));

    col = col.push(group(
        ctx,
        "Анимации",
        vec![
            setting_item(ctx, "power", "Стиль кнопки", LAUNCH_NAMES[s.launch_style.min(4)], true,
                Some(container(cv(LaunchCanvas(preview_launch(app)), 120.0, 120.0)).center_x(Length::Fill).into()), None),
            block(vec![segmented_wrap(ctx, &LAUNCH_NAMES, s.launch_style, Message::SetLaunch)]),
            setting_item(ctx, "bolt", "Скорость", MOTION_NAMES[s.anim_speed.min(3)], true,
                Some(segmented(ctx, &MOTION_NAMES, s.anim_speed, true, None, Message::SetAnim)), None),
        ],
    ));

    {
        let cfg = s.anim.clone();
        let mut items: Vec<El<'_>> = vec![anim_toggle(
            ctx,
            &cfg,
            "bolt",
            "Анимация кнопки",
            "Полностью выключает движение кнопки",
            |c, v| c.enabled = v,
            |c| c.enabled,
        )];
        if cfg.enabled {
            let a1 = cfg.clone();
            let a2 = cfg.clone();
            items.push(setting_item(
                ctx,
                "tune",
                "Сила деформации",
                &format!("{:.0}%", cfg.intensity * 100.0),
                true,
                Some(m3_slider(ctx, 0.0..=2.0, cfg.intensity, move |x| {
                    let mut n = a1.clone();
                    n.intensity = x;
                    Message::SetAnimCfg(n)
                })),
                None,
            ));
            items.push(setting_item(
                ctx,
                "bolt",
                "Темп",
                &format!("x{:.2}", cfg.tempo),
                true,
                Some(m3_slider(ctx, 0.2..=3.0, cfg.tempo, move |x| {
                    let mut n = a2.clone();
                    n.tempo = x;
                    Message::SetAnimCfg(n)
                })),
                None,
            ));
            items.push(anim_toggle(ctx, &cfg, "bolt", "Всплеск при подключении", "Расходящиеся кольца", |c, v| c.burst = v, |c| c.burst));
            items.push(anim_toggle(ctx, &cfg, "bolt", "Движение в покое", "Медленное дыхание кнопки", |c, v| c.idle_motion = v, |c| c.idle_motion));
            items.push(anim_toggle(ctx, &cfg, "power", "Анимация иконки", "Сжатие и покачивание при подключении", |c, v| c.icon_fx = v, |c| c.icon_fx));
            items.push(anim_toggle(ctx, &cfg, "power", "Эффект нажатия", "Кнопка уменьшается под пальцем", |c, v| c.press = v, |c| c.press));
        }
        if crate::platform::MOBILE {
            items.push(anim_toggle(ctx, &cfg, "bell", "Вибрация", "Отклик при нажатии и подключении", |c, v| c.haptics = v, |c| c.haptics));
        }
        col = col.push(group(ctx, "Анимация кнопки", items));
    }

    let mtu_idx = MTU_VALUES.iter().position(|v| *v == s.mtu).unwrap_or(2);
    let mut conn = vec![
        setting_item(ctx, "power", "Автоподключение", "Подключаться при запуске", false,
            Some(switch(ctx, s.auto_connect, Message::SetAutoConnect(!s.auto_connect))), Some(Message::SetAutoConnect(!s.auto_connect))),
        setting_item(ctx, "dns", "DNS", DNS_NAMES[s.dns_preset.min(4)], true,
            Some(segmented(ctx, &DNS_NAMES, s.dns_preset, true, None, Message::SetDnsPreset)), None),
    ];
    if s.dns_preset >= 4 {
        conn.push(block(vec![field(ctx, "Свои DNS", "1.1.1.1, 8.8.8.8", &s.dns_custom, Message::SetDnsCustom, None)]));
    }
    conn.push(setting_item(ctx, "memory", "MTU", &format!("{}", s.mtu), true,
        Some(segmented(ctx, &["1280", "1400", "1500"], mtu_idx, true, None, Message::SetMtu)), None));
    if !crate::platform::MOBILE {
        conn.push(setting_item(ctx, "code", "Ядро Xray",
            &app.cores.iter().find(|x| x.id == s.active_core).or(app.cores.first()).map(|x| x.label.clone()).unwrap_or_else(|| "не установлено".into()),
            false, Some(icon("open", 20.0, c.on_surface_variant).into()), Some(Message::SheetOpen(Sheet::Cores))));
    }
    if crate::platform::MOBILE && s.dns_preset >= 4 {
        conn.push(block(vec![text_button(ctx, "Вставить DNS из буфера", c.primary, Message::PasteInto(1))]));
    }
    col = col.push(group(ctx, "Подключение", conn));

    if crate::platform::MOBILE {
        let t = s.tuning.clone();
        let mut items: Vec<El<'_>> = vec![
            tuning_toggle(ctx, &t, "IPv6", "Выключите, если сайты зависают из-за IPv6", |t, v| t.ipv6 = v, |t| t.ipv6),
            tuning_toggle(ctx, &t, "Sniffing", "Определять домен по HTTP, TLS и QUIC", |t, v| t.sniffing = v, |t| t.sniffing),
            tuning_toggle(ctx, &t, "Блокировать QUIC", "Закрывает UDP 443, браузеры переходят на TCP", |t, v| t.block_quic = v, |t| t.block_quic),
            tuning_toggle(ctx, &t, "Локальная сеть напрямую", "10.x, 172.16.x, 192.168.x, 169.254.x мимо VPN", |t, v| t.bypass_lan = v, |t| t.bypass_lan),
            tuning_toggle(ctx, &t, "Mux", "Для VLESS, VMess, Trojan, Shadowsocks. Не работает с XTLS Vision", |t, v| t.mux = v, |t| t.mux),
            tuning_toggle(ctx, &t, "Сеть без лимита", "Система не считает VPN-трафик мобильным", |t, v| t.unmetered = v, |t| t.unmetered),
        ];
        {
            let base = t.clone();
            items.push(setting_item(ctx, "memory", "Уровень лога Xray", LOG_NAMES[t.log_level.min(4)], true,
                Some(segmented_wrap(ctx, &LOG_NAMES, t.log_level, move |i| {
                    let mut n = base.clone();
                    n.log_level = i;
                    Message::SetTuning(n)
                })), None));
        }
        {
            let base = t.clone();
            items.push(setting_item(ctx, "memory", "Domain strategy", DOMAIN_NAMES[t.domain_strategy.min(2)], true,
                Some(segmented(ctx, &DOMAIN_NAMES, t.domain_strategy, true, None, move |i| {
                    let mut n = base.clone();
                    n.domain_strategy = i;
                    Message::SetTuning(n)
                })), None));
        }
        if t.mux {
            items.push(tuning_slider(ctx, &t, "Потоков Mux", format!("{}", t.mux_concurrency), 1.0..=64.0, t.mux_concurrency as f32, |n, x| n.mux_concurrency = x as i32));
        }
        items.push(tuning_slider(ctx, &t, "Таймаут простоя", format!("{} с", t.conn_idle), 30.0..=1800.0, t.conn_idle as f32, |n, x| n.conn_idle = x as i32));
        items.push(tuning_slider(
            ctx,
            &t,
            "Буфер на соединение",
            if t.buffer_kb == 0 { "по умолчанию".to_string() } else { format!("{} КБ", t.buffer_kb) },
            0.0..=2048.0,
            t.buffer_kb as f32,
            |n, x| n.buffer_kb = ((x / 64.0) as i32) * 64,
        ));
        col = col.push(group(ctx, "Низкоуровневые", items));

        let mut rc: Vec<El<'_>> = vec![tuning_toggle(ctx, &t, "Переподключаться при обрыве", "Иначе VPN остановится при первой ошибке", |t, v| t.reconnect = v, |t| t.reconnect)];
        if t.reconnect {
            rc.push(tuning_slider(ctx, &t, "Попыток за 2 минуты", format!("{}", t.retries), 1.0..=20.0, t.retries as f32, |n, x| n.retries = x as i32));
            rc.push(tuning_slider(ctx, &t, "Пауза между попытками", format!("{} с", t.retry_delay), 1.0..=30.0, t.retry_delay as f32, |n, x| n.retry_delay = x as i32));
        }
        col = col.push(group(ctx, "Авто-переподключение", rc));

        let mut apps: Vec<El<'_>> = Vec::new();
        {
            let base = t.clone();
            apps.push(setting_item(ctx, "apps", "Режим", APP_MODES[t.app_mode.min(2)], true,
                Some(segmented(ctx, &APP_MODES, t.app_mode, true, None, move |i| {
                    let mut n = base.clone();
                    n.app_mode = i;
                    Message::SetTuning(n)
                })), None));
        }
        if t.app_mode != 0 {
            let base = t.clone();
            apps.push(block(vec![
                field(ctx, "Пакеты через запятую", "org.telegram.messenger, com.discord", &t.apps, move |x| {
                    let mut n = base.clone();
                    n.apps = x;
                    Message::SetTuning(n)
                }, None),
                text_button(ctx, "Вставить из буфера", c.primary, Message::PasteInto(0)),
                tx(ctx, Ty::BodySmall, format!("Распознано пакетов: {}", t.app_list().len()), c.on_surface_variant).into(),
            ]));
        }
        col = col.push(group(ctx, "Приложения", apps));

        col = col.push(group(
            ctx,
            "Интерфейс",
            vec![
                setting_item(ctx, "palette", "Цвета системы (Material You)", "Палитра из обоев. Нужен Android 12+", false,
                    Some(switch(ctx, s.dynamic, Message::SetDynamic(!s.dynamic))), Some(Message::SetDynamic(!s.dynamic))),
                setting_item(ctx, "screen", "Обычный интерфейс (Compose)", "Перезапустит приложение", false, None, Some(Message::SwitchUi("compose".into()))),
                setting_item(ctx, "screen", "Интерфейс на egui (бета)", "Перезапустит приложение", false, None, Some(Message::SwitchUi("egui".into()))),
            ],
        ));
    }

    let timeout_names = PING_TIMEOUT_NAMES;
    let interval = if s.auto_check {
        field(ctx, "Интервал проверки сервисов, мин", "60", &app.interval_text, Message::SetInterval, None)
    } else {
        gap(0.0, 0.0)
    };
    let mut ping_items = vec![
        setting_item(ctx, "bolt", "Способ", PING_MODES[s.ping_mode.min(4)], true,
            Some(segmented(ctx, &PING_MODES, s.ping_mode, true, None, Message::SetPingMode)), None),
        setting_item(ctx, "refresh", "Таймаут", timeout_names[s.ping_timeout.min(2)], true,
            Some(segmented(ctx, &timeout_names, s.ping_timeout, true, None, Message::SetPingTimeout)), None),
        setting_item(ctx, "bolt", "Автопроверка", "Проверять после добавления", false,
            Some(switch(ctx, s.auto_ping, Message::SetAutoPing(!s.auto_ping))), Some(Message::SetAutoPing(!s.auto_ping))),
        setting_item(ctx, "apps", "Сортировка", "Быстрые серверы выше", false,
            Some(switch(ctx, s.sort_ping, Message::SetSortPing(!s.sort_ping))), Some(Message::SetSortPing(!s.sort_ping))),
        setting_item(ctx, "shield", "Проверка сервисов раз в интервал", "Пока VPN включён", false,
            Some(switch(ctx, s.auto_check, Message::SetAutoCheck(!s.auto_check))), Some(Message::SetAutoCheck(!s.auto_check))),
    ];
    if s.auto_check {
        ping_items.push(block(vec![interval]));
    }
    col = col.push(group(ctx, "Проверка пинга", ping_items));

    if !crate::platform::MOBILE {
        col = col.push(group(
            ctx,
            "Работа в фоне",
            vec![
                setting_item(ctx, "close", "Сворачивать в трей", "Закрытие окна не отключает VPN", false,
                    Some(switch(ctx, s.close_to_tray, Message::SetCloseToTray(!s.close_to_tray))), Some(Message::SetCloseToTray(!s.close_to_tray))),
                setting_item(ctx, "power", "Запуск с системой", "Автозапуск при входе", false,
                    Some(switch(ctx, s.autostart, Message::SetAutostart(!s.autostart))), Some(Message::SetAutostart(!s.autostart))),
            ],
        ));
    }

    col = col.push(group(
        ctx,
        "Сброс",
        vec![setting_item(ctx, "refresh", "Настройки по умолчанию", "Вернуть все параметры к исходным", false, None, Some(Message::Ask(Confirm::Reset)))],
    ));

    let about = container(
        column![
            tx(ctx, Ty::TitleLarge, "FasdeqVPN", c.on_primary_container),
            tx(ctx, Ty::BodyMedium, "Версия 1.6", c.on_primary_container),
            row![
                text_button_icon(ctx, "person", "fasdeq13", c.on_primary_container, Message::OpenUrl("https://github.com/fasdeq13".into())),
                text_button_icon(ctx, "code", "Fasdeq13/FasdeqVPN", c.on_primary_container, Message::OpenUrl("https://github.com/Fasdeq13/FasdeqVPN".into())),
            ]
            .spacing(4),
        ]
        .spacing(4),
    )
    .width(Length::Fill)
    .padding(pad(20.0, 22.0, 14.0, 22.0))
    .style(move |_| rounded(c.primary_container, 28.0));
    col = col.push(about);

    column![
        sheet_header(ctx, "Настройки", Message::SheetClose(Sheet::Settings)),
        scrollbox(ctx, col).height(Length::Fill)
    ]
    .into()
}

fn segmented_wrap<'a>(ctx: &Ctx, names: &[&str], selected: usize, on: impl Fn(usize) -> Message + 'a) -> El<'a> {
    let c = ctx.c;
    let mut r = row![].spacing(8);
    for (i, n) in names.iter().enumerate() {
        let sel = i == selected;
        let chip = container(txs(
            ctx,
            Ty::LabelLarge,
            n.to_string(),
            if sel { c.on_secondary_container } else { c.on_surface },
        ))
        .padding(pad(8.0, 14.0, 8.0, 14.0))
        .style(move |_| {
            if sel {
                rounded(c.secondary_container, 16.0)
            } else {
                outlined(c.outline, 1.0, 16.0, None)
            }
        });
        r = r.push(mouse_area(chip).on_press(on(i)));
    }
    scrollable(r)
        .direction(scrollable::Direction::Horizontal(scrollable::Scrollbar::new().width(0).scroller_width(0)))
        .into()
}

fn preview_launch(app: &App) -> Launch {
    let c = app.scheme;
    let t = app.anims.time;
    Launch {
        style: app.s.launch_style,
        phase: t * 1.2,
        amp: 0.05,
        orbit: 0.0,
        aura: 1.0,
        level: 1.0,
        burst: 1.0,
        body: c.primary,
        idle: c.secondary_container,
        content: c.on_primary,
        ring: c.primary,
        accent: c.tertiary,
        base_ratio: 0.34,
        scale: 1.0,
    }
}

fn menu<'a>(app: &'a App, ctx: &Ctx) -> Option<El<'a>> {
    let (name, at) = app.menu.as_ref()?;
    let c = ctx.c;
    let group = app.groups.iter().find(|g| &g.name == name)?;
    let item = |icon_name: &str, label: &str, color: Color, m: Message| -> El<'_> {
        mouse_area(
            container(
                row![icon(icon_name, 20.0, color), txs(ctx, Ty::BodyLarge, label.to_string(), c.on_surface)]
                    .spacing(14)
                    .align_y(Alignment::Center),
            )
            .padding(pad(12.0, 20.0, 12.0, 16.0))
            .width(Length::Fill),
        )
        .on_press(m)
        .into()
    };
    let mut col = column![
        item("edit", "Переименовать", c.on_surface_variant, Message::RenameOpen(RenameTarget::Group(name.clone()), name.clone())),
        item("bolt", "Пинг группы", c.on_surface_variant, Message::PingGroup(name.clone())),
    ];
    if !group.url.is_empty() {
        col = col.push(item("refresh", "Обновить подписку", c.on_surface_variant, Message::RefreshGroup(name.clone())));
    }
    col = col
        .push(item("delete", "Очистить группу", c.on_surface_variant, Message::Ask(Confirm::ClearGroup(name.clone()))))
        .push(item("close", "Удалить группу", c.error, Message::Ask(Confirm::DeleteGroup(name.clone()))));
    let w = 260.0;
    let x = at.x.min(app.win_size.width - w - 12.0).max(12.0);
    let y = at.y.min(app.win_size.height - 260.0).max(12.0);
    let panel = container(col)
        .width(w)
        .padding(pad(8.0, 0.0, 8.0, 0.0))
        .style(move |_| rounded(c.container_highest, 16.0));
    let scrim = mouse_area(container(iced::widget::space()).width(Length::Fill).height(Length::Fill)).on_press(Message::MenuClose);
    let placed = column![vgap(y), row![hgap(x), panel]];
    Some(stack![scrim, placed].into())
}

fn dialog<'a>(app: &'a App, ctx: &Ctx) -> Option<El<'a>> {
    let c = ctx.c;
    let scrim = |inner: El<'a>| -> El<'a> {
        stack![
            mouse_area(
                container(iced::widget::space())
                    .width(Length::Fill)
                    .height(Length::Fill)
                    .style(move |_| rounded(alpha(Color::BLACK, 0.45), 0.0))
            ),
            container(inner).width(Length::Fill).height(Length::Fill).center_x(Length::Fill).center_y(Length::Fill)
        ]
        .into()
    };
    if let Some(conf) = &app.confirm {
        let (title, text, ok) = match conf {
            Confirm::Selected => ("Удалить серверы?", format!("Будет удалено: {}", app.selection.len()), "Удалить"),
            Confirm::DeleteGroup(n) => ("Удалить группу?", format!("Группа «{}» и все её серверы будут удалены", n), "Удалить"),
            Confirm::ClearGroup(n) => ("Очистить группу?", format!("Все серверы группы «{}» будут удалены", n), "Очистить"),
            Confirm::Conflict(n) => (
                "Запущен другой VPN-клиент",
                format!("Обнаружено: {}. Он управляет тем же системным прокси и сетью, поэтому FasdeqVPN может не работать. Выключите его и подключитесь снова.", n),
                "Всё равно",
            ),
            Confirm::Reset => ("Сбросить настройки?", "Все параметры вернутся к значениям по умолчанию".to_string(), "Сбросить"),
        };
        let card = container(
            column![
                tx(ctx, Ty::HeadlineSmall, title, c.on_surface),
                tx(ctx, Ty::BodyMedium, text, c.on_surface_variant),
                row![
                    fill(),
                    text_button(ctx, "Отмена", c.primary, Message::ConfirmCancel),
                    text_button(ctx, ok, c.error, Message::ConfirmApply),
                ]
                .spacing(8),
            ]
            .spacing(14),
        )
        .width(360)
        .padding(24)
        .style(move |_| rounded(c.container_high, 28.0));
        return Some(scrim(card.into()));
    }
    if let Some((target, value)) = &app.rename {
        let label = match target {
            RenameTarget::Server(_) => "Название сервера",
            RenameTarget::Group(_) => "Название группы",
        };
        let card = container(
            column![
                tx(ctx, Ty::HeadlineSmall, "Переименовать", c.on_surface),
                field(ctx, label, "", value, Message::RenameInput, Some(Message::RenameApply)),
                row![
                    fill(),
                    text_button(ctx, "Отмена", c.primary, Message::RenameCancel),
                    text_button(ctx, "Сохранить", c.primary, Message::RenameApply),
                ]
                .spacing(8),
            ]
            .spacing(14),
        )
        .width(380)
        .padding(24)
        .style(move |_| rounded(c.container_high, 28.0));
        return Some(scrim(card.into()));
    }
    None
}
