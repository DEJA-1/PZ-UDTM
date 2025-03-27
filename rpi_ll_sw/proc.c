#include <stdio.h>
#include <string.h>
#include <stdint.h>
#include <stdlib.h>

#include <errno.h>
#include <fcntl.h>
#include <grp.h>
#include <dirent.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <pwd.h>

#include "proc.h"

#define PROC_PATH "/proc/"
#define PROC_CHUNK 32

#define PROC_UTIME_SSCANF_COUNT 17

proc_ctx_t *proc_ctxs = NULL;
uint32_t proc_count = 0;
uint32_t proc_chunk_count = 0;

static inline int get_proc_utime(proc_ctx_t *proc_ctx, char *line)
{
    int pid;
    char comm[256];
    char state;
    int ppid;
    int pgrp;
    int session;
    int tty_nr;
    int tpgid;
    unsigned int flags;
    unsigned long minflt;
    unsigned long cminflt;
    unsigned long majflt;
    unsigned long cmajflt;
    unsigned long utime;
    unsigned long stime;
    
    int result = sscanf(line,
                        "%d %s %c %d %d %d %d %d %u %lu %lu %lu %lu %lu %lu", // Format string
                        &pid, comm, &state, &ppid, &pgrp, &session, &tty_nr, &tpgid, &flags,
                        &minflt, &cminflt, &majflt, &cmajflt, &utime, &stime);
    proc_ctx->utime_new = utime + stime;

    return result;
}

int read_user_processes(void) {
  DIR *rdir = opendir(PROC_PATH);
  struct dirent *ent;

  if (rdir == NULL) {
    perror("opendir:");
    return -1;
  }

  uint32_t local_proc_count = 0;
  while ((ent = readdir(rdir)) != NULL) {
    char dir_path[32];

    snprintf(dir_path, sizeof(dir_path), "%s%s", PROC_PATH, ent->d_name);

    struct stat dir_stat;
    if (stat(dir_path, &dir_stat) == -1) {
      perror("stat");
      continue;
    }

    uint32_t num;
    if (S_ISDIR(dir_stat.st_mode) && sscanf(ent->d_name, "%u", &num) == 1) {
      if (local_proc_count / PROC_CHUNK >= proc_chunk_count) {
        proc_ctxs = reallocarray(proc_ctxs, (++proc_chunk_count)*PROC_CHUNK, sizeof(proc_ctx_t));

        if (proc_ctxs == NULL) {
          perror("reallocarray");
          closedir(rdir);
          return -1;
        }
      }

      char status_path[64];
      char stat_path[64];

      snprintf(status_path, sizeof(status_path), "%s/status", dir_path);
      snprintf(stat_path, sizeof(stat_path), "%s/stat", dir_path);

      proc_ctx_t proc_ctx;

      if (get_proc_info(status_path, stat_path, &proc_ctx, num) != -1) {
        memcpy(&(proc_ctxs[local_proc_count++]), &proc_ctx, sizeof(proc_ctx_t));
      }
    }
  }

  proc_count = local_proc_count;

  closedir(rdir);
  return proc_count;
}

static int get_user_name(uint32_t uid, char *name) {
  struct passwd *pw;

  if ((pw = getpwuid(uid)) == NULL) {
    perror("getpwuid:");
    return -1;
  }

  strncpy(name, pw->pw_name, strlen(pw->pw_name) + 1);

  return 0;
}

static int get_group_name(uint32_t gid, char *name) {
  struct group *g;

  if ((g = getgrgid(gid)) == NULL) {
    perror("getgrgid:");
    return -1;
  }

  strncpy(name, g->gr_name, strlen(g->gr_name) + 1);

  return 0;
}

int get_proc_info(const char *status_path, const char *stat_path, proc_ctx_t *proc_ctx, const int pid) {
  FILE *status_fd = fopen(status_path, "r");

  if (status_fd == NULL) {
    perror("fopen");
    return -1;
  }

  size_t len = 0;
  char *line = NULL;

  if (sscanf(status_path, "/proc/%u/status", &(proc_ctx->proc_id)) != 1) {
    fprintf(stderr, "Can't get process id from path: %s\n", status_path);
    free(line);
    fclose(status_fd);
    return -1;
  }

  while ((getline(&line, &len, status_fd)) != -1) {
    if (sscanf(line, "Name: %[^\n]\n", proc_ctx->proc_name) == 1) {
      break;
    }
  }

  while ((getline(&line, &len, status_fd)) != -1) {
    if (sscanf(line, "State: %[^\n]", proc_ctx->state) == 1) {
      break;
    }
  }

  while ((getline(&line, &len, status_fd)) != -1) {
    if (sscanf(line, "Uid: %u", &(proc_ctx->uid)) == 1) {
      break;
    }
  }

  if (proc_ctx->uid == 0) {
    free(line);
    fclose(status_fd);
    return -1;
  }

  while ((getline(&line, &len, status_fd)) != -1) {
    if (sscanf(line, "Gid: %u", &(proc_ctx->gid)) == 1) {
      break;
    }
  }

  while ((getline(&line, &len, status_fd)) != -1) {
    if (sscanf(line, "VmPeak: %u kB", &(proc_ctx->vm_max_size_kB)) == 1) {
      break;
    }
  }

  while ((getline(&line, &len, status_fd)) != -1) {
    if (sscanf(line, "VmSize: %u kB", &(proc_ctx->vm_size_kB)) == 1) {
      break;
    }
  }

  while ((getline(&line, &len, status_fd)) != -1) {
    if (sscanf(line, "VmSwap: %u kB", &(proc_ctx->vm_swap_kB)) == 1) {
      break;
    }
  }

  while ((getline(&line, &len, status_fd)) != -1) {
    if (sscanf(line, "Threads: %u", &(proc_ctx->threads)) == 1) {
      break;
    }
  }

  while ((getline(&line, &len, status_fd)) != -1) {
    if (sscanf(line, "Cpus_allowed: %x", &(proc_ctx->max_cpus)) == 1) {
      break;
    }
  }

  fclose(status_fd);

  if (get_user_name(proc_ctx->uid, proc_ctx->user) == -1) {
    return -1;
  }

  if (get_group_name(proc_ctx->gid, proc_ctx->group) == -1) {
    return -1;
  }

  proc_ctx->utime_old = proc_ctx->utime_new;

  FILE *stat_fd = fopen(stat_path, "r");

  if (stat_fd == NULL) {
    perror("fopen");
    return -1;
  }

  getline(&line, &len, stat_fd);
  int ret = get_proc_utime(proc_ctx, line);

  fclose(stat_fd);

  if (line != NULL) {
    free(line);
  }

  return 0;
}

void print_procs(FILE *fd) {
  for (int i=0; i<proc_count; i++) {
    print_proc_ctx(proc_ctxs[i], fd);
    fprintf(fd, "\n");
  }
}

int print_proc_ctx(proc_ctx_t proc_ctx, FILE *fd) {
  fprintf(fd, "Proc: %u %s\n", proc_ctx.proc_id, proc_ctx.proc_name);
  fprintf(fd, "State: %s\n", proc_ctx.state);
  fprintf(fd, "User: %u %s\n", proc_ctx.uid, proc_ctx.user);
  fprintf(fd, "Group: %u %s\n", proc_ctx.gid, proc_ctx.group);
  fprintf(fd, "Memory: %u/%u kB\n", proc_ctx.vm_size_kB, proc_ctx.vm_max_size_kB);
  fprintf(fd, "Swap: %u kB\n", proc_ctx.vm_swap_kB);
  fprintf(fd, "Threads: %u\n", proc_ctx.threads);
  fprintf(fd, "Max_cpus: %u\n", proc_ctx.max_cpus);
  //fprintf(fd, "Utime: %lu\n", proc_ctx.utime_new - proc_ctx.utime_old);

  fprintf(fd, "\n");

  return 0;
}
