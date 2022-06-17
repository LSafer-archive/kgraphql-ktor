package net.lsafer.kgraphql.ktor

import com.apurebase.kgraphql.schema.dsl.types.InputValuesDSL
import com.apurebase.kgraphql.Context
import com.apurebase.kgraphql.schema.Subscriber
import com.apurebase.kgraphql.schema.dsl.PropertyDSL
import com.apurebase.kgraphql.schema.dsl.ResolverDSL
import com.apurebase.kgraphql.schema.dsl.types.InputValueDSL
import com.apurebase.kgraphql.schema.model.FunctionWrapper
import com.apurebase.kgraphql.schema.model.InputValueDef
import com.apurebase.kgraphql.schema.structure.validateName
import com.fasterxml.jackson.databind.ObjectWriter
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.reflect
import kotlin.reflect.typeOf

/**
 * An alternative for [InputValuesDSL.arg]
 */
fun <T : Any> ResolverDSL.Target.arg(
    kClass: KClass<T>,
    kType: KType? = null,
    block: InputValueDSL<T>.() -> Unit
) {
    val inputValueDSL = InputValueDSL(kClass, kType).apply(block)

    addInputValues(listOf(inputValueDSL.toKQLInputValue()))
}

/**
 * An alternative for [InputValuesDSL.arg]
 */
inline fun <reified T : Any> ResolverDSL.Target.arg(
    noinline block: InputValueDSL<T>.() -> Unit
) {
    arg(T::class, typeOf<T>(), block)
}

/**
 * The scope of the function passed to [resolve].
 */
class ResolveScope(
    val context: Context,
    val arguments: Map<String, Any?>
) {
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> arg(name: String): T {
        val type = typeOf<T>()
        val argument = arguments[name]
        if (argument === null && !type.isMarkedNullable)
            error("Nullable argument is used as a non-nullable.")
        val klass = type.jvmErasure
        if (!klass.isInstance(argument))
            error("Argument of type ${argument?.javaClass} is used as ${klass.simpleName}")
        return argument as T
    }
}

/**
 * An alternative for [PropertyDSL.resolver]
 */
fun <T : Any, R> PropertyDSL<T, R>.resolve(
    block: suspend ResolveScope.(T) -> R
) {
    val inputValuesProperty =
        PropertyDSL::class.declaredMemberProperties
            .first { it.name == "inputValues" }
    val resolverFunction = PropertyDSL::class.declaredMemberFunctions
        .first { it.name == "resolver" }
    inputValuesProperty.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val inputValues = inputValuesProperty.get(this) as List<InputValueDef<*>>
    resolverFunction.isAccessible = true
    resolverFunction.call(this, FunctionWrapperAdapter(
        inputValues = inputValues,
        block = block
    ))
}

/* Bad design leads to bad bodging */
class FunctionWrapperAdapter<O : Any, T>(
    val inputValues: List<InputValueDef<*>>,
    val block: suspend ResolveScope.(O) -> T,
) : FunctionWrapper<T> {
    /**
     * This is used to pass the receiver as the
     * first argument.
     */
    override val hasReceiver = true

    /**
     * This is used to obtain the return type.
     *
     * It would be easier if it was as simple as
     * `returnType: KType`
     */
    @OptIn(ExperimentalReflectionOnLambdas::class)
    override val kFunction: KFunction<T>
        get() = block.reflect()!!

    /**
     * This is used to "automagically" decide the
     * supported parameters.
     */
    override val argumentsDescriptor by lazy {
        mapOf("_ctx_" to typeOf<Context>()) +
                inputValues.associate {
                    val name = it.name
                    validateName(name)
                    name to it.kType!!
                }
    }

    /**
     * I don't know for what is this used.
     */
    override fun hasReturnType() = true

    /**
     * I don't know for what is this used.
     *
     * It was set to always return the number of
     * arguments (excluding receiver) the function
     * supports.
     */
    override fun arity(): Int {
        error("Not Implemented: FunctionWrapperAdapter.arity()")
    }

    override suspend fun invoke(vararg args: Any?): T? {
        @Suppress("UNCHECKED_CAST")
        val receiver = args[0] as O
        val context = args[1] as Context
        val arguments = inputValues.zip(args.drop(2)).associate {
            it.first.name to it.second
        }
        val scope = ResolveScope(context, arguments)

        return block(scope, receiver)
    }

    /**
     * I don't know for what is this used.
     *
     * It was set to always throw an error in the
     * original implementation.
     */
    override suspend fun invoke(
        args: List<Any?>,
        subscriptionArgs: List<String>,
        objectWriter: ObjectWriter
    ): T? {
        error("Not Implemented: FunctionWrapperAdapter.invoke()")
    }

    /**
     * I don't know for what is this used.
     *
     * In the original implementation, this was
     * used to alter some global instance.
     */
    override fun subscribe(subscription: String, subscriber: Subscriber) {
        error("Not Implemented: FunctionWrapperAdapter.subscribe()")
    }

    /**
     * I don't know for what is this used.
     *
     * In the original implementation, this was
     * used to alter some global instance.
     */
    override fun unsubscribe(subscription: String) {
        error("Not Implemented: FunctionWrapperAdapter.unsubscribe()")
    }
}
