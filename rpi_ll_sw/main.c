#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <time.h>

#include "proc.h"
#include "ram.h"
#include "cpu.h"

pthread_t monitor_thread;

#define MONITOR_TIMEOUT_S 5

void *
monitor_task(void *params)
{
  init_cpu_ctx();

  while(0xDEADBEEF) {
    read_ram_usage();
    read_user_processes();
    update_cpu_ctx();

    FILE *ram = fopen("/tmp/ram_tmp", "w+");
    if (ram == NULL) {
      perror("fopen ram_tmp:");
      break;
    }

    print_ram_usage(ram);
    fclose(ram);

    FILE *cpu = fopen("/tmp/cpu_tmp", "w+");
    if (cpu == NULL) {
      perror("fopen cpu_tmp:");
      break;
    }

    print_cpu_ctx(cpu);
    fclose(cpu);

    FILE *proc = fopen("/tmp/proc_tmp", "w+");
    if (proc == NULL) {
      perror("fopen proc_tmp:");
      break;
    }

    print_procs(proc);
    fclose(proc);

    rename("/tmp/ram_tmp", "/tmp/ram");
    rename("/tmp/cpu_tmp", "/tmp/cpu");
    rename("/tmp/proc_tmp", "/tmp/proc");

    sleep(MONITOR_TIMEOUT_S);
  }

  deinit_cpu_ctx();
}

int
main(void)
{
  pthread_create(&monitor_thread, NULL, monitor_task, NULL);

  pthread_join(monitor_thread, NULL);
  return 0;
}
