#include <stdio.h>
#include <string.h>
#include "../fq_plugin.h"

static int g_fd = -1;
static char g_buf[512];

int fq_abi_version(void) { return FQ_ABI_VERSION; }

const char *fq_info(void) {
    return "{\"id\":\"echo\",\"name\":\"Echo\",\"version\":\"1\"}";
}

const char *fq_prepare(const char *config_json) {
    snprintf(g_buf, sizeof g_buf,
             "{\"address\":[\"10.8.0.2/32\"],\"routes\":[\"0.0.0.0/0\"],\"dns\":[\"1.1.1.1\"],\"mtu\":1400,\"len\":%zu}",
             strlen(config_json));
    return g_buf;
}

int fq_start(const char *config_json, int tun_fd) {
    if (tun_fd < -1) return 1;
    g_fd = tun_fd;
    return 0;
}

void fq_stop(void) { g_fd = -1; }

int fq_ping(const char *config_json, int timeout_ms) { return 42; }

const char *fq_last_error(void) { return "echo: нет ошибки"; }

const char *fq_stats(void) { return "{\"rx\":0,\"tx\":0}"; }
