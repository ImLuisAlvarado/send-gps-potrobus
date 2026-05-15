package mx.itson.sendgpspotrobus

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import mx.itson.sendgpspotrobus.utils.Constants
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException


class LoginActivity : AppCompatActivity() {

    private lateinit var etCorreo: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var prefs: SharedPreferences

    private val client = OkHttpClient()
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val BASE_URL = Constants.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("potrobus_prefs", MODE_PRIVATE)

        // Si ya hay sesión activa, ir directo al tracking
        if (prefs.getInt("id_unidad", -1) != -1) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)

        etCorreo     = findViewById(R.id.et_correo)
        etPassword   = findViewById(R.id.et_password)
        btnLogin     = findViewById(R.id.btn_login)
        progressBar  = findViewById(R.id.progress_bar)
        tvError      = findViewById(R.id.tv_error)

        btnLogin.setOnClickListener { intentarLogin() }
    }

    private fun intentarLogin() {
        val correo   = etCorreo.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (correo.isEmpty() || password.isEmpty()) {
            tvError.text = "Ingresa correo y contraseña"
            tvError.visibility = View.VISIBLE
            return
        }

        tvError.visibility  = View.GONE
        btnLogin.isEnabled  = false
        progressBar.visibility = View.VISIBLE

        val bodyJson = JSONObject().apply {
            put("correo", correo)
            put("password", password)
        }.toString()

        Log.d("LOGIN_DEBUG", "Enviando: $bodyJson")

        val request = Request.Builder()
            .url("$BASE_URL/api/choferes/login")
            .post(bodyJson.toRequestBody(JSON_MEDIA))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled     = true
                    tvError.text           = "Sin conexión al servidor"
                    tvError.visibility     = View.VISIBLE
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled     = true

                    if (response.isSuccessful && body != null) {
                        try {
                            val json   = JSONObject(body)
                            val chofer = json.getJSONObject("chofer")
                            val idUnidad  = chofer.getInt("id_unidad")
                            val nombre    = "${chofer.getString("nombre")} ${chofer.getString("apellido")}"
                            val numEco    = chofer.optString("numero_economico", "—")

                            // Guardar sesión
                            val accessToken = json.getString("access_token")
                            prefs.edit()
                                .putInt("id_unidad", idUnidad)
                                .putString("nombre_chofer", nombre)
                                .putString("numero_economico", numEco)
                                .putString("jwt_token", accessToken)
                                .apply()

                            goToMain()

                        } catch (e: Exception) {
                            tvError.text       = "Error al procesar respuesta"
                            tvError.visibility = View.VISIBLE
                        }
                    } else {
                        val msg = try {
                            JSONObject(body ?: "").optString("error", "Credenciales inválidas")
                        } catch (e: Exception) {
                            "Credenciales inválidas"
                        }
                        tvError.text       = msg
                        tvError.visibility = View.VISIBLE
                    }
                }
                response.close()
            }
        })
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}