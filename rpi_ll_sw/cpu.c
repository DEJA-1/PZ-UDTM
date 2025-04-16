#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <fcntl.h>
#include <stdint.h>

#include "cpu.h"

#define CPU_STAT_PATH "/proc/stat"
#define CPU_STAT_NUM 7
#define RPI_TEMP_CPU_PATH "/sys/class/thermal/thermal_zone0/temp"

cpu_ctx_t cpu_ctx_glob;
cpu_ctx_t new_cpu_ctx;

  static inline int64_t
get_proc_num(void)
{
  return sysconf(_SC_NPROCESSORS_CONF);
}

  static inline int
read_core_ctx_nonum(core_ctx_t *core_ctx, char *line)
{
    return (sscanf(line, "cpu %u %u %u %u %u %u %u", 
          &(core_ctx->user_norm),
          &(core_ctx->user_nice),
          &(core_ctx->kernel_proc),
          &(core_ctx->idle),
          &(core_ctx->iowait),
          &(core_ctx->irq),
          &(core_ctx->soft_irq)));
}

  static inline int
read_core_ctx(core_ctx_t *core_ctx, char *line)
{
    uint32_t num;
    return (sscanf(line, "cpu%u %u %u %u %u %u %u %u", 
          &num,
          &(core_ctx->user_norm),
          &(core_ctx->user_nice),
          &(core_ctx->kernel_proc),
          &(core_ctx->idle),
          &(core_ctx->iowait),
          &(core_ctx->irq),
          &(core_ctx->soft_irq)) - 1);
}

  static int
read_cpu_ctx(cpu_ctx_t *cpu_ctx)
{
  FILE *stat_fd = fopen(CPU_STAT_PATH, "r");
  
  if (stat_fd == NULL){
    perror("fopen:");
    return -1;
  }

  size_t len = 0;
  char *line = NULL;

  while ((getline(&line, &len, stat_fd)) != -1) {
    if (read_core_ctx_nonum(&(cpu_ctx->full_cpu_ctx), line) == CPU_STAT_NUM) {
      break;
    }
  }

  for(int i=0; i<cpu_ctx->core_num; i++) {
    while ((getline(&line, &len, stat_fd)) != -1) {
      if (read_core_ctx(&(cpu_ctx->core_ctxs[i]), line) == CPU_STAT_NUM) {
        break;
      }
    }
  }

  fclose(stat_fd);

  FILE *temp_fd = fopen(RPI_TEMP_CPU_PATH, "r");

  if (temp_fd == NULL)
  {
    perror("fopen");
    return -1;
  }

  while ((getline(&line, &len, stat_fd)) != -1) {
    if (sscanf(line, "%lu", &(cpu_ctx->rpi_cpu_temp_mC)) == 1){
      break;
    }
  }

  fclose(temp_fd);

  return 0;
}


  static int
update_core_ctx(core_ctx_t *old, core_ctx_t new)
{
  old->user_norm = new.user_norm;
  old->user_nice = new.user_nice;
  old->kernel_proc = new.kernel_proc;
  old->idle = new.idle;
  old->iowait = new.iowait;
  old->irq = new.irq;
  old->soft_irq = new.soft_irq;

  return 0;
}

  int
update_cpu_ctx(void)
{
  FILE *temp_fd = fopen(RPI_TEMP_CPU_PATH, "r");

  update_core_ctx(&(cpu_ctx_glob.full_cpu_ctx), new_cpu_ctx.full_cpu_ctx);

  for(int i=0; i<cpu_ctx_glob.core_num; i++){
    update_core_ctx(&(cpu_ctx_glob.core_ctxs[i]), new_cpu_ctx.core_ctxs[i]);
  }

  if (temp_fd == NULL){
    perror("fopen");
    return -1;
  }

  char *line = NULL;
  size_t len = 0;

  while ((getline(&line, &len, temp_fd)) != -1) {
    if (sscanf(line, "%lu", &(cpu_ctx_glob.rpi_cpu_temp_mC)) == 1){
      break;
    }
  }

  fclose(temp_fd);

  read_cpu_ctx(&new_cpu_ctx);

  return 0;
}

  int
init_cpu_ctx(void)
{
  cpu_ctx_glob.core_num = get_proc_num();
  cpu_ctx_glob.core_ctxs = (core_ctx_t *)calloc(cpu_ctx_glob.core_num, sizeof(core_ctx_t));

  new_cpu_ctx.core_num = cpu_ctx_glob.core_num;
  new_cpu_ctx.core_ctxs = (core_ctx_t *)calloc(cpu_ctx_glob.core_num, sizeof(core_ctx_t));

  read_cpu_ctx(&cpu_ctx_glob);
  read_cpu_ctx(&new_cpu_ctx);
}
  void
deinit_cpu_ctx(void)
{
  if (cpu_ctx_glob.core_ctxs != NULL){
    free(cpu_ctx_glob.core_ctxs);
  }
  if (new_cpu_ctx.core_ctxs != NULL){
    free(cpu_ctx_glob.core_ctxs);
  }
}

  static void
print_core_ctx(FILE *fd, core_ctx_t core_ctx, core_ctx_t old_ctx)
{
  fprintf(fd, "User norm: %lu\n", core_ctx.user_norm - old_ctx.user_norm);
  fprintf(fd, "User nice: %lu\n", core_ctx.user_nice - old_ctx.user_nice);
  fprintf(fd, "Kernel: %lu\n", core_ctx.kernel_proc - old_ctx.kernel_proc);
  fprintf(fd, "Idle: %lu\n", core_ctx.idle - old_ctx.idle);
  fprintf(fd, "Iowait: %lu\n", core_ctx.iowait - old_ctx.iowait);
  fprintf(fd, "Irq: %lu\n", core_ctx.irq - old_ctx.irq);
  fprintf(fd, "Soft irq: %lu\n", core_ctx.soft_irq - old_ctx.soft_irq);
}

  void
print_cpu_ctx(FILE *fd)
{
  fprintf(fd, "CPU temp: %u\n\n", cpu_ctx_glob.rpi_cpu_temp_mC);

  fprintf(fd, "Full CPU:\n");
  print_core_ctx(fd, new_cpu_ctx.full_cpu_ctx, cpu_ctx_glob.full_cpu_ctx);
  fprintf(fd, "\n");

  for(int i=0; i<cpu_ctx_glob.core_num; i++) {
    fprintf(fd, "Core %u:\n", i);
    print_core_ctx(fd, new_cpu_ctx.core_ctxs[i], cpu_ctx_glob.core_ctxs[i]);
    fprintf(fd, "\n");
  }
}
