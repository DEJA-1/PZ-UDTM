#pragma once

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdint.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/spi/spidev.h>

// MAX31856 Register Addresses
#define CR0_REG      0x00
#define CR1_REG      0x01
#define MASK_REG     0x02
#define CJHF_REG     0x03
#define CJLF_REG     0x04
#define LTHFTH_REG   0x05
#define LTHFTL_REG   0x06
#define LTLFTH_REG   0x07
#define LTLFTL_REG   0x08
#define CJTO_REG     0x09
#define CJTH_REG     0x0A
#define CJTL_REG     0x0B
#define LTCBH_REG    0x0C
#define LTCBM_REG    0x0D
#define LTCBL_REG    0x0E
#define SR_REG       0x0F

// Thermocouple Types
#define TC_TYPE_B    0x00
#define TC_TYPE_E    0x01
#define TC_TYPE_J    0x02
#define TC_TYPE_K    0x03
#define TC_TYPE_N    0x04
#define TC_TYPE_R    0x05
#define TC_TYPE_S    0x06
#define TC_TYPE_T    0x07

extern float ext_tmp;

int init_temp_sensor(void);
void deinit_temp_sensor(void);
float max31856_read_cold_junction(void);
float max31856_read_thermocouple(void);

void read_ext_temp(void);
void save_ext_temp(FILE *f);
