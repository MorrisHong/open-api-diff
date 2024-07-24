import io.swagger.v3.oas.models.OpenAPI
import java.io.File
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue

val baseCommit = args[0]
val targetCommit = args[1]

fun getSpecForCommit(commitHash: String): OpenAPI {
    val specContent = "git show $commitHash:spec.yaml".runCommand()
    return OpenAPIV3Parser().readContents(specContent).openAPI
}