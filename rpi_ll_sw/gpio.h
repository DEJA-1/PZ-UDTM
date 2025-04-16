#pragma once

int gpio_init_all(void);
void gpio_deinit_all(void);
int gpio_set_value(uint8_t gpio, uint8_t value);
