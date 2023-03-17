package com.example.mystagram_2.navigation

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mystagram_2.R
import com.example.mystagram_2.navigation.model.AlarmDTO
import com.example.mystagram_2.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase


class DetailViewFragment : Fragment() {

var firestore : FirebaseFirestore? = null
var uid :  String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail_view, container, false)


        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid
        var detailrv = view.findViewById<RecyclerView>(R.id.detailrv)

        detailrv.adapter = DetailViewRecyclerViewAdapter()
        detailrv.layoutManager = LinearLayoutManager(activity)

        return view
    }

    // 인스타 메인페이지에 게시글들이 보이게 하는 리스트어댑터
    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        var contentUidList : ArrayList<String> = arrayListOf()


        init{

            // 스토어 안에 있는 모든 이미지들을 시간 순으로 정렬하여 싹 다 가져옴.
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()

                if(querySnapshot == null) return@addSnapshotListener
                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)
                    // 사진에 대한 계정 정보와 사진 정보를 변수에 담음
                    contentDTOs.add(item!!)
                    // 사진마다 각자 자동으로 생성된 아이디를 uid 리스트 변수에 추가
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail,parent,false)

            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        }

        override fun getItemCount(): Int {

            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            var viewholder  = (holder as CustomViewHolder).itemView

            viewholder.findViewById<TextView>(R.id.detailviewitem_profile_textview).text = contentDTOs!![position].userId
            Glide.with(holder.itemView).load(contentDTOs!![position].imageUrl).into(viewholder.findViewById(R.id.detailviewitem_imageview_content))
            viewholder.findViewById<TextView>(R.id.detailviewitem_explain_textview).text = contentDTOs!![position].explain
            viewholder.findViewById<TextView>(R.id.detailviewitem_favoritecounter_textview).text = "Likes "+ contentDTOs!![position].favoriteCount
            Glide.with(holder.itemView).load(contentDTOs!![position].imageUrl).into(viewholder.findViewById(R.id.detailviewitem_profile_img))


            //좋아요 이벤트

            var favoriteimg = viewholder.findViewById<ImageView>(R.id.detailviewitem_favorite_imageview)

            // 좋아요 이미지를 클릭했을 때
            favoriteimg.setOnClickListener {
                favoriteEvent(position)
            }

            // 좋아요를 눌렀을 때 하트 사진이 바뀌도록 하는 기능

            //해당 게시글의 좋아요 목록에 현재 로그인한 계정의 아이디가 있으면 색칠된 하트이미지
            if(contentDTOs!![position].favorites.containsKey(uid)){
                favoriteimg.setImageResource(R.drawable.ic_favorite)
            }else{ // 아니면 빈 하트
                favoriteimg.setImageResource(R.drawable.ic_favorite_border)

            }


            // 인스타 메인에서 사진을 올린 계정의 프로필 이미지를 누르면
            // 그 해당 사진의 주인의 계정 정보를 넘겨주면서 유저페이지로 이동함.
            viewholder.findViewById<ImageView>(R.id.detailviewitem_profile_img).setOnClickListener {

                var fragment = UserFragment()
                var bundle = Bundle() // 사진 주인의 정보를 넘겨줄

                // 번들에 사진 주인 계정의 정보를 넣음
                bundle.putString("destinationUid",contentDTOs[position].uid)
                bundle.putString("userid",contentDTOs[position].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()


            }
            // 해당 사진의 댓글 이미지를 클릭하면 해당 게시글의 주인에 대한 정보와
            // 현재 로그인한 계정의 정보를 전달하면서
            // 댓글을 작성할 수 있는 페이지로 이동함
            viewholder.findViewById<ImageView>(R.id.detailviewitem_comment_imageview).setOnClickListener {v->
                var intent = Intent(v.context,CommentActivity::class.java)
                intent.putExtra("contentUid",contentUidList[position])
                intent.putExtra("destinationUid",contentDTOs[position].uid)
                startActivity(intent)
            }
        }
        fun favoriteEvent(position : Int){

            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])

            // Transaction -> 디비에 들어있는 내용에 대한 delete, get, set, update 읽고 쓰는 작업

            firestore?.runTransaction{ transaction ->

                var uid = FirebaseAuth.getInstance().currentUser?.uid

                // 파이어스토어의 해당 images콜렉션 안에 있는 이미지 정보를 ContentDTO모델 형식으로 가져온다.

                var contentDTO= transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                // 사진 좋아요 목록에 이미 좋아요를 누른 사람의 계정이 들어있으면 
                // 좋아요 취소를 하는 것이 목적이므로 해당 사진의 좋아요 목록에서 자신을 삭제하고
                // 좋아요 수도 하나 줄인다.
                if(contentDTO!!.favorites.containsKey(uid)){
                    //이미 좋아요 누른것
                    contentDTO?.favoriteCount = contentDTO.favoriteCount - 1
                    contentDTO?.favorites?.remove(uid)
                }else{
                    //이제 처음 누름
                    // 처음 누르는 것이므로 좋아요 목록에 자신을 추가하고 해당 사진의 좋아요 수를 늘린다.
                    contentDTO?.favoriteCount = contentDTO.favoriteCount + 1
                    contentDTO?.favorites!![uid!!] = true
                    favoriteAlarm(contentDTOs[position].uid!!)
                }
                // 디비에 원래 있던 데이터를 변경된 데이터로 수정하여 집어넣는다.
                transaction.set(tsDoc,contentDTO)
            }
        }
        //좋아요를 눌렀을 때 좋아요를 당한사람에게 알람이 가게하는 이벤트
        fun favoriteAlarm(destinationUid : String){

            var alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid // 좋아요를 당한 유저
            alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email // 좋아요를 누른 현재 로그인한 유저
            alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()

            // 누가 누구의 게시글에 좋아요를 눌렀는지 알려줌.
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        }

    }


}