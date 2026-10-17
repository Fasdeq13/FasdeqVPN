use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jstring};
use jni::JNIEnv;

mod api;

#[no_mangle]
pub extern "system" fn Java_app_fasdeq_vpn_Native_parseLink(
    mut env: JNIEnv,
    _class: JClass,
    conf: JString,
) -> jstring {
    let input: String = env.get_string(&conf).map(|s| s.into()).unwrap_or_default();
    let result = api::parse_link(&input);
    env.new_string(result).map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_app_fasdeq_vpn_Native_prepareImport(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jstring {
    let text: String = env.get_string(&input).map(|s| s.into()).unwrap_or_default();
    let result = api::prepare_import(&text);
    env.new_string(result).map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_app_fasdeq_vpn_Native_fetchSubscription(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
) -> jstring {
    let u: String = env.get_string(&url).map(|s| s.into()).unwrap_or_default();
    let result = api::fetch_subscription(&u);
    env.new_string(result).map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_app_fasdeq_vpn_Native_ping(
    mut env: JNIEnv,
    _class: JClass,
    conf: JString,
    endpoint: JString,
    mode: jint,
    timeout_ms: jlong,
) -> jstring {
    let c: String = env.get_string(&conf).map(|s| s.into()).unwrap_or_default();
    let ep: String = env.get_string(&endpoint).map(|s| s.into()).unwrap_or_default();
    let result = api::measure_ping(&c, &ep, mode, timeout_ms as u64);
    env.new_string(result).map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_app_fasdeq_vpn_Native_innerConfig(
    mut env: JNIEnv,
    _class: JClass,
    outbound: JString,
    socks: jint,
    http: jint,
    dns: JString,
) -> jstring {
    let o: String = env.get_string(&outbound).map(|s| s.into()).unwrap_or_default();
    let d: String = env.get_string(&dns).map(|s| s.into()).unwrap_or_default();
    let dns_list: Vec<String> = d.split(',').map(|s| s.trim().to_string()).filter(|s| !s.is_empty()).collect();
    let result = api::inner_config(&o, socks as u16, http as u16, &dns_list);
    env.new_string(result).map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
}
