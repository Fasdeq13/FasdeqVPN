use serde_json::{json, Value};
use std::collections::HashMap;
use std::ffi::{c_char, c_int, c_long, c_void, CStr, CString};
use std::sync::atomic::{AtomicI64, Ordering};
use std::sync::{Mutex, OnceLock};

type InfoFn = unsafe extern "C" fn() -> *const c_char;
type PrepareFn = unsafe extern "C" fn(*const c_char) -> *const c_char;
type StartFn = unsafe extern "C" fn(*const c_char, c_int) -> c_int;
type StopFn = unsafe extern "C" fn();
type PingFn = unsafe extern "C" fn(*const c_char, c_int) -> c_int;
type ErrorFn = unsafe extern "C" fn() -> *const c_char;
type StatsFn = unsafe extern "C" fn() -> *const c_char;
type AbiFn = unsafe extern "C" fn() -> c_int;

struct Plugin {
    lib: usize,
    info: InfoFn,
    prepare: Option<PrepareFn>,
    start: Option<StartFn>,
    stop: Option<StopFn>,
    ping: Option<PingFn>,
    error: Option<ErrorFn>,
    stats: Option<StatsFn>,
}

static NEXT: AtomicI64 = AtomicI64::new(1);

fn table() -> &'static Mutex<HashMap<i64, Plugin>> {
    static T: OnceLock<Mutex<HashMap<i64, Plugin>>> = OnceLock::new();
    T.get_or_init(|| Mutex::new(HashMap::new()))
}

unsafe fn sym<T: Copy>(lib: *mut c_void, name: &str) -> Option<T> {
    let c = CString::new(name).ok()?;
    let p = libc::dlsym(lib, c.as_ptr());
    if p.is_null() {
        None
    } else {
        Some(std::mem::transmute_copy::<*mut c_void, T>(&p))
    }
}

unsafe fn text(p: *const c_char) -> String {
    if p.is_null() {
        String::new()
    } else {
        CStr::from_ptr(p).to_string_lossy().into_owned()
    }
}

pub fn open(path: &str) -> Value {
    let Ok(c) = CString::new(path) else {
        return json!({"ok": false, "error": "bad path"});
    };
    unsafe {
        let lib = libc::dlopen(c.as_ptr(), libc::RTLD_NOW | libc::RTLD_LOCAL);
        if lib.is_null() {
            let err = text(libc::dlerror());
            return json!({"ok": false, "error": if err.is_empty() { "dlopen failed".to_string() } else { err }});
        }
        if let Some(abi) = sym::<AbiFn>(lib, "fq_abi_version") {
            let v = abi();
            if v != 1 {
                libc::dlclose(lib);
                return json!({"ok": false, "error": format!("ABI {} не поддерживается", v)});
            }
        } else {
            libc::dlclose(lib);
            return json!({"ok": false, "error": "нет fq_abi_version"});
        }
        let Some(info) = sym::<InfoFn>(lib, "fq_info") else {
            libc::dlclose(lib);
            return json!({"ok": false, "error": "нет fq_info"});
        };
        let plugin = Plugin {
            lib: lib as usize,
            info,
            prepare: sym(lib, "fq_prepare"),
            start: sym(lib, "fq_start"),
            stop: sym(lib, "fq_stop"),
            ping: sym(lib, "fq_ping"),
            error: sym(lib, "fq_last_error"),
            stats: sym(lib, "fq_stats"),
        };
        let info_text = text((plugin.info)());
        let handle = NEXT.fetch_add(1, Ordering::SeqCst);
        table().lock().unwrap().insert(handle, plugin);
        json!({"ok": true, "handle": handle, "info": info_text})
    }
}

pub fn call(handle: i64, op: &str, config: &str, fd: i32, arg: i64) -> Value {
    let guard = table().lock().unwrap();
    let Some(p) = guard.get(&handle) else {
        return json!({"ok": false, "error": "плагин не загружен"});
    };
    let cfg = CString::new(config).unwrap_or_default();
    unsafe {
        let last_error = |p: &Plugin| p.error.map(|f| text(f())).unwrap_or_default();
        match op {
            "prepare" => match p.prepare {
                Some(f) => {
                    let out = text(f(cfg.as_ptr()));
                    match serde_json::from_str::<Value>(&out) {
                        Ok(v) => json!({"ok": true, "value": v}),
                        Err(_) => json!({"ok": false, "error": "fq_prepare вернул не JSON"}),
                    }
                }
                None => json!({"ok": false, "error": "нет fq_prepare"}),
            },
            "start" => match p.start {
                Some(f) => {
                    let code = f(cfg.as_ptr(), fd as c_int);
                    if code == 0 {
                        json!({"ok": true})
                    } else {
                        let e = last_error(p);
                        json!({"ok": false, "error": if e.is_empty() { format!("fq_start: {}", code) } else { e }})
                    }
                }
                None => json!({"ok": false, "error": "нет fq_start"}),
            },
            "stop" => {
                if let Some(f) = p.stop {
                    f();
                }
                json!({"ok": true})
            }
            "ping" => match p.ping {
                Some(f) => {
                    let ms = f(cfg.as_ptr(), arg as c_int);
                    json!({"ok": true, "ms": ms})
                }
                None => json!({"ok": false, "error": "нет fq_ping"}),
            },
            "stats" => match p.stats {
                Some(f) => json!({"ok": true, "value": text(f())}),
                None => json!({"ok": false, "error": "нет fq_stats"}),
            },
            _ => json!({"ok": false, "error": "неизвестная операция"}),
        }
    }
}

pub fn close(handle: i64) -> Value {
    let mut guard = table().lock().unwrap();
    if let Some(p) = guard.remove(&handle) {
        unsafe {
            if let Some(f) = p.stop {
                f();
            }
            libc::dlclose(p.lib as *mut c_void);
        }
    }
    json!({"ok": true})
}

#[allow(dead_code)]
fn _unused(_: c_long) {}
