#include <signal.h>
#include <stdio.h>
#include <stdlib.h>

#include "proc.h"
#include "cpu.h"

#if KILL_PROC_THRESHOLD > 99 || KILL_PROC_THRESHOLD < 0
#error "KILL_PROC_THRESHOLD must be between 0 and 99, change in .config"
#endif

void
monitor_procs(void)
{
  const uint64_t proc_sum = GET_CPU_TIME(cpu_ctx_glob.full_cpu_ctx);
  const uint64_t proc_thresh = (KILL_PROC_THRESHOLD*proc_sum)/100;

  for (int i=0; i<proc_count; i++) {
    if ((proc_ctxs[i].utime_new - proc_ctxs[i].utime_old) >= proc_thresh)
    {
      if (kill(proc_ctxs[i].proc_id, SIGABRT) == EPERM) {
        fprintf(stderr, "This process doesn't have permission to kill, run with sudo\n");
        exit(-1);
      }
    }
  }
}
