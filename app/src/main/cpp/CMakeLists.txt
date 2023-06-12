cmake_minimum_required(VERSION 3.10.2)

project("ndkadvanced")

add_library(
        native-lib
        SHARED
        native-lib.cpp
        dgif_lib.c
        gifalloc.c)

find_library(log-lib
        log)

find_library(jnigraphics-lib
        jnigraphics)

target_link_libraries(
        native-lib
        ${jnigraphics-lib}
        ${log-lib})