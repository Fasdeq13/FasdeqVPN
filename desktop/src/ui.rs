use crate::app::Message;
use crate::theme::{alpha, lerp, Scheme};
use iced::border::Radius;
use iced::font::{Family, Weight};
use iced::widget::{
    button, canvas, column, container, mouse_area, row, scrollable, slider, space, svg, text,
    text_input,
};
use iced::{Alignment, Background, Border, Color, Element, Font, Length, Padding, Shadow};
use std::collections::HashMap;
use std::sync::OnceLock;

pub type El<'a> = Element<'a, Message>;

#[derive(Clone, Copy)]
pub struct Ctx {
    pub c: Scheme,
    pub ts: f32,
    pub time: f32,
}

pub const ROBOTO: &str = "Roboto";

pub fn font(weight: Weight) -> Font {
    Font {
        family: Family::Name(ROBOTO),
        weight,
        ..Font::DEFAULT
    }
}

#[derive(Clone, Copy)]
pub enum Ty {
    HeadlineMedium,
    HeadlineSmall,
    TitleLarge,
    TitleMedium,
    TitleSmall,
    BodyLarge,
    BodyMedium,
    BodySmall,
    LabelLarge,
    LabelMedium,
}

fn spec(ty: Ty) -> (f32, Weight) {
    match ty {
        Ty::HeadlineMedium => (28.0, Weight::Bold),
        Ty::HeadlineSmall => (24.0, Weight::Bold),
        Ty::TitleLarge => (22.0, Weight::Bold),
        Ty::TitleMedium => (16.0, Weight::Bold),
        Ty::TitleSmall => (14.0, Weight::Medium),
        Ty::BodyLarge => (16.0, Weight::Normal),
        Ty::BodyMedium => (14.0, Weight::Normal),
        Ty::BodySmall => (12.0, Weight::Normal),
        Ty::LabelLarge => (14.0, Weight::Bold),
        Ty::LabelMedium => (12.0, Weight::Medium),
    }
}

pub fn tx<'a>(ctx: &Ctx, ty: Ty, s: impl text::IntoFragment<'a>, color: Color) -> text::Text<'a> {
    let (size, weight) = spec(ty);
    text(s).size(size * ctx.ts).font(font(weight)).color(color)
}

pub fn txs<'a>(ctx: &Ctx, ty: Ty, s: impl text::IntoFragment<'a>, color: Color) -> text::Text<'a> {
    tx(ctx, ty, s, color).wrapping(text::Wrapping::None)
}

macro_rules! icons {
    ($($name:literal),*) => {
        fn icon_table() -> &'static HashMap<&'static str, svg::Handle> {
            static T: OnceLock<HashMap<&'static str, svg::Handle>> = OnceLock::new();
            T.get_or_init(|| {
                let mut m = HashMap::new();
                $(m.insert($name, svg::Handle::from_memory(include_bytes!(concat!("../assets/icons/", $name, ".svg")).as_slice()));)*
                m
            })
        }
    };
}

icons!(
    "add", "apps", "arrow_down", "arrow_up", "battery", "bell", "bolt", "check", "close", "code",
    "delete", "dns", "edit", "expand", "file", "info", "memory", "moon", "more", "open", "palette",
    "paste", "person", "power", "refresh", "screen", "select_all", "settings", "shield", "sun",
    "theme_auto", "tile", "tune"
);

pub fn icon<'a>(name: &str, size: f32, color: Color) -> svg::Svg<'a> {
    let handle = icon_table()
        .get(name)
        .cloned()
        .unwrap_or_else(|| icon_table()["info"].clone());
    svg(handle)
        .width(size)
        .height(size)
        .style(move |_, _| svg::Style { color: Some(color) })
}

pub fn pad(top: f32, right: f32, bottom: f32, left: f32) -> Padding {
    Padding {
        top,
        right,
        bottom,
        left,
    }
}

pub fn rounded(bg: Color, r: f32) -> container::Style {
    container::Style {
        background: Some(Background::Color(bg)),
        border: Border {
            radius: r.into(),
            ..Border::default()
        },
        ..container::Style::default()
    }
}

pub fn rounded4(bg: Color, tl: f32, tr: f32, br: f32, bl: f32) -> container::Style {
    container::Style {
        background: Some(Background::Color(bg)),
        border: Border {
            radius: Radius {
                top_left: tl,
                top_right: tr,
                bottom_right: br,
                bottom_left: bl,
            },
            ..Border::default()
        },
        ..container::Style::default()
    }
}

pub fn outlined(color: Color, width: f32, r: f32, bg: Option<Color>) -> container::Style {
    container::Style {
        background: bg.map(Background::Color),
        border: Border {
            color,
            width,
            radius: r.into(),
        },
        ..container::Style::default()
    }
}

pub fn gap<'a>(w: f32, h: f32) -> El<'a> {
    space().width(w).height(h).into()
}

pub fn hgap<'a>(w: f32) -> El<'a> {
    space().width(w).into()
}

pub fn vgap<'a>(h: f32) -> El<'a> {
    space().height(h).into()
}

pub fn fill<'a>() -> El<'a> {
    space().width(Length::Fill).into()
}

#[derive(Clone, Copy, PartialEq)]
pub enum Kind {
    Filled,
    Tonal,
    Text,
    Error,
}

fn state_layer(base: Color, content: Color, status: button::Status) -> Color {
    match status {
        button::Status::Hovered => lerp(base, content, 0.08),
        button::Status::Pressed => lerp(base, content, 0.12),
        _ => base,
    }
}

fn btn_style(
    bg: Option<Color>,
    fg: Color,
    disabled_bg: Color,
    disabled_fg: Color,
    r: f32,
) -> impl Fn(&iced::Theme, button::Status) -> button::Style {
    move |_, status| {
        if status == button::Status::Disabled {
            return button::Style {
                background: bg.map(|_| Background::Color(disabled_bg)),
                text_color: disabled_fg,
                border: Border {
                    radius: r.into(),
                    ..Border::default()
                },
                ..button::Style::default()
            };
        }
        let background = match bg {
            Some(b) => Some(Background::Color(state_layer(b, fg, status))),
            None => match status {
                button::Status::Hovered => Some(Background::Color(alpha(fg, 0.08))),
                button::Status::Pressed => Some(Background::Color(alpha(fg, 0.12))),
                _ => None,
            },
        };
        button::Style {
            background,
            text_color: fg,
            border: Border {
                radius: r.into(),
                ..Border::default()
            },
            shadow: Shadow::default(),
            snap: false,
        }
    }
}

fn colors(ctx: &Ctx, kind: Kind) -> (Option<Color>, Color) {
    let c = &ctx.c;
    match kind {
        Kind::Filled => (Some(c.primary), c.on_primary),
        Kind::Tonal => (Some(c.secondary_container), c.on_secondary_container),
        Kind::Text => (None, c.primary),
        Kind::Error => (Some(c.error_container), c.on_error_container),
    }
}

pub fn pill<'a>(
    ctx: &Ctx,
    icon_name: Option<&str>,
    label: &str,
    kind: Kind,
    height: f32,
    msg: Option<Message>,
) -> El<'a> {
    let (bg, fg) = colors(ctx, kind);
    let c = &ctx.c;
    let mut r = row![].spacing(8).align_y(Alignment::Center);
    if let Some(name) = icon_name {
        r = r.push(icon(name, 18.0, fg));
    }
    r = r.push(txs(ctx, Ty::LabelLarge, label.to_string(), fg));
    let left = if icon_name.is_some() { 16.0 } else { 24.0 };
    let enabled = msg.is_some();
    let mut b = button(container(r).height(Length::Fill).align_y(Alignment::Center))
        .padding(pad(0.0, 24.0, 0.0, left))
        .height(height)
        .style(btn_style(
            bg,
            fg,
            alpha(c.on_surface, 0.12),
            alpha(c.on_surface, 0.38),
            height / 2.0,
        ));
    if let Some(m) = msg {
        b = b.on_press(m);
    }
    let _ = enabled;
    container(b).center_y(Length::Shrink).into()
}

pub fn pill_busy<'a>(ctx: &Ctx, spinner: El<'a>, label: &str, kind: Kind, msg: Option<Message>) -> El<'a> {
    let (bg, fg) = colors(ctx, kind);
    let c = &ctx.c;
    let r = row![spinner, txs(ctx, Ty::LabelLarge, label.to_string(), fg)]
        .spacing(8)
        .align_y(Alignment::Center);
    let mut b = button(container(r).height(Length::Fill).align_y(Alignment::Center))
        .padding(pad(0.0, 24.0, 0.0, 16.0))
        .height(40)
        .style(btn_style(
            bg,
            fg,
            alpha(c.on_surface, 0.12),
            alpha(c.on_surface, 0.38),
            20.0,
        ));
    if let Some(m) = msg {
        b = b.on_press(m);
    }
    b.into()
}

pub fn icon_btn<'a>(
    ctx: &Ctx,
    name: &str,
    icon_size: f32,
    bg: Option<Color>,
    fg: Option<Color>,
    size: f32,
    msg: Option<Message>,
) -> El<'a> {
    let c = &ctx.c;
    let fg = fg.unwrap_or(c.on_surface_variant);
    let content = container(icon(name, icon_size, fg))
        .center_x(Length::Fixed(size))
        .center_y(Length::Fixed(size));
    let mut b = button(content)
        .padding(0)
        .width(size)
        .height(size)
        .style(btn_style(
            bg,
            fg,
            alpha(c.on_surface, 0.12),
            alpha(c.on_surface, 0.38),
            size / 2.0,
        ));
    if let Some(m) = msg {
        b = b.on_press(m);
    }
    b.into()
}

pub fn tonal_icon<'a>(ctx: &Ctx, name: &str, size: f32, msg: Option<Message>) -> El<'a> {
    icon_btn(
        ctx,
        name,
        size,
        Some(ctx.c.secondary_container),
        Some(ctx.c.on_secondary_container),
        40.0,
        msg,
    )
}

pub fn plain_icon<'a>(ctx: &Ctx, name: &str, size: f32, msg: Option<Message>) -> El<'a> {
    icon_btn(ctx, name, size, None, Some(ctx.c.on_surface_variant), 40.0, msg)
}

pub fn text_button<'a>(ctx: &Ctx, label: &str, color: Color, msg: Message) -> El<'a> {
    let c = &ctx.c;
    button(container(txs(ctx, Ty::LabelLarge, label.to_string(), color)).height(Length::Fill).align_y(Alignment::Center))
        .padding(pad(0.0, 12.0, 0.0, 12.0))
        .height(40)
        .style(btn_style(
            None,
            color,
            alpha(c.on_surface, 0.12),
            alpha(c.on_surface, 0.38),
            20.0,
        ))
        .on_press(msg)
        .into()
}

pub fn text_button_icon<'a>(ctx: &Ctx, name: &str, label: &str, color: Color, msg: Message) -> El<'a> {
    let c = &ctx.c;
    button(
        container(
            row![icon(name, 18.0, color), txs(ctx, Ty::LabelLarge, label.to_string(), color)]
                .spacing(8)
                .align_y(Alignment::Center),
        )
        .height(Length::Fill)
        .align_y(Alignment::Center),
    )
    .padding(pad(0.0, 12.0, 0.0, 12.0))
    .height(40)
    .style(btn_style(
        None,
        color,
        alpha(c.on_surface, 0.12),
        alpha(c.on_surface, 0.38),
        20.0,
    ))
    .on_press(msg)
    .into()
}

pub fn switch<'a>(ctx: &Ctx, on: bool, msg: Message) -> El<'a> {
    let c = &ctx.c;
    let track_bg = if on { c.primary } else { c.container_highest };
    let border = if on { c.primary } else { c.outline };
    let thumb_bg = if on { c.on_primary } else { c.outline };
    let thumb = if on { 24.0 } else { 16.0 };
    let inner: El<'a> = if on {
        container(icon("check", 16.0, c.primary))
            .center_x(Length::Fixed(24.0))
            .center_y(Length::Fixed(24.0))
            .style(move |_| rounded(thumb_bg, 12.0))
            .into()
    } else {
        container(space())
            .width(thumb)
            .height(thumb)
            .style(move |_| rounded(thumb_bg, 8.0))
            .into()
    };
    let pad_left = if on { 24.0 } else { 6.0 };
    let t = container(inner)
        .width(52)
        .height(32)
        .padding(pad(0.0, 0.0, 0.0, pad_left))
        .align_y(Alignment::Center)
        .style(move |_| outlined(border, 2.0, 16.0, Some(track_bg)));
    mouse_area(t).on_press(msg).into()
}

pub fn segmented<'a>(
    ctx: &Ctx,
    options: &[&str],
    selected: usize,
    enabled: bool,
    icons_list: Option<&[&str]>,
    on: impl Fn(usize) -> Message,
) -> El<'a> {
    let c = &ctx.c;
    let n = options.len();
    let mut r = row![].width(Length::Fill);
    for (i, label) in options.iter().enumerate() {
        let sel = selected == i;
        let fg = if sel { c.on_secondary_container } else { c.on_surface };
        let bg = if sel { Some(c.secondary_container) } else { None };
        let outline = c.outline;
        let left = if i == 0 { 20.0 } else { 0.0 };
        let right = if i == n - 1 { 20.0 } else { 0.0 };
        let mut content = row![].spacing(8).align_y(Alignment::Center);
        if sel {
            content = content.push(icon("check", 18.0, fg));
        } else if let Some(list) = icons_list {
            content = content.push(icon(list[i], 18.0, fg));
        }
        content = content.push(txs(ctx, Ty::LabelLarge, label.to_string(), fg));
        let cell = container(content)
            .width(Length::Fill)
            .height(40)
            .center_x(Length::Fill)
            .center_y(Length::Fixed(40.0))
            .style(move |_| container::Style {
                background: bg.map(Background::Color),
                border: Border {
                    color: outline,
                    width: 1.0,
                    radius: Radius {
                        top_left: left,
                        bottom_left: left,
                        top_right: right,
                        bottom_right: right,
                    },
                },
                ..container::Style::default()
            });
        let area = mouse_area(cell);
        let area = if enabled { area.on_press(on(i)) } else { area };
        r = r.push(container(area).width(Length::Fill));
    }
    let wrapped = container(r).width(Length::Fill);
    if enabled {
        wrapped.into()
    } else {
        container(wrapped).style(|_| container::Style::default()).into()
    }
}

pub fn m3_slider<'a>(
    ctx: &Ctx,
    range: std::ops::RangeInclusive<f32>,
    value: f32,
    on: impl Fn(f32) -> Message + 'a,
) -> El<'a> {
    let c = ctx.c;
    slider(range, value, on)
        .step(0.01)
        .style(move |_, _| slider::Style {
            rail: slider::Rail {
                backgrounds: (
                    Background::Color(c.primary),
                    Background::Color(c.secondary_container),
                ),
                width: 6.0,
                border: Border {
                    radius: 3.0.into(),
                    ..Border::default()
                },
            },
            handle: slider::Handle {
                shape: slider::HandleShape::Circle { radius: 10.0 },
                background: Background::Color(c.primary),
                border_width: 0.0,
                border_color: Color::TRANSPARENT,
            },
        })
        .into()
}

pub fn field<'a>(
    ctx: &Ctx,
    label: &str,
    placeholder: &str,
    value: &str,
    on_input: impl Fn(String) -> Message + 'a,
    on_submit: Option<Message>,
) -> El<'a> {
    let c = ctx.c;
    let input = text_input(placeholder, value)
        .on_input(on_input)
        .size(16.0 * ctx.ts)
        .font(font(Weight::Normal))
        .padding(pad(16.0, 16.0, 16.0, 16.0))
        .style(move |_, status| {
            let focused = matches!(status, text_input::Status::Focused { .. });
            text_input::Style {
                background: Background::Color(Color::TRANSPARENT),
                border: Border {
                    color: if focused { c.primary } else { c.outline },
                    width: if focused { 2.0 } else { 1.0 },
                    radius: 14.0.into(),
                },
                icon: c.on_surface_variant,
                placeholder: alpha(c.on_surface_variant, 0.7),
                value: c.on_surface,
                selection: alpha(c.primary, 0.35),
            }
        });
    let input = match on_submit {
        Some(m) => input.on_submit(m),
        None => input,
    };
    let lbl = container(txs(ctx, Ty::BodySmall, label.to_string(), c.on_surface_variant))
        .padding(pad(0.0, 4.0, 0.0, 4.0));
    column![
        container(lbl).padding(pad(0.0, 0.0, 0.0, 12.0)),
        container(input).padding(pad(0.0, 0.0, 0.0, 0.0)),
    ]
    .spacing(2)
    .into()
}

pub fn scroll_style(c: Scheme) -> impl Fn(&iced::Theme, scrollable::Status) -> scrollable::Style {
    move |_, status| {
        let active = matches!(
            status,
            scrollable::Status::Hovered { .. } | scrollable::Status::Dragged { .. }
        );
        let rail = scrollable::Rail {
            background: None,
            border: Border::default(),
            scroller: scrollable::Scroller {
                background: Background::Color(alpha(c.outline, if active { 0.7 } else { 0.35 })),
                border: Border {
                    radius: 3.0.into(),
                    ..Border::default()
                },
            },
        };
        scrollable::Style {
            container: container::Style::default(),
            vertical_rail: rail,
            horizontal_rail: rail,
            gap: None,
            auto_scroll: scrollable::AutoScroll {
                background: Background::Color(Color::TRANSPARENT),
                border: Border::default(),
                shadow: Shadow::default(),
                icon: Color::TRANSPARENT,
            },
        }
    }
}

pub fn scrollbox<'a>(ctx: &Ctx, content: impl Into<El<'a>>) -> scrollable::Scrollable<'a, Message> {
    scrollable(content)
        .direction(scrollable::Direction::Vertical(
            scrollable::Scrollbar::new().width(5).scroller_width(5).margin(2),
        ))
        .style(scroll_style(ctx.c))
}

pub fn cv<'a, P>(program: P, w: f32, h: f32) -> El<'a>
where
    P: canvas::Program<Message> + 'a,
{
    canvas(program).width(w).height(h).into()
}

pub fn group<'a>(ctx: &Ctx, title: &str, children: Vec<El<'a>>) -> El<'a> {
    let c = ctx.c;
    let inner = container(column(children)).padding(pad(6.0, 0.0, 6.0, 0.0)).width(Length::Fill).style(move |_| rounded(c.container_high, 28.0));
    column![
        container(txs(ctx, Ty::LabelLarge, title.to_string(), c.primary)).padding(pad(0.0, 0.0, 0.0, 12.0)),
        inner
    ]
    .spacing(8)
    .into()
}

pub fn setting_item<'a>(
    ctx: &Ctx,
    icon_name: &str,
    title: &str,
    subtitle: &str,
    below: bool,
    trailing: Option<El<'a>>,
    on_press: Option<Message>,
) -> El<'a> {
    let c = ctx.c;
    let badge = container(icon(icon_name, 22.0, c.on_secondary_container))
        .center_x(Length::Fixed(44.0))
        .center_y(Length::Fixed(44.0))
        .style(move |_| rounded(c.secondary_container, 22.0));
    let mut col = column![
        tx(ctx, Ty::TitleMedium, title.to_string(), c.on_surface),
        tx(ctx, Ty::BodyMedium, subtitle.to_string(), c.on_surface_variant),
    ]
    .width(Length::Fill);
    let mut r = row![badge, hgap(14.0)].align_y(Alignment::Center);
    let mut side: Option<El<'a>> = None;
    if let Some(t) = trailing {
        if below {
            col = col.push(vgap(10.0)).push(t);
        } else {
            side = Some(t);
        }
    }
    r = r.push(col);
    if let Some(t) = side {
        r = r.push(hgap(10.0)).push(t);
    }
    let body = container(r).padding(pad(12.0, 18.0, 12.0, 18.0)).width(Length::Fill);
    match on_press {
        Some(m) => mouse_area(body).on_press(m).into(),
        None => body.into(),
    }
}

pub fn block<'a>(children: Vec<El<'a>>) -> El<'a> {
    container(column(children).spacing(10))
        .padding(pad(10.0, 18.0, 10.0, 18.0))
        .width(Length::Fill)
        .into()
}
