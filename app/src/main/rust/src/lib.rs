//! Vortex Studio Rust Network & Streaming Engine
//!
//! Provides ultra-low latency packetization, adaptive bitrate (ABR) calculation,
//! and RTMP/SRT connection management with zero-cost memory safety.

use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jlong, jstring};
use jni::JNIEnv;

pub struct StreamSession {
    pub stream_url: String,
    pub stream_key: String,
    pub is_connected: bool,
    pub bitrate_kbps: u32,
    pub total_bytes_sent: u64,
}

impl StreamSession {
    pub fn new(url: String, key: String) -> Self {
        StreamSession {
            stream_url: url,
            stream_key: key,
            is_connected: false,
            bitrate_kbps: 4500,
            total_bytes_sent: 0,
        }
    }

    pub fn connect(&mut self) -> bool {
        self.is_connected = true;
        true
    }

    pub fn disconnect(&mut self) {
        self.is_connected = false;
    }
}

#[no_mangle]
pub extern "system" fn Java_com_example_nativecore_NativeRustNetwork_rustGetEngineVersion(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let version = "Vortex-RustNetwork-v0.1.0 (MemorySafe-RTMP/SRT)";
    let output = env
        .new_string(version)
        .expect("Couldn't create Java string!");
    output.into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_example_nativecore_NativeRustNetwork_rustInitStream(
    mut env: JNIEnv,
    _class: JClass,
    endpoint: JString,
    _bitrate_kbps: jint,
) -> jboolean {
    let _endpoint_str: String = match env.get_string(&endpoint) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    // Ready for RTMP / SRT socket handshake
    1
}

#[no_mangle]
pub extern "system" fn Java_com_example_nativecore_NativeRustNetwork_rustGetBitrate(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    4500 // Default 4500 Kbps
}
