import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class DungeonGame(private var dungeon: Array<IntArray>) : ApplicationAdapter() {
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var uiFont: BitmapFont
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var uiStage: Stage
    private lateinit var skin: Skin
    private lateinit var knightTexture: Texture
    private lateinit var lavaTexture: Texture
    private lateinit var groundTexture: Texture
    public var knightPosition = Vector2(0f, 0f)
    private var path: List<Vector2> = emptyList()
    public var pathIndex = 0
    private var animationTimer = 0f
    public var isAnimating = false
    private var minHealth = 0
    private var fontScaleFactor = 0f
    private var uiScaleFactor = 1f

    override fun create() {
        batch = SpriteBatch()
        font = BitmapFont()
        uiFont = BitmapFont()
        shapeRenderer = ShapeRenderer()
        camera = OrthographicCamera()

        // Initialize viewport with virtual dimensions based on dungeon size
        val m = dungeon.size.toFloat()
        val n = dungeon[0].size.toFloat()
        viewport = FitViewport(n, m + 1f, camera) // +1 for health text space
        camera.setToOrtho(true) // Y-down for top-left origin

        // Load textures (wrap in try-catch to log errors)
        try {
            knightTexture = Texture("sprites/Assassin.gif")
            lavaTexture = Texture("sprites/Lava_Tile.gif")
            groundTexture = Texture("sprites/Black_Marble_Floor.gif")
        } catch (e: Exception) {
            Gdx.app.log("DungeonGame", "Texture loading error: ${e.message}")
        }

        // Update font scale
        updateFontScale()

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

        // Create generate button
        val generateButton = TextButton("New Map", skin)
        uiStage.addActor(generateButton)

        // Add click listener to button
        generateButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                Gdx.app.log("DungeonGame", "Button clicked")
                generateNewDungeon()
            }
        })

        // Initial UI scale and position
        updateUIScaleAndPosition()

        // Set input multiplexer
        val multiplexer = InputMultiplexer()
        multiplexer.addProcessor(uiStage)
        multiplexer.addProcessor(GameInputProcessor(this))
        Gdx.input.inputProcessor = multiplexer
    }

    private fun generateNewDungeon() {
        val m = dungeon.size
        val n = dungeon.size
        dungeon = Array(m) { IntArray(n) { Random.nextInt(-1000, 1001) } }
        val result = calculateMinHealthAndPath(dungeon)
        minHealth = result.first
        path = result.second.map { Vector2(it.second.toFloat(), it.first.toFloat()) }
        knightPosition = Vector2(0f, 0f)
        isAnimating = false
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        uiStage.viewport.update(width, height, true)
        updateFontScale()
        updateUIScaleAndPosition()
    }

    private fun updateFontScale() {
        // Calculate scale factor based on viewport (pixels per virtual unit)
        val scale = min(viewport.screenWidth.toFloat() / viewport.worldWidth, viewport.screenHeight.toFloat() / viewport.worldHeight)
        fontScaleFactor = (scale / 50f) * 1.5f // Relative to original 50px cell design
        font.data.setScale(0.03f, -0.03f) // Flip Y for text
        font.setUseIntegerPositions(false)
    }

    private fun updateUIScaleAndPosition() {
        uiScaleFactor = Gdx.graphics.width.toFloat() / 800f
        uiFont.data.setScale(uiScaleFactor)
        // Find and update the button
        uiStage.actors.forEach { actor ->
            if (actor is TextButton && actor.text.toString() == "New Map") {
                actor.pack() // Recalculate size after scaling
                actor.setPosition(viewport.screenWidth - (actor.width * uiScaleFactor), 10f * uiScaleFactor) // Scale padding too
            }
        }
    }

    override fun render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        camera.update()
        batch.projectionMatrix = camera.combined
        shapeRenderer.projectionMatrix = camera.combined

        // Draw dungeon grid with SpriteBatch
        batch.begin()
        for (i in dungeon.indices) {
            for (j in dungeon[i].indices) {
                val x = j.toFloat()
                val y = i.toFloat()
                // Draw appropriate texture based on cell value
                val texture = when {
                    dungeon[i][j] < 0 -> lavaTexture // Demons
                    dungeon[i][j] > 0 -> groundTexture // Orbs
                    else -> null // Empty cells will use ShapeRenderer
                }
                if (texture != null) {
                    batch.setColor(Color.WHITE)
                    batch.draw(
                        texture, x, y,
                        0f, 0f, 1f, 1f, // Draw width/height = 1x1 virtual unit
                        1f, 1f, // Scale = 1f (no additional scaling needed)
                        0f, // No rotation
                        0, 0, texture.width, texture.height, // Source region (full texture)
                        false, true // Flip Y to correct orientation
                    )
                }
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
        // Draw minimum health
        font.draw(batch, "Min Health: $minHealth", 0.1f, dungeon.size.toFloat())
        batch.end()

        // Draw empty cells with ShapeRenderer
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for (i in dungeon.indices) {
            for (j in dungeon[i].indices) {
                if (dungeon[i][j] == 0) { // Only for empty cells
                    val x = j.toFloat()
                    val y = i.toFloat()
                    shapeRenderer.color = Color(0.5f, 0.5f, 0.5f, 0.5f) // Gray for empty
                    shapeRenderer.rect(x, y, 1f, 1f)
                }
            }
        }
        shapeRenderer.end()

        // Update and draw UI stage
        uiStage.act(Gdx.graphics.deltaTime)
        uiStage.draw()

        // Animation logic for knight movement
        if (isAnimating) {
            animationTimer += Gdx.graphics.deltaTime
            if (animationTimer >= 0.5f && pathIndex < path.size) {
                knightPosition = path[pathIndex]
                pathIndex++
                animationTimer = 0f
                if (pathIndex >= path.size) {
                    isAnimating = false
                }
            }
        }
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
        uiFont.dispose()
        knightTexture.dispose()
        lavaTexture.dispose()
        groundTexture.dispose()
        shapeRenderer.dispose()
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
