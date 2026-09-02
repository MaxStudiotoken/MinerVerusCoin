cmake_minimum_required(VERSION 3.22.1)
project(verus_engine)

add_library(verus_engine SHARED verus_engine.cpp)

find_library(log_lib log)
target_link_libraries(verus_engine ${log_lib})
