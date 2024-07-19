package com.malicankaya.fotografpaylasma.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.recyclerview.widget.RecyclerView
import com.malicankaya.fotografpaylasma.databinding.RecyclerRowBinding
import com.malicankaya.fotografpaylasma.model.Post
import com.squareup.picasso.Picasso

class PostAdapter(val postList: ArrayList<Post>) : RecyclerView.Adapter<PostAdapter.PostVH>() {

    class PostVH(var binding: RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostVH {
        val binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return PostVH(binding)
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    override fun onBindViewHolder(holder: PostVH, position: Int) {
        holder.binding.recyclerEmailText.text = postList[position].email
        holder.binding.recyclerCommentText.text = postList[position].comment
        Picasso.get().load(postList[position].imageUrl).into(holder.binding.recyclerImageView)
    }


}