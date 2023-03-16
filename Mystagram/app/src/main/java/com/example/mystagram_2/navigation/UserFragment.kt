package com.example.mystagram_2.navigation

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import com.example.mystagram_2.LoginActivity
import com.example.mystagram_2.MainActivity
import com.example.mystagram_2.R
import com.example.mystagram_2.navigation.model.AlarmDTO
import com.example.mystagram_2.navigation.model.ContentDTO
import com.example.mystagram_2.navigation.model.FollowDTO
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.snapshots
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.NonDisposableHandle.parent
import org.w3c.dom.Text


class UserFragment : Fragment() {

    var fragmentView : View? = null
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var auth : FirebaseAuth? = null
    var currentUserUid : String? = null
    var registration: ListenerRegistration? = null

    companion object{
        var PICK_PROFILE_FROM_ALBUM = 10
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)

        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        if(uid == currentUserUid){
            //mypage
            var sighoutbtn = fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)
            sighoutbtn?.text = getString(R.string.signout)
            sighoutbtn?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity,LoginActivity::class.java))
                auth?.signOut()
            }
        }else{
            var sighoutbtn = fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)
            sighoutbtn?.text = getString(R.string.follow)
            var mainactivity = ( activity as MainActivity)
            mainactivity?.findViewById<TextView>(R.id.toolbar_username)?.text = arguments?.getString("userid")

            mainactivity?.findViewById<ImageView>(R.id.toolbar_btn_back)?.setOnClickListener {
                mainactivity.findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId = R.id.action_home
            }
            mainactivity.findViewById<ImageView>(R.id.toolbar_title_image)?.visibility = View.GONE
            mainactivity.findViewById<TextView>(R.id.toolbar_username)?.visibility = View.VISIBLE
            mainactivity.findViewById<ImageView>(R.id.toolbar_btn_back)?.visibility = View.VISIBLE
            sighoutbtn?.setOnClickListener {
                requestFollow()

            }


            //otheruserpage
        }
        var rv = fragmentView?.findViewById<RecyclerView>(R.id.account_rv)
        rv?.adapter = UserFragmentRecyclerViewAdapter()
        rv?.layoutManager = GridLayoutManager(activity,3)


        fragmentView?.findViewById<ImageView>(R.id.account_iv_profile)?.setOnClickListener {

            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent,PICK_PROFILE_FROM_ALBUM)
        }

//        getProfileImage()
        getFollowerAndFollowing()
        return fragmentView
    }

    fun getFollowerAndFollowing(){

        var followbtn = fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)
        registration = firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->

            if(documentSnapshot == null) return@addSnapshotListener

            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            if(followDTO?.followingCount != null){
                fragmentView?.findViewById<TextView>(R.id.account_tv_following_count)?.text = followDTO?.followingCount?.toString()
            }
            if(followDTO?.followerCount != null){
                fragmentView?.findViewById<TextView>(R.id.account_tv_follower_count)?.text = followDTO?.followerCount?.toString()

                if(followDTO?.followers!!.containsKey(currentUserUid!!)){
                    //이미 팔로우 하고 있는 경우
                    followbtn?.text = getString(R.string.follow_cancel)
                    followbtn?.background?.setColorFilter(ContextCompat.getColor(activity as MainActivity,R.color.white),PorterDuff.Mode.MULTIPLY)
                }else{
                    if(uid != currentUserUid){
                        followbtn?.text = getString(R.string.follow)

                        followbtn?.background?.setColorFilter(ContextCompat.getColor(activity as MainActivity,R.color.purple_200),PorterDuff.Mode.MULTIPLY)

                    }

                }

            }
        }
    }

    fun requestFollow() {

        //Save data to my account 내 계정이 누구를 팔로우 하는가
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[uid!!] = true

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }
            if (followDTO.followings.containsKey(uid)) {
                followDTO?.followingCount = followDTO.followingCount - 1
                followDTO?.followings?.remove(uid)

            } else {

                followDTO?.followingCount = followDTO.followingCount + 1
                followDTO.followings[uid!!] = true
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }

        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)


                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            if (followDTO!!.followers.containsKey(currentUserUid)) {
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            } else {
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
            }
            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }
    fun followerAlarm(desinationUid : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = desinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

    }

    fun getProfileImage(){
        registration = firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            if(documentSnapshot.data != null){

                var url = documentSnapshot?.data!!["image"]
                Glide.with(this).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.findViewById(R.id.account_iv_profile!!))
            } // Glide Activity 작동 안됨. 왜지 ... activity!!
        }
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        init{
            registration = firestore?.collection("images")?.whereEqualTo("uid",uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)

                }
                fragmentView?.findViewById<TextView>(R.id.account_tv_post_count)?.text = contentDTOs.size.toString()
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
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageview)
        }


    }

    override fun onStop() {
        super.onStop()

        registration?.remove()
    }
}