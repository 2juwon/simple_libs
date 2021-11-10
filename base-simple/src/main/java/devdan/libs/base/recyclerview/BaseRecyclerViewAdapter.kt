package devdan.libs.base.recyclerview

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

abstract class BaseRecyclerViewAdapter<T, VH : ViewHolderDataViewBinding<T>>(
    callback: DiffUtil.ItemCallback<T>,
    private val binding: ViewDataBinding

) : ListAdapter<T, VH>(callback) {

    init {
        this.setHasStableIds(true)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(currentList[position])
    }
}