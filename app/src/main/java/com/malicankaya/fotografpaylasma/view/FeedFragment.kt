package com.malicankaya.fotografpaylasma.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.malicankaya.fotografpaylasma.R
import com.malicankaya.fotografpaylasma.adapter.PostAdapter
import com.malicankaya.fotografpaylasma.databinding.FragmentFeedBinding
import com.malicankaya.fotografpaylasma.model.Post

class FeedFragment : Fragment(),PopupMenu.OnMenuItemClickListener {

    private var _binding : FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var popupMenu : PopupMenu

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var postList: ArrayList<Post> = arrayListOf()
    private var adapter: PostAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        db = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFeedBinding.inflate(inflater,container,false)
        val view = binding.root
        return view
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.floatingActionButton.setOnClickListener { menuTiklandi(it) }

        popupMenu = PopupMenu(requireContext(),binding.floatingActionButton)
        var inflater = popupMenu.menuInflater
        inflater.inflate(R.menu.my_popup_menu,popupMenu.menu)
        popupMenu.setOnMenuItemClickListener(this)

        getAllPosts()

        adapter = PostAdapter(postList)
        binding.feedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.feedRecyclerView.adapter = adapter
    }

    private fun getAllPosts(){
        db.collection("Posts")
            .orderBy("date",Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null){
                    Toast.makeText(requireContext(),error.localizedMessage,Toast.LENGTH_LONG).show()
                }else{
                    if (value != null && !value.isEmpty){
                        postList.clear()
                        val posts = value.documents
                        posts.forEach {document ->
                            val comment = document.get("comment") as String
                            val email = auth.currentUser!!.email!!
                            val imageUrl = document.get("imageUrl") as String

                            postList.add(Post(email,comment, imageUrl))
                        }
                        //bu kod bloğu olmadan veriler gelmiyor
                        adapter?.notifyDataSetChanged()

                    }
                }
            }
    }

    private fun menuTiklandi(view : View){
        popupMenu.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.postItem -> {
                val action = FeedFragmentDirections.actionFeedFragmentToYuklemeFragment()
                Navigation.findNavController(requireView()).navigate(action)
            }

            R.id.cikisItem -> {
                //cikis islemi
                auth.signOut()
                val action = FeedFragmentDirections.actionFeedFragmentToKullaniciFragment()
                Navigation.findNavController(requireView()).navigate(action)
            }
        }

        return true
    }

}