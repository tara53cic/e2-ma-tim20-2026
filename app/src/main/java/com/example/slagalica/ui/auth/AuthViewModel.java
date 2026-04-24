package com.example.slagalica.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.AuthRepository;
import com.example.slagalica.domain.models.User;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthViewModel extends ViewModel {

    private final AuthRepository repository;
    private final FirebaseFirestore db;

    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> registerSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> passwordResetSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public AuthViewModel() {
        this.repository = new AuthRepository();
        this.db = FirebaseFirestore.getInstance();
    }

    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getRegisterSuccess() { return registerSuccess; }
    public LiveData<Boolean> getLoginSuccess() { return loginSuccess; }
    public LiveData<Boolean> getLogoutSuccess() { return logoutSuccess; }
    public LiveData<Boolean> getPasswordResetSuccess() { return passwordResetSuccess; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void register(String email, String username, String region, String password, String repeatPassword) {
        if (email.isEmpty() || username.isEmpty() || region.isEmpty() || password.isEmpty()) {
            errorMessage.setValue("Sva polja moraju biti popunjena.");
            return;
        }
        if (!password.equals(repeatPassword)) {
            errorMessage.setValue("Lozinke se ne poklapaju.");
            return;
        }

        isLoading.setValue(true);
        repository.registerUser(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser fUser = authResult.getUser();
                if (fUser != null) {
                    User newUser = new User(email, username, region);
                    repository.saveUserToFirestore(fUser.getUid(), newUser)
                        .addOnSuccessListener(aVoid -> {
                            repository.sendEmailVerification();
                            repository.logout();
                            isLoading.setValue(false);
                            registerSuccess.setValue(true);
                        })
                        .addOnFailureListener(e -> {
                            isLoading.setValue(false);
                            errorMessage.setValue("Greška pri čuvanju podataka: " + e.getMessage());
                        });
                }
            })
            .addOnFailureListener(e -> {
                isLoading.setValue(false);
                errorMessage.setValue("Greška pri registraciji: " + e.getMessage());
            });
    }

    public void login(String emailOrUsername, String password) {
        if (emailOrUsername.isEmpty() || password.isEmpty()) {
            errorMessage.setValue("Unesite email/korisničko ime i lozinku.");
            return;
        }

        isLoading.setValue(true);
        if (emailOrUsername.contains("@")) {
            performFirebaseLogin(emailOrUsername, password);
        } else {
            //Ako korisnik unosi korisničko ime, tražimo mail
            db.collection("users")
                .whereEqualTo("username", emailOrUsername)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String email = queryDocumentSnapshots.getDocuments().get(0).getString("email");
                        if (email != null) {
                            performFirebaseLogin(email, password);
                        } else {
                            isLoading.setValue(false);
                            errorMessage.setValue("Korisnik nije pronađen.");
                        }
                    } else {
                        isLoading.setValue(false);
                        errorMessage.setValue("Korisnik sa ovim korisničkim imenom ne postoji.");
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("Greška pri pretrazi korisnika.");
                });
        }
    }

    private void performFirebaseLogin(String email, String password) {
        repository.loginUser(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = repository.getCurrentUser();
                if (user != null && user.isEmailVerified()) {
                    isLoading.setValue(false);
                    loginSuccess.setValue(true);
                } else {
                    repository.logout();
                    isLoading.setValue(false);
                    errorMessage.setValue("Morate verifikovati email pre prijavljivanja.");
                }
            })
            .addOnFailureListener(e -> {
                isLoading.setValue(false);
                errorMessage.setValue("Pogrešni kredencijali ili greška pri prijavi.");
            });
    }

    public void logout() {
        repository.logout();
        logoutSuccess.setValue(true);
    }

    public void updatePassword(String oldPassword, String newPassword, String repeatNewPassword) {
        if (oldPassword.isEmpty() || newPassword.isEmpty() || repeatNewPassword.isEmpty()) {
            errorMessage.setValue("Sva polja moraju biti popunjena.");
            return;
        }
        if (!newPassword.equals(repeatNewPassword)) {
            errorMessage.setValue("Nove lozinke se ne poklapaju.");
            return;
        }

        FirebaseUser user = repository.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            errorMessage.setValue("Korisnik nije ulogovan.");
            return;
        }

        isLoading.setValue(true);
        com.google.firebase.auth.AuthCredential credential =
            com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(), oldPassword);

        user.reauthenticate(credential)
            .addOnSuccessListener(aVoid -> {
                user.updatePassword(newPassword)
                    .addOnSuccessListener(aVoid1 -> {
                        isLoading.setValue(false);
                        passwordResetSuccess.setValue(true);
                    })
                    .addOnFailureListener(e -> {
                        isLoading.setValue(false);
                        errorMessage.setValue("Greška pri promeni lozinke: " + e.getMessage());
                    });
            })
            .addOnFailureListener(e -> {
                isLoading.setValue(false);
                errorMessage.setValue("Neispravna stara lozinka.");
            });
    }
}
