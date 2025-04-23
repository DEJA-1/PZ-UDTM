#pragma once

#include <stdint.h>

enum header {
  kill_proc = 0,
  gpio_set
};

typedef struct __attribute__((packed)){
  uint8_t gpio_num;
  uint8_t gpio_val;
} gpio_data_t;

typedef struct{
  uint32_t pid;
} kill_data_t;

typedef union{
  kill_data_t kill;
  gpio_data_t gpio;
} data_t;

typedef struct __attribute__((packed)){
  uint8_t header;
  data_t data;
} packet_t;

int dispatch(packet_t *pack);
