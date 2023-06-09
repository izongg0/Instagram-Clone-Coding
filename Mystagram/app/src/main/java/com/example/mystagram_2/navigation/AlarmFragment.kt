package com.example.mystagram_2.navigation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Glide.init
import com.bumptech.glide.request.RequestOptions
import com.example.mystagram_2.R
import com.example.mystagram_2.databinding.ActivityMainBinding
import com.example.mystagram_2.databinding.FragmentAlarmBinding
import com.example.mystagram_2.navigation.model.AlarmDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class AlarmFragment : Fragment() {
    val binding by lazy { FragmentAlarmBinding.inflate(layoutInflater) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
//        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_alarm, container, false)

        var alarmrv = binding.alarmrv
        alarmrv.adapter = AlarmRecyclerAdapter()
        alarmrv.layoutManager = LinearLayoutManager(activity)


        return binding.root
    }


    inner class AlarmRecyclerAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var alarmDTOList : ArrayList<AlarmDTO> = arrayListOf()


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment,parent,false)

            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view : View) : RecyclerView.ViewHolder(view)

        init {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            // 현재 로그인한 계정을 타겟으로 한 모든 알림들을 가져옴
            FirebaseFirestore.getInstance().collection("alarms").whereEqualTo("destinationUid",uid).addSnapshotListener { querySnapshot, error ->
                alarmDTOList.clear()
                if(querySnapshot == null) return@addSnapshotListener
                for(snapshot in querySnapshot.documents){
                    alarmDTOList.add(snapshot.toObject(AlarmDTO::class.java)!!)

                }
                notifyDataSetChanged()
            }

        }

        override fun getItemCount(): Int {
            return alarmDTOList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var view = holder.itemView

            FirebaseFirestore.getInstance().collection("profileImages").document(alarmDTOList[position].uid!!).get().addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val url = task.result!!["images"]
                    Glide.with(view.context).load(url).apply(RequestOptions().circleCrop()).into(view.findViewById(R.id.commentviewitem_imageview_profile))
                }
            }

            var profiletxt = view.findViewById<TextView>(R.id.commentviewitem_textview_profile)

            when(alarmDTOList[position].kind){

                0 ->{ // 좋아요 알림
                    val str0 = alarmDTOList[position].userId + getString(R.string.alarm_favorite)
                    profiletxt.text = str0
                }
                1 ->{ // 댓글 알림
                    val str0 = alarmDTOList[position].userId + " " + getString(R.string.alarm_comment) + " of "+ alarmDTOList[position].message
                    profiletxt.text = str0
                }
                2 ->{ // 팔로우 알림
                    val str0 = alarmDTOList[position].userId +" " + getString(R.string.alarm_follow)
                    profiletxt.text = str0
                }
            }
            view.findViewById<TextView>(R.id.commentviewitem_textview_comment).visibility = View.INVISIBLE
        }

    }
}