cmake_minimum_required(VERSION 3.14)
project(gpu_assignment C)

set(CMAKE_C_STANDARD 99)

add_executable(gpu_assignment main.c trajectory_calculation.cpp kernel.c)