#include <drogon/drogon.h>
#include <drogon/orm/DbClient.h>

#include <cstdint>
#include <fstream>
#include <map>
#include <sstream>
#include <stdexcept>
#include <string>

// load sql file
std::string loadSql(const std::string& path) {
    std::ifstream file(path);
    if (!file.is_open()) {
        throw std::runtime_error("Could not open SQL file: " + path);
    }

    std::stringstream buffer;
    buffer << file.rdbuf();
    return buffer.str();
}

int main() {
    drogon::app()
        .loadConfigFile("backend/config.local.json")
        // check HTTP server response
        .registerHandler(
            "/api/health",
            [](const drogon::HttpRequestPtr&,
               std::function<void(const drogon::HttpResponsePtr&)>&& callback) {
                Json::Value json;
                json["status"] = "ok";

                callback(drogon::HttpResponse::newHttpJsonResponse(json));
            },
            {drogon::Get}
        )
        // search for components matching a name, part number, tag, or alias
        .registerHandler(
            "/api/components",
            [](const drogon::HttpRequestPtr& request,
               std::function<void(const drogon::HttpResponsePtr&)>&& callback) {
                auto db = drogon::app().getDbClient("default");
                std::string search = request->getParameter("search");

                db->execSqlAsync(
                    loadSql("backend/sql/search_components.sql"),
                    [callback](const drogon::orm::Result& result) {
                        Json::Value components(Json::arrayValue);
                        // maps component ids to their index in the components array to prevent duplicates
                        std::map<std::int64_t, Json::ArrayIndex> indices;

                        for (const auto& row : result) {
                            auto id = row["id"].as<std::int64_t>();
                            auto found = indices.find(id);

                            if (found == indices.end()) {
                                Json::Value component;
                                component["id"] = Json::Int64(id);
                                component["name"] = row["name"].as<std::string>();
                                component["requires_checkout"] = row["requires_checkout"].as<bool>();
                                component["stock"] = Json::Value(Json::arrayValue);

                                for (const auto* field : {
                                    "description", "part_number", "brand", "category", 
                                    "power_requirements", "documentation_url"
                                }) {
                                    if (row[field].isNull())
                                        component[field] = Json::nullValue;
                                    else 
                                        component[field] = row[field].as<std::string>();
                                }
                                auto index = components.size();
                                components.append(component);
                                found = indices.emplace(id, index).first;
                            }
                            
                            // add inventory information to the component's stock array
                            if (!row["inventory_id"].isNull()) {
                                Json::Value stock;
                                stock["inventory_id"] = Json::Int64(row["inventory_id"].as<std::int64_t>());
                                stock["location_id"] = Json::Int64(row["location_id"].as<std::int64_t>());
                                stock["location"] = row["location"].as<std::string>();

                                if (row["quantity"].isNull()) 
                                    stock["quantity"] = Json::nullValue;
                                else 
                                    stock["quantity"] = row["quantity"].as<int>();
                                

                                if (row["stock_level"].isNull()) 
                                    stock["stock_level"] = Json::nullValue;
                                else 
                                    stock["stock_level"] = row["stock_level"].as<std::string>();

                                components[found->second]["stock"].append(stock);
                            }
                        }

                        callback(drogon::HttpResponse::newHttpJsonResponse(components));
                    },
                    [callback](const drogon::orm::DrogonDbException& error) {
                        LOG_ERROR << error.base().what();
                        Json::Value json;
                        json["error"] = "Database query failed";

                        auto response = drogon::HttpResponse::newHttpJsonResponse(json);
                        response->setStatusCode(drogon::k500InternalServerError);
                        callback(response);
                    },
                    "%" + search + "%"
                );
            },
            {drogon::Get}
        )
        .addListener("127.0.0.1", 8080)
        .run();

    return 0;
}