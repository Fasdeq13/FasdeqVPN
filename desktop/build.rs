fn main() {
    println!("cargo::rustc-check-cfg=cfg(mobile)");
    let target = std::env::var("CARGO_CFG_TARGET_OS").unwrap_or_default();
    if target == "android" || std::env::var("CARGO_FEATURE_SIM").is_ok() {
        println!("cargo::rustc-cfg=mobile");
    }
    if target == "windows" {
        let mut res = winresource::WindowsResource::new();
        res.set_icon("assets/app.ico");
        res.set("ProductName", "FasdeqVPN");
        res.set("FileDescription", "FasdeqVPN");
        let _ = res.compile();
    }
}
