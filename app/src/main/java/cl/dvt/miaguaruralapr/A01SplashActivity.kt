package cl.dvt.miaguaruralapr

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class A01SplashActivity : AppCompatActivity() {
    companion object{
        var currentApr: AprObject? = null
    }

    private val SPLASH_TIME_OUT:Long=3000 /** en milisegundos*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_a01_splash)

        Handler().postDelayed({
            verifyUserIsLoggedIn()
        }, SPLASH_TIME_OUT)

    }

    private fun verifyUserIsLoggedIn(){
        //verificando uid existente
        val uid = FirebaseAuth.getInstance().uid
        Log.d("CurrentUser", "UID : $uid")
        if (uid==null){
            //si no existe uid
            val intent = Intent(this, A02LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
        else{
            //si existe uid
            getUser(uid)
        }
    }

    private fun getUser(uidApr:String){
        val ref = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(uidApr)
        ref.get()
            .addOnSuccessListener {document  ->
                currentApr = document.toObject(AprObject::class.java)
                Log.d("CurrentUser", "User Data: $currentApr")
                startActivity(Intent(this,MainActivity::class.java)) /* cargar MainActivity */
                finish()                                                            /* close this activity*/

            }
            .addOnFailureListener{
                Log.d("CurrentUser", "User Data: error listener",it)
            }
    }

}
