package devdan.libs.base.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import devdan.libs.base.extensions.hideKeyboard
import devdan.libs.base.viewmodel.BaseViewModel

abstract class BaseFragment<VB : ViewDataBinding> : Fragment() {
    protected lateinit var binding: VB
    protected lateinit var currentView: View

    @LayoutRes
    protected abstract fun getLayoutId(): Int

    protected abstract fun getVariables(): Map<Int, ViewModel>

    protected abstract fun initObserver(view: View)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, getLayoutId(), container, false
        )
        currentView = binding.root
        return currentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getVariables().entries.forEach {
            binding.setVariable(it.key, it.value)
            if (it.value is BaseViewModel) {
                (it.value as BaseViewModel).let { viewModel ->
                    viewModel.hideKeyboardEvent.observe(viewLifecycleOwner, {
                        hideKeyboard()
                    })
                }
            }
        }

        binding.lifecycleOwner = viewLifecycleOwner
        binding.executePendingBindings()

        initObserver(view)
    }

    override fun onDestroyView() {
        if (view != null) {
            val parentViewGroup = view?.parent as ViewGroup?
            parentViewGroup?.removeAllViews()
        }

        super.onDestroyView()

        if (::binding.isInitialized) binding.unbind()
    }

    protected fun navigate(@IdRes resId: Int) {
        findNavController().navigate(resId)
    }

    protected fun navigate(navDirections: NavDirections) {
        findNavController().navigate(navDirections)
    }
}