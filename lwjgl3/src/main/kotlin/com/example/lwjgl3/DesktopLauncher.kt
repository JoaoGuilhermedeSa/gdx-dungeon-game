package com.example.lwjgl3

import DungeonGame
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import kotlin.random.Random

object DesktopLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration()
        config.setTitle("Dungeon Game")
        config.setWindowedMode(800, 600)
        val colSize = Random.nextInt(1, 201)
        val dungeon = Array(Random.nextInt(1, 201)) { IntArray(colSize) { Random.nextInt(-1000, 1001) } }
        Lwjgl3Application(DungeonGame(dungeon), config)
    }
}
