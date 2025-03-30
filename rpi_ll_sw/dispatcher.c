#include <stdint.h>
#include <signal.h>

#include "dispatcher.h"
#include "gpio.c"

int kill_pid(int pid)
{
  printf("Killing proc%u\n", pid);
  return kill(pid, SIGABRT);
}

int dispatch(packet_t *pack)
{
  switch(pack->header) {
    case ((uint8_t)gpio_set):
      {
        return gpio_set_value(pack->data.gpio.gpio_num, pack->data.gpio.gpio_val);
      }

    case ((uint8_t)kill_proc):
      {
        return kill_pid(pack->data.kill.pid);
      }
  };
}
