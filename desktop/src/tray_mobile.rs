use iced::futures::Stream;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum TrayCmd {
    Show,
    Toggle,
    Quit,
}

pub fn available() -> bool {
    false
}

pub fn send(_cmd: TrayCmd) {}

fn render(size: u32) -> resvg::tiny_skia::Pixmap {
    let data = include_bytes!("../assets/app.svg");
    let tree = resvg::usvg::Tree::from_data(data, &resvg::usvg::Options::default()).expect("app icon");
    let mut pixmap = resvg::tiny_skia::Pixmap::new(size, size).expect("pixmap");
    let scale = size as f32 / tree.size().width();
    resvg::render(
        &tree,
        resvg::tiny_skia::Transform::from_scale(scale, scale),
        &mut pixmap.as_mut(),
    );
    pixmap
}

pub fn raster(size: u32) -> (Vec<u8>, u32, u32) {
    let pixmap = render(size);
    let mut out = Vec::with_capacity((size * size * 4) as usize);
    for p in pixmap.pixels() {
        let c = p.demultiply();
        out.extend_from_slice(&[c.red(), c.green(), c.blue(), c.alpha()]);
    }
    (out, size, size)
}

pub fn png(size: u32) -> Vec<u8> {
    render(size).encode_png().unwrap_or_default()
}

pub fn init() {}

pub fn stream() -> impl Stream<Item = TrayCmd> {
    iced::futures::stream::pending::<TrayCmd>()
}
