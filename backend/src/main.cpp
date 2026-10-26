#include <drogon/drogon.h>
#include <drogon/orm/DbClient.h>

#include <cstdint>
#include <fstream>
#include <map>
#include <sstream>
#include <stdexcept>
#include <string>

#include <sodium.h>

std::string loadSql(const std::string& path);
std::string hashPassword(const std::string& password);
bool verifyPassword(const std::string& password, const std::string& hash);

drogon::HttpResponsePtr errorResponse(drogon::HttpStatusCode status, const std::string& message);

void registerHealthRoute();
void registerComponentRoute(const std::string& searchSql);
void registerUserRoute(const std::string& registerSql);
void registerLoginRoute(const std::string& loginSql);
void registerCurrentUserRoute();
void registerLogoutRoute();

int main() {
    if (sodium_init() < 0) {
        throw std::runtime_error("Failed to initialize sodium library");
    }

    std::string searchSql;
    std::string registerSql;
    std::string loginSql;

    try {
        searchSql = loadSql("backend/sql/search_components.sql");
        registerSql = loadSql("backend/sql/register_user.sql");
        loginSql = loadSql("backend/sql/login_user.sql");
    } catch (const std::exception& error) {
        LOG_ERROR << error.what();
        return 1;
    }

    drogon::app().loadConfigFile("backend/config.local.json").enableSession(1200);

    registerHealthRoute();
    registerComponentRoute(searchSql);
    registerUserRoute(registerSql);
    registerLoginRoute(loginSql);
    registerCurrentUserRoute();
    registerLogoutRoute();

    drogon::app().addListener("127.0.0.1", 8080).run();
    return 0;
}

drogon::HttpResponsePtr errorResponse(drogon::HttpStatusCode status, const std::string& message) {
    Json::Value json;
    json["error"] = message;
    auto response = drogon::HttpResponse::newHttpJsonResponse(json);
    response->setStatusCode(status);
    return response;
}

void registerHealthRoute() {
    drogon::app().registerHandler("/api/health",
            [](const drogon::HttpRequestPtr&, std::function<void(const drogon::HttpResponsePtr&)>&& callback) {
                Json::Value json;
                json["status"] = "ok";

                callback(drogon::HttpResponse::newHttpJsonResponse(json));
            },
            {drogon::Get});
}

void registerComponentRoute(const std::string& searchSql) {
    drogon::app().registerHandler("/api/components",
            [searchSql](
                const drogon::HttpRequestPtr& request, std::function<void(const drogon::HttpResponsePtr&)>&& callback) {
                auto db = drogon::app().getDbClient("default");
                std::string search = request->getParameter("search");

                db->execSqlAsync(
                    searchSql,
                    [callback](const drogon::orm::Result& result) {
                        Json::Value components(Json::arrayValue);
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

                                for (const auto* field : {"description", "part_number", "brand", "category",
                                         "power_requirements", "documentation_url"}) {
                                    if (row[field].isNull())
                                        component[field] = Json::nullValue;
                                    else
                                        component[field] = row[field].as<std::string>();
                                }
                                auto index = components.size();
                                components.append(component);
                                found = indices.emplace(id, index).first;
                            }

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
                        callback(errorResponse(drogon::k500InternalServerError, "Database query failed"));
                    },
                    "%" + search + "%");
            },
            {drogon::Get});
}

void registerUserRoute(const std::string& registerSql) {
    drogon::app().registerHandler("/api/register",
            [registerSql](
                const drogon::HttpRequestPtr& request, std::function<void(const drogon::HttpResponsePtr&)>&& callback) {
                auto json = request->getJsonObject();
                if (!json || !json->isObject() || !json->isMember("display_name") || !json->isMember("email") ||
                    !json->isMember("password")) {
                    callback(errorResponse(drogon::k400BadRequest, "Missing required fields"));
                    return;
                }

                if (!(*json)["display_name"].isString() || !(*json)["email"].isString() ||
                    !(*json)["password"].isString()) {
                    callback(errorResponse(drogon::k400BadRequest, "Invalid field types"));
                    return;
                }

                std::string displayName = (*json)["display_name"].asString();
                std::string email = (*json)["email"].asString();
                std::string password = (*json)["password"].asString();

                if (displayName.empty() || email.empty() || password.empty()) {
                    callback(errorResponse(drogon::k400BadRequest, "Fields cannot be empty"));
                    return;
                }

                std::string passwordHash = hashPassword(password);
                auto db = drogon::app().getDbClient("default");
                db->execSqlAsync(
                    registerSql,
                    [callback](const drogon::orm::Result& result) {
                        if (result.empty()) {
                            callback(errorResponse(drogon::k409Conflict, "Email is already registered"));
                            return;
                        }
                        Json::Value json;
                        json["id"] = Json::Int64(result[0]["id"].as<std::int64_t>());

                        auto response = drogon::HttpResponse::newHttpJsonResponse(json);
                        response->setStatusCode(drogon::k201Created);
                        callback(response);
                    },
                    [callback](const drogon::orm::DrogonDbException& error) {
                        LOG_ERROR << error.base().what();
                        callback(errorResponse(drogon::k500InternalServerError, "Database query failed"));
                    },
                    displayName, email, passwordHash);
            },
            {drogon::Post});
}

void registerLoginRoute(const std::string& loginSql) {
    drogon::app().registerHandler("/api/login",
            [loginSql](
                const drogon::HttpRequestPtr& request, std::function<void(const drogon::HttpResponsePtr&)>&& callback) {
                auto json = request->getJsonObject();

                if (!json || !json->isObject() || !json->isMember("email") || !json->isMember("password")) {
                    callback(errorResponse(drogon::k400BadRequest, "Missing required fields"));
                    return;
                }

                if (!(*json)["email"].isString() || !(*json)["password"].isString()) {
                    callback(errorResponse(drogon::k400BadRequest, "Invalid field types"));
                    return;
                }

                std::string email = (*json)["email"].asString();
                std::string password = (*json)["password"].asString();

                if (email.empty() || password.empty()) {
                    callback(errorResponse(drogon::k400BadRequest, "Fields cannot be empty"));
                    return;
                }

                auto db = drogon::app().getDbClient("default");
                db->execSqlAsync(
                    loginSql,
                    [callback, password, request](const drogon::orm::Result& result) {
                        if (result.empty()) {
                            callback(errorResponse(drogon::k401Unauthorized, "Invalid email or password"));
                            return;
                        }

                        std::string storedHash = result[0]["password_hash"].as<std::string>();
                        if (!verifyPassword(password, storedHash)) {
                            callback(errorResponse(drogon::k401Unauthorized, "Invalid email or password"));
                            return;
                        }

                        auto session = request->session();
                        session->clear();
                        session->changeSessionIdToClient();
                        session->insert("user_id", result[0]["id"].as<std::int64_t>());

                        Json::Value responseJson;
                        responseJson["message"] = "Login successful";

                        callback(drogon::HttpResponse::newHttpJsonResponse(responseJson));
                    },
                    [callback](const drogon::orm::DrogonDbException& error) {
                        LOG_ERROR << error.base().what();

                        callback(errorResponse(drogon::k500InternalServerError, "Database query failed"));
                    },
                    email);
            },
            {drogon::Post});
}

void registerCurrentUserRoute() {
    drogon::app().registerHandler("/api/me",
            [](const drogon::HttpRequestPtr& request, std::function<void(const drogon::HttpResponsePtr&)>&& callback) {
                auto session = request->session();

                if (!session->find("user_id")) {
                    callback(errorResponse(drogon::k401Unauthorized, "Not logged in"));
                    return;
                }

                auto userId = session->get<std::int64_t>("user_id");
                auto db = drogon::app().getDbClient("default");

                db->execSqlAsync(
                    "SELECT id, display_name, role FROM users WHERE id = $1",
                    [callback, session](const drogon::orm::Result& result) {
                        if (result.empty()) {
                            session->clear();

                            callback(errorResponse(drogon::k401Unauthorized, "Not logged in"));
                            return;
                        }

                        const auto& row = result[0];

                        Json::Value json;
                        json["id"] = Json::Int64(row["id"].as<std::int64_t>());
                        json["display_name"] = row["display_name"].as<std::string>();
                        json["role"] = row["role"].as<std::string>();

                        callback(drogon::HttpResponse::newHttpJsonResponse(json));
                    },
                    [callback](const drogon::orm::DrogonDbException& error) {
                        LOG_ERROR << error.base().what();

                        callback(errorResponse(drogon::k500InternalServerError, "Database query failed"));
                    },
                    userId);
            },
            {drogon::Get});
}

void registerLogoutRoute() {
    drogon::app().registerHandler("/api/logout",
            [](const drogon::HttpRequestPtr& request, std::function<void(const drogon::HttpResponsePtr&)>&& callback) {
                auto session = request->session();
                session->clear();
                session->changeSessionIdToClient();

                Json::Value json;
                json["message"] = "Logged out";

                callback(drogon::HttpResponse::newHttpJsonResponse(json));
            },
            {drogon::Post});
}

std::string loadSql(const std::string& path) {
    std::ifstream file(path);
    if (!file.is_open()) {
        throw std::runtime_error("Could not open SQL file: " + path);
    }

    std::stringstream buffer;
    buffer << file.rdbuf();
    return buffer.str();
}

std::string hashPassword(const std::string& password) {
    std::string hash(crypto_pwhash_STRBYTES, '\0');
    if (crypto_pwhash_str(&hash[0], password.c_str(), password.size(), crypto_pwhash_OPSLIMIT_INTERACTIVE,
            crypto_pwhash_MEMLIMIT_INTERACTIVE) != 0) {
        throw std::runtime_error("Failed to hash password");
    }
    hash.resize(std::char_traits<char>::length(hash.c_str()));
    return hash;
}

bool verifyPassword(const std::string& password, const std::string& hash) {
    return crypto_pwhash_str_verify(hash.c_str(), password.c_str(), password.size()) == 0;
}