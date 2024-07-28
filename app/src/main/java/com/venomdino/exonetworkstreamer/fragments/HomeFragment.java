package com.venomdino.exonetworkstreamer.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;

import com.google.android.material.textfield.TextInputEditText;
import com.venomdino.exonetworkstreamer.BuildConfig;
import com.venomdino.exonetworkstreamer.R;
import com.venomdino.exonetworkstreamer.activities.PlayerActivity;
import com.venomdino.exonetworkstreamer.adapters.CustomSpinnerAdapter;
import com.venomdino.exonetworkstreamer.databinding.FragmentHomeBinding;
import com.venomdino.exonetworkstreamer.helpers.CustomMethods;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@UnstableApi
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private Activity activity;
    private String userAgent;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        activity = getActivity();

        if (activity != null){

            String[] userAgentBrowserNames = getResources().getStringArray(R.array.agent_browsers_names);
            String userAgentPlaceholder = "User-agent (Default)";

            CustomSpinnerAdapter userAgentAdapter = new CustomSpinnerAdapter(activity, userAgentBrowserNames, userAgentPlaceholder);

            binding.userAgentSpinner.setAdapter(userAgentAdapter);

            binding.userAgentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {

                    userAgentAdapter.setShowPlaceholder(false);
                    // Handle the selected item

                    if (position == 1) {
                        userAgent = getString(R.string.chrome_android_agent);
                    } else if (position == 2) {
                        userAgent = getString(R.string.firefox_android_agent);
                    } else if (position == 3) {
                        userAgent = getString(R.string.chrome_windows_agent);
                    } else if (position == 4) {
                        userAgent = getString(R.string.firefox_windows_agent);
                    } else if (position == 5) {

                        float density = getResources().getDisplayMetrics().density;
                        int marginHorizontal = (int) (20 * density);

                        FrameLayout container = new FrameLayout(activity);
                        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        params.leftMargin = marginHorizontal;
                        params.rightMargin = marginHorizontal;

                        final EditText editText = new EditText(activity);
                        editText.setHint("Enter custom user-agent");
                        editText.setLayoutParams(params);
                        container.addView(editText);


                        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                        alert.setCancelable(false);
                        alert.setTitle("Custom User-Agent");
                        alert.setView(container);

                        alert.setPositiveButton("OK", (dialog, whichButton) -> {

                            String customUserAgent = editText.getText().toString();

                            if (customUserAgent.equals("")) {
                                userAgent = getString(R.string.app_name) + "/" + BuildConfig.VERSION_NAME + " (Linux; Android " + Build.VERSION.RELEASE + ")";
                                binding.userAgentSpinner.setSelection(0);
                            } else {
                                userAgent = customUserAgent;
                            }

                            dialog.dismiss();
                        });

                        alert.show();

                    } else {
                        userAgent = getString(R.string.app_name) + "/" + BuildConfig.VERSION_NAME + " (Linux; Android " + Build.VERSION.RELEASE + ")";
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
//        ------------------------------------------------------------------------------------------

            String[] drmSchemes = getResources().getStringArray(R.array.drm_schemes);

            String drmSchemePlaceholder = "DrmScheme (Widevine)";

            CustomSpinnerAdapter drmSchemeAdapter = new CustomSpinnerAdapter(activity, drmSchemes, drmSchemePlaceholder);

            binding.drmSchemeSelector.setAdapter(drmSchemeAdapter);

            binding.drmSchemeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    drmSchemeAdapter.setShowPlaceholder(false);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

//        ------------------------------------------------------------------------------------------

            binding.playBtn.setOnClickListener(view -> {

                boolean shouldStartPlaying = true;

                String mediaStreamUrl = Objects.requireNonNull(binding.mediaStreamUrlEt.getText()).toString().trim();
                String drmLicenceUrl = Objects.requireNonNull(binding.drmLicenceUrlEt.getText()).toString().trim();
                String refererValue = Objects.requireNonNull(binding.refererEt.getText()).toString();
                String originValue = Objects.requireNonNull(binding.originEt.getText()).toString();


                int selectedDrmScheme = binding.drmSchemeSelector.getSelectedItemPosition();

                if (mediaStreamUrl.equalsIgnoreCase("")) {
                    binding.mediaStreamUrlTil.setErrorEnabled(true);
                    binding.mediaStreamUrlTil.setError("Media stream link required.");
                    shouldStartPlaying = false;
                } else if (!CustomMethods.isValidURL(mediaStreamUrl)) {
                    binding.mediaStreamUrlTil.setErrorEnabled(true);
                    binding.mediaStreamUrlTil.setError("Invalid Link.");
                    shouldStartPlaying = false;
                }

                if (!drmLicenceUrl.equalsIgnoreCase("")) {

                    if (!CustomMethods.isValidURL(drmLicenceUrl)) {
                        binding.drmLicenceUrlTil.setErrorEnabled(true);
                        binding.drmLicenceUrlTil.setError("Invalid Link.");
                        shouldStartPlaying = false;
                    }
                }

                if (shouldStartPlaying) {

                    binding.mediaStreamUrlTil.setErrorEnabled(false);
                    binding.drmLicenceUrlTil.setErrorEnabled(false);

                    Intent intent = new Intent(activity, PlayerActivity.class);
                    intent.putExtra("mediaFileUrlOrPath", mediaStreamUrl);
                    intent.putExtra("drmLicenceUrl", drmLicenceUrl);
                    intent.putExtra("refererValue", refererValue);
                    intent.putExtra("originValue", originValue);
                    intent.putExtra("userAgent", userAgent);
                    intent.putExtra("selectedDrmScheme", selectedDrmScheme);
                    startActivity(intent);
                }
            });

//        ------------------------------------------------------------------------------------------

            binding.formContainer.setOnClickListener(view -> {

                List<TextInputEditText> textInputEditTextList = findAllTextInputEditText();

                for (TextInputEditText editText : textInputEditTextList) {
                    editText.clearFocus();
                    CustomMethods.hideSoftKeyboard(activity, editText);
                }
            });
        }
    }

    //    ==============================================================================================

    private List<TextInputEditText> findAllTextInputEditText() {

        List<TextInputEditText> editTextList = new ArrayList<>();

        editTextList.add(binding.mediaStreamUrlEt);
        editTextList.add(binding.drmLicenceUrlEt);
        editTextList.add(binding.refererEt);

        return editTextList;
    }
}