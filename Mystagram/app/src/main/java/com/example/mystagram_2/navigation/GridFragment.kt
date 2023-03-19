package com.example.mystagram_2.navigation

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mystagram_2.R
import com.example.mystagram_2.databinding.ActivityMainBinding
import com.example.mystagram_2.databinding.FragmentGridBinding
import com.example.mystagram_2.navigation.model.ContentDTO
import com.example.mystagram_2.navigation.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration


class GridFragment : Fragment() {
    val binding by lazy { FragmentGridBinding.inflate(layoutInflater) }

    var registration: ListenerRegistration? = null

    var testarray : Array<String>? = arrayOf()
    var fragmentView: View? = null

    var firestore : FirebaseFirestore? = null

    var auth : FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        auth = FirebaseAuth.getInstance()

        var gridrv = binding.gridrv
        gridrv?.adapter = UserFragmentRecyclerViewAdapter()
        gridrv?.layoutManager = GridLayoutManager(activity,3)


        firestore = FirebaseFirestore.getInstance()

        var tsDocFollower = firestore?.collection("users")?.document(auth?.currentUser!!.uid)

//        tsDocFollower?.get()?.addOnSuccessListener { documentSnapshot ->
//            if (documentSnapshot.exists()) {
//                val name = documentSnapshot.get("followings")
//                                Log.d("eeeeee",name!!.javaClass.name)
//
//            } else
//            {
//            }
//        }?.addOnFailureListener { e ->
//            }



//
//        firestore?.runTransaction{ transaction ->
//
//            var test = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
//            binding.testtx.text = test!!.followings.toString()
//            Log.d("dddddd",test!!.followings.javaClass.name)
//
//            if( test.followings.containsKey("w3iGGIKVsxPkTVSjQiS2XlrAh9l1")){
//                Log.d("eeeeee","있다 !")
//
//            }
//        }



        return binding.root
    }
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        init{
            registration = firestore?.collection("images")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)

                }
                notifyDataSetChanged()

            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            var width = resources.displayMetrics.widthPixels/3
            var imageview = ImageView(parent.context)

            imageview.layoutParams = LinearLayoutCompat.LayoutParams(width,width)

            return CustomViewHolder(imageview)


        }
        inner class CustomViewHolder(var imageview : ImageView) : RecyclerView.ViewHolder(imageview){


        }


        override fun getItemCount(): Int {

            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            var imageview = (holder as CustomViewHolder).imageview
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(
                RequestOptions().centerCrop()).into(imageview)
        }


    }


}