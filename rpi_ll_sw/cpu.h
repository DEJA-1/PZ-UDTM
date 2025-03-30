#pragma once

#define GET_CPU_TIME(x) x.user_norm + x.user_nice + \
                        x.kernel_proc + x.idle + \
                        x.iowait + x.irq + x.soft_irq 

#include <stdint.h>

typedef struct{
  uint32_t user_norm;
  uint32_t user_nice;
  uint32_t kernel_proc;
  uint32_t idle;
  uint32_t iowait;
  uint32_t irq;
  uint32_t soft_irq;
} core_ctx_t;

typedef struct{
  uint32_t rpi_cpu_temp_mC;
  core_ctx_t  full_cpu_ctx;
  uint32_t core_num;
  core_ctx_t *core_ctxs;
} cpu_ctx_t;

extern cpu_ctx_t cpu_ctx_glob;

int update_cpu_ctx(void);
int init_cpu_ctx(void);
void deinit_cpu_ctx(void);
void print_cpu_ctx(FILE *fd);
