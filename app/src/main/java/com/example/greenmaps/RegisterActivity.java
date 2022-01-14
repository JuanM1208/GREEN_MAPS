package com.example.greenmaps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsuario, etEmail, etPassword, etConfirmPassword;
    private ProgressBar progressBar;
    private static final String TAG= "Registro";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getSupportActionBar().setTitle("Registro");

        Toast.makeText(RegisterActivity.this, "Ya puedes registrarte", Toast.LENGTH_LONG).show();

        progressBar = findViewById(R.id.progressBar);
        etUsuario = findViewById(R.id.etUsuario);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        Button btnRegistro = findViewById(R.id.btnRegistro);
        btnRegistro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Obtener los datos ingresados
                String usuario = etUsuario.getText().toString();
                String email = etEmail.getText().toString();
                String password = etPassword.getText().toString();
                String confirmPassword = etConfirmPassword.getText().toString();

                if (TextUtils.isEmpty(usuario)){
                    Toast.makeText(RegisterActivity.this, "Por favor ingresa tu nombre completo", Toast.LENGTH_LONG).show();
                    etUsuario.setError("Este campo es obligatorio");
                    etUsuario.requestFocus();
                }
                else if (TextUtils.isEmpty(email)){
                    Toast.makeText(RegisterActivity.this, "Por favor ingresa tu email", Toast.LENGTH_LONG).show();
                    etEmail.setError("Este campo es obligatorio");
                    etEmail.requestFocus();
                }
                else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    Toast.makeText(RegisterActivity.this, "Por favor ingresa un email válido", Toast.LENGTH_LONG).show();
                    etEmail.setError("La dirección de correo es inválida");
                    etEmail.requestFocus();
                }

                else if (TextUtils.isEmpty(password)){
                    Toast.makeText(RegisterActivity.this, "Por favor ingresa tu contraseña", Toast.LENGTH_LONG).show();
                    etPassword.setError("Este campo es obligatorio");
                    etPassword.requestFocus();
                }
                else if (password.length() < 6){
                    Toast.makeText(RegisterActivity.this, "La contraseña debe tener al menos 6 dígitos", Toast.LENGTH_LONG).show();
                    etPassword.setError("Contraseña demasiado corta");
                    etPassword.requestFocus();
                }
                else if (TextUtils.isEmpty(confirmPassword)){
                    Toast.makeText(RegisterActivity.this, "Por favor confirma tu contraseña", Toast.LENGTH_LONG).show();
                    etConfirmPassword.setError("Este campo es obligatorio");
                    etConfirmPassword.requestFocus();
                }
                else if (!password.equals(confirmPassword)){
                    Toast.makeText(RegisterActivity.this, "La contraseña no coincide", Toast.LENGTH_LONG).show();
                    etConfirmPassword.setError("No coinciden ambos campos");
                    etConfirmPassword.requestFocus();
                    //Limpia los campos de contraseña
                    etPassword.clearComposingText();
                    etConfirmPassword.clearComposingText();
                }
                else {
                    progressBar.setVisibility(View.VISIBLE);
                    registrarUsuario(usuario, email, password);
                }
            }
        });
    }

    //Registrar Usuario usando los datos ingresados
    private void registrarUsuario(String usuario, String email, String password) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(RegisterActivity.this,
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            FirebaseUser firebaseUser = auth.getCurrentUser();

                            //Actualizar nombre desplegable del usuario
                            UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(usuario).build();
                            firebaseUser.updateProfile(profileChangeRequest);

                            //Guardar datos de usuario en la Realtime Database de Firebase
                            ReadWriteUserDetails writeUserDetails = new ReadWriteUserDetails(usuario, email);

                            //Extrayendo referencia de Usuario desde la BD para "Regisrered Users"
                            DatabaseReference referenceProfile = FirebaseDatabase.getInstance().getReference("Registered Users");

                            referenceProfile.child(firebaseUser.getUid()).setValue(writeUserDetails).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    if (task.isSuccessful()){
                                        //Enviar email de verificación
                                        firebaseUser.sendEmailVerification();

                                        Toast.makeText(RegisterActivity.this, "Usuario registrado exitosamente. Por favor verifica tu email",
                                                Toast.LENGTH_LONG).show();
                                        progressBar.setVisibility(View.GONE);

                                        /*//Abrir perfil de usuario luego del registro exitoso
                                        Intent intent = new Intent(Registro.this, PerfilUsuario.class);
                                        //Para prevenir que el usuario regrese al registro presionando el botón de regreso
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();   //para cerrar el Activity de registro*/
                                    }
                                    else {
                                        Toast.makeText(RegisterActivity.this, "Registro fallido. Por favor intente de nuevo",
                                                Toast.LENGTH_LONG).show();
                                        progressBar.setVisibility(View.GONE);
                                    }
                                }
                            });

                        } else {
                            try {
                                throw task.getException();
                            }
                            catch (FirebaseAuthWeakPasswordException e){
                                etPassword.setError("Tu contraseña es muy débil, por favor agrega números y caracteres especiales");
                                etPassword.requestFocus();
                            }
                            catch (FirebaseAuthInvalidCredentialsException e){
                                etEmail.setError("Tu correo es inválido o ya está en uso. Reintenta con uno diferente");
                                etEmail.requestFocus();
                            }
                            catch (FirebaseAuthUserCollisionException e){
                                etEmail.setError("Ya hay un usuario registrado con este correo. Ingresa otro");
                                etEmail.requestFocus();
                            }
                            catch (Exception e){
                                Log.e(TAG, e.getMessage());
                                Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_LONG);
                            }
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }
}