package devdan.simple

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import devdan.libs.base.utils.Logger
import java.util.*
import kotlin.math.roundToInt

class TestActivity : Activity() {
    private var totalShootCount = 0
    private var shootCount = 0
    private var startTimeMillis: Long = System.currentTimeMillis();

    val rpsCheckSecond = 5
    val rpsCheckMillis = rpsCheckSecond * 1000.0;

    @SuppressLint("SetTextI18n")
    private fun calculateRPS() {
        val currentTimeMillis = System.currentTimeMillis()
        val diffMillis = currentTimeMillis - startTimeMillis
        timeTextView.text = "${rpsCheckMillis}ms"
        if (diffMillis >= rpsCheckMillis) {
            val rps: Int =
                (shootCount / ((diffMillis / rpsCheckMillis) * rpsCheckSecond)).roundToInt()
            Logger.e("ljw", "rps : $shootCount / ${diffMillis}")
            totalShootCount += shootCount
            rpsTextView.text = "rps : $rps"
            shootCount = 0
            startTimeMillis = currentTimeMillis
        }
        shootCount++
    }

    private val timer = Timer("shoot_time", true)
    private lateinit var timeTextView: TextView;
    private lateinit var shotCountTextView: TextView;
    private lateinit var rpsTextView: TextView;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        findViewById<Button>(R.id.btn_shoot).setOnClickListener {
            calculateRPS()
            shotCountTextView.text = "totalshot: ${totalShootCount}, shoot : $shootCount"
        }

        timeTextView = findViewById(R.id.text_tick)
        shotCountTextView = findViewById(R.id.text_shoot)
        rpsTextView = findViewById(R.id.text_rps)

//        timer.schedule(
//            object : TimerTask() {
//                override fun run() {
//                    timeTextView.post {
//                        timeTextView.text = "end"
//                    }
//                }
//
//            }, 0L, 1000L
//        )
    }

    override fun onPause() {
        super.onPause()
        timer.cancel()
    }
}