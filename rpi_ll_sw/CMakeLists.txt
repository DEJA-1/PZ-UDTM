cmake_minimum_required(VERSION 3.30.1)
project(rpi_watch)

add_executable(${PROJECT_NAME}
  main.c
  proc.c
  ram.c
  cpu.c
  )

set(CMAKE_BUILD_TYPE Debug) # TODO: change to release
