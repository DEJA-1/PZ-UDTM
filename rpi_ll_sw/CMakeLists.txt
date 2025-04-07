cmake_minimum_required(VERSION 3.25.1)

project(rpi_watch)

find_package(PkgConfig REQUIRED)
#set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wextra")
pkg_check_modules(GPIOD REQUIRED libgpiod)

include(CheckIncludeFiles)
check_include_files("linux/spi/spidev.h" HAVE_SPIDEV_H)
if(NOT HAVE_SPIDEV_H)
    message(FATAL_ERROR "SPIdev headers not found - install linux kernel headers")
endif()

add_executable(${PROJECT_NAME}
  main.c
  proc.c
  ram.c
  cpu.c
  dispatcher.c
  gpio.c
  temp.c
  )

target_include_directories(${PROJECT_NAME} PRIVATE
    ${GPIOD_INCLUDE_DIRS}
    /usr/include
)

# Link libraries
target_link_libraries(${PROJECT_NAME}
    ${GPIOD_LIBRARIES}
)

set(CMAKE_BUILD_TYPE Debug)
