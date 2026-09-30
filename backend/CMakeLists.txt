cmake_minimum_required(VERSION 3.25)

project(makerspace_backend
    VERSION 0.1.0
    LANGUAGES CXX
)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

find_package(Drogon CONFIG REQUIRED)

add_executable(makerspace_backend
    src/main.cpp
)

target_link_libraries(makerspace_backend
    PRIVATE
    Drogon::Drogon
)

add_custom_command(TARGET makerspace_backend POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_directory
        "C:/Users/cater/projects/vcpkg/installed/x64-windows/bin"
        "$<TARGET_FILE_DIR:makerspace_backend>"
)