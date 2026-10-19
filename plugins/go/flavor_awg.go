//go:build awg

package main

import (
	"os"

	"github.com/amnezia-vpn/amneziawg-go/conn"
	"github.com/amnezia-vpn/amneziawg-go/device"
	"github.com/amnezia-vpn/amneziawg-go/tun"
)

const (
	flavorID   = "amneziawg"
	flavorName = "AmneziaWG"
)

var extraKeys = []string{"jc", "jmin", "jmax", "s1", "s2", "s3", "s4", "h1", "h2", "h3", "h4", "i1", "i2", "i3", "i4", "i5"}

var version = "dev"

func startDevice(fd int, mtu int, uapi string) (func(), error) {
	file := os.NewFile(uintptr(fd), "tun")
	dev, err := tun.CreateTUNFromFile(file, mtu)
	if err != nil {
		return nil, err
	}
	logger := device.NewLogger(device.LogLevelError, "fq-awg")
	d := device.NewDevice(dev, conn.NewDefaultBind(), logger)
	if err := d.IpcSet(uapi); err != nil {
		d.Close()
		return nil, err
	}
	if err := d.Up(); err != nil {
		d.Close()
		return nil, err
	}
	statsFn = func() string {
		out, _ := d.IpcGet()
		return out
	}
	return func() { d.Close() }, nil
}
