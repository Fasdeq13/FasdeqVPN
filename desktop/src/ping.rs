use crate::model::{PingResult, Server};
use std::io::Read;
use std::net::{SocketAddr, TcpStream, ToSocketAddrs, UdpSocket};
use std::process::{Command, Stdio};
use std::time::{Duration, Instant};

pub const FAIL: i32 = -1;
pub const SILENT: i32 = -2;

pub fn target(endpoint: &str) -> Option<(String, u16)> {
    let e = endpoint.trim();
    if e.is_empty() {
        return None;
    }
    let host;
    let port: u32;
    if e.starts_with('[') {
        let end = e.find(']')?;
        host = e[1..end].to_string();
        port = e[end + 1..].trim_start_matches(':').parse().ok()?;
    } else {
        let c = e.rfind(':').filter(|c| *c > 0)?;
        host = e[..c].to_string();
        port = e[c + 1..].parse().ok()?;
    }
    if host.is_empty() || !(1..=65535).contains(&port) {
        None
    } else {
        Some((host, port as u16))
    }
}

fn resolve(host: &str, port: u16) -> Option<SocketAddr> {
    (host, port).to_socket_addrs().ok()?.next()
}

fn ms(t0: Instant) -> i32 {
    (t0.elapsed().as_millis() as i32).max(1)
}

pub fn tcp(host: &str, port: u16, timeout: u64) -> i32 {
    let Some(addr) = resolve(host, port) else {
        return FAIL;
    };
    let t0 = Instant::now();
    match TcpStream::connect_timeout(&addr, Duration::from_millis(timeout)) {
        Ok(_) => ms(t0),
        Err(_) => FAIL,
    }
}

pub fn udp(host: &str, port: u16, timeout: u64) -> i32 {
    let Some(addr) = resolve(host, port) else {
        return FAIL;
    };
    let bind = if addr.is_ipv6() { "[::]:0" } else { "0.0.0.0:0" };
    let Ok(socket) = UdpSocket::bind(bind) else {
        return FAIL;
    };
    if socket.connect(addr).is_err() {
        return FAIL;
    }
    let _ = socket.set_read_timeout(Some(Duration::from_millis(timeout)));
    let payload: [u8; 16] = rand::random();
    let t0 = Instant::now();
    if socket.send(&payload).is_err() {
        return FAIL;
    }
    let mut buf = [0u8; 512];
    match socket.recv(&mut buf) {
        Ok(_) => ms(t0),
        Err(e) => match e.kind() {
            std::io::ErrorKind::ConnectionRefused | std::io::ErrorKind::ConnectionReset => ms(t0),
            std::io::ErrorKind::WouldBlock | std::io::ErrorKind::TimedOut => SILENT,
            _ => SILENT,
        },
    }
}

fn parse_time(out: &str) -> Option<f64> {
    let lower = out.to_lowercase();
    let mut search = 0;
    while let Some(pos) = lower[search..].find("time") {
        let after = search + pos + 4;
        let rest = &lower[after..];
        let rest = rest.trim_start();
        if let Some(r) = rest.strip_prefix('=').or_else(|| rest.strip_prefix('<')) {
            let digits: String = r
                .trim_start()
                .chars()
                .take_while(|c| c.is_ascii_digit() || *c == '.')
                .collect();
            if let Ok(v) = digits.parse::<f64>() {
                return Some(v);
            }
        }
        search = after;
    }
    None
}

pub fn icmp(host: &str, timeout: u64) -> i32 {
    let seconds = ((timeout / 1000).max(1)).to_string();
    let millis = timeout.max(1000).to_string();
    let mut cmd = Command::new("ping");
    if cfg!(target_os = "windows") {
        cmd.args(["-n", "1", "-w", &millis, host]);
    } else if cfg!(target_os = "macos") {
        cmd.args(["-c", "1", "-t", &seconds, host]);
    } else {
        cmd.args(["-c", "1", "-W", &seconds, host]);
    }
    cmd.stdout(Stdio::piped()).stderr(Stdio::null()).stdin(Stdio::null());
    crate::xray::hide_window(&mut cmd);
    let t0 = Instant::now();
    let Ok(mut child) = cmd.spawn() else {
        return FAIL;
    };
    let mut stdout = child.stdout.take();
    let reader = std::thread::spawn(move || {
        let mut out = Vec::new();
        if let Some(s) = stdout.as_mut() {
            let _ = s.read_to_end(&mut out);
        }
        String::from_utf8_lossy(&out).to_string()
    });
    let limit = Instant::now() + Duration::from_secs(timeout / 1000 + 3);
    let success = loop {
        match child.try_wait() {
            Ok(Some(status)) => break status.success(),
            Ok(None) => {
                if Instant::now() > limit {
                    let _ = child.kill();
                    let _ = child.wait();
                    break false;
                }
                std::thread::sleep(Duration::from_millis(25));
            }
            Err(_) => break false,
        }
    };
    let out = reader.join().unwrap_or_default();
    match parse_time(&out) {
        Some(v) => (v as i32).max(1),
        None => {
            if success {
                ms(t0)
            } else {
                FAIL
            }
        }
    }
}

pub fn measure(server: &Server, mode: usize, timeout: u64) -> PingResult {
    let Some((host, port)) = target(&server.endpoint) else {
        return PingResult {
            tcp: Some(FAIL),
            ..Default::default()
        };
    };
    let udp_native = server.conf.to_lowercase().contains("[interface]");
    match mode {
        1 => PingResult {
            tcp: Some(tcp(&host, port, timeout)),
            ..Default::default()
        },
        2 => {
            let u = udp(&host, port, timeout);
            if u == SILENT {
                let i = icmp(&host, timeout);
                if i > 0 {
                    return PingResult {
                        icmp: Some(i),
                        ..Default::default()
                    };
                }
            }
            PingResult {
                udp: Some(u),
                ..Default::default()
            }
        }
        3 => {
            let h = host.clone();
            let worker = std::thread::spawn(move || tcp(&h, port, timeout));
            let u = udp(&host, port, timeout);
            let t = worker.join().unwrap_or(FAIL);
            PingResult {
                tcp: Some(t),
                udp: Some(u),
                ..Default::default()
            }
        }
        4 => PingResult {
            icmp: Some(icmp(&host, timeout)),
            ..Default::default()
        },
        _ => {
            if udp_native {
                let i = icmp(&host, timeout);
                if i > 0 {
                    PingResult {
                        icmp: Some(i),
                        ..Default::default()
                    }
                } else {
                    PingResult {
                        udp: Some(udp(&host, port, timeout)),
                        ..Default::default()
                    }
                }
            } else {
                PingResult {
                    tcp: Some(tcp(&host, port, timeout)),
                    ..Default::default()
                }
            }
        }
    }
}
