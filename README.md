# Makerspace Inventory

A personal project I'm building to organize and search electronics components in a makerspace. The goal is to make it easier to find components and check how many are available in different storage locations.

## Current Features

- Search components by name, category, part number, or tag
- View component details and available stock by location
- Support both exact quantities and approximate stock levels
- Open component documentation
- Import inventory data from CSV using a Python script

## Technologies

- **Backend:** C++20, Drogon
- **Database:** PostgreSQL
- **Frontend:** Java, JavaFX
- **Tools:** CMake, vcpkg, Maven

## Running the Project

The project uses PostgreSQL for storing component and inventory data, a C++ backend for handling requests, and a JavaFX desktop application for the interface.

To run it locally, you'll need PostgreSQL 15+, CMake, vcpkg, Java 17, and Maven. The backend configuration can be created from `backend/config.example.json`.

The database structure is in `backend/sql/schema.sql`, and the backend and client are built separately using CMake and Maven.

## Status

This is still a work in progress. Component searching and stock viewing are implemented, and I'm currently working on user accounts and inventory management features.