#include <gpiod.h>

#include <stdint.h>
#include <stdio.h>

#include "gpio.h"

#define GPIOCHIP "gpiochip0"

struct gpiod_chip *chip;
struct gpiod_line *lines[30];

int gpio_init_all(void)
{

  chip = gpiod_chip_open_by_name(GPIOCHIP);

  if (!chip) {
    perror("Error openiing GPIO chip");
    return -1;
  }

  for (int i=0; i<11; i++) {
    lines[i] = gpiod_chip_get_line(chip, i);

    if (!lines[i]) {
      fprintf(stderr, "Error getting line :%d\n", i);
      return -1;
    }

    const int ret = gpiod_line_request_output(lines[i], "gpio_init", 0);

    if (ret < 0) {
      fprintf(stderr, "Error setting line :%d as output\n", i);
      return -1;
    }
  }

  for (int i=15; i<30; i++) {
    lines[i] = gpiod_chip_get_line(chip, i);

    if (!lines[i]) {
      fprintf(stderr, "Error getting line :%d\n", i);
      return -1;
    }

    const int ret = gpiod_line_request_output(lines[i], "gpio_init", 0);

    if (ret < 0) {
      fprintf(stderr, "Error setting line :%d as output\n", i);
      return -1;
    }
  }

  return 0;
}

void gpio_deinit_all(void)
{
  for (int i=0; i<10; i++)
  {
    if(lines[i]) {
      gpiod_line_release(lines[i]);
    }
  }
  
  for (int i=15; i<30; i++)
  {
    if(lines[i]) {
      gpiod_line_release(lines[i]);
    }
  }
  
  if (chip) {
    gpiod_chip_close(chip);
  }
}

int gpio_set_value(uint8_t gpio, uint8_t value)
{
    if (value != 0 && value != 1) {
        fprintf(stderr, "Value must be 0 or 1\n");
        return -1;
    }

    if (gpio >= 30) {
        fprintf(stderr, "GPIO %d not available or not initialized\n", gpio);
        return -1;
    }

    if (lines[gpio] == NULL) {
        fprintf(stderr, "GPIO %d not available or not initialized\n", gpio);
        return -1;
    }

    if (gpiod_line_set_value(lines[gpio], value) < 0) {
        perror("Error setting GPIO value");
        return -1;
    }

    return 0;
}
