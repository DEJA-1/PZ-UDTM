#include <stdint.h>
#include <stdio.h>

#include "ram.h"

ram_ctx_t ram_ctx = {0, 0, 0};

int
read_ram_usage(void)
{ 
  FILE *status_fd = fopen(RAM_PATH, "r");

  if (status_fd == NULL) {
    perror("fopen");
    return -1;
  }

  size_t len = 0;
  char *line = NULL;

  while ((getline(&line, &len, status_fd)) != -1) {
    if (sscanf(line, "MemTotal: %u kB\n", &(ram_ctx.mem_total)) == 1) {
      break;
    }
  }

  while ((getline(&line, &len, status_fd)) != -1) {
    if (sscanf(line, "MemFree: %u kB\n", &(ram_ctx.mem_free)) == 1) {
      break;
    }
  }

  while ((getline(&line, &len, status_fd)) != -1) {
    if (sscanf(line, "MemAvailable: %u kB\n", &(ram_ctx.mem_available)) == 1) {
      break;
    }
  }

  fclose(status_fd);

  return 0;
}

void print_ram_usage(FILE *fd)
{
  fprintf(fd, "Ram total: %u kB\n", ram_ctx.mem_total);
  fprintf(fd, "Ram free: %u\ kB\n", ram_ctx.mem_free);
  fprintf(fd, "Ram available: %u kB\n", ram_ctx.mem_available);
}
