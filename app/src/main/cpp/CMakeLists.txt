cmake_minimum_required(VERSION 3.22.1)

project("performance_enhancer")

add_library(
        performance_enhancer
        SHARED
        performance_enhancer.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        performance_enhancer
        ${log-lib})
