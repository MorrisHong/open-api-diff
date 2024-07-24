package kr.gracelove.openapidiffdemo.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.tags.Tag
import org.apache.commons.lang3.ObjectUtils
import org.apache.commons.lang3.StringUtils
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus

@Configuration
class OpenApiConfiguration {
    fun createApiResponse(description: String?, schema: Schema<*>?): ApiResponse {
        val mediaType = MediaType()
        mediaType.schema(schema)
        return ApiResponse()
            .description(description)
            .content(Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, mediaType))
    }

    fun createApiResponse(httpStatus: HttpStatus, requestUrl: String, method: String): ApiResponse {
        val map: MutableMap<String, Any> = HashMap()
        map["requiredKey"] = "Keys that are optionally response"
        val mediaType = MediaType()
        mediaType.schema(Schema<Any?>().example(map).properties(errorProperties()))
        return ApiResponse()
            .description(httpStatus.reasonPhrase)
            .content(Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, mediaType))
    }

    fun createDefaultResponses(apiResponses: ApiResponses) {
//        apiResponses.addApiResponse("400", createApiResponse("BadRequest", null));
        apiResponses.addApiResponse("401", createApiResponse("Unauthorized", null))
        apiResponses.addApiResponse("403", createApiResponse("Forbidden", null))
        apiResponses.addApiResponse("404", createApiResponse("Not Found", null))
        //        apiResponses.addApiResponse("500", createApiResponse("Server Error", null));
    }

    private fun createDefaultResponses(apiResponses: ApiResponses, requestUrl: String, method: String) {
        apiResponses.addApiResponse("400", createApiResponse(HttpStatus.BAD_REQUEST, requestUrl, method))
        apiResponses.addApiResponse("400", createApiResponse(HttpStatus.BAD_REQUEST, requestUrl, method))
        apiResponses.addApiResponse("401", createApiResponse(HttpStatus.UNAUTHORIZED, requestUrl, method))
        apiResponses.addApiResponse("403", createApiResponse(HttpStatus.FORBIDDEN, requestUrl, method))
        apiResponses.addApiResponse("404", createApiResponse(HttpStatus.NOT_FOUND, requestUrl, method))
    }

    @Bean
    fun external(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .addOpenApiCustomizer { openApi ->
                val securityScheme = SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .name(HttpHeaders.AUTHORIZATION)
                    .`in`(SecurityScheme.In.HEADER)
                openApi.components
                    .addSecuritySchemes("API Token", securityScheme)
                openApi.paths.values
                    .forEach { pathItem -> pathItem.readOperations().forEach { createDefaultResponses(it.responses) } }
            }
            .group("external")
            .pathsToMatch(EXTERNAL_API_PATH)
            .build()
    }

    @Bean
    fun internal(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .addOpenApiCustomizer { openApi ->
                openApi.tags = (openApi.tags ?: emptyList())
                    .sortedBy { tag -> StringUtils.stripAccents(tag.name) }
            }
            .group("internal")
            .pathsToMatch("/api/**")
            .build()
    }

    @Bean
    fun develop(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .addOpenApiCustomizer { openApi: OpenAPI ->
                openApi.tags = openApi.tags
                    ?.sortedWith(Comparator.comparing { tag: Tag -> StringUtils.stripAccents(tag.name) })
                    ?: emptyList()
            }
            .group("develop")
            .apply {
                pathsToMatch(
                    "/api/admin/**",
                    "/_internal/proxy/**",
                    "/api/user/**",
                    "/api/dd/**",
                    "/api/tags/**",
                    "/api/server-agent/**",
                    "/_internal/cabinet/**",
                    "/_internal/gateway/**"
                )
            }
            .build()
    }

    @Bean
    fun openApi(): OpenAPI {
        val extensions = HashMap<String, Any>()
        val logo = HashMap<String, String>()
        logo["url"] = "/api/docs/assets/querypie.svg"
        logo["altText"] = "QueryPie logo"
        extensions["x-logo"] = logo
        return OpenAPI()
            .info(
                Info()
                    .title(TITLE)
                    .extensions(extensions)
                    .description(DESCRIPTION)
                    .contact(Contact().name("QueryPie").url("https://chequer.atlassian.net/servicedesk/customer"))
                    .version("0.9"),
            )
    }

    private fun errorProperties(): Map<String, Schema<*>> {
        val responseErrorProperties: MutableMap<String, Schema<*>> = HashMap()
        val errorProperties: MutableMap<String, Schema<*>> = HashMap()
        errorProperties["code"] = createSchema("Error Code", OpenApiDataTypes.STRING.type)
        errorProperties["message"] = createSchema("Error Message", OpenApiDataTypes.STRING.type)
        errorProperties["path"] = createSchema("Request Method And Path", OpenApiDataTypes.STRING.type)
        errorProperties["status"] = createSchema("Response Http Status", OpenApiDataTypes.INTEGER.type)
        responseErrorProperties["error"] =
            createSchema("error Object", OpenApiDataTypes.OBJECT.type).properties(errorProperties)
        responseErrorProperties["requiredKey"] =
            createSchema("Keys that are optionally response", OpenApiDataTypes.STRING.type)
        return responseErrorProperties
    }

    private fun findByMethod(pathItem: PathItem): String {
        return if (ObjectUtils.isNotEmpty(pathItem.get)) {
            "GET"
        } else if (ObjectUtils.isNotEmpty(pathItem.post)) {
            "POST"
        } else if (ObjectUtils.isNotEmpty(pathItem.put)) {
            "PUT"
        } else if (ObjectUtils.isNotEmpty(pathItem.patch)) {
            "PATCH"
        } else if (ObjectUtils.isNotEmpty(pathItem.delete)) {
            "DELETE"
        } else {
            ""
        }
    }

    private fun createSchema(description: String, type: String): Schema<*> {
        return Schema<Any?>().description(description).type(type)
    }

    private enum class OpenApiDataTypes(val type: String) {
        STRING("string"),
        NUMBER("number"),
        INTEGER("integer"),
        BOOLEAN("boolean"),
        ARRAY("array"),
        OBJECT("object"),
    }

    companion object {
        private const val EXTERNAL_API_PATH = "/api/external/**"
        private const val TITLE = "QueryPie API"
        private const val DESCRIPTION = "QueryPie API"
    }
}