# kgraphql-ktor

A simple integration for kgraphql and ktor

### Introduction

I believe graphql doesn't need a plugin. Instead, it needs a
routing extension just like the `get` and `post` extensions.

So, This library is just some routing extensions to handle
graphql requests.

### How to apply?

This is an example of using this library:

```kotlin
fun Route.configureGraphql() {
    route("/graphql") {
        graphqlPlayground()
        graphql {
            wrapErrors = true

            schema {
                firstSchema()
                secondSchema()
                thierdSchema()

                firstRootSchema()
                secondRootSchema()
                thierdRootSchema()
            }
        }
    }
}
```
