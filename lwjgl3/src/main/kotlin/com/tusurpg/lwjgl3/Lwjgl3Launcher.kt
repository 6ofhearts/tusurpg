@file:JvmName("Lwjgl3Launcher")

package com.tusurpg.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.tusurpg.tusurpg

/** Launches the desktop (LWJGL3) application. */
fun main() {
    Lwjgl3Application(tusurpg(), Lwjgl3ApplicationConfiguration().apply {
        setTitle("TUSUR_RPG")
        setWindowedMode(640, 480)
        setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))
        useVsync(false) //не использовать вертикальную синхронизацию
    })
}
