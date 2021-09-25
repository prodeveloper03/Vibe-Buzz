package com.sounak.vibebuzz.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.sounak.vibebuzz.R
import com.sounak.vibebuzz.data.entities.Song
import kotlinx.android.synthetic.main.list_item.view.*
import kotlinx.android.synthetic.main.swipe_item.view.tvPrimary
import javax.inject.Inject

class SongAdapter @Inject constructor(
    private  val glide: RequestManager
) : RecyclerView.Adapter<SongAdapter.SongViewHolder> (){

    class SongViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView)

    private val diffCallback = object : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return  oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }


    private  val differ = AsyncListDiffer(this,diffCallback)

    var song : List<Song>
        get() = differ.currentList
    set(value) = differ.submitList(value)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.list_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song  = song[position]
        holder.itemView.apply {
            tvPrimary.text = song.name
            tvSecondary.text = song.artistName
            glide.load(song.image).into(ivItemImage)

            setOnClickListener {
                onItemClickListner?.let {click ->
                    click(song)

                }
            }
        }
    }
    private var onItemClickListner : ((Song)-> Unit) ?= null

    fun setOnItemListener(listener: (Song)->Unit){
        onItemClickListner = listener
    }



    override fun getItemCount(): Int {
        return song.size
    }
}