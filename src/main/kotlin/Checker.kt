package com.polimerconsumer

import java.io.File
import java.net.URLClassLoader
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Usage: <MainClassName> <JAR1> <JAR2> ... <JARN>")
        exitProcess(1)
    }

    val mainClassName = args[0]
    val jarPaths = args.drop(1).map { File(it).toURI().toURL() }
    val loader = URLClassLoader(jarPaths.toTypedArray())

    try {
        val mainClass = loader.loadClass(mainClassName)
        val dependenciesMet = checkDependencies(mainClass, loader)

        if (dependenciesMet) {
            println("All dependencies for $mainClassName are satisfied.")
        } else {
            println("Some dependencies for $mainClassName are missing.")
        }
    } catch (e: ClassNotFoundException) {
        println("Main class $mainClassName not found in the provided JAR files.")
        exitProcess(1)
    }
}

fun checkDependencies(clazz: Class<*>, loader: ClassLoader): Boolean {
    clazz.declaredFields.forEach {
        if (!isDependencyAvailable(it.type, loader)) {
            println("Missing dependency: ${it.type.name}")
            return false
        }
    }

    clazz.declaredMethods.forEach { method ->
        method.parameterTypes.forEach {
            if (!isDependencyAvailable(it, loader)) {
                println("Missing dependency: ${it.name}")
                return false
            }
        }
        if (!isDependencyAvailable(method.returnType, loader)) {
            println("Missing dependency: ${method.returnType.name}")
            return false
        }
    }

    val superclass = clazz.superclass
    if (superclass != null && !checkDependencies(superclass, loader)) {
        return false
    }

    return true
}

fun isDependencyAvailable(type: Class<*>, loader: ClassLoader): Boolean {
    return when {
        type.isPrimitive -> true
        type == Void.TYPE -> true
        type.isArray -> isDependencyAvailable(type.componentType, loader)
        else -> {
            try {
                loader.loadClass(type.name)
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }
    }
}
