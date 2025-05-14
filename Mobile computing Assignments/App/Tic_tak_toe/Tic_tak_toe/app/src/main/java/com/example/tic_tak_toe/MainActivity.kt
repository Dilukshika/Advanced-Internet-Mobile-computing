package com.example.tic_tak_toe

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// Sound Manager class to handle all game sounds
class SoundManager(private val context: Context) {
    private var winSound: MediaPlayer? = null
    private var drawSound: MediaPlayer? = null
    private var timerSound: MediaPlayer? = null

    init {
       winSound = MediaPlayer.create(context, R.raw.winning)
        drawSound = MediaPlayer.create(context, R.raw.nothing)
       timerSound = MediaPlayer.create(context, R.raw.timer2)
    }

    fun playWinSound() {
        winSound?.start()
    }

    fun playDrawSound() {
        drawSound?.start()
    }

    fun playTimerSound() {
        timerSound?.start()
    }

    fun release() {
        winSound?.release()
        drawSound?.release()
        timerSound?.release()

        winSound = null
        drawSound = null
        timerSound = null
    }
}

class MainActivity : AppCompatActivity() {
    private lateinit var gameBoard: Array<CharArray>
    private var turn = 'X'
    private lateinit var tableLayout: TableLayout
    private lateinit var turnTextView: TextView
    private lateinit var resetButton: Button
    private var playerXScore = 0
    private var playerOScore = 0
    private lateinit var playerXScoreTextView: TextView
    private lateinit var playerOScoreTextView: TextView
    private lateinit var resetScoreButton: Button
    private lateinit var timerTextView: TextView
    private var timer: CountDownTimer? = null
    private val TURN_TIME_MILLIS = 15000L // 15 seconds per turn

    // Sound related properties
    private lateinit var soundManager: SoundManager
    private var isLowTimeWarningPlayed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize sound manager
        soundManager = SoundManager(this)

        // Initialize views
        turnTextView = findViewById(R.id.textView)
        tableLayout = findViewById(R.id.table_layout)
        resetButton = findViewById(R.id.rstbutton)
        playerXScoreTextView = findViewById(R.id.playerXScore)
        playerOScoreTextView = findViewById(R.id.playerOScore)
        resetScoreButton = findViewById(R.id.restartbtn)
        timerTextView = findViewById(R.id.timerTextView)

        // Handle system bar insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize game board
        gameBoard = Array(3) { CharArray(3) { ' ' } }

        // Set reset button click listener
        resetButton.setOnClickListener {
            startNewGame()
        }
        resetScoreButton.setOnClickListener {
            resetScores()
        }

        // Start the game
        startNewGame()
    }

    private fun startTimer() {
        timer?.cancel() // Cancel any existing timer
        isLowTimeWarningPlayed = false // Reset warning flag

        timer = object : CountDownTimer(TURN_TIME_MILLIS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                timerTextView.text = "Time left: ${secondsLeft}s"

                // Play timer warning sound when 3 seconds remain
                if (secondsLeft <= 5 && !isLowTimeWarningPlayed) {
                    soundManager.playTimerSound()
                    isLowTimeWarningPlayed = true
                }
            }

            override fun onFinish() {
                // Time's up! Switch turns
                timerTextView.text = "Time's up!"
                if (!checkGameStatus()) {
                    turn = if (turn == 'X') 'O' else 'X'
                    turnTextView.text = String.format(resources.getString(R.string.turn), turn)
                    startTimer() // Start timer for next player
                }
            }
        }.start()
    }

    private fun resetScores() {
        timer?.cancel()
        playerXScore = 0
        playerOScore = 0
        playerXScoreTextView.text = "Player X: $playerXScore"
        playerOScoreTextView.text = "Player O: $playerOScore"
        startNewGame()
    }

    private fun startNewGame() {
        timer?.cancel()
        turn = 'X'
        turnTextView.text = String.format(resources.getString(R.string.turn), turn)

        // Reset game board
        for (i in gameBoard.indices) {
            for (j in gameBoard[i].indices) {
                gameBoard[i][j] = ' '
                val cell = (tableLayout.getChildAt(i) as TableRow).getChildAt(j) as TextView
                cell.text = ""
                cell.setOnClickListener {
                    cellClickListener(i, j)
                }
            }
        }

        // Start timer for first turn
        startTimer()
    }

    private fun cellClickListener(row: Int, column: Int) {
        // Check if the cell is empty
        if (gameBoard[row][column] == ' ') {
            // Update the game board and the cell
            gameBoard[row][column] = turn
            val cell = (tableLayout.getChildAt(row) as TableRow).getChildAt(column) as TextView
            cell.text = turn.toString()

            // Check the game status (win or draw)
            if (checkGameStatus()) {
                timer?.cancel()
                return
            }

            // Switch turns between 'X' and 'O'
            turn = if (turn == 'X') 'O' else 'X'
            turnTextView.text = String.format(resources.getString(R.string.turn), turn)

            // Reset timer for next turn
            startTimer()
        }
    }

    private fun checkGameStatus(): Boolean {
        val winner = when {
            isWinner('X') -> 'X'
            isWinner('O') -> 'O'
            else -> null
        }

        return when {
            winner != null -> {
                updateScore(winner)
                soundManager.playWinSound() // Play win sound
                showGameResult(String.format(resources.getString(R.string.winner), winner))
                true
            }
            isBoardFull() -> {
                soundManager.playDrawSound() // Play draw sound
                showGameResult(resources.getString(R.string.draw))
                true
            }
            else -> false
        }
    }

    private fun isWinner(player: Char): Boolean {
        // Check rows, columns, and diagonals
        return (0..2).any { i ->
            (gameBoard[i][0] == player && gameBoard[i][1] == player && gameBoard[i][2] == player) ||
                    (gameBoard[0][i] == player && gameBoard[1][i] == player && gameBoard[2][i] == player)
        } || (gameBoard[0][0] == player && gameBoard[1][1] == player && gameBoard[2][2] == player) ||
                (gameBoard[0][2] == player && gameBoard[1][1] == player && gameBoard[2][0] == player)
    }

    private fun isBoardFull(): Boolean {
        return gameBoard.all { row -> row.all { it != ' ' } }
    }

    private fun updateScore(winner: Char) {
        when (winner) {
            'X' -> playerXScoreTextView.text = "Player X: ${++playerXScore}"
            'O' -> playerOScoreTextView.text = "Player O: ${++playerOScore}"
        }
    }

    private fun showGameResult(message: String) {
        turnTextView.text = message

        // Show a dialog with the result and reset the game when dismissed
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                startNewGame()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel() // Clean up timer when activity is destroyed
        soundManager.release() // Clean up sound resources
    }
}