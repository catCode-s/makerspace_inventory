#include <drogon/drogon.h>

int main()
{
    drogon::app()
        .registerHandler(
            "/api/health",
            [](const drogon::HttpRequestPtr&,
               std::function<void(const drogon::HttpResponsePtr&)>&& callback)
            {
                Json::Value json;
                json["status"] = "ok";

                auto response =
                    drogon::HttpResponse::newHttpJsonResponse(json);

                callback(response);
            },
            {drogon::Get}
        )
        .addListener("127.0.0.1", 8080)
        .run();

    return 0;
}