package com.example.mystagram_2.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.example.mystagram_2.R
import com.example.mystagram_2.databinding.ActivityAddPhotoBinding
import com.example.mystagram_2.databinding.ActivityMainBinding
import com.example.mystagram_2.navigation.model.ContentDTO
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    val binding by lazy { ActivityAddPhotoBinding.inflate(layoutInflater) }

    var PICK_IMAGE_FROM_ALBUM = 0
    var storage : FirebaseStorage? = null
    var photoUri : Uri? = null //  사진uri를 저장하기 위한 변수
    var auth : FirebaseAuth? = null
    var firestore : FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //스토리지 초기화
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        //앨범열기
        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        // 바텀 네비게이션에서 addImage버튼 클릭하면 사진을 고르는 화면이 나온다.
        startActivityForResult(photoPickerIntent,PICK_IMAGE_FROM_ALBUM)

        //업로드이벤트
        binding.addphotoBtn.setOnClickListener {
            contentUpload()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_FROM_ALBUM){
            if(resultCode == Activity.RESULT_OK){
                // 사진을 선택했을때, 선택한 사진의 uri가 변수에 들어감.
                photoUri = data?.data

                // 선택한 사진이 사진올리는 페이지에 미리보기로 나옴.
                binding.addphotoImg.setImageURI(photoUri)

            }else{
                //취소버튼 눌렀을 때
                finish()
            }
        }
    }
    fun contentUpload(){

        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        // 사진 파일 이름이 중복되지 않도록 업로드하는 시간으로 지정.
        var imageFileName = "IMAGE_" + timestamp + "_.png"

        // storage에서 images폴더 내부에 imageFilename의 파일이름으로 저장되도록 하는 변수
        var storageRef = storage?.reference?.child("images")?.child(imageFileName)


        // 함수가 실행되는 순서를 참조하면 이 함수가 실행 될때는
        // 전역으로 선언한 photoUri변수 안에 선택한 이미지 주소가 들어있음.
        storageRef?.putFile(photoUri!!)?.continueWithTask{task : Task<UploadTask.TaskSnapshot>->
            return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener { uri ->
            var contentDTO = ContentDTO()

            // 사진을 올리는 계정 정보, 사진설명, 사진 주소, 현재 시간 등을 contentDTO변수에 담음.
            contentDTO.imageUrl = uri.toString()
            contentDTO.uid = auth?.currentUser?.uid
            contentDTO.userId = auth?.currentUser?.email
            contentDTO.explain = binding.addphotoEditExplain.text.toString()
            contentDTO.timestamp = System.currentTimeMillis()


            // 업로드한 이미지와 계정에 대한 정보를 디비에 저장함.
            firestore?.collection("images")?.document()?.set(contentDTO)

            setResult(Activity.RESULT_OK)

            // 업로드가 완료되면 창을 닫음.
            finish()

        }

//        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
//            storageRef.downloadUrl.addOnSuccessListener {  uri ->
//                var contentDTO = ContentDTO()
//
//                contentDTO.imageUrl = uri.toString()
//                contentDTO.uid = auth?.currentUser?.uid
//                contentDTO.userId = auth?.currentUser?.email
//                contentDTO.explain = findViewById<EditText>(R.id.)
//                contentDTO.timestamp = System.currentTimeMillis()
//                firestore?.collection("images")?.document()?.set(contentDTO)
//                setResult(Activity.RESULT_OK)
//            }
//
//
//
//        }
    }
}