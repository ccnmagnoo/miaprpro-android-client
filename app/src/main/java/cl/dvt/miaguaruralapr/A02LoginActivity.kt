package cl.dvt.miaguaruralapr

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import cl.dvt.miaguaruralapr.A01SplashActivity.Companion.currentApr
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_a02_login.*

class A02LoginActivity : AppCompatActivity() {

    companion object{
        val TAG="Login"
    }
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_a02_login)

        //F.01 Login campos
        login_button_login.setOnClickListener{
            val email = email_editText_login.text.toString()
            val password = password_editText_login.text.toString()
            login_button_login.text = "ingresando..."
            progressBar_login.visibility = View.VISIBLE

            Log.d(TAG, "Attempt login with email/pb: $email/*** ")
            //F.01.01 Verificando textos en campos
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this,"Ingrese datos", Toast.LENGTH_SHORT).show()
                if(email.isEmpty()){
                    email_editText_login?.error = "vacio"
                }
                if(password.isEmpty()){
                    password_editText_login?.error = "vacio"
                }
                login_button_login.text = "Ingresar"
                progressBar_login.visibility = View.INVISIBLE
                return@setOnClickListener
            }
            loginAttempt(email,password)

        }

        //F02. Botón Ir a Registro
        goToRegister_textView_login.setOnClickListener {
            val intent = Intent(this, cl.dvt.miaguaruralapr.A03RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                .or(Intent.FLAG_ACTIVITY_NEW_TASK) /** cuando vuelves se cierra el activity register*/
            startActivity(intent)
        }

        //F03. Botón Recuperar contraseña
        recoverPassword_textView_login.setOnClickListener{
            if (email_editText_login.text.isEmpty()){
                email_editText_login.error = "vacio"
            }else if(!email_editText_login.text.toString().isEmailValid()){
                email_editText_login.error = "inválido"
            }else{
                val auth = FirebaseAuth.getInstance()
                val emailAddress = email_editText_login.text.toString()
                auth.sendPasswordResetEmail(emailAddress)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "mensaje enviado a $emailAddress")
                            recoverPassword_textView_login.text = "recupere su clave en $emailAddress"
                            Toast.makeText(baseContext, "enviamos un mensaje a su correo $emailAddress", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private  fun loginAttempt(email:String, password:String){

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if(!it.isSuccessful) return@addOnCompleteListener
                val user = FirebaseAuth.getInstance().currentUser

                if(user!!.isEmailVerified){
                     getUser(it.result?.user!!.uid) /* cargar objeto userApr*/
                     Log.d(TAG,"login correctamente: ${it.result?.user?.uid}")

                 } else{
                     login_button_login.text = "verifique en correo primero"
                     progressBar_login.visibility = View.INVISIBLE
                     Log.d(TAG,"revise su email: ${user.email}")
                 }
            }
            .addOnFailureListener{
                Toast.makeText(baseContext, "identificación errada", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "login falló: ${it.message}")
            }
    }

    private fun getUser(uidApr:String){
        //obteniendo documento USER
        val ref = FirebaseFirestore.getInstance()
            .collection("userApr")
            .document(uidApr)
        ref.get()
            .addOnSuccessListener {documentSnapshot  ->
                currentApr = documentSnapshot.toObject(AprUser::class.java)
                Log.d("CurrentUser", "User Data: $currentApr")
                //Iniciar Main screen
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            .addOnFailureListener{
                Log.d("CurrentUser", "User Data: error listener",it)
            }
    }

    private fun String.isEmailValid(): Boolean {
        return !TextUtils.isEmpty(this) && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
    }

}
