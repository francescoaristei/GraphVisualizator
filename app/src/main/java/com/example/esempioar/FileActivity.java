package com.example.esempioar;


/**
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.EditText;
import java.util.Collections;

    /**
     * The main {@link Activity} for the Drive API migration sample app.
     */
    public class FileActivity extends AppCompatActivity {
        private static final String TAG = "MainActivity";

        private static final int REQUEST_CODE_SIGN_IN = 1;
        private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;
        private static final int REQUEST_CODE_SIGN_OUT = 3;
        private GoogleSignInClient client;

        private DriveServiceHelper mDriveServiceHelper;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.actvity_tesi);
            // Set the onClick listeners for the button bar.
            findViewById(R.id.download).setOnClickListener(view -> openFilePicker());
            findViewById(R.id.logout).setOnClickListener(view -> requestSignOut());

            // Authenticate the user. For most apps, this should be done when the user performs an
            // action that requires Drive access rather than in onCreate.
            requestSignIn();
        }

        private void requestSignOut() {
            client.signOut()
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                        }
                    });
            client = null;
            Intent intent = new Intent(this,MainActivity.class);
            startActivityForResult(intent,REQUEST_CODE_SIGN_OUT);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
            switch (requestCode) {
                case REQUEST_CODE_SIGN_IN:
                    if (resultCode == Activity.RESULT_OK && resultData != null) {
                        handleSignInResult(resultData);
                    }
                    break;

                case REQUEST_CODE_OPEN_DOCUMENT:
                    if (resultCode == Activity.RESULT_OK && resultData != null) {
                        Uri uri = resultData.getData();
                        if (uri != null) {
                            openFileFromFilePicker(uri);
                        }
                    }
                    break;
                case REQUEST_CODE_SIGN_OUT:
                    if (resultCode == Activity.RESULT_OK && resultData != null) {
                        startActivity(resultData);
                    }

            }
            super.onActivityResult(requestCode, resultCode, resultData);
        }

        /**
         * Starts a sign-in activity using {@link #REQUEST_CODE_SIGN_IN}.
         */
        private void requestSignIn() {
            Log.d(TAG, "Requesting sign-in");

            GoogleSignInOptions signInOptions =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestScopes(new Scope(DriveScopes.DRIVE))
                            .build();


            //GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);
            client = GoogleSignIn.getClient(this, signInOptions);

            // The result of the sign-in Intent is handled in onActivityResult.
            startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        }

        /**
         * Handles the {@code result} of a completed sign-in activity initiated from {@link
         * #requestSignIn()}.
         */
        private void handleSignInResult(Intent result) {
            GoogleSignIn.getSignedInAccountFromIntent(result)
                    .addOnSuccessListener(googleAccount -> {
                        Log.d(TAG, "Signed in as " + googleAccount.getEmail());

                        // Use the authenticated account to sign in to the Drive service.
                        GoogleAccountCredential credential =
                                GoogleAccountCredential.usingOAuth2(
                                        this, Collections.singleton(DriveScopes.DRIVE));
                        credential.setSelectedAccount(googleAccount.getAccount());
                        Drive googleDriveService =
                                new Drive.Builder(
                                        AndroidHttp.newCompatibleTransport(),
                                        new GsonFactory(),
                                        credential)
                                        .setApplicationName("Drive API Migration")
                                        .build();

                        // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                        // Its instantiation is required before handling any onClick actions.
                        mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                    })
                    .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
        }

        /**
         * Opens the Storage Access Framework file picker using {@link #REQUEST_CODE_OPEN_DOCUMENT}.
         */
        private void openFilePicker() {
            if (mDriveServiceHelper != null) {
                Log.d(TAG, "Opening file picker.");

                Intent pickerIntent = mDriveServiceHelper.createFilePickerIntent();

                // The result of the SAF Intent is handled in onActivityResult.
                startActivityForResult(pickerIntent, REQUEST_CODE_OPEN_DOCUMENT);
            }
        }

        /**
         * Opens a file from its {@code uri} returned from the Storage Access Framework file picker
         * initiated by {@link #openFilePicker()}.
         */
        private void openFileFromFilePicker(Uri uri) {
            if (mDriveServiceHelper != null) {
                Log.d(TAG, "Opening " + uri.getPath());

                mDriveServiceHelper.openFileUsingStorageAccessFramework(getContentResolver(), uri, this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS))
                        .addOnSuccessListener(file -> {
                            Intent intent = new Intent(this, CameraActivity.class);
                            String name = file.getName();
                            intent.putExtra("name",name);
                            startActivity(intent);
                        })
                        .addOnFailureListener(exception ->
                                Log.e(TAG, "Unable to open file from picker.", exception));
            }
        }
    }
