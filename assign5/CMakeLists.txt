cmake_minimum_required(VERSION 3.14)
project(untitled2 C)
include_directories(~/.openmpi/include)
set(CMAKE_C_STANDARD 11)

add_executable(untitled2 main.c)