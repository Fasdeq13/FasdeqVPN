use crate::theme::{alpha, lerp};
use iced::mouse;
use iced::widget::canvas::{self, gradient, path, Frame, Geometry, LineCap, Path, Stroke};
use iced::{Color, Point, Radians, Rectangle, Renderer, Size, Theme, Vector};
use std::f32::consts::PI;

pub const TAU: f32 = PI * 2.0;

fn circle(f: &mut Frame, c: Point, r: f32, color: Color) {
    f.fill(&Path::circle(c, r), color);
}

fn ring(f: &mut Frame, c: Point, r: f32, w: f32, color: Color) {
    f.stroke(
        &Path::circle(c, r),
        Stroke::default().with_width(w).with_color(color),
    );
}

pub fn blob_path(cx: f32, cy: f32, base: f32, lobes: f32, amp: f32, offset: f32) -> Path {
    Path::new(|b| {
        let steps = 120;
        for i in 0..=steps {
            let th = i as f32 * TAU / steps as f32;
            let rr = base * (1.0 + amp * (lobes * th + offset).cos());
            let p = Point::new(cx + rr * th.cos(), cy + rr * th.sin());
            if i == 0 {
                b.move_to(p);
            } else {
                b.line_to(p);
            }
        }
        b.close();
    })
}

fn arc_path(c: Point, r: f32, start: f32, sweep: f32) -> Path {
    Path::new(|b| {
        b.arc(path::Arc {
            center: c,
            radius: r,
            start_angle: Radians(start),
            end_angle: Radians(start + sweep),
        });
    })
}

#[derive(Clone, Copy)]
pub struct Launch {
    pub style: usize,
    pub phase: f32,
    pub amp: f32,
    pub orbit: f32,
    pub aura: f32,
    pub level: f32,
    pub burst: f32,
    pub body: Color,
    pub idle: Color,
    pub content: Color,
    pub ring: Color,
    pub accent: Color,
    pub base_ratio: f32,
    pub scale: f32,
}

pub fn draw_launch(f: &mut Frame, size: Size, p: &Launch) {
    let cx = size.width / 2.0;
    let cy = size.height / 2.0;
    let center = Point::new(cx, cy);
    let base = size.width.min(size.height) * p.base_ratio;
    f.translate(Vector::new(cx, cy));
    f.scale(p.scale);
    f.translate(Vector::new(-cx, -cy));
    let act = p.orbit.max(p.aura);
    if p.burst < 1.0 {
        ring(
            f,
            center,
            base * (1.0 + 1.25 * p.burst),
            16.0 * (1.0 - p.burst) + 2.0,
            alpha(p.ring, (1.0 - p.burst) * 0.55),
        );
        ring(
            f,
            center,
            base * (1.0 + 0.7 * p.burst),
            8.0 * (1.0 - p.burst) + 1.0,
            alpha(p.ring, (1.0 - p.burst) * 0.25),
        );
    }
    match p.style {
        1 => {
            for k in 0..4 {
                let t = (p.phase * (0.12 + 0.18 * p.orbit) + k as f32 * 0.25) % 1.0;
                ring(
                    f,
                    center,
                    base * (1.02 + 0.95 * t),
                    3.0,
                    alpha(p.ring, (1.0 - t) * (0.12 + 0.3 * act)),
                );
            }
            circle(f, center, base * (1.0 + 0.025 * (p.phase * 3.0).sin() * p.orbit), p.body);
        }
        2 => {
            for r in 0..3 {
                let radius = base * (1.28 + 0.27 * r as f32);
                ring(f, center, radius, 1.5, alpha(p.ring, 0.10 + 0.22 * act));
                let dir = if r % 2 == 0 { 1.0 } else { -1.0 };
                let count = 2 + r;
                for i in 0..count {
                    let a = dir * p.phase * (1.1 + 0.4 * r as f32) * (0.6 + 0.8 * p.orbit)
                        + i as f32 * (TAU / count as f32);
                    circle(
                        f,
                        Point::new(cx + radius * a.cos(), cy + radius * a.sin()),
                        4.0 + r as f32,
                        alpha(p.ring, 0.35 + 0.65 * act),
                    );
                }
            }
            circle(f, center, base, p.body);
        }
        3 => {
            circle(f, center, base, p.idle);
            if p.level > 0.01 {
                let top = cy + base - 2.0 * base * p.level;
                let wave_amp = base * 0.07 * (0.4 + 0.6 * (1.0 - p.level * 0.5));
                for layer in 0..2 {
                    let shift = if layer == 0 { 1.7 } else { 0.0 };
                    let path = Path::new(|b| {
                        let steps = 64;
                        let x0 = cx - base;
                        let w = 2.0 * base;
                        let mut lows = Vec::with_capacity(steps + 1);
                        for i in 0..=steps {
                            let x = x0 + w * i as f32 / steps as f32;
                            let dx = (x - cx).abs().min(base);
                            let h = (base * base - dx * dx).sqrt();
                            let wave = top + wave_amp * (x / base * 3.2 + p.phase * 2.0 + shift).sin();
                            let y = wave.clamp(cy - h, cy + h);
                            lows.push(Point::new(x, cy + h));
                            if i == 0 {
                                b.move_to(Point::new(x, y));
                            } else {
                                b.line_to(Point::new(x, y));
                            }
                        }
                        for pt in lows.into_iter().rev() {
                            b.line_to(pt);
                        }
                        b.close();
                    });
                    f.fill(
                        &path,
                        alpha(p.ring, if layer == 0 { 0.45 } else { 0.95 }),
                    );
                }
            }
        }
        4 => {
            circle(f, center, base, p.body);
            let strength = 0.25 + 0.75 * act;
            let spin = p.phase * (0.5 + p.orbit * 2.0);
            let widths = [18.0f32, 10.0, 4.0];
            let alphas = [0.10f32, 0.20, 0.90];
            let segments = 90;
            for g in 0..3 {
                let a = alphas[g] * strength;
                for i in 0..segments {
                    let t = i as f32 / segments as f32;
                    let col = if t < 1.0 / 3.0 {
                        alpha(p.ring, a * (t * 3.0))
                    } else if t < 2.0 / 3.0 {
                        alpha(lerp(p.ring, p.accent, (t - 1.0 / 3.0) * 3.0), a)
                    } else {
                        alpha(p.accent, a * (1.0 - (t - 2.0 / 3.0) * 3.0))
                    };
                    let start = spin + t * TAU;
                    f.stroke(
                        &arc_path(center, base * 1.2, start, TAU / segments as f32 + 0.02),
                        Stroke::default().with_width(widths[g]).with_color(col),
                    );
                }
            }
        }
        _ => {
            if p.aura > 0.01 {
                for k in 0..2 {
                    let t = (p.phase * 0.18 + k as f32 * 0.5) % 1.0;
                    circle(
                        f,
                        center,
                        base * (1.06 + 0.6 * t),
                        alpha(p.ring, (1.0 - t) * 0.22 * p.aura),
                    );
                }
            }
            if p.orbit > 0.01 {
                for i in 0..8 {
                    let a = p.phase * 1.6 + i as f32 * (TAU / 8.0);
                    let r = base
                        * (1.3 + 0.1 * (p.phase * 2.2 + i as f32).sin())
                        * (0.7 + 0.3 * p.orbit);
                    let dot = 3.5 + 2.5 * (p.phase * 3.0 + i as f32 * 1.3).sin().abs();
                    circle(
                        f,
                        Point::new(cx + r * a.cos(), cy + r * a.sin()),
                        dot,
                        alpha(p.ring, 0.85 * p.orbit),
                    );
                }
            }
            let path = Path::new(|b| {
                let steps = 140;
                for i in 0..=steps {
                    let th = i as f32 * TAU / steps as f32;
                    let rr = base * (1.0 + p.amp * (8.0 * th + p.phase).cos());
                    let pt = Point::new(cx + rr * th.cos(), cy + rr * th.sin());
                    if i == 0 {
                        b.move_to(pt);
                    } else {
                        b.line_to(pt);
                    }
                }
                b.close();
            });
            f.fill(&path, p.body);
            if p.orbit > 0.01 {
                let rad = base * 0.78;
                f.stroke(
                    &arc_path(
                        center,
                        rad,
                        p.phase * 1.2,
                        (90.0 + 60.0 * (p.phase * 2.0).sin()).to_radians(),
                    ),
                    Stroke::default()
                        .with_width(5.0)
                        .with_color(alpha(p.content, p.orbit * 0.9))
                        .with_line_cap(LineCap::Round),
                );
            }
        }
    }
}

pub struct LaunchCanvas(pub Launch);

impl<M> canvas::Program<M> for LaunchCanvas {
    type State = ();

    fn draw(
        &self,
        _: &(),
        renderer: &Renderer,
        _: &Theme,
        bounds: Rectangle,
        _: mouse::Cursor,
    ) -> Vec<Geometry> {
        let mut f = Frame::new(renderer, bounds.size());
        draw_launch(&mut f, bounds.size(), &self.0);
        vec![f.into_geometry()]
    }
}

pub struct Backdrop {
    pub t: f32,
    pub c1: Color,
    pub c2: Color,
    pub c3: Color,
}

impl<M> canvas::Program<M> for Backdrop {
    type State = ();

    fn draw(&self, _: &(), renderer: &Renderer, _: &Theme, bounds: Rectangle, _: mouse::Cursor) -> Vec<Geometry> {
        let mut f = Frame::new(renderer, bounds.size());
        let (w, h, t) = (bounds.width, bounds.height, self.t);
        f.fill(
            &blob_path(w * (0.15 + 0.05 * t.sin()), h * (0.35 + 0.05 * t.cos()), w * 0.22, 5.0, 0.08, t * 2.0),
            alpha(self.c1, 0.45),
        );
        f.fill(
            &blob_path(
                w * (0.88 + 0.04 * t.cos()),
                h * (0.55 + 0.06 * (t * 2.0).sin()),
                w * 0.26,
                6.0,
                0.07,
                -t * 2.0,
            ),
            alpha(self.c2, 0.40),
        );
        f.fill(
            &blob_path(
                w * (0.5 + 0.08 * (t * 3.0).sin()),
                h * (0.98 + 0.03 * (t * 2.0).cos()),
                w * 0.3,
                4.0,
                0.09,
                t,
            ),
            alpha(self.c3, 0.35),
        );
        vec![f.into_geometry()]
    }
}

pub struct ConnectWave {
    pub p: f32,
    pub color: Color,
    pub y: f32,
}

impl<M> canvas::Program<M> for ConnectWave {
    type State = ();

    fn draw(&self, _: &(), renderer: &Renderer, _: &Theme, bounds: Rectangle, _: mouse::Cursor) -> Vec<Geometry> {
        let mut f = Frame::new(renderer, bounds.size());
        if self.p < 1.0 {
            let center = Point::new(bounds.width / 2.0, self.y);
            let max_r = bounds.width.hypot(bounds.height);
            ring(&mut f, center, max_r * self.p, 110.0, alpha(self.color, (1.0 - self.p) * 0.24));
            circle(&mut f, center, max_r * self.p * 0.75, alpha(self.color, (1.0 - self.p) * 0.10));
        }
        vec![f.into_geometry()]
    }
}

pub struct Illustration {
    pub t: f32,
    pub bob: f32,
    pub back: Color,
    pub rack: Color,
    pub rack_edge: Color,
    pub primary: Color,
    pub secondary: Color,
    pub tertiary: Color,
    pub shield_fill: Color,
    pub on_shield: Color,
}

fn sparkle(f: &mut Frame, x: f32, y: f32, r: f32, color: Color) {
    let p = Path::new(|b| {
        b.move_to(Point::new(x, y - r));
        b.quadratic_curve_to(Point::new(x, y), Point::new(x + r, y));
        b.quadratic_curve_to(Point::new(x, y), Point::new(x, y + r));
        b.quadratic_curve_to(Point::new(x, y), Point::new(x - r, y));
        b.quadratic_curve_to(Point::new(x, y), Point::new(x, y - r));
        b.close();
    });
    f.fill(&p, color);
}

fn round_rect(x: f32, y: f32, w: f32, h: f32, r: f32) -> Path {
    Path::rounded_rectangle(Point::new(x, y), Size::new(w, h), r.into())
}

impl<M> canvas::Program<M> for Illustration {
    type State = ();

    fn draw(&self, _: &(), renderer: &Renderer, _: &Theme, bounds: Rectangle, _: mouse::Cursor) -> Vec<Geometry> {
        let mut f = Frame::new(renderer, bounds.size());
        let (w, h, t) = (bounds.width, bounds.height, self.t);
        let (cx, cy) = (w / 2.0, h / 2.0);
        f.fill(&blob_path(cx, cy, w * 0.36, 7.0, 0.06, t), self.back);
        f.fill(&blob_path(cx, cy, w * 0.28, 5.0, 0.05, -t * 1.5), alpha(self.back, 0.55));
        let box_w = w * 0.46;
        let box_h = h * 0.15;
        let left = cx - box_w / 2.0 - w * 0.06;
        for i in 0..3 {
            let top = cy - h * 0.2 + i as f32 * (box_h + h * 0.04);
            let path = round_rect(left, top, box_w, box_h, box_h * 0.4);
            f.fill(&path, self.rack);
            f.stroke(&path, Stroke::default().with_width(1.5).with_color(self.rack_edge));
            let led_y = top + box_h / 2.0;
            for j in 0..3 {
                let a = 0.35 + 0.65 * (t * 2.0 + i as f32 * 1.3 + j as f32 * 0.9).sin().abs();
                let c = match j {
                    0 => self.primary,
                    1 => self.secondary,
                    _ => self.tertiary,
                };
                circle(
                    &mut f,
                    Point::new(left + box_h * 0.45 + j as f32 * box_h * 0.3, led_y),
                    box_h * 0.11,
                    alpha(c, a),
                );
            }
            f.fill(
                &round_rect(left + box_w * 0.55, led_y - box_h * 0.06, box_w * 0.35, box_h * 0.12, box_h * 0.06),
                self.rack_edge,
            );
        }
        let sx = cx + w * 0.24;
        let sy = cy - h * 0.2 + self.bob;
        let sw = w * 0.17;
        let sh = h * 0.22;
        let shield = Path::new(|b| {
            b.move_to(Point::new(sx, sy - sh));
            b.line_to(Point::new(sx + sw, sy - sh * 0.55));
            b.line_to(Point::new(sx + sw, sy + sh * 0.05));
            b.bezier_curve_to(
                Point::new(sx + sw, sy + sh * 0.6),
                Point::new(sx + sw * 0.5, sy + sh * 0.9),
                Point::new(sx, sy + sh),
            );
            b.bezier_curve_to(
                Point::new(sx - sw * 0.5, sy + sh * 0.9),
                Point::new(sx - sw, sy + sh * 0.6),
                Point::new(sx - sw, sy + sh * 0.05),
            );
            b.line_to(Point::new(sx - sw, sy - sh * 0.55));
            b.close();
        });
        f.fill(&shield, self.shield_fill);
        f.stroke(&shield, Stroke::default().with_width(2.5).with_color(self.primary));
        let check = Path::new(|b| {
            b.move_to(Point::new(sx - sw * 0.45, sy + sh * 0.02));
            b.line_to(Point::new(sx - sw * 0.1, sy + sh * 0.35));
            b.line_to(Point::new(sx + sw * 0.5, sy - sh * 0.3));
        });
        f.stroke(
            &check,
            Stroke::default()
                .with_width(3.5)
                .with_color(self.on_shield)
                .with_line_cap(LineCap::Round),
        );
        for k in 0..4 {
            let a = 0.25 + 0.75 * (t * 2.0 + k as f32 * 1.6).sin().abs();
            let px = cx + w * (0.34 * (k as f32 * 1.7 + 0.4).cos());
            let py = cy + h * (0.34 * (k as f32 * 2.1 + 0.2).sin());
            sparkle(&mut f, px, py, 4.0 + 3.0 * a, alpha(self.primary, a));
        }
        vec![f.into_geometry()]
    }
}

pub struct HeaderBlobs {
    pub spin: f32,
    pub accent: Color,
    pub primary: Color,
    pub lobes: (f32, f32),
}

impl<M> canvas::Program<M> for HeaderBlobs {
    type State = ();

    fn draw(&self, _: &(), renderer: &Renderer, _: &Theme, bounds: Rectangle, _: mouse::Cursor) -> Vec<Geometry> {
        let mut f = Frame::new(renderer, bounds.size());
        let (cx, cy) = (bounds.width / 2.0, bounds.height / 2.0);
        let m = bounds.width.min(bounds.height);
        f.fill(&blob_path(cx, cy, m * 0.4, 6.0, 0.07, -self.spin * 1.3), self.accent);
        f.fill(&blob_path(cx, cy, m * 0.36, 8.0, 0.06, self.spin), self.primary);
        vec![f.into_geometry()]
    }
}

pub struct CookieLogo {
    pub spin: f32,
    pub pulse: f32,
    pub primary: Color,
    pub tertiary: Color,
}

impl<M> canvas::Program<M> for CookieLogo {
    type State = ();

    fn draw(&self, _: &(), renderer: &Renderer, _: &Theme, bounds: Rectangle, _: mouse::Cursor) -> Vec<Geometry> {
        let mut f = Frame::new(renderer, bounds.size());
        let (cx, cy) = (bounds.width / 2.0, bounds.height / 2.0);
        let base = bounds.width.min(bounds.height) * 0.4;
        f.fill(
            &blob_path(cx, cy, base, 6.0, self.pulse, -self.spin * 1.4),
            alpha(self.tertiary, 0.45),
        );
        f.fill(&blob_path(cx, cy, base, 8.0, self.pulse, self.spin), self.primary);
        vec![f.into_geometry()]
    }
}

pub struct BorderRing {
    pub angle: f32,
    pub c1: Color,
    pub c2: Color,
    pub c3: Color,
    pub radius: f32,
    pub width: f32,
}

impl<M> canvas::Program<M> for BorderRing {
    type State = ();

    fn draw(&self, _: &(), renderer: &Renderer, _: &Theme, bounds: Rectangle, _: mouse::Cursor) -> Vec<Geometry> {
        let mut f = Frame::new(renderer, bounds.size());
        let c = Point::new(bounds.width / 2.0, bounds.height / 2.0);
        let r = bounds.width.max(bounds.height) / 2.0;
        let a = self.angle;
        let start = Point::new(c.x - r * a.cos(), c.y - r * a.sin());
        let end = Point::new(c.x + r * a.cos(), c.y + r * a.sin());
        let grad = gradient::Linear::new(start, end)
            .add_stop(0.0, self.c1)
            .add_stop(0.33, self.c2)
            .add_stop(0.66, self.c3)
            .add_stop(1.0, self.c1);
        let half = self.width / 2.0;
        let path = round_rect(
            half,
            half,
            bounds.width - self.width,
            bounds.height - self.width,
            self.radius,
        );
        f.stroke(
            &path,
            Stroke {
                width: self.width,
                style: canvas::Style::Gradient(grad.into()),
                ..Stroke::default()
            },
        );
        vec![f.into_geometry()]
    }
}

pub struct Spinner {
    pub t: f32,
    pub color: Color,
    pub width: f32,
}

impl<M> canvas::Program<M> for Spinner {
    type State = ();

    fn draw(&self, _: &(), renderer: &Renderer, _: &Theme, bounds: Rectangle, _: mouse::Cursor) -> Vec<Geometry> {
        let mut f = Frame::new(renderer, bounds.size());
        let c = Point::new(bounds.width / 2.0, bounds.height / 2.0);
        let r = bounds.width.min(bounds.height) / 2.0 - self.width / 2.0;
        let sweep = (40.0 + 200.0 * (0.5 + 0.5 * (self.t * 2.4).sin())).to_radians();
        f.stroke(
            &arc_path(c, r, self.t * 5.0, sweep),
            Stroke::default()
                .with_width(self.width)
                .with_color(self.color)
                .with_line_cap(LineCap::Round),
        );
        vec![f.into_geometry()]
    }
}

pub struct ServiceLogo {
    pub index: usize,
    pub alpha: f32,
    pub best: bool,
    pub failed: bool,
    pub ring: Color,
    pub error: Color,
}

impl<M> canvas::Program<M> for ServiceLogo {
    type State = ();

    fn draw(&self, _: &(), renderer: &Renderer, _: &Theme, bounds: Rectangle, _: mouse::Cursor) -> Vec<Geometry> {
        let mut f = Frame::new(renderer, bounds.size());
        let s = bounds.width.min(bounds.height);
        let c = Point::new(s / 2.0, s / 2.0);
        if self.best {
            circle(&mut f, c, s * 0.78, alpha(self.ring, 0.25));
            ring(&mut f, c, s * 0.72, 1.5, self.ring);
        }
        let a = self.alpha;
        let white = Color { a, ..Color::WHITE };
        match self.index {
            0 => {
                let discord = Color { a, ..crate::theme::rgb(0x5865F2) };
                f.fill(&round_rect(0.0, 0.0, s, s, s * 0.28), discord);
                f.fill(&round_rect(s * 0.2, s * 0.28, s * 0.6, s * 0.42, s * 0.2), white);
                circle(&mut f, Point::new(s * 0.3, s * 0.72), s * 0.07, white);
                circle(&mut f, Point::new(s * 0.7, s * 0.72), s * 0.07, white);
                f.fill(&round_rect(s * 0.34, s * 0.43, s * 0.08, s * 0.13, s * 0.04), discord);
                f.fill(&round_rect(s * 0.58, s * 0.43, s * 0.08, s * 0.13, s * 0.04), discord);
            }
            1 => {
                let tg = Color { a, ..crate::theme::rgb(0x2AABEE) };
                circle(&mut f, c, s / 2.0, tg);
                let plane = Path::new(|b| {
                    b.move_to(Point::new(s * 0.22, s * 0.50));
                    b.line_to(Point::new(s * 0.78, s * 0.27));
                    b.line_to(Point::new(s * 0.66, s * 0.74));
                    b.line_to(Point::new(s * 0.48, s * 0.60));
                    b.line_to(Point::new(s * 0.38, s * 0.70));
                    b.line_to(Point::new(s * 0.39, s * 0.56));
                    b.close();
                });
                f.fill(&plane, white);
                let fold = Path::new(|b| {
                    b.move_to(Point::new(s * 0.39, s * 0.56));
                    b.line_to(Point::new(s * 0.70, s * 0.36));
                    b.line_to(Point::new(s * 0.48, s * 0.60));
                    b.close();
                });
                f.fill(&fold, alpha(tg, a * 0.35));
            }
            _ => {
                let yt = Color { a, ..crate::theme::rgb(0xFF0000) };
                f.fill(&round_rect(0.0, s * 0.14, s, s * 0.72, s * 0.22), yt);
                let play = Path::new(|b| {
                    b.move_to(Point::new(s * 0.42, s * 0.34));
                    b.line_to(Point::new(s * 0.42, s * 0.66));
                    b.line_to(Point::new(s * 0.68, s * 0.50));
                    b.close();
                });
                f.fill(&play, white);
            }
        }
        if self.failed {
            f.stroke(
                &Path::line(Point::new(s * 0.12, s * 0.88), Point::new(s * 0.88, s * 0.12)),
                Stroke::default()
                    .with_width(s * 0.1)
                    .with_color(self.error)
                    .with_line_cap(LineCap::Round),
            );
        }
        vec![f.into_geometry()]
    }
}
