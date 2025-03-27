#pragma once 

#include <stdint.h>

typedef struct
{
  uint32_t proc_id;
  char proc_name[64];

  char state[16];

  uint32_t uid;
  char user[64];

  uint32_t gid;
  char group[64];

  uint32_t thread_count;

  uint64_t vm_max_size_kB;
  uint64_t vm_size_kB;
  uint64_t vm_swap_kB;

  uint32_t threads;
  uint32_t max_cpus;

  uint64_t utime_new;
  uint64_t utime_old;
} proc_ctx_t;

typedef struct
{
  uint32_t pid;
  uint64_t utime;
} utime_ctx_t;

int read_user_processes(void);
int get_proc_info(const char *status_path, const char *stat_path, proc_ctx_t *proc_ctx, int pid);
int print_proc_ctx(proc_ctx_t proc_ctx, FILE *fd);
void print_procs(FILE *fd);
