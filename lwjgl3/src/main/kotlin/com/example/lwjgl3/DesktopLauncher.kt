package com.example.lwjgl3

import DungeonGame
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

object DesktopLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration()
        config.setTitle("Dungeon Game")
        config.setWindowedMode(800, 600)
        val dungeon = arrayOf(
            intArrayOf(-2, -3, 3, 4, 7),
            intArrayOf(-5, -10, 1, -10, 9),
            intArrayOf(10, 30, -5, -7, -3),
            intArrayOf(10, 20, -5, -4, -6),
            intArrayOf(10, 10, -5, 1,  3)
        )
        Lwjgl3Application(DungeonGame(dungeon), config)
    }
}
