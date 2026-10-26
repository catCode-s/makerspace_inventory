cmake_minimum_required(VERSION 3.25)

project(makerspace_backend
    VERSION 0.1.0
    LANGUAGES CXX
)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

find_package(Drogon CONFIG REQUIRED)
find_package(unofficial-sodium CONFIG REQUIRED)

add_executable(makerspace_backend src/main.cpp)

target_link_libraries(makerspace_backend PRIVATE Drogon::Drogon)
target_link_libraries(makerspace_backend PRIVATE unofficial-sodium::sodium)