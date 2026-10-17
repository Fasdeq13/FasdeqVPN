pub mod api;

use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jstring};
use jni::JNIEnv;
use serde_json::{json, Value};

fn text(env: &mut JNIEnv, s: &JString) -> String {
    env.get_string(s).map(|v| v.into()).unwrap_or_default()
}

fn reply(env: &mut JNIEnv, v: Value) -> jstring {
    env.new_string(v.to_string())
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

fn guarded(env: &mut JNIEnv, f: impl FnOnce(&mut JNIEnv) -> Value + std::panic::UnwindSafe) -> jstring {
    let out = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| f(env)))
        .unwrap_or_else(|_| json!({"ok": false, "error": "internal"}));
    reply(env, out)
}

#[no_mangle]
pub extern "system" fn Java_app_fasdeq_vpn_Native_parseLink(mut env: JNIEnv, _: JClass, conf: JString) -> jstring {
    let conf = text(&mut env, &conf);
    guarded(&mut env, move |_| api::parse_link(&conf))
}

#[no_mangle]
pub extern "system" fn Java_app_fasdeq_vpn_Native_prepareImport(mut env: JNIEnv, _: JClass, input: JString) -> jstring {
    let input = text(&mut env, &input);
    guarded(&mut env, move |_| api::prepare_import(&input))
}

#[no_mangle]
pub extern "system" fn Java_app_fasdeq_vpn_Native_fetchSubscription(mut env: JNIEnv, _: JClass, url: JString) -> jstring {
    let url = text(&mut env, &url);
    guarded(&mut env, move |_| api::fetch_subscription(&url))
}

#[no_mangle]
pub extern "system" fn Java_app_fasdeq_vpn_Native_ping(
    mut env: JNIEnv,
    _: JClass,
    conf: JString,
    endpoint: JString,
    mode: jint,
    timeout_ms: jlong,
) -> jstring {
    let conf = text(&mut env, &conf);
    let endpoint = text(&mut env, &endpoint);
    guarded(&mut env, move |_| api::measure_ping(&conf, &endpoint, mode, timeout_ms.max(500) as u64))
}

#[no_mangle]
pub extern "system" fn Java_app_fasdeq_vpn_Native_innerConfig(
    mut env: JNIEnv,
    _: JClass,
    outbound: JString,
    socks: jint,
    http: jint,
    dns: JString,
) -> jstring {
    let outbound = text(&mut env, &outbound);
    let dns: Vec<String> = text(&mut env, &dns)
        .split(',')
        .map(|s| s.trim().to_string())
        .filter(|s| !s.is_empty())
        .collect();
    guarded(&mut env, move |_| api::inner_config(&outbound, socks as u16, http as u16, &dns))
}
