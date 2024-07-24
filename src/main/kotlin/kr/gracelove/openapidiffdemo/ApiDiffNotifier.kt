package kr.gracelove.openapidiffdemo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import java.io.File

data class ApiChange(
    val type: ChangeType,
    val path: String,
    val method: String,
    val apiType: ApiType,
    val description: String
)

enum class ChangeType { ADDED, MODIFIED, DELETED }
enum class ApiType { EXTERNAL, INTERNAL }

fun getSpecForCommit(commitHash: String): OpenAPI {
    val specContent = "git show $commitHash:spec.yaml".runCommand()
    return OpenAPIV3Parser().readContents(specContent).openAPI
}

fun String.runCommand(): String {
    return Runtime.getRuntime().exec(this).inputStream.bufferedReader().readText()
}

fun compareSpecs(baseSpec: OpenAPI, targetSpec: OpenAPI): List<ApiChange> {
    val changes = mutableListOf<ApiChange>()

    // 새로 추가된 경로 확인
    targetSpec.paths.orEmpty().forEach { (path, targetItem) ->
        if (!baseSpec.paths.orEmpty().containsKey(path)) {
            targetItem.readOperationsMap().forEach { (method, operation) ->
                changes.add(ApiChange(
                    ChangeType.ADDED,
                    path,
                    method.name,
                    determineApiType(operation),
                    "New endpoint added"
                ))
            }
        }
    }

    // 실제 구현에서는 수정된 경로와 삭제된 경로도 확인.

    return changes
}

fun determineApiType(operation: io.swagger.v3.oas.models.Operation): ApiType {
    return when (operation.extensions?.get("x-api-type") as? String) {
        "external" -> ApiType.EXTERNAL
        else -> ApiType.INTERNAL
    }
}

fun formatDiffForSlack(changes: List<ApiChange>): String {
    return buildString {
        appendLine("API changes detected:")
        appendLine("```")
        changes.forEach { change ->
            val symbol = when (change.type) {
                ChangeType.ADDED -> "+"
                ChangeType.DELETED -> "-"
                ChangeType.MODIFIED -> "!"
            }
            appendLine("$symbol ${change.apiType} API: ${change.method} ${change.path}")
            if (change.type == ChangeType.MODIFIED) {
                appendLine("  ${change.description}")
            }
        }
        appendLine("```")
    }
}
//
//fun main(args: Array<String>) {
//    if (args.size != 2) {
//        println("Usage: ./gradlew apiDiff -PBASE_SHA=<base_commit> -PHEAD_SHA=<head_commit>")
//        return
//    }
//
//    val baseCommit = args[0]
//    val headCommit = args[1]
//
//    val baseSpec = getSpecForCommit(baseCommit)
//    val targetSpec = getSpecForCommit(headCommit)
//
//    val changes = compareSpecs(baseSpec, targetSpec)
//
//    if (changes.isNotEmpty()) {
//        val diffOutput = formatDiffForSlack(changes)
//        println(diffOutput)
//
//        val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
//        File("api_changes.json").writeText(mapper.writeValueAsString(changes))
//    } else {
//        println("No API changes detected between $baseCommit and $headCommit")
//    }
//}