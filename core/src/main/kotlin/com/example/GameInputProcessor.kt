import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2

class GameInputProcessor(private val game: DungeonGame, private val camera: OrthographicCamera) : InputProcessor {

    override fun keyDown(keycode: Int): Boolean {
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (!game.isAnimating) {
            game.isAnimating = true
            game.pathIndex = 0
            game.knightPosition = Vector2(0f, 0f)
            return true
        }
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        val dx = Gdx.input.getDeltaX(pointer).toFloat()
        val dy = Gdx.input.getDeltaY(pointer).toFloat()
        camera.translate(-dx * camera.zoom, dy * camera.zoom, 0f)
        camera.update()
        return true
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        var zoomAmount = amountY * 0.1f;
        if (camera.zoom + zoomAmount < 0.1f) {
            camera.zoom = 0.1f
        } else {
            camera.zoom += zoomAmount
        }
        camera.update()
        return true
    }
}
