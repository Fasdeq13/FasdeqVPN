use fasdeq_core::{importer, links, model::Server, ping};
use serde_json::{json, Value};

pub fn parse_link(conf: &str) -> String {
    match links::parse(conf) {
        Ok(p) => json!({"ok": true, "name": p.name, "endpoint": p.endpoint, "outbound": p.outbound}).to_string(),
        Err(e) => json!({"ok": false, "error": e}).to_string(),
    }
}

pub fn prepare_import(text: &str) -> String {
    let p = importer::prepare(text);
    match p.error {
        Some(e) => json!({"ok": false, "error": e}).to_string(),
        None => json!({
            "ok": true,
            "items": p.items.iter().map(|s| json!({"value": s.value, "url": s.is_url, "name": s.name})).collect::<Vec<_>>()
        }).to_string(),
    }
}

pub fn fetch_subscription(url: &str) -> String {
    match links::fetch_meta(url) {
        Ok(f) => {
            let body = links::decode_subscription(&f.body);
            json!({"ok": true, "group": f.group, "body": body}).to_string()
        }
        Err(e) => json!({"ok": false, "error": e}).to_string(),
    }
}

pub fn measure_ping(conf: &str, endpoint: &str, mode: i32, timeout_ms: u64) -> String {
    let server = Server {
        id: String::new(),
        name: String::new(),
        conf: conf.to_string(),
        endpoint: endpoint.to_string(),
        group: String::new(),
    };
    let r = ping::measure(&server, mode.max(0) as usize, timeout_ms);
    json!({"tcp": r.tcp, "udp": r.udp, "icmp": r.icmp, "best": r.best()}).to_string()
}

pub fn inner_config(outbound: &str, socks: u16, http: u16, dns: &[String]) -> String {
    match serde_json::from_str::<Value>(outbound) {
        Ok(o) => json!({"ok": true, "config": links::inner_config(o, socks, http, None, dns)}).to_string(),
        Err(e) => json!({"ok": false, "error": e.to_string()}).to_string(),
    }
}
