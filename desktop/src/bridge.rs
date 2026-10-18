use serde_json::{json, Value};

#[derive(Clone, Debug, Default)]
pub struct VpnState {
    pub up: bool,
    pub id: Option<String>,
    pub live: bool,
}

#[cfg(target_os = "android")]
mod imp {
    use jni::objects::{GlobalRef, JObject, JString, JValue};
    use jni::JavaVM;
    use std::sync::OnceLock;

    struct Ctx {
        vm: JavaVM,
        activity: GlobalRef,
        class: GlobalRef,
    }

    static CTX: OnceLock<Ctx> = OnceLock::new();

    pub fn init(vm_ptr: *mut std::ffi::c_void, activity_ptr: *mut std::ffi::c_void) {
        let vm = match unsafe { JavaVM::from_raw(vm_ptr as *mut _) } {
            Ok(v) => v,
            Err(_) => return,
        };
        let mut env = match vm.attach_current_thread() {
            Ok(e) => e,
            Err(_) => return,
        };
        let activity_obj = unsafe { JObject::from_raw(activity_ptr as *mut _) };
        let activity = match env.new_global_ref(&activity_obj) {
            Ok(g) => g,
            Err(_) => return,
        };
        let class = (|| -> Option<GlobalRef> {
            let loader = env
                .call_method(&activity_obj, "getClassLoader", "()Ljava/lang/ClassLoader;", &[])
                .ok()?
                .l()
                .ok()?;
            let name = env.new_string("app.fasdeq.vpn.RustBridge").ok()?;
            let class = env
                .call_method(
                    &loader,
                    "loadClass",
                    "(Ljava/lang/String;)Ljava/lang/Class;",
                    &[JValue::Object(&name)],
                )
                .ok()?
                .l()
                .ok()?;
            env.new_global_ref(&class).ok()
        })();
        drop(env);
        if let Some(class) = class {
            let _ = CTX.set(Ctx { vm, activity, class });
        }
    }

    pub fn call(op: &str, arg: &str) -> Option<String> {
        let ctx = CTX.get()?;
        let mut env = ctx.vm.attach_current_thread().ok()?;
        let op = env.new_string(op).ok()?;
        let arg = env.new_string(arg).ok()?;
        let class = unsafe { jni::objects::JClass::from_raw(ctx.class.as_obj().as_raw()) };
        let result = env.call_static_method(
            &class,
            "call",
            "(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
            &[
                JValue::Object(ctx.activity.as_obj()),
                JValue::Object(&op),
                JValue::Object(&arg),
            ],
        );
        if env.exception_check().unwrap_or(false) {
            let _ = env.exception_clear();
            return None;
        }
        let obj = result.ok()?.l().ok()?;
        if obj.is_null() {
            return None;
        }
        let js = JString::from(obj);
        let out: String = env.get_string(&js).ok()?.into();
        Some(out)
    }
}

#[cfg(target_os = "android")]
pub use imp::init;

#[cfg(target_os = "android")]
fn raw(op: &str, arg: &str) -> Option<String> {
    imp::call(op, arg)
}

#[cfg(not(target_os = "android"))]
fn raw(op: &str, arg: &str) -> Option<String> {
    match op {
        "dark" => Some("0".into()),
        "clipboard" => Some(String::new()),
        "state" => Some(json!({"up": false, "live": false}).to_string()),
        "start" => Some("ok".into()),
        "check" => {
            let _ = arg;
            Some("-1".into())
        }
        _ => None,
    }
}

pub fn start(payload: &Value) -> Result<String, String> {
    raw("start", &payload.to_string()).ok_or_else(|| "Не удалось обратиться к службе VPN".to_string())
}

pub fn stop() {
    let _ = raw("stop", "");
}

pub fn state() -> Option<VpnState> {
    let text = raw("state", "")?;
    let v: Value = serde_json::from_str(&text).ok()?;
    Some(VpnState {
        up: v.get("up").and_then(|x| x.as_bool()).unwrap_or(false),
        id: v.get("id").and_then(|x| x.as_str()).filter(|s| !s.is_empty()).map(|s| s.to_string()),
        live: v.get("live").and_then(|x| x.as_bool()).unwrap_or(false),
    })
}

pub fn clipboard() -> String {
    raw("clipboard", "").unwrap_or_default()
}

pub fn system_dark() -> Option<bool> {
    raw("dark", "").map(|s| s.trim() == "1")
}

pub fn system_colors(dark: bool) -> Option<Vec<i32>> {
    let text = raw("colors", if dark { "dark" } else { "light" })?;
    let v: Value = serde_json::from_str(&text).ok()?;
    let arr = v.as_array()?;
    let out: Vec<i32> = arr.iter().filter_map(|x| x.as_i64()).map(|x| x as i32).collect();
    if out.is_empty() {
        None
    } else {
        Some(out)
    }
}

pub fn switch_ui(mode: &str) {
    let _ = raw("switch_ui", mode);
}

pub fn haptic(strong: bool) {
    let _ = raw("haptic", if strong { "strong" } else { "light" });
}

pub fn service_check(outbound: &Value, url: &str) -> i32 {
    raw("check", &json!({"outbound": outbound, "url": url}).to_string())
        .and_then(|s| s.trim().parse::<i32>().ok())
        .unwrap_or(-1)
}

pub fn open_url(url: &str) {
    let _ = raw("open", url);
}

#[cfg(target_os = "android")]
static APP: std::sync::OnceLock<android_activity::AndroidApp> = std::sync::OnceLock::new();

#[cfg(target_os = "android")]
pub fn set_app(app: android_activity::AndroidApp) {
    let _ = APP.set(app);
}

pub fn keyboard(show: bool) {
    #[cfg(target_os = "android")]
    {
        if let Some(app) = APP.get() {
            if show {
                app.show_soft_input(true);
            } else {
                app.hide_soft_input(false);
            }
        }
    }
    #[cfg(not(target_os = "android"))]
    {
        let _ = show;
    }
}
