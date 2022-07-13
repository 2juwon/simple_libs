package devdan.simple

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import devdan.simple.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.buttonUsbSample.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.buttonCameraCaptureSample.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        binding.buttonTest.setOnClickListener {
            startActivity(Intent(this, TestActivity::class.java))
        }
    }
}