use std::sync::atomic::{AtomicU32, Ordering};

static SCALE: AtomicU32 = AtomicU32::new(0x3f80_0000);

pub fn set_scale(v: f32) {
    SCALE.store(v.to_bits(), Ordering::Relaxed);
}

pub fn scale() -> f32 {
    f32::from_bits(SCALE.load(Ordering::Relaxed))
}

pub fn d(ms: f32) -> f32 {
    (ms * scale()).max(1.0)
}

pub fn k(value: f32) -> f32 {
    let s = scale();
    (value / (s * s)).clamp(15.0, 30000.0)
}

#[derive(Clone, Copy, Debug)]
pub struct Spring {
    pub x: f32,
    pub v: f32,
    pub target: f32,
    ratio: f32,
    stiffness: f32,
}

impl Spring {
    pub fn new(x: f32, ratio: f32, stiffness: f32) -> Spring {
        Spring {
            x,
            v: 0.0,
            target: x,
            ratio,
            stiffness,
        }
    }

    pub fn to(&mut self, target: f32) {
        self.target = target;
    }

    pub fn snap(&mut self, value: f32) {
        self.x = value;
        self.target = value;
        self.v = 0.0;
    }

    pub fn settled(&self) -> bool {
        (self.x - self.target).abs() < 0.0005 && self.v.abs() < 0.0005
    }

    pub fn step(&mut self, dt: f32) {
        if self.settled() {
            self.x = self.target;
            self.v = 0.0;
            return;
        }
        let stiff = k(self.stiffness);
        let c = 2.0 * self.ratio * stiff.sqrt();
        let steps = ((dt / 0.004).ceil() as i32).clamp(1, 20);
        let h = dt / steps as f32;
        for _ in 0..steps {
            let a = -stiff * (self.x - self.target) - c * self.v;
            self.v += a * h;
            self.x += self.v * h;
        }
    }
}

fn bezier(t: f32, x1: f32, y1: f32, x2: f32, y2: f32) -> f32 {
    let mut u = t;
    for _ in 0..8 {
        let cx = 3.0 * x1 * u * (1.0 - u).powi(2) + 3.0 * x2 * u * u * (1.0 - u) + u.powi(3);
        let dx = 3.0 * x1 * (1.0 - u).powi(2) + 6.0 * (x2 - x1) * u * (1.0 - u) + 3.0 * (1.0 - x2) * u * u;
        if dx.abs() < 1e-5 {
            break;
        }
        u -= (cx - t) / dx;
        u = u.clamp(0.0, 1.0);
    }
    3.0 * y1 * u * (1.0 - u).powi(2) + 3.0 * y2 * u * u * (1.0 - u) + u.powi(3)
}

pub fn ease(t: f32) -> f32 {
    bezier(t.clamp(0.0, 1.0), 0.4, 0.0, 0.2, 1.0)
}

#[derive(Clone, Copy, Debug)]
pub struct Tween {
    from: f32,
    to: f32,
    elapsed: f32,
    dur: f32,
    pub value: f32,
}

impl Tween {
    pub fn new(x: f32, dur_ms: f32) -> Tween {
        Tween {
            from: x,
            to: x,
            elapsed: dur_ms,
            dur: dur_ms,
            value: x,
        }
    }

    pub fn to(&mut self, target: f32) {
        if (target - self.to).abs() < 1e-6 {
            return;
        }
        self.from = self.value;
        self.to = target;
        self.elapsed = 0.0;
    }

    pub fn snap(&mut self, v: f32) {
        self.from = v;
        self.to = v;
        self.value = v;
        self.elapsed = self.dur;
    }

    pub fn step(&mut self, dt: f32) {
        if self.elapsed >= d(self.dur) {
            self.value = self.to;
            return;
        }
        self.elapsed += dt * 1000.0;
        let t = (self.elapsed / d(self.dur)).clamp(0.0, 1.0);
        self.value = self.from + (self.to - self.from) * ease(t);
    }
}
