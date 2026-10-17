use iced::Color;

#[derive(Clone, Copy)]
pub struct Palette {
    pub primary: Color,
    pub on_primary: Color,
    pub primary_container: Color,
    pub on_primary_container: Color,
    pub secondary: Color,
    pub on_secondary: Color,
    pub secondary_container: Color,
    pub on_secondary_container: Color,
    pub background: Color,
    pub on_surface: Color,
    pub on_surface_variant: Color,
    pub surface: Color,
    pub surface_variant: Color,
    pub outline: Color,
    pub error: Color,
    pub error_container: Color,
    pub on_error_container: Color,
    pub on_error: Color,
}

fn hex(r: u8, g: u8, b: u8) -> Color {
    Color::from_rgb(r as f32 / 255.0, g as f32 / 255.0, b as f32 / 255.0)
}

pub fn build(mode: usize) -> Palette {
    match mode {
        1 => light_palette(),
        2 => dark_palette(),
        _ => light_palette(),
    }
}

fn light_palette() -> Palette {
    Palette {
        primary: hex(103, 80, 164),
        on_primary: hex(255, 255, 255),
        primary_container: hex(234, 221, 255),
        on_primary_container: hex(33, 0, 93),
        secondary: hex(98, 91, 113),
        on_secondary: hex(255, 255, 255),
        secondary_container: hex(232, 222, 248),
        on_secondary_container: hex(30, 25, 43),
        background: hex(255, 251, 254),
        on_surface: hex(28, 27, 31),
        on_surface_variant: hex(73, 69, 79),
        surface: hex(242, 238, 246),
        surface_variant: hex(231, 224, 236),
        outline: hex(121, 116, 126),
        error: hex(179, 38, 30),
        error_container: hex(255, 218, 214),
        on_error_container: hex(65, 0, 2),
        on_error: hex(255, 255, 255),
    }
}

fn dark_palette() -> Palette {
    Palette {
        primary: hex(208, 188, 255),
        on_primary: hex(55, 30, 115),
        primary_container: hex(79, 55, 139),
        on_primary_container: hex(234, 221, 255),
        secondary: hex(204, 194, 220),
        on_secondary: hex(51, 45, 65),
        secondary_container: hex(74, 68, 88),
        on_secondary_container: hex(232, 222, 248),
        background: hex(28, 27, 31),
        on_surface: hex(230, 225, 229),
        on_surface_variant: hex(202, 196, 208),
        surface: hex(44, 43, 47),
        surface_variant: hex(73, 69, 79),
        outline: hex(148, 143, 153),
        error: hex(242, 184, 181),
        error_container: hex(140, 29, 24),
        on_error_container: hex(255, 218, 214),
        on_error: hex(96, 20, 16),
    }
}
