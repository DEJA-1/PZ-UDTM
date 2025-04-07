#include <fcntl.h>

#include "temp.h"

static const char *spi_dev = "/dev/spidev0.1";
static uint32_t spi_speed = 2500000; // 5MHz
static uint8_t spi_mode = SPI_MODE_1; // CPOL=0, CPHA=1
static uint8_t spi_bits = 8;
static int spi_fd;
static int cj_temp;


// Read from MAX31856 register
static int max31856_read_reg(uint8_t reg, uint8_t *buf, uint8_t len) {
    uint8_t tx_buf[len + 1];
    uint8_t rx_buf[len + 1];
    struct spi_ioc_transfer tr = {
        .tx_buf = (unsigned long)tx_buf,
        .rx_buf = (unsigned long)rx_buf,
        .len = len + 1,
        .delay_usecs = 0,
        .speed_hz = spi_speed,
        .bits_per_word = spi_bits,
    };

    tx_buf[0] = reg & 0x7F; // Set MSB for read
    for (int i = 1; i <= len; i++) {
        tx_buf[i] = 0x00;
    }

    if (ioctl(spi_fd, SPI_IOC_MESSAGE(1), &tr) < 0) {
        perror("SPI read failed");
        return -1;
    }

    for (int i = 0; i < len; i++) {
        buf[i] = rx_buf[i + 1];
    }

    return 0;
}

// Write to MAX31856 register
static int max31856_write_reg(uint8_t reg, uint8_t data) {
    uint8_t tx_buf[2];
    struct spi_ioc_transfer tr = {
        .tx_buf = (unsigned long)tx_buf,
        .rx_buf = 0,
        .len = 2,
        .delay_usecs = 0,
        .speed_hz = spi_speed,
        .bits_per_word = spi_bits,
    };

    tx_buf[0] = reg | 0x80; // Clear MSB for write
    tx_buf[1] = data;

    if (ioctl(spi_fd, SPI_IOC_MESSAGE(1), &tr) < 0) {
        perror("SPI write failed");
        return -1;
    }

    return 0;
}

static int init_spi(void) {
  spi_fd = open(spi_dev, O_RDWR);
  if (spi_fd < 0) {
    perror("Can't open SPI device");
    return -1;
  }

  if (ioctl(spi_fd, SPI_IOC_WR_MODE, &spi_mode) < 0) {
    perror("Can't set SPI mode");
    return -1;
  }

  if (ioctl(spi_fd, SPI_IOC_WR_BITS_PER_WORD, &spi_bits) < 0) {
    perror("Can't set bits per word");
    return -1;
  }

  if (ioctl(spi_fd, SPI_IOC_WR_MAX_SPEED_HZ, &spi_speed) < 0) {
    perror("Can't set max speed");
    return -1;
  }

  return 0;
}  

static int max31856_init(uint8_t tc_type) {
    // Set default CR0 (continous conversion, 50Hz noise rejection)
    if (max31856_write_reg(CR0_REG, 0x81) < 0) return -1;

    // Set thermocouple type
    if (max31856_write_reg(CR1_REG, tc_type) < 0) return -1;

    // Disable all fault masks
    if (max31856_write_reg(MASK_REG, 0xFF) < 0) return -1;

    return 0;
}

static int max31856_sanity_check() {
    uint8_t data;
    int errors = 0;


    if (max31856_read_reg(CR0_REG, &data, 1) < 0) {
        printf("CR0 read failed\n");
        return -1;
    }
    if (data != 0x81) {
        printf("[UNEXPECTED: expected 0x00]\n");
        errors++;
    }

    // 2. Check CR1 default value (expect 0x03 for K-type)
    if (max31856_read_reg(CR1_REG, &data, 1) < 0) {
        printf("CR1 read failed\n");
        return -1;
    }
    printf("CR1 Register: 0x%02X ", data);
    if ((data & 0x0F) != 0x03) {
        printf("[UNEXPECTED: expected 0x3X for K-type]\n");
        errors++;
    }

    // 3. Check MASK register (we initialized to 0xFF)
    if (max31856_read_reg(MASK_REG, &data, 1) < 0) {
        printf("MASK read failed\n");
        return -1;
    }
    if (data != 0xFF) {
        printf("[UNEXPECTED: expected 0xFF]\n");
        errors++;
    }

    // 4. Check SR register (should have no faults)
    if (max31856_read_reg(SR_REG, &data, 1) < 0) {
        printf("SR read failed\n");
        return -1;
    }
    if (data != 0x00) {
        printf("[WARNING: Faults detected]\n");
        if (data & 0x01) printf("  - Cold Junction Out-of-Range\n");
        if (data & 0x02) printf("  - Thermocouple Out-of-Range\n");
        if (data & 0x04) printf("  - Cold Junction High Fault\n");
        if (data & 0x08) printf("  - Cold Junction Low Fault\n");
        if (data & 0x10) printf("  - Thermocouple High Fault\n");
        if (data & 0x20) printf("  - Thermocouple Low Fault\n");
        if (data & 0x40) printf("  - Overvoltage/Undervoltage\n");
        errors++;
    }

    // 5. Check CJTO register (default 0x00)
    if (max31856_read_reg(CJTO_REG, &data, 1) < 0) {
        printf("CJTO read failed\n");
        return -1;
    }
    if (data != 0x00) {
        printf("[UNEXPECTED: expected 0x00]\n");
        errors++;
    }

    printf("Sanity check complete with %d errors\n\n", errors);
    return errors;
}

float max31856_read_thermocouple() {
    // Read temperature data (3 bytes)
    uint8_t temp_data[3];
    if (max31856_read_reg(LTCBH_REG, temp_data, 3) < 0) return -999.0;

    int32_t raw = (temp_data[0] << 16) | (temp_data[1] << 8) | temp_data[2];

    // Handle negative values (19-bit signed)
    if (raw & 0x800000) {
        raw -= 0x1000000;
    }

    return raw / 4096.0; // 19-bit resolution, 0.0078125°C per LSB
}

// Read cold junction temperature
float max31856_read_cold_junction() {
    uint8_t data[2];
    if (max31856_read_reg(CJTH_REG, data, 2) < 0) return -999.0;

    int16_t raw = (data[0] << 8) | data[1];
    return raw / 64.0; // 14-bit resolution, 0.015625°C per LSB
}

int init_temp_sensor(void) {
  int ret = init_spi();
  if (ret != 0) {
    return ret;
  }


  ret = max31856_init(TC_TYPE_K);

  if (ret != 0) {
    return ret;
  }

  ret = max31856_sanity_check();

  return ret;
}

void spi_close() {
  close(spi_fd);
}

void read_ext_temp(void) {
  ext_tmp = max31856_read_thermocouple();
}

void save_ext_temp(FILE *f) {
  fprintf(f, "Temp: %f\n", ext_tmp);
}
