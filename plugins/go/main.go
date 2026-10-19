package main

/*
#include <stdlib.h>
*/
import "C"

import (
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"net"
	"strings"
	"sync"
	"unsafe"
)

var (
	mu      sync.Mutex
	stopFn  func()
	lastErr string
	outBuf  *C.char
)

func ret(s string) *C.char {
	if outBuf != nil {
		C.free(unsafe.Pointer(outBuf))
	}
	outBuf = C.CString(s)
	return outBuf
}

type request struct {
	Conf     string                 `json:"conf"`
	Settings map[string]interface{} `json:"settings"`
}

type peer struct {
	kv map[string]string
}

type parsed struct {
	iface map[string]string
	peers []peer
}

func parseConf(text string) (*parsed, error) {
	p := &parsed{iface: map[string]string{}}
	section := ""
	for _, line := range strings.Split(text, "\n") {
		if i := strings.Index(line, "#"); i >= 0 {
			line = line[:i]
		}
		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}
		if strings.HasPrefix(line, "[") {
			section = strings.ToLower(line)
			if section == "[peer]" {
				p.peers = append(p.peers, peer{kv: map[string]string{}})
			}
			continue
		}
		i := strings.Index(line, "=")
		if i <= 0 {
			continue
		}
		k := strings.ToLower(strings.TrimSpace(line[:i]))
		v := strings.TrimSpace(line[i+1:])
		if section == "[interface]" {
			p.iface[k] = v
		} else if section == "[peer]" {
			p.peers[len(p.peers)-1].kv[k] = v
		}
	}
	if p.iface["privatekey"] == "" {
		return nil, errors.New("нет PrivateKey")
	}
	if len(p.peers) == 0 {
		return nil, errors.New("нет [Peer]")
	}
	return p, nil
}

func keyHex(b64 string) (string, error) {
	raw, err := base64.StdEncoding.DecodeString(strings.TrimSpace(b64))
	if err != nil || len(raw) != 32 {
		return "", errors.New("неверный ключ")
	}
	return hex.EncodeToString(raw), nil
}

func list(s string) []string {
	var out []string
	for _, x := range strings.Split(s, ",") {
		x = strings.TrimSpace(x)
		if x != "" {
			out = append(out, x)
		}
	}
	return out
}

func uapi(p *parsed) (string, error) {
	var b strings.Builder
	pk, err := keyHex(p.iface["privatekey"])
	if err != nil {
		return "", err
	}
	fmt.Fprintf(&b, "private_key=%s\n", pk)
	for _, k := range extraKeys {
		if v, ok := p.iface[k]; ok && v != "" {
			fmt.Fprintf(&b, "%s=%s\n", k, v)
		}
	}
	b.WriteString("replace_peers=true\n")
	for _, pr := range p.peers {
		pub, err := keyHex(pr.kv["publickey"])
		if err != nil {
			return "", err
		}
		fmt.Fprintf(&b, "public_key=%s\n", pub)
		if psk := pr.kv["presharedkey"]; psk != "" {
			h, err := keyHex(psk)
			if err != nil {
				return "", err
			}
			fmt.Fprintf(&b, "preshared_key=%s\n", h)
		}
		if ep := pr.kv["endpoint"]; ep != "" {
			addr, err := net.ResolveUDPAddr("udp", ep)
			if err != nil {
				return "", err
			}
			fmt.Fprintf(&b, "endpoint=%s\n", addr.String())
		}
		ka := pr.kv["persistentkeepalive"]
		if ka == "" || ka == "0" {
			ka = "25"
		}
		fmt.Fprintf(&b, "persistent_keepalive_interval=%s\n", ka)
		b.WriteString("replace_allowed_ips=true\n")
		allowed := list(pr.kv["allowedips"])
		if len(allowed) == 0 {
			allowed = []string{"0.0.0.0/0", "::/0"}
		}
		for _, a := range allowed {
			fmt.Fprintf(&b, "allowed_ip=%s\n", a)
		}
	}
	return b.String(), nil
}

func decode(config *C.char) (*parsed, error) {
	var r request
	if err := json.Unmarshal([]byte(C.GoString(config)), &r); err != nil {
		return nil, err
	}
	return parseConf(r.Conf)
}

//export fq_abi_version
func fq_abi_version() C.int { return 1 }

//export fq_info
func fq_info() *C.char {
	return ret(fmt.Sprintf(`{"id":"%s","name":"%s","version":"%s"}`, flavorID, flavorName, version))
}

//export fq_prepare
func fq_prepare(config *C.char) *C.char {
	p, err := decode(config)
	if err != nil {
		lastErr = err.Error()
		return ret(`{"error":"` + strings.ReplaceAll(lastErr, `"`, `'`) + `"}`)
	}
	routes := []string{}
	for _, pr := range p.peers {
		routes = append(routes, list(pr.kv["allowedips"])...)
	}
	if len(routes) == 0 {
		routes = []string{"0.0.0.0/0", "::/0"}
	}
	mtu := 1280
	if v := p.iface["mtu"]; v != "" {
		fmt.Sscanf(v, "%d", &mtu)
	}
	if mtu < 1200 || mtu > 1420 {
		mtu = 1280
	}
	out, _ := json.Marshal(map[string]interface{}{
		"address": list(p.iface["address"]),
		"dns":     list(p.iface["dns"]),
		"routes":  routes,
		"mtu":     mtu,
	})
	return ret(string(out))
}

//export fq_start
func fq_start(config *C.char, fd C.int) C.int {
	mu.Lock()
	defer mu.Unlock()
	if stopFn != nil {
		stopFn()
		stopFn = nil
	}
	p, err := decode(config)
	if err != nil {
		lastErr = err.Error()
		return 1
	}
	conf, err := uapi(p)
	if err != nil {
		lastErr = err.Error()
		return 2
	}
	mtu := 1280
	if v := p.iface["mtu"]; v != "" {
		fmt.Sscanf(v, "%d", &mtu)
	}
	stop, err := startDevice(int(fd), mtu, conf)
	if err != nil {
		lastErr = err.Error()
		return 3
	}
	stopFn = stop
	return 0
}

//export fq_stop
func fq_stop() {
	mu.Lock()
	defer mu.Unlock()
	if stopFn != nil {
		stopFn()
		stopFn = nil
	}
}

//export fq_last_error
func fq_last_error() *C.char { return ret(lastErr) }

func main() {}
