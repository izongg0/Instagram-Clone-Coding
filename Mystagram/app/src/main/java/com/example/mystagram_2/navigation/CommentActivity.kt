package com.example.mystagram_2.navigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mystagram_2.R
import com.example.mystagram_2.navigation.model.AlarmDTO
import com.example.mystagram_2.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class CommentActivity : AppCompatActivity() {
    var contentUid : String? = null
    var destinationUid : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        contentUid = intent.getStringExtra("contentUid")
        destinationUid = intent.getStringExtra("destinationUid")

        var commentrv = findViewById<RecyclerView>(R.id.commentrv)
        commentrv.adapter = CommentRecyclerViewAdapter()
        commentrv.layoutManager = LinearLayoutManager(this)
        findViewById<Button>(R.id.comment_btn_send)?.setOnClickListener {

            var comment = ContentDTO.Comment()
            comment.userId = FirebaseAuth.getInstance().currentUser?.email
            comment.uid = FirebaseAuth.getInstance().currentUser?.uid
            comment.comment  = findViewById<EditText>(R.id.comment_edit_message).text.toString()
            comment.timestamp = System.currentTimeMillis()

            FirebaseFirestore.getInstance().collection("images").document(contentUid!!).collection("comments").document().set(comment)
            commentAlarm(destinationUid!!,findViewById<EditText>(R.id.comment_edit_message).text.toString())
            findViewById<EditText>(R.id.comment_edit_message).setText("")
        }



    }

    fun commentAlarm(destinationUid : String,message : String){

        var alarmDTO = AlarmDTO()

        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.kind = 1
        alarmDTO.timestamp = System.currentTimeMillis()
        alarmDTO.message = message
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

    }

    inner class CommentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var comments : ArrayList<ContentDTO.Comment> = arrayListOf()
        init{
            FirebaseFirestore.getInstance().collection("images").document(contentUid!!).collection("comments")
                .orderBy("timestamp").addSnapshotListener { querySnapshot, error ->
                    comments.clear()
                    if(querySnapshot == null) return@addSnapshotListener

                    for(snapshot in querySnapshot.documents!!){
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)

                    }
                    notifyDataSetChanged()

                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment,parent,false)
            return CustomViewHolder(view)
        }

        private inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {

            return comments.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view = holder.itemView
            view.findViewById<TextView>(R.id.commentviewitem_textview_comment).text = comments[position].comment
            view.findViewById<TextView>(R.id.commentviewitem_textview_profile).text = comments[position].userId

            FirebaseFirestore.getInstance()
                .collection("profileImages")
                .document(comments[position].uid!!).get()
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        var url = task.result!!["images"]
                        Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop()).into(view.findViewById<ImageView>(R.id.commentviewitem_imageview_profile))
                    }
                }
        }


    }
}