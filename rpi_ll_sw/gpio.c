#include <stdint.h>
#include <stdio.h>

int gpio_set_value(uint8_t gpio, uint8_t value)
{
  printf("Setting gpio%u to %u\n", gpio, value);
  
  return 0;
}
