package devdan.libs.base.activity

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import devdan.libs.base.extensions.setStatusWhite

abstract class BaseActivity<VB : ViewDataBinding> : AppCompatActivity() {
    protected lateinit var binding: VB

    @LayoutRes
    protected abstract fun getLayoutId(): Int

    protected abstract fun getVariables(): Map<Int, ViewModel>

    protected abstract fun initObserver()

    protected var isSettingStatus = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        performDataBinding()

        if (isSettingStatus) setStatusWhite()

        initObserver()
    }

    private fun performDataBinding() {
        performViewDataBinding()
//        performAppBarDataBinding()
    }

    private fun performViewDataBinding() {
        binding = DataBindingUtil.setContentView(this, getLayoutId())
        getVariables().entries.forEach {
            binding.setVariable(it.key, it.value)
        }
        binding.lifecycleOwner = this
        binding.executePendingBindings()
    }

//    private fun performAppBarDataBinding() {
//        val appBarBinding: AppBarBinding
//        try {
//            val appBarField = binding.javaClass.getField("appbarLayout")
//            appBarBinding = appBarField.get(binding) as AppBarBinding
//            setSupportActionBar(appBarBinding.toolbar)
//        }
//        catch (e: Exception) {
//
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        if (::binding.isInitialized) binding.unbind()
    }

//    protected fun startActivity(activity: Class<*>) {
//        startActivity(Intent(this, activity))
//        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
//    }
//
//    protected fun startActivityWithFinish(activity: Class<*>) {
//        startActivityWithFinish(Intent(this, activity))
//        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
//    }
//
//    protected fun startActivityWithFinish(intent: Intent) {
//        startActivity(intent)
//        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
//        finish()
//    }
}