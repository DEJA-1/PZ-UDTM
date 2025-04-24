#pragma once

#include <stdint.h>

#define RAM_PATH "/proc/meminfo"

typedef struct{
  uint32_t mem_total;
  uint32_t mem_free;
  uint32_t mem_available;
} ram_ctx_t;

int read_ram_usage(void);
void print_ram_usage(FILE *fd);
