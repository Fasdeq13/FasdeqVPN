#ifndef FQ_PLUGIN_H
#define FQ_PLUGIN_H

#ifdef __cplusplus
extern "C" {
#endif

#define FQ_ABI_VERSION 1

int fq_abi_version(void);

const char *fq_info(void);

const char *fq_prepare(const char *config_json);

int fq_start(const char *config_json, int tun_fd);

void fq_stop(void);

int fq_ping(const char *config_json, int timeout_ms);

const char *fq_last_error(void);

const char *fq_stats(void);

#ifdef __cplusplus
}
#endif

#endif
