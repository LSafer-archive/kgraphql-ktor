# kgraphql-ktor

A simple integration for kgraphql and ktor

> NOTE: this library is just a bodging for kgraphql
>       and is not intended to be stable!
>       I might create a better library in the future.

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

### Thoughts about KGraphql

KGraphQL is not a stable library to use, and I don't think
the maintainers will continue supporting it.
First, the original maintainer `pgutkowski` just archived
the repository and the second maintainer `aPureBase` is
doing nothing to upgrade it!

I think KGraphql has so many faults and bad designs.
One of them is the ktor plugin with so poorly chosen
technics to receive execution parameters.
Another fault is the schema building technic.

I think the base builders should be more abstract with no
reflection or automation. Then, adding optional reflection
utilities as a syntax-sugar or whatever. But, KGraphQL just
loves reflection to the degree that you cannot escape it.

Bodging should be the easiest task to be done. Since you
don't need to think about any consequences. But, with
KGraphQL, you cannot bodge without using some "Illegal"
usage of the library like accessing private fields or
implementing a class with its functions all throwing an
exception just because you know the library will not invoke
them.
