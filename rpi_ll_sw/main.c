#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <time.h>
#include <strings.h>
#include <string.h>

#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>

#include "monitor.c"

#include "proc.h"
#include "ram.h"
#include "cpu.h"
#include "dispatcher.h"

pthread_t resources_thread;
pthread_t dispatcher_thread;

#define MS_TO_US(x) (x*1000)

const uint32_t key = KEY;

const int get_listener_socket(void) {
  int listener;
  socklen_t size;

  struct sockaddr_in servaddr;

  listener = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);

  bzero((char *)&servaddr, sizeof(servaddr));
  servaddr.sin_family = AF_INET;
  servaddr.sin_addr.s_addr = inet_addr("127.0.0.1");
  servaddr.sin_port = htons(TCP_PORT);

  int opt = 1;

  if(setsockopt(listener, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) != 0) {
    perror("setsockopt");
    exit(1);
  }

  if(bind(listener, (struct sockaddr *)&servaddr, sizeof(servaddr)) == -1){
    perror("bind");
    exit(1);
  }

  if(listen(listener, 5) == -1){
    perror("listen");
    exit(1);
  }

  return listener;
}

void * dispatcher_task(void *params)
{
  const int listener = get_listener_socket();
  struct sockaddr_in client_addr;
  socklen_t client_len = sizeof(client_addr);

  int client_sock; 
  while(0xDEADBEEF) {
    client_sock = accept(listener, (struct sockaddr *)&client_addr, &client_len);

    if (client_sock == -1) {
      perror("accept");
      continue;
    }

    uint8_t key_buf[4];
    if (recv(client_sock, key_buf, 4, 0) == 4) {
      if (memcmp(key_buf, &key, sizeof(key)) == 0) {
        break;
      }
    }

    close(client_sock);
  }

  uint8_t recv_data[8];

  printf("KEy accepted\n");

  while(0xDEADBEEF) {
    recv(client_sock, recv_data, 8, 0);

    const uint8_t cmd_ret = dispatch((packet_t *)recv_data);
    send(client_sock, &cmd_ret, 1, 0);
  }
}

void * resources_task(void *params)
{
  init_cpu_ctx();

  while(0xDEADBEEF) {
    read_ram_usage();
    read_user_processes();
    update_cpu_ctx();

    monitor_procs();

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

    usleep(MS_TO_US(MONITOR_TIMEOUT_MS));
  }

  deinit_cpu_ctx();
}

int main(void)
{
  pthread_create(&resources_thread, NULL, resources_task, NULL);
  pthread_create(&dispatcher_thread, NULL, dispatcher_task, NULL);

  pthread_join(dispatcher_thread, NULL);
  pthread_join(resources_thread, NULL);
  return 0;
}
