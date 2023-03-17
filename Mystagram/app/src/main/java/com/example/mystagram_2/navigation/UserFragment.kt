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

        // 메인페이지에서 전달받은 사진의 주인 계정의 정보를 받음
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // 여기엔 현재 로그인한 계정의 주인에 대한 정보가 담겨있음.
        currentUserUid = auth?.currentUser?.uid

        // 사진 주인의 프로필을 눌렀을 때 그 사진 주인이 현재 로그인한 계정일 경우.
        if(uid == currentUserUid){
            //mypage
            var sighoutbtn = fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)
            // 팔로우 하기 버튼이 아니라 로그아웃을 하는 버튼이 있음
            sighoutbtn?.text = getString(R.string.signout)
            sighoutbtn?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity,LoginActivity::class.java))
                auth?.signOut()
            }
        }else{
            // 사진 주인의 프로필을 눌렀을 때 그 사진 주인이 현재 로그인한 계정이 아니라 다른사람계정일 경우

            var sighoutbtn = fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)
            sighoutbtn?.text = getString(R.string.follow)
            var mainactivity = ( activity as MainActivity)
            // 툴바에 해당 사진의 주인의 아이디가 상단에 출력됨.
            mainactivity?.findViewById<TextView>(R.id.toolbar_username)?.text = arguments?.getString("userid")

            // 툴바의 백버튼을 누르면 다시 메인페이지로 이동함.
            mainactivity?.findViewById<ImageView>(R.id.toolbar_btn_back)?.setOnClickListener {
                mainactivity.findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId = R.id.action_home
            }
            mainactivity.findViewById<ImageView>(R.id.toolbar_title_image)?.visibility = View.GONE
            mainactivity.findViewById<TextView>(R.id.toolbar_username)?.visibility = View.VISIBLE
            mainactivity.findViewById<ImageView>(R.id.toolbar_btn_back)?.visibility = View.VISIBLE

            // follow버튼을 누르면 팔로우를 하는 함수 실행.
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
        // 팔로우 팔로워가 바뀌었을 때 유저페이지에서 보이는 팔로우 수, 팔로워 수 변경 이벤트

        var followbtn = fragmentView?.findViewById<Button>(R.id.account_btn_follow_signout)

        // 현제 유저페이지의 주인에 대한 모든 값을 가져옴
        registration = firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->

            if(documentSnapshot == null) return@addSnapshotListener

            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)

            // 현재 유저페이지 주인의 팔로잉과 팔로워 수가 널값이 아니라면 그 주인 디비에 있는 팔로워 수와 팔로잉 수를 출력함
            if(followDTO?.followingCount != null){
                fragmentView?.findViewById<TextView>(R.id.account_tv_following_count)?.text = followDTO?.followingCount?.toString()
            }
            if(followDTO?.followerCount != null){
                fragmentView?.findViewById<TextView>(R.id.account_tv_follower_count)?.text = followDTO?.followerCount?.toString()

                // 현재 로그인한 계정이 이미 팔로우를 하고 있는지 없는지에 따라 팔로우 버튼인지 팔로우 취소버튼인지 설정
                // 팔로워 카운트가 null인 경우에는 해당이 안되므로 여기 if문 안에 있는거 같음
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
        //디비의 유저 테이블의 현재 로그인한 유저의 아이디를 가져옴
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)

        //팔로우를 눌렀을 때 발생하는 상황을 현재 로그인한 계정과 사진의 주인 계정으로 나누어서 작성.

        // 현재 로그인한 계정.
        firestore?.runTransaction { transaction ->
            //현재 프로필 주인에 대한 정보를 followDTO변수에 FollowDTO모델 형식으로 넣음.
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)


            // 현재 로그인한 계정이 계정을 생성하고 처음 팔로우를 눌렀을 경우
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followings[uid!!] = true

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }
            // 이미 해당 사진 주인 계정의 팔로우 하고 있는경우
            if (followDTO.followings.containsKey(uid)) {
                followDTO?.followingCount = followDTO.followingCount - 1
                followDTO?.followings?.remove(uid)

            } else {
            // 팔로우를 하기위해 누른경우
                followDTO?.followingCount = followDTO.followingCount + 1
                followDTO.followings[uid!!] = true
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }

        //해당 유저페이지의 주인 입장
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            //계정을 생성한 이후 처음 팔로우를 당했을 때
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true

                // 해당 유저페이지의 주인에게 알림을 줌.
                followerAlarm(uid!!)


                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            // 원래 팔로워에 현재 로그인 한 계정이 존재하지 않았을 경우
            if (followDTO!!.followers.containsKey(currentUserUid)) {
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            } else {
                // 팔로우 취소 당했을 때

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
        alarmDTO.destinationUid = desinationUid // 어느 계정에 알림을 줄지
        alarmDTO.userId = auth?.currentUser?.email // 현재 로그인한 계정이 팔로우를 누르므로 유저페이지 주인에게
        // 자기 계정을 누가 팔로우를 눌렀는지 알려주기 위해 현재 로그인한 계정의 정보를 알려줌
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis() //언제 팔로우를 눌렀는지
        // alarm디비에 누가 누구계정의 팔로우를 눌렀는지 저장함.
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


    // 해당 프로필 주인이 게시한 사진 목록
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        // 사진 리스트를 담을 변수
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        init{
            registration = firestore?.collection("images")?.whereEqualTo("uid",uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

                if(querySnapshot == null) return@addSnapshotListener

                // 포문으로 해당 유저가 가진 사진들을 변수에 다 담음
                for(snapshot in querySnapshot.documents){
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)

                }
                // 해당 유저가 가진 사진(게시글)의 수를 게시글 수에 표시함
                fragmentView?.findViewById<TextView>(R.id.account_tv_post_count)?.text = contentDTOs.size.toString()
                notifyDataSetChanged()

            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            // 사진의 크기를 작게하여 배치
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

    //프라그먼트의 생명주기에 맞춰서 스냅샷을 종료해주어야함 .
    override fun onStop() {
        super.onStop()


        registration?.remove()
    }
}