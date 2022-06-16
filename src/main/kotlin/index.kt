package net.lsafer.kgraphql.ktor

import com.apurebase.kgraphql.ContextBuilder
import com.apurebase.kgraphql.GraphQLError
import com.apurebase.kgraphql.GraphqlRequest
import com.apurebase.kgraphql.KtorGraphQLConfiguration
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import com.apurebase.kgraphql.schema.dsl.SchemaConfigurationDSL
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.apurebase.kgraphql.KGraphQL.Companion.schema as createSchema
import com.apurebase.kgraphql.context as createContext

class GraphqlEndpointConfiguration : SchemaConfigurationDSL() {
    internal var schemaBlock: (SchemaBuilder.() -> Unit)? = null
    internal var contextBlock: (ContextBuilder.(ApplicationCall) -> Unit)? = null

    /**
     * Specify how to build the context.
     */
    fun context(block: ContextBuilder.(ApplicationCall) -> Unit) {
        contextBlock = block
    }

    /**
     * Specify how to build the schema.
     */
    fun schema(block: SchemaBuilder.() -> Unit) {
        schemaBlock = block
    }
}

/**
 * Builds a route to match graphql `POST` requests
 * and respond to them with the result of executing
 * a graphql schema built with the given [builder].
 */
fun Route.graphql(
    path: String = "",
    builder: GraphqlEndpointConfiguration.() -> Unit = {}
) {
    val configuration = GraphqlEndpointConfiguration().apply(builder)
    val schema = createSchema {
        this.configuration = configuration
        configuration.schemaBlock?.invoke(this)
    }

    post(path) {
        val request = call.receive<GraphqlRequest>()
        val context = createContext {
            configuration.contextBlock?.invoke(this, call)
        }
        try {
            val result = schema.execute(
                request.query,
                request.variables.toString(),
                context
            )

            call.respondText(result, contentType = ContentType.Application.Json)
        } catch (error: GraphQLError) {
            // TODO: externalize formatting error
            val result = format(error)

            call.respondText(result, contentType = ContentType.Application.Json)
        }
    }
}

/**
 * Builds a route to match graphql `GET` requests
 * and respond to them with the graphql playground
 * html.
 */
fun Route.graphqlPlayground(
    path: String = ""
) {
    get(path) {
        @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val playgroundHtml =
            KtorGraphQLConfiguration::class.java.classLoader.getResource("playground.html")
                .readBytes()
        call.respondBytes(playgroundHtml, contentType = ContentType.Text.Html)
    }
}

internal fun format(error: GraphQLError): String {
    val message = error.message
    val locations = error.locations

    return buildJsonObject {
        put("errors", buildJsonArray {
            addJsonObject {
                put("message", message)
                put("locations", buildJsonArray {
                    locations?.forEach {
                        addJsonObject {
                            put("line", it.line)
                            put("column", it.column)
                        }
                    }
                })
                put("path", buildJsonArray {
                    // TODO: Build this path. https://spec.graphql.org/June2018/#example-90475
                })
                put("extensions", buildJsonObject {
                    // TODO: Find a way to provide extensions
                    //  https://github.com/graphql/graphql-spec/blob/main/spec/Section%207%20--%20Response.md
                })
            }
        })
    }.toString()
}
