import javax.swing.*
import java.awt.*
import java.awt.event.*

class MarioGame : JPanel(), ActionListener, KeyListener {
    // --- プレイヤーの設定 ---
    private var x = 100
    private var y = 300
    private var vy = 0.0
    private val gravity = 0.8
    private val jumpPower = -20.0
    private var onGround = false
    private val marioSize = 50

    // --- ゲーム状態 ---
    private var isGameOver = false
    private var isGameClear = false
    private var defeatCount = 0
    private var isBossActive = false

    // --- 入力管理 ---
    private var leftPressed = false
    private var rightPressed = false
    private var spacePressed = false

    // --- キャラクタークラス ---
    class Enemy(val startX: Int, val startY: Int) {
        var x = startX
        var y = startY
        var isAlive = true
        val width = 40
        val height = 40
        fun getBounds() = Rectangle(x, y, width, height)
        fun reset() { x = startX; y = startY; isAlive = true }
        fun draw(g: Graphics) {
            if (isAlive) {
                g.color = Color.ORANGE
                g.fillRect(x, y, width, height)
            }
        }
    }

    class Boss(var x: Int, var y: Int) {
        var hp = 3
        var speed = 4
        val width = 100
        val height = 150
        fun getBounds() = Rectangle(x, y, width, height)
        fun update() {
            x += speed
            if (x > 650 || x < 50) speed *= -1
        }
    }

    private val enemies = listOf(
        Enemy(400, 310),
        Enemy(600, 310),
        Enemy(200, 310)
    )
    private var boss: Boss? = null

    init {
        isFocusable = true
        addKeyListener(this)
        Timer(16, this).start() // 60FPS
    }

    private fun resetGame() {
        x = 100; y = 300; vy = 0.0
        defeatCount = 0; isBossActive = false; boss = null
        isGameOver = false; isGameClear = false
        enemies.forEach { it.reset() }
    }

    // --- メインループ (更新処理) ---
    override fun actionPerformed(e: ActionEvent?) {
        if (isGameOver || isGameClear) return

        // 左右移動
        if (leftPressed) x -= 5
        if (rightPressed) x += 5

        // 重力処理 (落下中は重力を強くするマリオ仕様)
        val currentGravity = if (vy > 0) gravity * 2.0 else gravity
        vy += currentGravity
        y += vy.toInt()

        // 地面判定
        if (y >= 300) {
            y = 300
            vy = 0.0
            onGround = true
        } else {
            onGround = false
        }

        val marioRect = Rectangle(x, y, marioSize, marioSize)

        // 敵との判定
        for (enemy in enemies) {
            if (enemy.isAlive && marioRect.intersects(enemy.getBounds())) {
                if (vy > 0 && y < enemy.y - 10) { // 踏みつけ
                    enemy.isAlive = false
                    defeatCount++
                    vy = if (spacePressed) -15.0 else -10.0 // 踏んだ時もボタン押しで高く跳ねる
                } else {
                    isGameOver = true
                }
            }
        }

        // ボス出現・判定
        if (defeatCount >= 3 && !isBossActive && !isGameClear) {
            isBossActive = true
            boss = Boss(600, 200)
        }

        if (isBossActive) {
            boss?.let { b ->
                b.update()
                if (marioRect.intersects(b.getBounds())) {
                    if (vy > 0 && y < b.y + 20) {
                        vy = if (spacePressed) -16.0 else -10.0
                        b.hp -= 1
                        if (b.hp <= 0) { isBossActive = false; isGameClear = true }
                    } else {
                        isGameOver = true
                    }
                }
            }
        }
        repaint()
    }

    // --- 描画処理 ---
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D

        // 背景
        g2d.color = Color(135, 206, 235); g2d.fillRect(0, 0, width, height)
        // 地面
        g2d.color = Color(34, 139, 34); g2d.fillRect(0, 350, width, 100)

        // キャラクター描画
        enemies.forEach { it.draw(g) }
        boss?.let {
            g2d.color = Color.MAGENTA; g2d.fillRect(it.x, it.y, it.width, it.height)
            g2d.color = Color.WHITE; g2d.drawString("BOSS HP: ${it.hp}", it.x + 20, it.y - 10)
        }

        // マリオ
        g2d.color = when {
            isGameClear -> Color.YELLOW
            isGameOver -> Color.GRAY
            else -> Color.RED
        }
        g2d.fillRect(x, y, marioSize, marioSize)

        // UI表示
        g2d.color = Color.BLACK
        g2d.setFont(Font("Arial", Font.BOLD, 16))
        g2d.drawString("Kills: $defeatCount / 3", 20, 30)

        if (isGameOver || isGameClear) {
            g2d.color = Color(0, 0, 0, 150)
            g2d.fillRect(0, 0, width, height)
            g2d.color = Color.WHITE
            g2d.setFont(Font("Arial", Font.BOLD, 40))
            val msg = if (isGameClear) "COURSE CLEAR!" else "GAME OVER"
            g2d.drawString(msg, width / 2 - 150, height / 2)
            g2d.setFont(Font("Arial", Font.PLAIN, 20))
            g2d.drawString("Press 'R' to Restart", width / 2 - 90, height / 2 + 50)
        }
    }

    // --- キー入力 ---
    override fun keyPressed(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_LEFT -> leftPressed = true
            KeyEvent.VK_RIGHT -> rightPressed = true
            KeyEvent.VK_SPACE -> {
                if (onGround && !isGameOver && !isGameClear) {
                    vy = jumpPower
                }
                spacePressed = true
            }
            KeyEvent.VK_R -> if (isGameOver || isGameClear) resetGame()
        }
    }

    override fun keyReleased(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_LEFT -> leftPressed = false
            KeyEvent.VK_RIGHT -> rightPressed = false
            KeyEvent.VK_SPACE -> {
                spacePressed = false
                if (vy < -5.0) vy = -5.0 // 可変ジャンプ：離すと減速
            }
        }
    }
    override fun keyTyped(e: KeyEvent?) {}
}

fun main() {
    val frame = JFrame("Kotlin Mario Complete Edition")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.add(MarioGame())
    frame.setSize(800, 450)
    frame.isResizable = false
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
}
