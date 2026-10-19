//go:build !awg

package main

import (
	"os"

	"golang.zx2c4.com/wireguard/conn"
	"golang.zx2c4.com/wireguard/device"
	"golang.zx2c4.com/wireguard/tun"
)

const (
	flavorID   = "wireguard-go"
	flavorName = "WireGuard-Go"
)

var extraKeys = []string{}

var version = "dev"

func startDevice(fd int, mtu int, uapi string) (func(), error) {
	file := os.NewFile(uintptr(fd), "tun")
	dev, err := tun.CreateTUNFromFile(file, mtu)
	if err != nil {
		return nil, err
	}
	logger := device.NewLogger(device.LogLevelError, "fq-wg")
	d := device.NewDevice(dev, conn.NewDefaultBind(), logger)
	if err := d.IpcSet(uapi); err != nil {
		d.Close()
		return nil, err
	}
	if err := d.Up(); err != nil {
		d.Close()
		return nil, err
	}
	return func() { d.Close() }, nil
}
