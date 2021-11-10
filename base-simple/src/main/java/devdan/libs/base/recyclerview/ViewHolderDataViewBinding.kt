package devdan.libs.base.recyclerview

import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

abstract class ViewHolderDataViewBinding<ITEM>(
    private val binding: ViewDataBinding,
) : RecyclerView.ViewHolder(binding.root) {
    protected abstract fun setVariableId(): Int

    fun bind(item: ITEM) {
        try {
            binding.setVariable(setVariableId(), item)
            binding.executePendingBindings()
        } catch (e: Exception) {
            e.printStackTrace()
            binding.unbind()
        }

        onBind(item)
    }

    protected abstract fun onBind(item: ITEM)
}