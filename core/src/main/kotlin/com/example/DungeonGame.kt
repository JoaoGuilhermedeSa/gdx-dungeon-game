import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.random.nextInt

class DungeonGame(private var dungeon: Array<IntArray>) : ApplicationAdapter() {
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var uiFont: BitmapFont
    private lateinit var camera: OrthographicCamera
    private var zoom = 1.0f
    private lateinit var viewport: ExtendViewport
    private lateinit var uiStage: Stage
    private lateinit var skin: Skin
    private lateinit var minHealthLabel: Label
    private lateinit var generateButton: TextButton
    private lateinit var table: Table
    private lateinit var knightTexture: Texture
    private lateinit var lavaTexture: Texture
    private lateinit var groundTexture: Texture
    private lateinit var fireEffectTexture: Texture
    private lateinit var healEffectTexture: Texture
    private lateinit var fireAnimation: Animation<TextureRegion>
    private lateinit var healAnimation: Animation<TextureRegion>
    private val activeEffects = mutableListOf<ActiveEffect>()
    private lateinit var backgroundMusic: Music
    private lateinit var healSound: Sound
    private lateinit var fireSound: Sound
    var knightPosition = Vector2(0f, 0f)
    private var path: List<Vector2> = emptyList()
    var pathIndex = 0
    private var animationTimer = 0f
    var isAnimating = false
    private var minHealth = 0

    data class ActiveEffect(val animation: Animation<TextureRegion>, val position: Vector2, var stateTime: Float = 0f)

    override fun create() {
        batch = SpriteBatch()
        font = BitmapFont()
        uiFont = BitmapFont()
        uiFont.data.setScale(2f) // Larger font for UI
        camera = OrthographicCamera()

        // Initialize viewport with virtual dimensions based on dungeon size
        val m = dungeon.size.toFloat()
        val n = dungeon[0].size.toFloat()
        viewport = ExtendViewport(n, m, camera) // +1 for health text space
        camera.setToOrtho(true) // Y-down for top-left origin
        camera.zoom = zoom

        // Load textures (wrap in try-catch to log errors)
        try {
            knightTexture = Texture("sprites/Assassin.gif")
            lavaTexture = Texture("sprites/Lava_Tile.gif")
            groundTexture = Texture("sprites/Black_Marble_Floor.gif")
            fireEffectTexture = Texture("sprites/fire_effect.png")
            healEffectTexture = Texture("sprites/heal_effect.png")

            // Load audio
            backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/ost.mp3"))
            healSound = Gdx.audio.newSound(Gdx.files.internal("audio/heal.mp3"))
            fireSound = Gdx.audio.newSound(Gdx.files.internal("audio/fire.mp3"))
        } catch (e: Exception) {
            Gdx.app.log("DungeonGame", "Asset loading error: ${e.message}")
        }

        // Play background music
        backgroundMusic.isLooping = true
        backgroundMusic.volume = 50f
        backgroundMusic.play()

        // Create animations
        fireAnimation = createAnimation(fireEffectTexture, 10, 64, 64)
        healAnimation = createAnimation(healEffectTexture, 7, 32, 64)

        // Update font scale
        font.data.setScale(0.03f, -0.03f) // Flip Y for text
        font.setUseIntegerPositions(false)

        // Calculate path and min health
        val result = calculateMinHealthAndPath(dungeon)
        minHealth = result.first
        path = result.second.map { Vector2(it.second.toFloat(), it.first.toFloat()) }

        // Set up UI stage with ScreenViewport for pixel-perfect UI
        uiStage = Stage(ScreenViewport())

        // Create simple skin with uiFont
        skin = Skin()
        skin.add("default-font", uiFont, BitmapFont::class.java)

        val buttonStyle = TextButton.TextButtonStyle()
        buttonStyle.font = uiFont
        skin.add("default", buttonStyle)

        // Create min health label
        val labelStyle = Label.LabelStyle(uiFont, Color.WHITE)
        minHealthLabel = Label("Min Health: $minHealth", labelStyle)

        // Create generate button
        generateButton = TextButton("New Map", skin)

        // Add click listener to button
        generateButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                Gdx.app.log("DungeonGame", "Button clicked")
                generateNewDungeon()
            }
        })

        // Set up table layout
        table = Table()
        table.setFillParent(true)
        uiStage.addActor(table)

        table.top().left()
        table.add(minHealthLabel).pad(10f)
        table.row()
        table.add(generateButton).pad(10f)

        // Set input multiplexer
        val multiplexer = InputMultiplexer()
        multiplexer.addProcessor(uiStage)
        multiplexer.addProcessor(GameInputProcessor(this, camera))
        Gdx.input.inputProcessor = multiplexer
    }

    private fun createAnimation(texture: Texture, frameCount: Int, frameWidth: Int, frameHeight: Int): Animation<TextureRegion> {
        val frames = TextureRegion.split(texture, frameWidth, frameHeight)[0]
        for (frame in frames) {
            frame.flip(false, true)
        }
        val animation = Animation<TextureRegion>(0.1f, *frames)
        animation.playMode = Animation.PlayMode.NORMAL
        return animation
    }

    private fun generateNewDungeon() {
        val m = Random.nextInt(1, 201)
        val n = Random.nextInt(1, 201)
        dungeon = Array(m) { IntArray(n) { Random.nextInt(-1000, 1001) } }
        val result = calculateMinHealthAndPath(dungeon)
        minHealth = result.first
        path = result.second.map { Vector2(it.second.toFloat(), it.first.toFloat()) }
        knightPosition = Vector2(0f, 0f)
        isAnimating = false
        activeEffects.clear()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        camera.zoom = zoom
        uiStage.viewport.update(width, height, true)
    }



    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Animation logic for knight movement
        if (isAnimating) {
            animationTimer += Gdx.graphics.deltaTime
            if (animationTimer >= 0.5f && pathIndex < path.size) {
                knightPosition = path[pathIndex]
                pathIndex++
                animationTimer = 0f

                val tileX = knightPosition.x.toInt()
                val tileY = knightPosition.y.toInt()
                if (dungeon[tileY][tileX] < 0) {
                    activeEffects.add(ActiveEffect(fireAnimation, Vector2(tileX.toFloat(), tileY.toFloat())))
                    fireSound.play()
                } else {
                    activeEffects.add(ActiveEffect(healAnimation, Vector2(tileX.toFloat(), tileY.toFloat())))
                    healSound.play()
                }

                if (pathIndex >= path.size) {
                    isAnimating = false
                }
            }
        }

        viewport.apply()
        camera.position.set(knightPosition.x + 0.5f, knightPosition.y + 0.5f, 0f)
        camera.update()
        batch.projectionMatrix = camera.combined

        val m = dungeon.size
        val n = dungeon[0].size

        val worldWidth = viewport.worldWidth
        val worldHeight = viewport.worldHeight

        val camX = camera.position.x
        val camY = camera.position.y

        val startX = max(0, (camX - worldWidth / 2 * camera.zoom).toInt())
        val endX = min(n - 1, (camX + worldWidth / 2 * camera.zoom).toInt())
        val startY = max(0, (camY - worldHeight / 2 * camera.zoom).toInt())
        val endY = min(m - 1, (camY + worldHeight / 2 * camera.zoom).toInt())

        // Draw dungeon grid with SpriteBatch
        batch.begin()
        for (i in startY..endY) {
            for (j in startX..endX) {
                val x = j.toFloat()
                val y = i.toFloat()
                // Draw appropriate texture based on cell value
                val texture = when {
                    dungeon[i][j] < 0 -> lavaTexture // Demons
                    else -> groundTexture // Orbs
                }
                batch.setColor(Color.WHITE)
                batch.draw(
                    texture, x, y,
                    0f, 0f, 1f, 1f, // Draw width/height = 1x1 virtual unit
                    1f, 1f, // Scale = 1f (no additional scaling needed)
                    0f, // No rotation
                    0, 0, texture.width, texture.height, // Source region (full texture)
                    false, true // Flip Y to correct orientation
                )
                // Draw cell value (centered in cell)
                batch.setColor(Color.WHITE)
                font.draw(batch, dungeon[i][j].toString(), x + 0.2f, y + 0.6f)
            }
        }
        // Draw knight
        batch.setColor(Color.WHITE)
        batch.draw(
            knightTexture, knightPosition.x, knightPosition.y,
            0f, 0f, 1f, 1f, // Draw width/height = 1x1 virtual unit
            1f, 1f, // Scale = 1f
            0f, // No rotation
            0, 0, knightTexture.width, knightTexture.height, // Source region (full texture)
            false, true // Flip Y to correct orientation
        )

        // Draw active effects
        val iterator = activeEffects.iterator()
        while (iterator.hasNext()) {
            val effect = iterator.next()
            effect.stateTime += Gdx.graphics.deltaTime
            if (effect.animation.isAnimationFinished(effect.stateTime)) {
                iterator.remove()
            } else {
                val frame = effect.animation.getKeyFrame(effect.stateTime)
                batch.draw(frame, effect.position.x, effect.position.y, 1f, 1f)
            }
        }
        batch.end()

        // Update and draw UI stage
        uiStage.act(Gdx.graphics.deltaTime)
        uiStage.draw()

        minHealthLabel.setText("Min Health: $minHealth")
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
        uiFont.dispose()
        knightTexture.dispose()
        lavaTexture.dispose()
        groundTexture.dispose()
        fireEffectTexture.dispose()
        healEffectTexture.dispose()
        backgroundMusic.dispose()
        healSound.dispose()
        fireSound.dispose()
        skin.dispose()
        uiStage.dispose()
    }

    private fun calculateMinHealthAndPath(dungeon: Array<IntArray>): Pair<Int, List<Pair<Int, Int>>> {
        val m = dungeon.size
        val n = dungeon[0].size
        val dp = Array(m + 1) { IntArray(n + 1) }
        val path = mutableListOf<Pair<Int, Int>>()

        // Initialize dp array
        for (i in m downTo 0) {
            for (j in n downTo 0) {
                if (i == m || j == n) {
                    dp[i][j] = Int.MAX_VALUE
                }
            }
        }

        // Set base case for princess's cell
        dp[m][n - 1] = 1
        dp[m - 1][n] = 1

        // Fill dp table
        for (i in m - 1 downTo 0) {
            for (j in n - 1 downTo 0) {
                val need = min(dp[i + 1][j], dp[i][j + 1]) - dungeon[i][j]
                dp[i][j] = max(1, need)
            }
        }

        // Reconstruct optimal path
        var i = 0
        var j = 0
        path.add(Pair(i, j))
        while (i < m - 1 || j < n - 1) {
            if (i == m - 1) {
                j++
            } else if (j == n - 1) {
                i++
            } else {
                if (dp[i + 1][j] <= dp[i][j + 1]) {
                    i++
                } else {
                    j++
                }
            }
            path.add(Pair(i, j))
        }

        return Pair(dp[0][0], path)
    }
}
