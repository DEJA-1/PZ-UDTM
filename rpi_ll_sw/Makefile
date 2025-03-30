CONFIG_MONITOR_TIMEOUT_MS:=3000
CONFIG_KILL_PROC_THRESHOLD:=90
CONFIG_TCP_PORT:=31337
CONFIG_KEY:=0xDEADBEEF

ifneq ($(wildcard .config),)
	include .config
else
ifeq (,$(filter clean,$(MAKECMDGOALS)))
	$(error Create .config file)
endif
endif

CFLAGS += -DMONITOR_TIMEOUT_MS=$(CONFIG_MONITOR_TIMEOUT_MS)
CFLAGS += -DKILL_PROC_THRESHOLD=$(CONFIG_KILL_PROC_THRESHOLD)
CFLAGS += -DTCP_PORT=$(CONFIG_TCP_PORT)
CFLAGS += -DKEY=$(CONFIG_KEY)

all: build/ ninja

build/: 
	CFLAGS="$(CFLAGS)" ./build.sh

ninja: build/
	ninja -C build/

clean:
	rm -rf build/
