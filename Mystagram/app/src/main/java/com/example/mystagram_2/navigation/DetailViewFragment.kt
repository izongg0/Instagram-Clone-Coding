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

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        var contentUidList : ArrayList<String> = arrayListOf()



        init{

            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()

                if(querySnapshot == null) return@addSnapshotListener
                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
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


            //좋아요 이베트

            var favoriteimg = viewholder.findViewById<ImageView>(R.id.detailviewitem_favorite_imageview)
            favoriteimg.setOnClickListener {
                favoriteEvent(position)
            }
            if(contentDTOs!![position].favorites.containsKey(uid)){
                favoriteimg.setImageResource(R.drawable.ic_favorite)
            }else{
                favoriteimg.setImageResource(R.drawable.ic_favorite_border)

            }
            viewholder.findViewById<ImageView>(R.id.detailviewitem_profile_img).setOnClickListener {

                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid",contentDTOs[position].uid)
                bundle.putString("userid",contentDTOs[position].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()


            }
            viewholder.findViewById<ImageView>(R.id.detailviewitem_comment_imageview).setOnClickListener {v->
                var intent = Intent(v.context,CommentActivity::class.java)
                intent.putExtra("contentUid",contentUidList[position])
                intent.putExtra("destinationUid",contentDTOs[position].uid)
                startActivity(intent)
            }
        }
        fun favoriteEvent(position : Int){
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction{ transaction ->

                var uid = FirebaseAuth.getInstance().currentUser?.uid
                var contentDTO= transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                if(contentDTO!!.favorites.containsKey(uid)){
                    //이미 좋아요 누른것
                    contentDTO?.favoriteCount = contentDTO.favoriteCount - 1
                    contentDTO?.favorites?.remove(uid)
                }else{
                    //이제 처음 누름
                    contentDTO?.favoriteCount = contentDTO.favoriteCount + 1
                    contentDTO?.favorites!![uid!!] = true
                    favoriteAlarm(contentDTOs[position].uid!!)
                }
                transaction.set(tsDoc,contentDTO)
            }
        }
        fun favoriteAlarm(destinationUid : String){

            var alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
            alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        }

    }


}