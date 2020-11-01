package cl.dvt.miaguaruralapr

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import cl.dvt.miaguaruralapr.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {
    companion object{
        var user: User? = null
    }

    private val splashTimeOut:Long=3000 /** en milisegundos*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed({
            verifyUserIsLoggedIn()
        }, splashTimeOut)

    }

    private fun verifyUserIsLoggedIn(){
        //verificando uid existente
        val uid = FirebaseAuth.getInstance().uid
        Log.d("CurrentUser", "UID : $uid")
        if (uid==null){
            //si no existe uid
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
        else{
            //si existe uid
            fetchUser(uid)
        }
    }

    private fun fetchUser(uidApr:String){
        val ref = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(uidApr)
        ref.get()
            .addOnSuccessListener {document  ->
                user = document.toObject(User::class.java)
                Log.d("User", "User Data: $user")
                startActivity(Intent(this,MainActivity::class.java)) /* cargar MainActivity */
                finish()                                                            /* close this activity*/

            }
            .addOnFailureListener{
                Log.d("User", "User Data: error listener",it)
            }
    }

}
